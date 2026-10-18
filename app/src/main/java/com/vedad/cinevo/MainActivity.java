package com.vedad.cinevo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.vedad.cinevo.data.remote.RetrofitClient;
import com.vedad.cinevo.databinding.ActivityMainBinding;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.model.MovieResponse;
import com.vedad.cinevo.ui.details.MovieDetailsActivity;
import com.vedad.cinevo.ui.home.MovieAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MovieAdapter adapter;
    private Call<MovieResponse> currentCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        adapter = new MovieAdapter();
        binding.recyclerMovies.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerMovies.setAdapter(adapter);

        adapter.setOnMovieClickListener(movie -> {
            Intent intent = new Intent(this, MovieDetailsActivity.class);
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
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Movie> results = response.body().getResults();
                    adapter.setMovies(results);
                    binding.recyclerMovies.scrollToPosition(0);

                    if (results.isEmpty() && emptyMessage != null) {
                        Toast.makeText(MainActivity.this, emptyMessage, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this,
                            "Greška: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                Toast.makeText(MainActivity.this,
                        "Nema veze s internetom", Toast.LENGTH_SHORT).show();
            }
        });
    }
}