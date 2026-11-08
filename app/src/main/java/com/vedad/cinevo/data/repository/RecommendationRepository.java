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
import com.vedad.cinevo.model.MovieDetails;
import com.vedad.cinevo.model.MovieResponse;
import com.vedad.cinevo.model.PersonCredits;
import com.vedad.cinevo.model.PersonSearchResponse;
import com.vedad.cinevo.recommendation.RecommendationEngine;
import com.vedad.cinevo.util.AppSettings;
import com.vedad.cinevo.util.ResourceExplainText;

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
    private static final int MAX_DIRECTORS = 2;
    private static final int MAX_BACKFILL_PER_RUN = 10;
    private static final int RESULT_LIMIT = 20;

    private final SavedMovieDao dao;
    private final AppSettings settings;
    private final TmdbApi api = RetrofitClient.getApi();
    private final Context appContext;

    public RecommendationRepository(Context context) {
        dao = AppDatabase.getInstance(context).savedMovieDao();
        settings = new AppSettings(context);
        appContext = context.getApplicationContext();
    }

    public List<Movie> compute() throws IOException {
        List<SavedMovie> saved = dao.getAll();
        if (saved.isEmpty()) return new ArrayList<>();

        backfillDirectors(saved);

        double minRating = settings.getMinRating();
        int minVotes = settings.getMinVotes();

        Map<Integer, Double> profile = RecommendationEngine.buildProfile(saved);
        Map<Integer, Movie> candidates = new LinkedHashMap<>();
        Map<Integer, Integer> recCounts = new HashMap<>();
        Map<Integer, List<String>> becauseOf = new HashMap<>();
        Map<Integer, String> directorOfMovie = new HashMap<>();
        Set<Integer> directorMatchIds = new HashSet<>();
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
                    || (SavedMovie.WATCHED.equals(s.listType) && s.userRating >= 8);
            if (positiveSeed && used < MAX_FAVORITES_FOR_B && seededIds.add(s.id)) {
                used++;
                Response<MovieResponse> response = api.getRecommendations(
                        s.id, BuildConfig.TMDB_API_KEY, LANGUAGE, 1).execute();
                for (Movie m : bodyOrEmpty(response)) {
                    if (!RecommendationEngine.isQualityCandidate(m, minRating, minVotes)) continue;
                    candidates.putIfAbsent(m.getId(), m);
                    recCounts.merge(m.getId(), 1, Integer::sum);
                    becauseOf.computeIfAbsent(m.getId(), k -> new ArrayList<>()).add(s.title);
                }
            }
        }

        List<Integer> favoriteDirectors = RecommendationEngine.topDirectors(
                RecommendationEngine.buildDirectorProfile(saved), MAX_DIRECTORS);
        for (int directorId : favoriteDirectors) {
            String directorName = null;
            Response<PersonSearchResponse.Person> personResponse = api.getPerson(
                    directorId, BuildConfig.TMDB_API_KEY, LANGUAGE).execute();
            if (personResponse.isSuccessful() && personResponse.body() != null) {
                directorName = personResponse.body().getName();
            }

            Response<PersonCredits> response = api.getPersonMovieCredits(
                    directorId, BuildConfig.TMDB_API_KEY, LANGUAGE).execute();
            if (!response.isSuccessful() || response.body() == null
                    || response.body().getCrew() == null) {
                continue;
            }
            for (PersonCredits.CreditMovie m : response.body().getCrew()) {
                if (!"Director".equals(m.getJob())) continue;
                if (!RecommendationEngine.isQualityCandidate(m, minRating, minVotes)) continue;
                candidates.putIfAbsent(m.getId(), m);
                directorMatchIds.add(m.getId());
                if (directorName != null) directorOfMovie.putIfAbsent(m.getId(), directorName);
            }
        }

        List<RecommendationEngine.ScoredMovie> ranked = RecommendationEngine.rank(
                profile, candidates.values(), recCounts, directorMatchIds,
                excludeIds, RESULT_LIMIT);

        List<Movie> result = new ArrayList<>();
        RecommendationEngine.ExplainText explainText = new ResourceExplainText(appContext);
        for (RecommendationEngine.ScoredMovie scored : ranked) {
            Movie m = scored.movie;
            m.setReason(RecommendationEngine.explain(m, profile,
                    becauseOf.get(m.getId()), directorOfMovie.get(m.getId()), explainText));
            result.add(m);
            Log.d(TAG, String.format(Locale.US, "REC %s -> score %.3f | %s",
                    m.getTitle(), scored.score, m.getReason()));
        }
        return result;
    }

    private void backfillDirectors(List<SavedMovie> saved) throws IOException {
        Map<Integer, String> fetched = new HashMap<>();
        int calls = 0;

        for (SavedMovie s : saved) {
            if (s.directorIds != null) continue;
            if (SavedMovie.DISMISSED.equals(s.listType)) continue;

            String ids = fetched.get(s.id);
            if (ids == null) {
                if (calls >= MAX_BACKFILL_PER_RUN) continue;
                calls++;

                Response<MovieDetails> response = api.getMovieDetails(
                        s.id, BuildConfig.TMDB_API_KEY, LANGUAGE, "credits").execute();
                if (!response.isSuccessful() || response.body() == null) continue;

                ids = directorIdsOf(response.body());
                fetched.put(s.id, ids);
                dao.updateDirectors(s.id, ids);
            }
            s.directorIds = ids;
        }
    }

    private String directorIdsOf(MovieDetails details) {
        List<String> ids = new ArrayList<>();
        if (details.getCredits() != null && details.getCredits().getCrew() != null) {
            for (MovieDetails.Crew member : details.getCredits().getCrew()) {
                String id = String.valueOf(member.getId());
                if ("Director".equals(member.getJob()) && !ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return TextUtils.join(",", ids);
    }

    private List<Movie> bodyOrEmpty(Response<MovieResponse> response) {
        if (response.isSuccessful() && response.body() != null
                && response.body().getResults() != null) {
            return response.body().getResults();
        }
        return Collections.emptyList();
    }
}