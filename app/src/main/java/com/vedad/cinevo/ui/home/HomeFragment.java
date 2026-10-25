package com.vedad.cinevo.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.vedad.cinevo.BuildConfig;
import com.vedad.cinevo.data.remote.RetrofitClient;
import com.vedad.cinevo.databinding.FragmentHomeBinding;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.model.MovieResponse;
import com.vedad.cinevo.ui.details.MovieDetailsActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private MovieAdapter adapter;
    private Call<MovieResponse> currentCall;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new MovieAdapter();
        binding.recyclerMovies.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerMovies.setAdapter(adapter);

        adapter.setOnMovieClickListener(movie -> {
            Intent intent = new Intent(requireContext(), MovieDetailsActivity.class);
            intent.putExtra(MovieDetailsActivity.EXTRA_MOVIE_ID, movie.getId());
            startActivity(intent);
        });

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String q = query.trim();
                if (!q.isEmpty()) {
                    searchMovies(q);
                    binding.searchView.clearFocus();
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    loadPopularMovies();
                }
                return true;
            }
        });

        loadPopularMovies();
    }

    private void loadPopularMovies() {
        execute(RetrofitClient.getApi()
                .getPopularMovies(BuildConfig.TMDB_API_KEY, "en-US", 1), null);
    }

    private void searchMovies(String query) {
        execute(RetrofitClient.getApi()
                        .searchMovies(BuildConfig.TMDB_API_KEY, "en-US", query, 1),
                "Nema rezultata za: " + query);
    }

    private void execute(Call<MovieResponse> call, String emptyMessage) {
        if (currentCall != null) currentCall.cancel();
        currentCall = call;

        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                if (call.isCanceled() || !isAdded() || binding == null) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Movie> results = response.body().getResults();
                    adapter.setMovies(results);
                    binding.recyclerMovies.scrollToPosition(0);

                    if (results.isEmpty() && emptyMessage != null) {
                        Toast.makeText(requireContext(), emptyMessage, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "Greška: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                if (call.isCanceled() || !isAdded() || binding == null) return;
                Toast.makeText(requireContext(),
                        "Nema veze s internetom", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentCall != null) currentCall.cancel();
        binding = null;
    }
}