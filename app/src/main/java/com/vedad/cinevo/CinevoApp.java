package com.vedad.cinevo;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

import com.vedad.cinevo.util.AppSettings;

public class CinevoApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppCompatDelegate.setDefaultNightMode(new AppSettings(this).getThemeMode());
    }
}