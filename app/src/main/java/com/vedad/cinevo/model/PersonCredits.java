package com.vedad.cinevo.model;

import java.util.List;

public class PersonCredits {
    private List<Movie> cast;
    private List<CreditMovie> crew;

    public List<Movie> getCast() { return cast; }
    public List<CreditMovie> getCrew() { return crew; }

    public static class CreditMovie extends Movie {
        private String job;

        public String getJob() { return job; }
    }
}