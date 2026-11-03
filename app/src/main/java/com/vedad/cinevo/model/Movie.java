package com.vedad.cinevo.model;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class Movie {
    private int id;
    private String title;
    private String overview;
    private transient int userRating;
    private double popularity;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("vote_average")
    private double voteAverage;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("genre_ids")
    private List<Integer> genreIds;

    @SerializedName("vote_count")
    private int voteCount;

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getOverview() { return overview; }
    public String getPosterPath() { return posterPath; }
    public double getPopularity() { return popularity; }
    public double getVoteAverage() { return voteAverage; }
    public int getUserRating() { return userRating; }
    public String getReleaseDate() { return releaseDate; }
    public List<Integer> getGenreIds() {
        return genreIds != null ? genreIds : Collections.emptyList();
    }
    public void setGenreIds(List<Integer> genreIds) {
        this.genreIds = genreIds;
    }

    public Movie() {
    }
    public Movie(int id, String title, String overview, String posterPath,
                 double voteAverage, String releaseDate) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.posterPath = posterPath;
        this.voteAverage = voteAverage;
        this.releaseDate = releaseDate;
    }
    public void setUserRating(int userRating) { this.userRating = userRating; }
}
