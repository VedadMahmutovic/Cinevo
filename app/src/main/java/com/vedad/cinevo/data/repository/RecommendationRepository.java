package com.vedad.cinevo.data.repository;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.vedad.cinevo.BuildConfig;
import com.vedad.cinevo.data.local.AppDatabase;
import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.data.local.SavedMovieDao;
import com.vedad.cinevo.data.remote.RetrofitClient;
import com.vedad.cinevo.data.remote.TmdbApi;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.model.MovieResponse;
import com.vedad.cinevo.recommendation.RecommendationEngine;
import com.vedad.cinevo.util.AppSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Response;

public class RecommendationRepository {

    private static final String TAG = "Cinevo";
    private static final String LANGUAGE = "en-US";
    private static final int TOP_GENRES = 3;
    private static final int MAX_FAVORITES_FOR_B = 5;
    private static final int RESULT_LIMIT = 20;

    private final SavedMovieDao dao;
    private final AppSettings settings;
    private final TmdbApi api = RetrofitClient.getApi();

    public RecommendationRepository(Context context) {
        dao = AppDatabase.getInstance(context).savedMovieDao();
        settings = new AppSettings(context);
    }

    public List<Movie> compute() throws IOException {
        List<SavedMovie> saved = dao.getAll();
        if (saved.isEmpty()) return new ArrayList<>();

        double minRating = settings.getMinRating();
        int minVotes = settings.getMinVotes();

        Map<Integer, Double> profile = RecommendationEngine.buildProfile(saved);
        Map<Integer, Movie> candidates = new LinkedHashMap<>();
        Map<Integer, Integer> recCounts = new HashMap<>();
        Set<Integer> excludeIds = new HashSet<>();

        List<Integer> topGenres = RecommendationEngine.topGenres(profile, TOP_GENRES);
        if (!topGenres.isEmpty()) {
            String genres = TextUtils.join("|", topGenres);
            Response<MovieResponse> response = api.discoverMovies(
                            BuildConfig.TMDB_API_KEY, LANGUAGE, genres, "popularity.desc",
                            minVotes, 1)
                    .execute();
            for (Movie m : bodyOrEmpty(response)) {
                if (RecommendationEngine.isQualityCandidate(m, minRating, minVotes)) {
                    candidates.putIfAbsent(m.getId(), m);
                }
            }
        }

        Set<Integer> seededIds = new HashSet<>();
        int used = 0;
        for (SavedMovie s : saved) {
            excludeIds.add(s.id);
            boolean positiveSeed = SavedMovie.FAVORITE.equals(s.listType)
                    || (SavedMovie.WATCHED.equals(s.listType) && s.userRating >= 4);
            if (positiveSeed && used < MAX_FAVORITES_FOR_B && seededIds.add(s.id)) {
                used++;
                Response<MovieResponse> response = api.getRecommendations(
                        s.id, BuildConfig.TMDB_API_KEY, LANGUAGE, 1).execute();
                for (Movie m : bodyOrEmpty(response)) {
                    if (!RecommendationEngine.isQualityCandidate(m, minRating, minVotes)) continue;
                    candidates.putIfAbsent(m.getId(), m);
                    recCounts.merge(m.getId(), 1, Integer::sum);
                }
            }
        }

        List<RecommendationEngine.ScoredMovie> ranked = RecommendationEngine.rank(
                profile, candidates.values(), recCounts, excludeIds, RESULT_LIMIT);

        List<Movie> result = new ArrayList<>();
        for (RecommendationEngine.ScoredMovie s : ranked) {
            result.add(s.movie);
            Log.d(TAG, String.format(Locale.US, "REC %s -> score %.3f",
                    s.movie.getTitle(), s.score));
        }
        return result;
    }

    private List<Movie> bodyOrEmpty(Response<MovieResponse> response) {
        if (response.isSuccessful() && response.body() != null
                && response.body().getResults() != null) {
            return response.body().getResults();
        }
        return Collections.emptyList();
    }
}