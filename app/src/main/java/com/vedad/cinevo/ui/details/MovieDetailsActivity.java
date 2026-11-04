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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vedad.cinevo.BuildConfig;
import com.vedad.cinevo.R;
import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.data.remote.RetrofitClient;
import com.vedad.cinevo.data.repository.RuntimeRepository;
import com.vedad.cinevo.data.repository.SavedMovieRepository;
import com.vedad.cinevo.databinding.ActivityMovieDetailsBinding;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.model.MovieDetails;
import com.vedad.cinevo.ui.home.MovieAdapter;
import com.vedad.cinevo.ui.person.PersonMoviesActivity;
import com.vedad.cinevo.ui.settings.SettingsActivity;
import com.vedad.cinevo.util.AppSettings;
import com.vedad.cinevo.util.GenreUtils;
import com.vedad.cinevo.util.RatingFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_MOVIE_ID = "movie_id";
    private static final String BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/w780";
    private static final String POSTER_BASE_URL = "https://image.tmdb.org/t/p/w342";

    private ActivityMovieDetailsBinding binding;
    private MovieDetailsViewModel viewModel;
    private MovieDetails currentDetails;
    private boolean isFavorite = false;
    private boolean inWatchlist = false;
    private MovieAdapter similarAdapter;
    private WebView trailerWebView;
    private ProviderAdapter streamAdapter;
    private ProviderAdapter rentAdapter;
    private ProviderAdapter buyAdapter;
    private CastAdapter castAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMovieDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, 0, 0, bars.bottom);

            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) binding.btnBack.getLayoutParams();
            params.topMargin = bars.top + (int) (8 * getResources().getDisplayMetrics().density);
            binding.btnBack.setLayoutParams(params);
            return insets;
        });

        binding.btnBack.setOnClickListener(v -> finish());

        streamAdapter = new ProviderAdapter();
        rentAdapter = new ProviderAdapter();
        buyAdapter = new ProviderAdapter();
        setupProviderList(binding.recyclerStream, streamAdapter);
        setupProviderList(binding.recyclerRent, rentAdapter);
        setupProviderList(binding.recyclerBuy, buyAdapter);

        int movieId = getIntent().getIntExtra(EXTRA_MOVIE_ID, -1);
        if (movieId == -1) {
            finish();
            return;
        }
        viewModel = new ViewModelProvider(this).get(MovieDetailsViewModel.class);

        similarAdapter = new MovieAdapter();
        similarAdapter.observeStatuses(this, new SavedMovieRepository(this).getStatuses());
        similarAdapter.setFixedItemWidth((int) (130 * getResources().getDisplayMetrics().density));
        binding.recyclerSimilar.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerSimilar.setAdapter(similarAdapter);
        similarAdapter.setOnMovieClickListener(movie -> {
            Intent intent = new Intent(this, MovieDetailsActivity.class);
            intent.putExtra(EXTRA_MOVIE_ID, movie.getId());
            startActivity(intent);
        });

        castAdapter = new CastAdapter();
        binding.recyclerCast.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerCast.setAdapter(castAdapter);
        binding.recyclerCast.setNestedScrollingEnabled(false);
        castAdapter.setOnCastClickListener(member -> openPerson(member.getId(), member.getName(),
                member.getProfilePath(), PersonMoviesActivity.ROLE_ACTOR));

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
            int half = rating == null ? 0 : rating;
            binding.ratingBar.setRating(half / 2f);
            binding.tvRatingHint.setText(half == 0
                    ? "Ocijeni film"
                    : "Tvoja ocjena: " + RatingFormatter.stars(half) + "/5");
            binding.btnRemoveRating.setVisibility(
                    half == 0 ? android.view.View.GONE : android.view.View.VISIBLE);
        });

        binding.ratingBar.setOnRatingChangeListener(rating -> {
            if (currentDetails == null) return;
            int half = Math.round(rating * 2);
            if (half >= 1) saveRating(half);
        });

        binding.btnRemoveRating.setOnClickListener(v -> {
            if (currentDetails != null) {
                viewModel.remove(currentDetails.getId(), SavedMovie.WATCHED);
            }
        });
        binding.btnDetailsRetry.setOnClickListener(v -> loadDetails(movieId));

        loadDetails(movieId);
    }

    private void loadDetails(int movieId) {
        showLoading();
        RetrofitClient.getApi()
                .getMovieDetails(movieId, BuildConfig.TMDB_API_KEY, "en-US",
                        "videos,credits,recommendations,watch/providers")
                .enqueue(new Callback<MovieDetails>() {
                    @Override
                    public void onResponse(Call<MovieDetails> call, Response<MovieDetails> response) {
                        if (isFinishing() || isDestroyed()) return;
                        if (response.isSuccessful() && response.body() != null) {
                            showContent();
                            showDetails(response.body());
                        } else {
                            showError("Greška pri učitavanju filma (" + response.code() + ")");
                        }
                    }

                    @Override
                    public void onFailure(Call<MovieDetails> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        showError("Nema veze s internetom");
                    }
                });
    }

    private void showLoading() {
        binding.progressDetails.setVisibility(android.view.View.VISIBLE);
        binding.stateViewDetails.setVisibility(android.view.View.GONE);
        binding.scrollContent.setVisibility(android.view.View.INVISIBLE);
    }

    private void showContent() {
        binding.progressDetails.setVisibility(android.view.View.GONE);
        binding.stateViewDetails.setVisibility(android.view.View.GONE);
        binding.scrollContent.setVisibility(android.view.View.VISIBLE);
    }

    private void showError(String message) {
        binding.progressDetails.setVisibility(android.view.View.GONE);
        binding.tvDetailsState.setText(message);
        binding.stateViewDetails.setVisibility(android.view.View.VISIBLE);
        binding.scrollContent.setVisibility(android.view.View.INVISIBLE);
    }

    private void showDetails(MovieDetails d) {
        currentDetails = d;
        RuntimeRepository.put(d.getId(), d.getRuntime());
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

        binding.chipGroupGenres.removeAllViews();
        if (d.getGenres() != null) {
            for (MovieDetails.Genre g : d.getGenres()) {
                Chip chip = new Chip(this);
                chip.setText(g.getName());
                chip.setClickable(false);
                chip.setCheckable(false);
                binding.chipGroupGenres.addView(chip);
            }
        }

        if (d.getPosterPath() != null) {
            Glide.with(this).load(POSTER_BASE_URL + d.getPosterPath()).into(binding.imgPoster);
        }

        binding.tvOverview.setText(TextUtils.isEmpty(d.getOverview())
                ? "Opis nije dostupan." : d.getOverview());

        List<MovieDetails.Cast> allCast = new ArrayList<>();
        if (d.getCredits() != null && d.getCredits().getCast() != null) {
            allCast.addAll(d.getCredits().getCast());
        }
        viewModel.setFullCast(allCast);

        List<MovieDetails.Cast> preview = new ArrayList<>(
                allCast.size() > 15 ? allCast.subList(0, 15) : allCast);
        castAdapter.setCast(preview);

        binding.sectionCast.setVisibility(
                allCast.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);

        boolean hasMore = allCast.size() > 15;
        binding.btnCastAll.setVisibility(hasMore
                ? android.view.View.VISIBLE : android.view.View.GONE);
        binding.btnCastAll.setText("Pogledaj sve (" + allCast.size() + ")");
        binding.btnCastAll.setOnClickListener(v ->
                new CastBottomSheet().show(getSupportFragmentManager(), "cast"));

        List<MovieDetails.Crew> directors = directorsOf(d);
        if (directors.isEmpty()) {
            binding.tvDirector.setVisibility(android.view.View.GONE);
        } else {
            List<String> names = new ArrayList<>();
            for (MovieDetails.Crew c : directors) names.add(c.getName());
            binding.tvDirector.setText("Režija: " + TextUtils.join(", ", names));
            binding.tvDirector.setVisibility(android.view.View.VISIBLE);
            binding.tvDirector.setOnClickListener(v -> openDirector(directors));
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

        List<String> directorIds = new ArrayList<>();
        for (MovieDetails.Crew director : directorsOf(currentDetails)) {
            directorIds.add(String.valueOf(director.getId()));
        }

        movie.directorIds = TextUtils.join(",", directorIds);
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
        binding.tvWatchTitle.setText("Gdje gledati (" + regionName(region) + ")");
        binding.tvWatchRegion.setText("Promijeni državu u postavkama");
        binding.tvWatchRegion.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        MovieDetails.RegionProviders providers = null;
        if (d.getWatchProviders() != null && d.getWatchProviders().getResults() != null) {
            providers = d.getWatchProviders().getResults().get(region);
        }

        boolean any = bindGroup(binding.groupStream, streamAdapter,
                providers != null ? providers.getFlatrate() : null);
        any |= bindGroup(binding.groupRent, rentAdapter,
                providers != null ? providers.getRent() : null);
        any |= bindGroup(binding.groupBuy, buyAdapter,
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

    private boolean bindGroup(android.view.View group, ProviderAdapter adapter,
                              List<MovieDetails.Provider> list) {
        if (list == null || list.isEmpty()) {
            group.setVisibility(android.view.View.GONE);
            return false;
        }
        adapter.setProviders(list);
        group.setVisibility(android.view.View.VISIBLE);
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

    private void setupProviderList(RecyclerView list, ProviderAdapter adapter) {
        list.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        list.setAdapter(adapter);
        list.setNestedScrollingEnabled(false);
    }

    private String regionName(String code) {
        String[] codes = getResources().getStringArray(R.array.region_values);
        String[] names = getResources().getStringArray(R.array.region_entries);
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(code)) return names[i];
        }
        return code;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentDetails != null) showWatchProviders(currentDetails);
    }

    private List<MovieDetails.Crew> directorsOf(MovieDetails d) {
        List<MovieDetails.Crew> result = new ArrayList<>();
        if (d.getCredits() == null || d.getCredits().getCrew() == null) return result;
        for (MovieDetails.Crew member : d.getCredits().getCrew()) {
            if (!"Director".equals(member.getJob())) continue;
            boolean exists = false;
            for (MovieDetails.Crew c : result) {
                if (c.getId() == member.getId()) exists = true;
            }
            if (!exists) result.add(member);
        }
        return result;
    }

    private void openDirector(List<MovieDetails.Crew> directors) {
        if (directors.size() == 1) {
            MovieDetails.Crew c = directors.get(0);
            openPerson(c.getId(), c.getName(), c.getProfilePath(), PersonMoviesActivity.ROLE_DIRECTOR);
            return;
        }
        String[] names = new String[directors.size()];
        for (int i = 0; i < names.length; i++) names[i] = directors.get(i).getName();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Izaberi reditelja")
                .setItems(names, (dialog, which) -> {
                    MovieDetails.Crew c = directors.get(which);
                    openPerson(c.getId(), c.getName(), c.getProfilePath(),
                            PersonMoviesActivity.ROLE_DIRECTOR);
                })
                .show();
    }

    private void openPerson(int id, String name, String photo, String role) {
        Intent intent = new Intent(this, PersonMoviesActivity.class);
        intent.putExtra(PersonMoviesActivity.EXTRA_ID, id);
        intent.putExtra(PersonMoviesActivity.EXTRA_NAME, name);
        intent.putExtra(PersonMoviesActivity.EXTRA_PHOTO, photo);
        intent.putExtra(PersonMoviesActivity.EXTRA_ROLE, role);
        startActivity(intent);
    }
}