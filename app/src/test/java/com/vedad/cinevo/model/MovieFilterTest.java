package com.vedad.cinevo.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MovieFilterTest {

    @Test
    public void toQueryMap_includesOriginalLanguage() {
        MovieFilter f = new MovieFilter();
        f.originalLanguage = "ko";

        assertEquals("ko", f.toQueryMap().get("with_original_language"));
    }

    @Test
    public void toQueryMap_omitsLanguageWhenNotSet() {
        assertFalse(new MovieFilter().toQueryMap().containsKey("with_original_language"));
    }

    @Test
    public void language_countsAsFilter() {
        MovieFilter f = new MovieFilter();
        assertFalse(f.hasFilters());

        f.originalLanguage = "ja";

        assertTrue(f.hasFilters());
        assertEquals(1, f.activeCount());
    }

    @Test
    public void activeCount_countsEachGroupOnce() {
        MovieFilter f = new MovieFilter();
        f.genreIds.add(28);
        f.genreIds.add(12);
        f.yearFrom = 2000;
        f.yearTo = 2020;
        f.originalLanguage = "fr";

        assertEquals(3, f.activeCount());
    }

    @Test
    public void copy_keepsLanguageAndGenresIndependently() {
        MovieFilter f = new MovieFilter();
        f.originalLanguage = "ko";
        f.genreIds.add(18);

        MovieFilter c = f.copy();

        assertEquals("ko", c.originalLanguage);
        assertEquals(f.genreIds, c.genreIds);
        assertNotSame(f.genreIds, c.genreIds);
    }

    @Test
    public void toQueryMap_genresJoinedWithComma() {
        MovieFilter f = new MovieFilter();
        f.genreIds.add(28);
        f.genreIds.add(12);

        assertEquals("28,12", f.toQueryMap().get("with_genres"));
    }

    @Test
    public void toQueryMap_ratingSortRequiresEnoughVotes() {
        MovieFilter f = new MovieFilter();
        f.sortBy = MovieFilter.SORT_RATING;

        assertEquals("300", f.toQueryMap().get("vote_count.gte"));
    }
}