package com.vedad.cinevo.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.PreferenceManager;

public class AppSettings {

    public static final String KEY_THEME = "pref_theme";
    public static final String KEY_MIN_RATING = "pref_min_rating";
    public static final String KEY_MIN_VOTES = "pref_min_votes";
    private static final String KEY_GRID_COLUMNS = "pref_grid_columns";
    public static final String KEY_WATCH_REGION = "pref_watch_region";

    private final SharedPreferences prefs;

    public AppSettings(Context context) {
        prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
    }

    public static int themeModeFor(String value) {
        if ("light".equals(value)) return AppCompatDelegate.MODE_NIGHT_NO;
        if ("dark".equals(value)) return AppCompatDelegate.MODE_NIGHT_YES;
        return AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
    }

    public int getThemeMode() {
        return themeModeFor(prefs.getString(KEY_THEME, "system"));
    }

    public double getMinRating() {
        return prefs.getInt(KEY_MIN_RATING, 6);
    }

    public int getMinVotes() {
        return prefs.getInt(KEY_MIN_VOTES, 100);
    }

    public int getGridColumns() {
        return prefs.getInt(KEY_GRID_COLUMNS, 2);
    }

    public void setGridColumns(int columns) {
        prefs.edit().putInt(KEY_GRID_COLUMNS, columns).apply();
    }

    public String getWatchRegion() {
        return prefs.getString(KEY_WATCH_REGION, "BA");
    }
}