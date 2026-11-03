package com.vedad.cinevo.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "saved_movies", primaryKeys = {"id", "listType"})
public class SavedMovie {

    public static final String FAVORITE = "FAVORITE";
    public static final String WATCHLIST = "WATCHLIST";
    public static final String WATCHED = "WATCHED";

    public int id;

    @NonNull
    public String listType = FAVORITE;

    public String title;
    public String posterPath;
    public double voteAverage;
    public String releaseDate;
    public String overview;
    public long addedAt;
    public String genreIds;

    public String directorIds;

    @ColumnInfo(defaultValue = "0")
    public int userRating;
}