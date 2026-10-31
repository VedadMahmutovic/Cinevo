package com.vedad.cinevo.util;

public class RuntimeFormatter {
    public static String format(int minutes) {
        if (minutes < 60) return minutes + " min";
        int hours = minutes / 60;
        int rest = minutes % 60;
        return rest == 0 ? hours + "h" : hours + "h " + rest + "min";
    }
}