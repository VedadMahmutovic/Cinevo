package com.vedad.cinevo.util;

import android.content.Context;

import com.vedad.cinevo.R;

import java.util.HashMap;
import java.util.Map;

public class GenreLabels {

    private static final Map<Integer, Integer> RES = new HashMap<>();

    static {
        RES.put(28, R.string.genre_28);
        RES.put(12, R.string.genre_12);
        RES.put(16, R.string.genre_16);
        RES.put(35, R.string.genre_35);
        RES.put(80, R.string.genre_80);
        RES.put(99, R.string.genre_99);
        RES.put(18, R.string.genre_18);
        RES.put(10751, R.string.genre_10751);
        RES.put(14, R.string.genre_14);
        RES.put(36, R.string.genre_36);
        RES.put(27, R.string.genre_27);
        RES.put(10402, R.string.genre_10402);
        RES.put(9648, R.string.genre_9648);
        RES.put(10749, R.string.genre_10749);
        RES.put(878, R.string.genre_878);
        RES.put(10770, R.string.genre_10770);
        RES.put(53, R.string.genre_53);
        RES.put(10752, R.string.genre_10752);
        RES.put(37, R.string.genre_37);
    }

    public static String of(Context context, int genreId) {
        Integer res = RES.get(genreId);
        return res != null ? context.getString(res) : String.valueOf(genreId);
    }

    public static String of(Context context, int genreId, String fallback) {
        Integer res = RES.get(genreId);
        return res != null ? context.getString(res) : fallback;
    }
}