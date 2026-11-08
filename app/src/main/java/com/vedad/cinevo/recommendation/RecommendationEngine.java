package com.vedad.cinevo.recommendation;

import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.util.GenreUtils;
import com.vedad.cinevo.util.GenreCatalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

public class RecommendationEngine {

    public static final double WEIGHT_FAVORITE = 1.0;
    public static final double WEIGHT_WATCHLIST = 0.5;
    public static final double W_COSINE = 0.6;
    public static final double W_RATING = 0.2;
    public static final double W_COUNT = 0.2;
    public static final double W_DIRECTOR = 0.15;
    public static final double MIN_DIRECTOR_WEIGHT = 1.0;
    public static final int MIN_VOTE_COUNT = 100;
    public static final double MIN_RATING = 6.0;
    public static final double WEIGHT_DISMISSED = -0.25;

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

    public static double weightOf(SavedMovie s) {
        if (SavedMovie.FAVORITE.equals(s.listType)) return WEIGHT_FAVORITE;
        if (SavedMovie.WATCHLIST.equals(s.listType)) return WEIGHT_WATCHLIST;
        if (SavedMovie.DISMISSED.equals(s.listType)) return WEIGHT_DISMISSED;
        if (SavedMovie.WATCHED.equals(s.listType) && s.userRating >= 1) {
            double stars = s.userRating / 2.0;
            return Math.max(-1.0, Math.min(1.0, (stars - 3) / 2.0));
        }
        return 0.0;
    }

    public static Map<Integer, Double> buildProfile(List<SavedMovie> saved) {
        Map<Integer, Double> profile = new HashMap<>();
        for (SavedMovie s : saved) {
            double weight = weightOf(s);
            if (weight == 0.0) continue;
            for (int genreId : new HashSet<>(GenreUtils.parse(s.genreIds))) {
                Double current = profile.get(genreId);
                profile.put(genreId, (current == null ? 0.0 : current) + weight);
            }
        }
        return profile;
    }

    public static Map<Integer, Double> buildDirectorProfile(List<SavedMovie> saved) {
        Map<Integer, Double> profile = new HashMap<>();
        for (SavedMovie s : saved) {
            double weight = weightOf(s);
            if (weight <= 0.0) continue;
            for (int directorId : new HashSet<>(GenreUtils.parse(s.directorIds))) {
                Double current = profile.get(directorId);
                profile.put(directorId, (current == null ? 0.0 : current) + weight);
            }
        }
        return profile;
    }

    public static List<Integer> topGenres(Map<Integer, Double> profile, int n) {
        return topByWeight(profile, n, 0.0);
    }

    public static List<Integer> topDirectors(Map<Integer, Double> profile, int n) {
        return topByWeight(profile, n, MIN_DIRECTOR_WEIGHT);
    }

    private static List<Integer> topByWeight(Map<Integer, Double> profile, int n,
                                             double minWeight) {
        List<Map.Entry<Integer, Double>> entries = new ArrayList<>(profile.entrySet());
        Collections.sort(entries, (a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<Integer> result = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : entries) {
            if (result.size() == n) break;
            if (e.getValue() > 0 && e.getValue() >= minWeight) result.add(e.getKey());
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
        if (normProfile == 0.0) return 0.0;
        double normMovie = Math.sqrt(genres.size());

        return dot / (normProfile * normMovie);
    }

    public static List<ScoredMovie> rank(Map<Integer, Double> profile,
                                         Collection<Movie> candidates,
                                         Map<Integer, Integer> recCounts,
                                         Set<Integer> excludeIds,
                                         int limit) {
        return rank(profile, candidates, recCounts, new HashSet<Integer>(), excludeIds, limit);
    }

    public static List<ScoredMovie> rank(Map<Integer, Double> profile,
                                         Collection<Movie> candidates,
                                         Map<Integer, Integer> recCounts,
                                         Set<Integer> directorMatchIds,
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
            double directorBonus = directorMatchIds.contains(m.getId()) ? W_DIRECTOR : 0.0;

            double score = W_COSINE * cosine + W_RATING * rating
                    + W_COUNT * countNorm + directorBonus;
            scored.add(new ScoredMovie(m, score));
        }

        Collections.sort(scored, (a, b) -> Double.compare(b.score, a.score));
        return scored.size() > limit ? new ArrayList<>(scored.subList(0, limit)) : scored;
    }

    public static String explain(Movie movie, Map<Integer, Double> profile,
                                 List<String> seedTitles, String directorName) {
        List<String> parts = new ArrayList<>();

        if (seedTitles != null && !seedTitles.isEmpty()) {
            String first = seedTitles.get(0);
            parts.add(seedTitles.size() == 1
                    ? "Jer ti se sviđa " + first
                    : "Jer ti se sviđa " + first + " i još " + (seedTitles.size() - 1));
        }

        if (directorName != null && !directorName.isEmpty()) {
            parts.add("Režija: " + directorName);
        }

        List<Integer> shared = new ArrayList<>();
        for (int genreId : new HashSet<>(movie.getGenreIds())) {
            Double weight = profile.get(genreId);
            if (weight != null && weight > 0) shared.add(genreId);
        }
        Collections.sort(shared, (a, b) -> Double.compare(profile.get(b), profile.get(a)));
        if (!shared.isEmpty()) {
            StringBuilder genres = new StringBuilder();
            for (int i = 0; i < Math.min(2, shared.size()); i++) {
                if (i > 0) genres.append(", ");
                genres.append(GenreCatalog.nameOf(shared.get(i)));
            }
            parts.add("Žanr: " + genres);
        }

        if (parts.isEmpty()) {
            parts.add(String.format(Locale.US, "Visoko ocijenjen (%.1f)", movie.getVoteAverage()));
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.size()); i++) {
            if (i > 0) result.append(" • ");
            result.append(parts.get(i));
        }
        return result.toString();
    }
}