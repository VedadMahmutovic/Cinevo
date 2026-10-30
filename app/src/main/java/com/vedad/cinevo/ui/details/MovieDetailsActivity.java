package com.vedad.cinevo.ui.details;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.vedad.cinevo.BuildConfig;
import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.data.remote.RetrofitClient;
import com.vedad.cinevo.databinding.ActivityMovieDetailsBinding;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.model.MovieDetails;
import com.vedad.cinevo.ui.home.MovieAdapter;
import com.vedad.cinevo.util.AppSettings;
import com.vedad.cinevo.util.GenreUtils;

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
    private MovieAdapter similarAdapter;
    private WebView trailerWebView;

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

        similarAdapter = new MovieAdapter();
        similarAdapter.setFixedItemWidth((int) (130 * getResources().getDisplayMetrics().density));
        binding.recyclerSimilar.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerSimilar.setAdapter(similarAdapter);
        similarAdapter.setOnMovieClickListener(movie -> {
            Intent intent = new Intent(this, MovieDetailsActivity.class);
            intent.putExtra(EXTRA_MOVIE_ID, movie.getId());
            startActivity(intent);
        });

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

        viewModel.ratingOf(movieId).observe(this, rating -> {
            int value = rating == null ? 0 : rating;
            binding.ratingBar.setRating(value);
            binding.tvRatingHint.setText(value == 0 ? "Ocijeni film" : "Tvoja ocjena: " + value + "/5");
            binding.btnRemoveRating.setVisibility(
                    value == 0 ? android.view.View.GONE : android.view.View.VISIBLE);
        });

        binding.ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (!fromUser || currentDetails == null) return;
            int value = Math.round(rating);
            if (value >= 1) saveRating(value);
        });

        binding.btnRemoveRating.setOnClickListener(v -> {
            if (currentDetails != null) {
                viewModel.remove(currentDetails.getId(), SavedMovie.WATCHED);
            }
        });

        loadDetails(movieId);
    }

    private void loadDetails(int movieId) {
        RetrofitClient.getApi()
                .getMovieDetails(movieId, BuildConfig.TMDB_API_KEY, "en-US", "videos,credits,recommendations,watch/providers")
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
        binding.sectionRating.setVisibility(android.view.View.VISIBLE);
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
            binding.btnTrailer.setOnClickListener(v -> toggleTrailer(trailerKey));
            binding.btnTrailerExternal.setOnClickListener(v -> openTrailerExternally(trailerKey));
        }

        if (d.getRecommendations() != null && d.getRecommendations().getResults() != null) {
            List<Movie> similar = new ArrayList<>();
            for (Movie m : d.getRecommendations().getResults()) {
                if (m.getPosterPath() != null) similar.add(m);
            }
            if (!similar.isEmpty()) {
                similarAdapter.setMovies(similar);
                binding.sectionSimilar.setVisibility(android.view.View.VISIBLE);
            }
        }
        showWatchProviders(d);
    }

    private SavedMovie buildSavedMovie(String listType) {
        SavedMovie movie = new SavedMovie();
        movie.id = currentDetails.getId();
        movie.listType = listType;
        movie.title = currentDetails.getTitle();
        movie.posterPath = currentDetails.getPosterPath();
        movie.voteAverage = currentDetails.getVoteAverage();
        movie.releaseDate = currentDetails.getReleaseDate();
        movie.overview = currentDetails.getOverview();
        movie.genreIds = GenreUtils.fromGenres(currentDetails.getGenres());
        movie.addedAt = System.currentTimeMillis();
        return movie;
    }

    private void toggle(String listType, boolean alreadySaved) {
        if (currentDetails == null) return;

        if (alreadySaved) {
            viewModel.remove(currentDetails.getId(), listType);
        } else {
            viewModel.add(buildSavedMovie(listType));
        }
    }

    private void saveRating(int value) {
        SavedMovie movie = buildSavedMovie(SavedMovie.WATCHED);
        movie.userRating = value;
        viewModel.add(movie);
        viewModel.remove(currentDetails.getId(), SavedMovie.WATCHLIST);
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

    private void showWatchProviders(MovieDetails d) {
        String region = new AppSettings(this).getWatchRegion();
        binding.sectionWatch.setVisibility(android.view.View.VISIBLE);
        binding.tvWatchRegion.setText("Država: " + region);

        MovieDetails.RegionProviders providers = null;
        if (d.getWatchProviders() != null && d.getWatchProviders().getResults() != null) {
            providers = d.getWatchProviders().getResults().get(region);
        }

        boolean any = bindProviders(binding.tvWatchStream, "Streaming",
                providers != null ? providers.getFlatrate() : null);
        any |= bindProviders(binding.tvWatchRent, "Iznajmljivanje",
                providers != null ? providers.getRent() : null);
        any |= bindProviders(binding.tvWatchBuy, "Kupovina",
                providers != null ? providers.getBuy() : null);

        binding.tvWatchEmpty.setVisibility(any
                ? android.view.View.GONE : android.view.View.VISIBLE);

        if (providers != null && providers.getLink() != null) {
            String link = providers.getLink();
            binding.btnWatchLink.setVisibility(android.view.View.VISIBLE);
            binding.btnWatchLink.setOnClickListener(v ->
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link))));
        } else {
            binding.btnWatchLink.setVisibility(android.view.View.GONE);
        }
    }

    private boolean bindProviders(TextView view, String label,
                                  List<MovieDetails.Provider> providers) {
        if (providers == null || providers.isEmpty()) {
            view.setVisibility(android.view.View.GONE);
            return false;
        }
        List<String> names = new ArrayList<>();
        for (MovieDetails.Provider p : providers) {
            names.add(p.getName());
        }
        view.setText(label + ": " + TextUtils.join(", ", names));
        view.setVisibility(android.view.View.VISIBLE);
        return true;
    }

    private void toggleTrailer(String videoKey) {
        if (trailerWebView == null) {
            showTrailer(videoKey);
        } else {
            hideTrailer();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void showTrailer(String videoKey) {
        if (!videoKey.matches("[A-Za-z0-9_-]+")) {
            openTrailerExternally(videoKey);
            return;
        }

        trailerWebView = new WebView(this);
        trailerWebView.setBackgroundColor(Color.BLACK);

        WebSettings settings = trailerWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        trailerWebView.setWebChromeClient(new WebChromeClient());

        int initialHeight = (int) (getResources().getDisplayMetrics().widthPixels * 9f / 16f);
        binding.trailerContainer.addView(trailerWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, initialHeight));
        binding.trailerContainer.setVisibility(android.view.View.VISIBLE);
        binding.trailerContainer.post(() -> {
            if (trailerWebView == null) return;
            ViewGroup.LayoutParams params = trailerWebView.getLayoutParams();
            params.height = binding.trailerContainer.getWidth() * 9 / 16;
            trailerWebView.setLayoutParams(params);
        });

        binding.btnTrailer.setText("Sakrij trailer");
        binding.btnTrailerExternal.setVisibility(android.view.View.VISIBLE);

        String html = "<!DOCTYPE html><html><head>"
                + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                + "<style>html,body{margin:0;height:100%;background:#000}"
                + "iframe{border:0;width:100%;height:100%}</style>"
                + "</head><body>"
                + "<iframe src='https://www.youtube.com/embed/" + videoKey + "?playsinline=1&rel=0' "
                + "allow='autoplay; encrypted-media; picture-in-picture' allowfullscreen "
                + "referrerpolicy='strict-origin-when-cross-origin'></iframe>"
                + "</body></html>";

        trailerWebView.loadDataWithBaseURL("https://" + getPackageName(), html,
                "text/html", "utf-8", null);
    }

    private void hideTrailer() {
        if (trailerWebView == null) return;
        binding.trailerContainer.removeAllViews();
        trailerWebView.loadUrl("about:blank");
        trailerWebView.destroy();
        trailerWebView = null;

        binding.trailerContainer.setVisibility(android.view.View.GONE);
        binding.btnTrailerExternal.setVisibility(android.view.View.GONE);
        binding.btnTrailer.setText("Pogledaj trailer");
    }

    private void openTrailerExternally(String videoKey) {
        startActivity(new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/watch?v=" + videoKey)));
    }

    @Override
    protected void onStop() {
        super.onStop();
        hideTrailer();
    }
}