package com.vedad.cinevo.ui.details;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.vedad.cinevo.BuildConfig;
import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.data.remote.RetrofitClient;
import com.vedad.cinevo.databinding.ActivityMovieDetailsBinding;
import com.vedad.cinevo.model.MovieDetails;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_MOVIE_ID = "movie_id";
    private static final String BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w780";

    private ActivityMovieDetailsBinding binding;
    private MovieDetailsViewModel viewModel;
    private MovieDetails currentDetails;
    private boolean isFavorite = false;
    private boolean inWatchlist = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMovieDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, bars.bottom);
            return insets;
        });

        int movieId = getIntent().getIntExtra(EXTRA_MOVIE_ID, -1);
        if (movieId == -1) {
            finish();
            return;
        }
        viewModel = new ViewModelProvider(this).get(MovieDetailsViewModel.class);

        viewModel.countOf(movieId, SavedMovie.FAVORITE).observe(this, count -> {
            isFavorite = count != null && count > 0;
            binding.btnFavorite.setText(isFavorite ? "♥ U favoritima" : "♡ Favorit");
        });

        viewModel.countOf(movieId, SavedMovie.WATCHLIST).observe(this, count -> {
            inWatchlist = count != null && count > 0;
            binding.btnWatchlist.setText(inWatchlist ? "✓ Na listi" : "+ Za gledanje");
        });

        binding.btnFavorite.setOnClickListener(v -> toggle(SavedMovie.FAVORITE, isFavorite));
        binding.btnWatchlist.setOnClickListener(v -> toggle(SavedMovie.WATCHLIST, inWatchlist));

        loadDetails(movieId);
    }

    private void loadDetails(int movieId) {
        RetrofitClient.getApi()
                .getMovieDetails(movieId, BuildConfig.TMDB_API_KEY, "en-US", "videos,credits")
                .enqueue(new Callback<MovieDetails>() {
                    @Override
                    public void onResponse(Call<MovieDetails> call, Response<MovieDetails> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            showDetails(response.body());
                        } else {
                            Toast.makeText(MovieDetailsActivity.this,
                                    "Greška: " + response.code(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieDetails> call, Throwable t) {
                        Toast.makeText(MovieDetailsActivity.this,
                                "Nema veze s internetom", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showDetails(MovieDetails d) {
        currentDetails = d;
        binding.tvTitle.setText(d.getTitle());

        if (!TextUtils.isEmpty(d.getTagline())) {
            binding.tvTagline.setText(d.getTagline());
            binding.tvTagline.setVisibility(android.view.View.VISIBLE);
        }

        List<String> info = new ArrayList<>();
        info.add(String.format(Locale.US, "★ %.1f", d.getVoteAverage()));
        if (d.getReleaseDate() != null && d.getReleaseDate().length() >= 4) {
            info.add(d.getReleaseDate().substring(0, 4));
        }
        if (d.getRuntime() > 0) {
            info.add(d.getRuntime() + " min");
        }
        binding.tvInfo.setText(TextUtils.join("  •  ", info));

        if (d.getGenres() != null) {
            List<String> genres = new ArrayList<>();
            for (MovieDetails.Genre g : d.getGenres()) {
                genres.add(g.getName());
            }
            binding.tvGenres.setText(TextUtils.join(", ", genres));
        }

        binding.tvOverview.setText(TextUtils.isEmpty(d.getOverview())
                ? "Opis nije dostupan." : d.getOverview());

        if (d.getCredits() != null && d.getCredits().getCast() != null) {
            List<String> cast = new ArrayList<>();
            for (MovieDetails.Cast c : d.getCredits().getCast()) {
                if (cast.size() == 8) break;
                cast.add(c.getName());
            }
            binding.tvCast.setText(TextUtils.join(", ", cast));
        }

        String imagePath = d.getBackdropPath() != null ? d.getBackdropPath() : d.getPosterPath();
        if (imagePath != null) {
            Glide.with(this).load(BACKDROP_BASE_URL + imagePath).into(binding.imgBackdrop);
        }

        String trailerKey = findTrailerKey(d);
        if (trailerKey != null) {
            binding.btnTrailer.setVisibility(android.view.View.VISIBLE);
            binding.btnTrailer.setOnClickListener(v -> startActivity(new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com/watch?v=" + trailerKey))));
        }
    }

    private void toggle(String listType, boolean alreadySaved) {
        if (currentDetails == null) return; // detalji se još učitavaju

        if (alreadySaved) {
            viewModel.remove(currentDetails.getId(), listType);
        } else {
            SavedMovie movie = new SavedMovie();
            movie.id = currentDetails.getId();
            movie.listType = listType;
            movie.title = currentDetails.getTitle();
            movie.posterPath = currentDetails.getPosterPath();
            movie.voteAverage = currentDetails.getVoteAverage();
            movie.releaseDate = currentDetails.getReleaseDate();
            movie.overview = currentDetails.getOverview();
            movie.addedAt = System.currentTimeMillis();
            viewModel.add(movie);
        }
    }

    private String findTrailerKey(MovieDetails d) {
        if (d.getVideos() == null || d.getVideos().getResults() == null) return null;
        for (MovieDetails.Video v : d.getVideos().getResults()) {
            if ("YouTube".equals(v.getSite()) && "Trailer".equals(v.getType())) {
                return v.getKey();
            }
        }
        return null;
    }
}