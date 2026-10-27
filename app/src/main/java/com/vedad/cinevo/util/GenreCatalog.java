package com.vedad.cinevo.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class GenreCatalog {

    private static final Map<Integer, String> GENRES = new LinkedHashMap<>();

    static {
        GENRES.put(28, "Akcija");
        GENRES.put(12, "Avantura");
        GENRES.put(16, "Animirani");
        GENRES.put(35, "Komedija");
        GENRES.put(80, "Krimi");
        GENRES.put(99, "Dokumentarni");
        GENRES.put(18, "Drama");
        GENRES.put(10751, "Porodični");
        GENRES.put(14, "Fantazija");
        GENRES.put(36, "Historijski");
        GENRES.put(27, "Horor");
        GENRES.put(10402, "Muzički");
        GENRES.put(9648, "Misterija");
        GENRES.put(10749, "Romantika");
        GENRES.put(878, "Naučna fantastika");
        GENRES.put(10770, "TV film");
        GENRES.put(53, "Triler");
        GENRES.put(10752, "Ratni");
        GENRES.put(37, "Vestern");
    }

    public static Map<Integer, String> all() {
        return Collections.unmodifiableMap(GENRES);
    }

    public static String nameOf(int id) {
        String name = GENRES.get(id);
        return name != null ? name : String.valueOf(id);
    }
}