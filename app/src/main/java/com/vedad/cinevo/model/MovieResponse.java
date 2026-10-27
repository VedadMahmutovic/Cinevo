package com.vedad.cinevo.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class MovieResponse {
    private int page;
    private List<Movie> results;

    public int getPage() { return page; }
    public List<Movie> getResults() { return results; }

    @SerializedName("total_pages")
    private int totalPages;

    public int getTotalPages() { return totalPages; }
}