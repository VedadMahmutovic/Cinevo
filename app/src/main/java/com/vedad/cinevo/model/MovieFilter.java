package com.vedad.cinevo.model;

import com.vedad.cinevo.util.GenreCatalog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MovieFilter {

    public static final String SORT_POPULARITY = "popularity.desc";
    public static final String SORT_RATING = "vote_average.desc";
    public static final String SORT_DATE = "primary_release_date.desc";
    public static final String SORT_DATE_ASC = "primary_release_date.asc";

    public Set<Integer> genreIds = new LinkedHashSet<>();
    public Integer yearFrom;
    public Integer yearTo;
    public float minRating = 0f;
    public Integer runtimeMin;
    public Integer runtimeMax;
    public String sortBy = SORT_POPULARITY;
    public boolean hasFilters() {
        return !genreIds.isEmpty()
                || yearFrom != null || yearTo != null
                || minRating > 0f
                || runtimeMin != null || runtimeMax != null;
    }

    public boolean isActive() {
        return hasFilters() || !SORT_POPULARITY.equals(sortBy);
    }

    public static String sortLabel(String sortBy) {
        if (SORT_RATING.equals(sortBy)) return "Ocjena";
        if (SORT_DATE.equals(sortBy)) return "Najnoviji prvo";
        if (SORT_DATE_ASC.equals(sortBy)) return "Najstariji prvo";
        return "Popularnost";
    }

    public MovieFilter copy() {
        MovieFilter c = new MovieFilter();
        c.genreIds = new LinkedHashSet<>(genreIds);
        c.yearFrom = yearFrom;
        c.yearTo = yearTo;
        c.minRating = minRating;
        c.runtimeMin = runtimeMin;
        c.runtimeMax = runtimeMax;
        c.sortBy = sortBy;
        return c;
    }
    public Map<String, String> toQueryMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("sort_by", sortBy);

        if (!genreIds.isEmpty()) {
            List<String> ids = new ArrayList<>();
            for (int id : genreIds) ids.add(String.valueOf(id));
            map.put("with_genres", join(",", ids));
        }

        if (yearFrom != null) map.put("primary_release_date.gte", yearFrom + "-01-01");
        if (yearTo != null) {
            map.put("primary_release_date.lte", yearTo + "-12-31");
        } else if (SORT_DATE.equals(sortBy)) {
            map.put("primary_release_date.lte",
                    new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()));
        }

        if (minRating > 0f) map.put("vote_average.gte", String.valueOf(minRating));

        boolean dateSort = SORT_DATE.equals(sortBy) || SORT_DATE_ASC.equals(sortBy);
        int minVotes = SORT_RATING.equals(sortBy) ? 300 : (dateSort ? 50 : (minRating > 0f ? 50 : 0));
        if (minVotes > 0) map.put("vote_count.gte", String.valueOf(minVotes));

        if (runtimeMin != null) map.put("with_runtime.gte", String.valueOf(runtimeMin));
        if (runtimeMax != null) map.put("with_runtime.lte", String.valueOf(runtimeMax));

        return map;
    }

    public String describe() {
        List<String> parts = new ArrayList<>();

        if (!genreIds.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (int id : genreIds) names.add(GenreCatalog.nameOf(id));
            parts.add(join(", ", names));
        }
        if (yearFrom != null || yearTo != null) {
            parts.add(range(yearFrom, yearTo, ""));
        }
        if (minRating > 0f) {
            parts.add(String.format(Locale.US, "★ %.1f+", minRating));
        }
        if (runtimeMin != null || runtimeMax != null) {
            parts.add(range(runtimeMin, runtimeMax, " min"));
        }

        return join(" • ", parts);
    }

    private static String range(Integer from, Integer to, String suffix) {
        if (from != null && to != null) return from + "–" + to + suffix;
        if (from != null) return "od " + from + suffix;
        return "do " + to + suffix;
    }

    private static String join(String separator, Collection<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            if (sb.length() > 0) sb.append(separator);
            sb.append(item);
        }
        return sb.toString();
    }
}