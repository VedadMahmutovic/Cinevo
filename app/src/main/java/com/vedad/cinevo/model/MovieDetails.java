package com.vedad.cinevo.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public class MovieDetails {
    private int id;
    private String title;
    private String tagline;
    private String overview;
    private int runtime;
    private Recommendations recommendations;

    @SerializedName("poster_path")
    private String posterPath;

    @SerializedName("backdrop_path")
    private String backdropPath;

    @SerializedName("vote_average")
    private double voteAverage;

    @SerializedName("release_date")
    private String releaseDate;

    @SerializedName("watch/providers")
    private WatchProviders watchProviders;

    private List<Genre> genres;
    private Videos videos;
    private Credits credits;

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getTagline() { return tagline; }
    public String getOverview() { return overview; }
    public int getRuntime() { return runtime; }
    public String getPosterPath() { return posterPath; }
    public String getBackdropPath() { return backdropPath; }
    public double getVoteAverage() { return voteAverage; }
    public String getReleaseDate() { return releaseDate; }
    public List<Genre> getGenres() { return genres; }
    public Videos getVideos() { return videos; }
    public Credits getCredits() { return credits; }
    public Recommendations getRecommendations() { return recommendations; }
    public WatchProviders getWatchProviders() { return watchProviders; }

    public static class Genre {
        private int id;
        private String name;

        public int getId() { return id; }
        public String getName() { return name; }
    }

    public static class Videos {
        private List<Video> results;

        public List<Video> getResults() { return results; }
    }

    public static class Video {
        private String key;
        private String site;
        private String type;

        public String getKey() { return key; }
        public String getSite() { return site; }
        public String getType() { return type; }
    }

    public static class Credits {
        private List<Cast> cast;

        public List<Cast> getCast() { return cast; }
    }

    public static class WatchProviders {
        private Map<String, RegionProviders> results;

        public Map<String, RegionProviders> getResults() { return results; }
    }

    public static class RegionProviders {
        private String link;
        private List<Provider> flatrate;
        private List<Provider> rent;
        private List<Provider> buy;

        public String getLink() { return link; }
        public List<Provider> getFlatrate() { return flatrate; }
        public List<Provider> getRent() { return rent; }
        public List<Provider> getBuy() { return buy; }
    }

    public static class Provider {
        @SerializedName("provider_name")
        private String name;

        public String getName() { return name; }

        @SerializedName("logo_path")
        private String logoPath;

        public String getLogoPath() { return logoPath; }
    }

    public static class Cast {
        private String name;

        public String getName() { return name; }
    }

    public static class Recommendations {
        private List<Movie> results;

        public List<Movie> getResults() { return results; }
    }
}