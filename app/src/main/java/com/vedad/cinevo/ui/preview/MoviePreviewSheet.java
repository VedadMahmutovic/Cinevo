package com.vedad.cinevo.ui.preview;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vedad.cinevo.BuildConfig;
import com.vedad.cinevo.R;
import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.data.remote.RetrofitClient;
import com.vedad.cinevo.data.repository.RuntimeRepository;
import com.vedad.cinevo.data.repository.SavedMovieRepository;
import com.vedad.cinevo.databinding.BottomSheetPreviewBinding;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.model.MovieDetails;
import com.vedad.cinevo.ui.details.MovieDetailsActivity;
import com.vedad.cinevo.util.GenreLabels;
import com.vedad.cinevo.util.RuntimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MoviePreviewSheet extends BottomSheetDialogFragment {

    private static final String ARG_ID = "id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_OVERVIEW = "overview";
    private static final String ARG_POSTER = "poster";
    private static final String ARG_RATING = "rating";
    private static final String ARG_DATE = "date";
    private static final String ARG_GENRES = "genres";

    private static final String POSTER_BASE_URL = "https://image.tmdb.org/t/p/w342";

    private static final int SHEET_ALPHA = 0xE0;

    private static final Map<Integer, Directors> DIRECTOR_CACHE = new ConcurrentHashMap<>();

    private static class Directors {
        final String names;
        final String ids;

        Directors(String names, String ids) {
            this.names = names;
            this.ids = ids;
        }
    }

    public static void show(FragmentManager fragmentManager, Movie movie) {
        Bundle args = new Bundle();
        args.putInt(ARG_ID, movie.getId());
        args.putString(ARG_TITLE, movie.getTitle());
        args.putString(ARG_OVERVIEW, movie.getOverview());
        args.putString(ARG_POSTER, movie.getPosterPath());
        args.putDouble(ARG_RATING, movie.getVoteAverage());
        args.putString(ARG_DATE, movie.getReleaseDate());
        args.putIntegerArrayList(ARG_GENRES, new ArrayList<>(movie.getGenreIds()));

        MoviePreviewSheet sheet = new MoviePreviewSheet();
        sheet.setArguments(args);
        sheet.show(fragmentManager, "preview");
    }

    private BottomSheetPreviewBinding binding;
    private SavedMovieRepository repository;
    private Call<MovieDetails> detailsCall;

    private int movieId;
    private String title;
    private String overview;
    private String posterPath;
    private double rating;
    private String releaseDate;
    private ArrayList<Integer> genreIds;

    private boolean isFavorite = false;
    private boolean inWatchlist = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetPreviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = requireArguments();
        movieId = args.getInt(ARG_ID);
        title = args.getString(ARG_TITLE);
        overview = args.getString(ARG_OVERVIEW);
        posterPath = args.getString(ARG_POSTER);
        rating = args.getDouble(ARG_RATING);
        releaseDate = args.getString(ARG_DATE);
        genreIds = args.getIntegerArrayList(ARG_GENRES);
        if (genreIds == null) genreIds = new ArrayList<>();

        repository = new SavedMovieRepository(requireContext());

        binding.tvPreviewTitle.setText(title);
        binding.tvPreviewOverview.setText(TextUtils.isEmpty(overview)
                ? getString(R.string.overview_missing) : overview);

        List<String> genreNames = new ArrayList<>();
        for (int id : genreIds) genreNames.add(GenreLabels.of(requireContext(), id));
        binding.tvPreviewGenres.setText(TextUtils.join(" · ", genreNames));
        binding.tvPreviewGenres.setVisibility(genreNames.isEmpty() ? View.GONE : View.VISIBLE);

        if (posterPath != null) {
            Glide.with(this).load(POSTER_BASE_URL + posterPath).into(binding.imgPreviewPoster);
        }

        Integer cachedRuntime = RuntimeRepository.getCached(movieId);
        Directors cachedDirectors = DIRECTOR_CACHE.get(movieId);
        renderInfo(cachedRuntime);
        renderDirectors(cachedDirectors);
        if (cachedRuntime == null || cachedDirectors == null) {
            loadExtras();
        }

        repository.countOf(movieId, SavedMovie.FAVORITE).observe(getViewLifecycleOwner(), count -> {
            isFavorite = count != null && count > 0;
            binding.btnPreviewFavorite.setText(
                    isFavorite ? R.string.favorite_added : R.string.favorite_add);
        });
        repository.countOf(movieId, SavedMovie.WATCHLIST).observe(getViewLifecycleOwner(), count -> {
            inWatchlist = count != null && count > 0;
            binding.btnPreviewWatchlist.setText(
                    inWatchlist ? R.string.watchlist_added : R.string.watchlist_add);
        });

        binding.btnPreviewFavorite.setOnClickListener(v -> toggle(SavedMovie.FAVORITE, isFavorite));
        binding.btnPreviewWatchlist.setOnClickListener(v -> toggle(SavedMovie.WATCHLIST, inWatchlist));

        binding.btnPreviewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MovieDetailsActivity.class);
            intent.putExtra(MovieDetailsActivity.EXTRA_MOVIE_ID, movieId);
            startActivity(intent);
            dismiss();
        });
    }

    private void loadExtras() {
        detailsCall = RetrofitClient.getApi().getMovieDetails(
                movieId, BuildConfig.TMDB_API_KEY, "en-US", "credits");

        detailsCall.enqueue(new Callback<MovieDetails>() {
            @Override
            public void onResponse(Call<MovieDetails> call, Response<MovieDetails> response) {
                if (call.isCanceled() || binding == null) return;
                if (!response.isSuccessful() || response.body() == null) return;

                MovieDetails details = response.body();
                RuntimeRepository.put(movieId, details.getRuntime());

                Directors directors = directorsOf(details);
                DIRECTOR_CACHE.put(movieId, directors);

                renderInfo(details.getRuntime() > 0 ? details.getRuntime() : null);
                renderDirectors(directors);
            }

            @Override
            public void onFailure(Call<MovieDetails> call, Throwable t) {
            }
        });
    }

    private Directors directorsOf(MovieDetails details) {
        List<String> names = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        if (details.getCredits() != null && details.getCredits().getCrew() != null) {
            for (MovieDetails.Crew member : details.getCredits().getCrew()) {
                String id = String.valueOf(member.getId());
                if ("Director".equals(member.getJob()) && !ids.contains(id)) {
                    ids.add(id);
                    names.add(member.getName());
                }
            }
        }
        return new Directors(TextUtils.join(", ", names), TextUtils.join(",", ids));
    }

    private void renderInfo(@Nullable Integer runtimeMinutes) {
        List<String> parts = new ArrayList<>();
        if (releaseDate != null && releaseDate.length() >= 4) {
            parts.add(releaseDate.substring(0, 4));
        }
        if (runtimeMinutes != null) {
            parts.add(RuntimeFormatter.format(runtimeMinutes));
        }
        if (rating > 0) {
            parts.add(String.format(Locale.US, "★ %.1f", rating));
        }
        binding.tvPreviewInfo.setText(TextUtils.join(" · ", parts));
    }

    private void renderDirectors(@Nullable Directors directors) {
        if (directors == null || directors.names.isEmpty()) {
            binding.tvPreviewDirector.setVisibility(View.GONE);
            return;
        }
        binding.tvPreviewDirector.setText(getString(R.string.directed_by, directors.names));
        binding.tvPreviewDirector.setVisibility(View.VISIBLE);
    }

    private void toggle(String listType, boolean alreadySaved) {
        if (alreadySaved) {
            repository.remove(movieId, listType);
            return;
        }

        SavedMovie row = new SavedMovie();
        row.id = movieId;
        row.listType = listType;
        row.title = title;
        row.posterPath = posterPath;
        row.voteAverage = rating;
        row.releaseDate = releaseDate;
        row.overview = overview;
        row.genreIds = TextUtils.join(",", genreIds);
        row.addedAt = System.currentTimeMillis();

        Directors known = DIRECTOR_CACHE.get(movieId);
        row.directorIds = known != null ? known.ids : null;

        repository.add(row);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!(getDialog() instanceof BottomSheetDialog)) return;

        BottomSheetBehavior<?> behavior = ((BottomSheetDialog) getDialog()).getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        View sheet = ((BottomSheetDialog) getDialog())
                .findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            sheet.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (sheet.getBackground() == null) return;
                            sheet.getBackground().mutate().setAlpha(SHEET_ALPHA);
                            sheet.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (detailsCall != null) detailsCall.cancel();
        binding = null;
    }
}