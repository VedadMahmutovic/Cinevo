package com.vedad.cinevo.util;

import android.text.TextUtils;

import com.vedad.cinevo.model.MovieDetails;

import java.util.ArrayList;
import java.util.List;

public class GenreUtils {

    public static String fromGenres(List<MovieDetails.Genre> genres) {
        if (genres == null) return "";
        List<String> ids = new ArrayList<>();
        for (MovieDetails.Genre g : genres) {
            ids.add(String.valueOf(g.getId()));
        }
        return TextUtils.join(",", ids);
    }
    public static List<Integer> parse(String csv) {
        List<Integer> result = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) return result;
        for (String part : csv.split(",")) {
            try {
                result.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }
}