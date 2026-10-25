package com.vedad.cinevo.ui.recommendations;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.vedad.cinevo.databinding.FragmentRecommendationsBinding;
import com.vedad.cinevo.ui.details.MovieDetailsActivity;
import com.vedad.cinevo.ui.home.MovieAdapter;

public class RecommendationsFragment extends Fragment {

    private FragmentRecommendationsBinding binding;
    private RecommendationsViewModel viewModel;
    private MovieAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentRecommendationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(RecommendationsViewModel.class);

        adapter = new MovieAdapter();
        binding.recyclerRecommendations.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.recyclerRecommendations.setAdapter(adapter);

        adapter.setOnMovieClickListener(movie -> {
            Intent intent = new Intent(requireContext(), MovieDetailsActivity.class);
            intent.putExtra(MovieDetailsActivity.EXTRA_MOVIE_ID, movie.getId());
            startActivity(intent);
        });

        binding.btnRefresh.setOnClickListener(v -> viewModel.load());

        viewModel.getRecommendations().observe(getViewLifecycleOwner(), movies -> {
            adapter.setMovies(movies);
            binding.tvMessage.setVisibility(movies.isEmpty() ? View.VISIBLE : View.GONE);
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progress.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getError().observe(getViewLifecycleOwner(), message -> {
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        viewModel.load();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}