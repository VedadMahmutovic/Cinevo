package com.vedad.cinevo;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.vedad.cinevo.data.remote.RetrofitClient;
import com.vedad.cinevo.databinding.ActivityMainBinding;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.model.MovieResponse;
import com.vedad.cinevo.ui.home.MovieAdapter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private MovieAdapter adapter;

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

        loadPopularMovies();
    }

    private void loadPopularMovies() {
        RetrofitClient.getApi()
                .getPopularMovies(BuildConfig.TMDB_API_KEY, "en-US", 1)
                .enqueue(new Callback<MovieResponse>() {
                    @Override
                    public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            adapter.setMovies(response.body().getResults());
                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Greška: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieResponse> call, Throwable t) {
                        Toast.makeText(MainActivity.this,
                                "Nema veze s internetom", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}