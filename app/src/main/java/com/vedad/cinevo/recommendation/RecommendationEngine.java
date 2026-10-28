package com.vedad.cinevo.recommendation;

import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.util.GenreUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecommendationEngine {

    public static final double WEIGHT_FAVORITE = 1.0;
    public static final double WEIGHT_WATCHLIST = 0.5;

    public static final double W_COSINE = 0.6;
    public static final double W_RATING = 0.2;
    public static final double W_COUNT = 0.2;

    public static final int MIN_VOTE_COUNT = 100;
    public static final double MIN_RATING = 6.0;

    public static boolean isQualityCandidate(Movie movie) {
        return isQualityCandidate(movie, MIN_RATING, MIN_VOTE_COUNT);
    }

    public static boolean isQualityCandidate(Movie movie, double minRating, int minVotes) {
        return movie.getPosterPath() != null
                && movie.getVoteCount() >= minVotes
                && movie.getVoteAverage() >= minRating;
    }

    public static class ScoredMovie {
        public final Movie movie;
        public final double score;

        public ScoredMovie(Movie movie, double score) {
            this.movie = movie;
            this.score = score;
        }
    }

    public static Map<Integer, Double> buildProfile(List<SavedMovie> saved) {
        Map<Integer, Double> profile = new HashMap<>();
        for (SavedMovie s : saved) {
            double weight = SavedMovie.FAVORITE.equals(s.listType)
                    ? WEIGHT_FAVORITE : WEIGHT_WATCHLIST;
            for (int genreId : new HashSet<>(GenreUtils.parse(s.genreIds))) {
                Double current = profile.get(genreId);
                profile.put(genreId, (current == null ? 0.0 : current) + weight);
            }
        }
        return profile;
    }

    public static List<Integer> topGenres(Map<Integer, Double> profile, int n) {
        List<Map.Entry<Integer, Double>> entries = new ArrayList<>(profile.entrySet());
        Collections.sort(entries, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < Math.min(n, entries.size()); i++) {
            result.add(entries.get(i).getKey());
        }
        return result;
    }

    public static double cosineSimilarity(Map<Integer, Double> profile, List<Integer> movieGenres) {
        Set<Integer> genres = new HashSet<>(movieGenres);
        if (profile.isEmpty() || genres.isEmpty()) return 0.0;

        double dot = 0.0;
        for (int g : genres) {
            Double w = profile.get(g);
            if (w != null) dot += w;
        }

        double normProfile = 0.0;
        for (double w : profile.values()) {
            normProfile += w * w;
        }
        normProfile = Math.sqrt(normProfile);
        double normMovie = Math.sqrt(genres.size());

        return dot / (normProfile * normMovie);
    }

    public static List<ScoredMovie> rank(Map<Integer, Double> profile,
                                         Collection<Movie> candidates,
                                         Map<Integer, Integer> recCounts,
                                         Set<Integer> excludeIds,
                                         int limit) {
        int maxCount = 1;
        for (int c : recCounts.values()) {
            maxCount = Math.max(maxCount, c);
        }

        List<ScoredMovie> scored = new ArrayList<>();
        for (Movie m : candidates) {
            if (excludeIds.contains(m.getId())) continue;

            double cosine = cosineSimilarity(profile, m.getGenreIds());
            double rating = m.getVoteAverage() / 10.0;
            Integer count = recCounts.get(m.getId());
            double countNorm = count == null ? 0.0 : (double) count / maxCount;

            double score = W_COSINE * cosine + W_RATING * rating + W_COUNT * countNorm;
            scored.add(new ScoredMovie(m, score));
        }

        Collections.sort(scored, (a, b) -> Double.compare(b.score, a.score));
        return scored.size() > limit ? new ArrayList<>(scored.subList(0, limit)) : scored;
    }
}