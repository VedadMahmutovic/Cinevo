package com.vedad.cinevo.util;

public class RatingFormatter {
    public static String stars(int halfStars) {
        return halfStars % 2 == 0
                ? String.valueOf(halfStars / 2)
                : String.valueOf(halfStars / 2.0);
    }
}