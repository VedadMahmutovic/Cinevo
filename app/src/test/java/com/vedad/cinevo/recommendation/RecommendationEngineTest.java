package com.vedad.cinevo.recommendation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.model.Movie;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecommendationEngineTest {

    private static final double DELTA = 1e-9;

    private SavedMovie saved(int id, String listType, String genres) {
        SavedMovie s = new SavedMovie();
        s.id = id;
        s.listType = listType;
        s.genreIds = genres;
        return s;
    }

    private Movie movie(int id, double rating, Integer... genres) {
        Movie m = new Movie(id, "Movie " + id, "", "/poster.jpg", rating, "2024-01-01");
        m.setGenreIds(Arrays.asList(genres));
        return m;
    }

    private Map<Integer, Double> profileOf(Object... pairs) {
        Map<Integer, Double> profile = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            profile.put((Integer) pairs[i], (Double) pairs[i + 1]);
        }
        return profile;
    }

    private List<RecommendationEngine.ScoredMovie> rank(Map<Integer, Double> profile,
                                                        List<Movie> candidates,
                                                        Map<Integer, Integer> recCounts,
                                                        Set<Integer> exclude,
                                                        int limit) {
        return RecommendationEngine.rank(profile, candidates, recCounts, exclude, limit);
    }

    private SavedMovie watched(int id, double stars, String genres) {
        SavedMovie s = saved(id, SavedMovie.WATCHED, genres);
        s.userRating = (int) Math.round(stars * 2);
        return s;
    }

    @Test
    public void buildProfile_favoriteCountsMoreThanWatchlist() {
        List<SavedMovie> saved = Arrays.asList(
                saved(1, SavedMovie.FAVORITE, "28,12"),
                saved(2, SavedMovie.WATCHLIST, "28"));

        Map<Integer, Double> profile = RecommendationEngine.buildProfile(saved);

        assertEquals(1.5, profile.get(28), DELTA);
        assertEquals(1.0, profile.get(12), DELTA);
    }

    @Test
    public void buildProfile_missingGenresDoNotCrash() {
        List<SavedMovie> saved = Arrays.asList(
                saved(1, SavedMovie.FAVORITE, null),
                saved(2, SavedMovie.FAVORITE, ""));

        assertTrue(RecommendationEngine.buildProfile(saved).isEmpty());
    }

    @Test
    public void topGenres_returnsHighestWeightsFirst() {
        Map<Integer, Double> profile = profileOf(28, 1.5, 12, 1.0, 35, 0.5);

        List<Integer> top = RecommendationEngine.topGenres(profile, 2);

        assertEquals(Arrays.asList(28, 12), top);
    }

    @Test
    public void cosine_sameDirectionIsOne() {
        Map<Integer, Double> profile = profileOf(28, 2.0);

        assertEquals(1.0, RecommendationEngine.cosineSimilarity(profile, Arrays.asList(28)), DELTA);
    }

    @Test
    public void cosine_noOverlapIsZero() {
        Map<Integer, Double> profile = profileOf(28, 1.0);

        assertEquals(0.0, RecommendationEngine.cosineSimilarity(profile, Arrays.asList(35)), DELTA);
    }

    @Test
    public void cosine_partialOverlapIsHalf() {
        Map<Integer, Double> profile = profileOf(28, 1.0, 12, 1.0);

        assertEquals(0.5, RecommendationEngine.cosineSimilarity(profile, Arrays.asList(28, 35)), DELTA);
    }

    @Test
    public void cosine_emptyProfileOrGenresIsZero() {
        assertEquals(0.0, RecommendationEngine.cosineSimilarity(
                new HashMap<>(), Arrays.asList(28)), DELTA);
        assertEquals(0.0, RecommendationEngine.cosineSimilarity(
                profileOf(28, 1.0), Collections.<Integer>emptyList()), DELTA);
    }

    @Test
    public void rank_exactScoreFormula() {
        Map<Integer, Double> profile = profileOf(28, 1.0);
        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(1, 2);

        List<RecommendationEngine.ScoredMovie> ranked = rank(profile,
                Arrays.asList(movie(1, 8.0, 28)), counts, new HashSet<Integer>(), 10);

        assertEquals(0.96, ranked.get(0).score, DELTA);
    }

    @Test
    public void rank_prefersMatchingGenre() {
        Map<Integer, Double> profile = profileOf(27, 1.0);

        List<RecommendationEngine.ScoredMovie> ranked = rank(profile,
                Arrays.asList(movie(1, 6.0, 35), movie(2, 6.0, 27)),
                new HashMap<Integer, Integer>(), new HashSet<Integer>(), 10);

        assertEquals(2, ranked.get(0).movie.getId());
    }

    @Test
    public void rank_bonusForRecommendedByMoreFavorites() {
        Map<Integer, Double> profile = profileOf(27, 1.0);
        Map<Integer, Integer> counts = new HashMap<>();
        counts.put(2, 3);

        List<RecommendationEngine.ScoredMovie> ranked = rank(profile,
                Arrays.asList(movie(1, 7.0, 27), movie(2, 7.0, 27)),
                counts, new HashSet<Integer>(), 10);

        assertEquals(2, ranked.get(0).movie.getId());
    }

    @Test
    public void rank_excludesAlreadySavedMovies() {
        Map<Integer, Double> profile = profileOf(27, 1.0);
        Set<Integer> exclude = new HashSet<>(Arrays.asList(1));

        List<RecommendationEngine.ScoredMovie> ranked = rank(profile,
                Arrays.asList(movie(1, 9.0, 27), movie(2, 5.0, 27)),
                new HashMap<Integer, Integer>(), exclude, 10);

        assertEquals(1, ranked.size());
        assertFalse(ranked.get(0).movie.getId() == 1);
    }

    @Test
    public void rank_respectsLimit() {
        Map<Integer, Double> profile = profileOf(27, 1.0);

        List<RecommendationEngine.ScoredMovie> ranked = rank(profile,
                Arrays.asList(movie(1, 5.0, 27), movie(2, 6.0, 27), movie(3, 7.0, 27)),
                new HashMap<Integer, Integer>(), new HashSet<Integer>(), 2);

        assertEquals(2, ranked.size());
        assertEquals(3, ranked.get(0).movie.getId());
    }

    @Test
    public void qualityFilter_rejectsLowRatingFewVotesAndNoPoster() {
        Movie good = movie(1, 7.0, 27);
        good.setVoteCount(500);

        Movie lowRating = movie(2, 4.0, 27);
        lowRating.setVoteCount(500);

        Movie fewVotes = movie(3, 8.0, 27);
        fewVotes.setVoteCount(5);

        Movie noPoster = new Movie(4, "Movie 4", "", null, 8.0, "2024-01-01");
        noPoster.setVoteCount(500);

        Movie boundary = movie(5, 6.0, 27);
        boundary.setVoteCount(100);

        assertTrue(RecommendationEngine.isQualityCandidate(good));
        assertFalse(RecommendationEngine.isQualityCandidate(lowRating));
        assertFalse(RecommendationEngine.isQualityCandidate(fewVotes));
        assertFalse(RecommendationEngine.isQualityCandidate(noPoster));
        assertTrue(RecommendationEngine.isQualityCandidate(boundary));
    }

    @Test
    public void buildProfile_ratingsMapToSignedWeights() {
        List<SavedMovie> saved = Arrays.asList(
                watched(1, 5, "28"),
                watched(2, 1, "35"),
                watched(3, 3, "18"));

        Map<Integer, Double> profile = RecommendationEngine.buildProfile(saved);

        assertEquals(1.0, profile.get(28), DELTA);
        assertEquals(-1.0, profile.get(35), DELTA);
        assertFalse(profile.containsKey(18));
    }

    @Test
    public void topGenres_ignoresNonPositiveWeights() {
        Map<Integer, Double> profile = profileOf(28, 1.0, 35, -1.0, 12, 0.0);

        assertEquals(Arrays.asList(28), RecommendationEngine.topGenres(profile, 3));
    }

    @Test
    public void cosine_negativeWeightLowersScore() {
        Map<Integer, Double> profile = profileOf(28, 1.0, 35, -1.0);

        assertEquals(-1.0 / Math.sqrt(2),
                RecommendationEngine.cosineSimilarity(profile, Arrays.asList(35)), DELTA);
    }

    @Test
    public void cosine_zeroProfileIsZeroNotNaN() {
        Map<Integer, Double> profile = profileOf(18, 0.0);

        assertEquals(0.0, RecommendationEngine.cosineSimilarity(profile, Arrays.asList(18)), DELTA);
    }

    @Test
    public void rank_dislikedGenreRanksLower() {
        Map<Integer, Double> profile = profileOf(27, 1.0, 35, -1.0);

        List<RecommendationEngine.ScoredMovie> ranked = rank(profile,
                Arrays.asList(movie(1, 7.0, 35), movie(2, 7.0, 27)),
                new HashMap<Integer, Integer>(), new HashSet<Integer>(), 10);

        assertEquals(2, ranked.get(0).movie.getId());
    }

    @Test
    public void buildProfile_halfStarRatingMapsLinearly() {
        Map<Integer, Double> profile = RecommendationEngine.buildProfile(
                Arrays.asList(watched(1, 4.5, "12")));

        assertEquals(0.75, profile.get(12), DELTA);
    }

    @Test
    public void buildDirectorProfile_usesOnlyPositiveSignals() {
        SavedMovie liked = saved(1, SavedMovie.FAVORITE, "28");
        liked.directorIds = "525";
        SavedMovie disliked = watched(2, 1.0, "35");
        disliked.directorIds = "999";

        Map<Integer, Double> profile = RecommendationEngine.buildDirectorProfile(
                Arrays.asList(liked, disliked));

        assertEquals(1.0, profile.get(525), DELTA);
        assertFalse(profile.containsKey(999));
    }

    @Test
    public void topDirectors_requiresMinimumWeight() {
        Map<Integer, Double> profile = profileOf(525, 1.0, 7, 0.5);

        assertEquals(Arrays.asList(525), RecommendationEngine.topDirectors(profile, 5));
    }

    @Test
    public void rank_directorMatchAddsBonus() {
        Map<Integer, Double> profile = profileOf(27, 1.0);
        Set<Integer> directorMatches = new HashSet<>(Arrays.asList(2));

        List<RecommendationEngine.ScoredMovie> ranked = RecommendationEngine.rank(profile,
                Arrays.asList(movie(1, 7.0, 27), movie(2, 7.0, 27)),
                new HashMap<Integer, Integer>(), directorMatches, new HashSet<Integer>(), 10);

        assertEquals(2, ranked.get(0).movie.getId());
        assertEquals(RecommendationEngine.W_DIRECTOR,
                ranked.get(0).score - ranked.get(1).score, DELTA);
    }
}