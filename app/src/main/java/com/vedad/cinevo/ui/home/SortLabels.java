package com.vedad.cinevo.ui.home;

import com.vedad.cinevo.R;
import com.vedad.cinevo.model.MovieFilter;

public class SortLabels {

    public static int full(String sortBy) {
        if (MovieFilter.SORT_RATING.equals(sortBy)) return R.string.sort_rating;
        if (MovieFilter.SORT_DATE.equals(sortBy)) return R.string.sort_date_desc;
        if (MovieFilter.SORT_DATE_ASC.equals(sortBy)) return R.string.sort_date_asc;
        return R.string.sort_popularity;
    }

    public static int shortLabel(String sortBy) {
        if (MovieFilter.SORT_RATING.equals(sortBy)) return R.string.sort_short_rating;
        if (MovieFilter.SORT_DATE.equals(sortBy)) return R.string.sort_short_newest;
        if (MovieFilter.SORT_DATE_ASC.equals(sortBy)) return R.string.sort_short_oldest;
        return R.string.sort_short_popularity;
    }
}