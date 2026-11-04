package com.vedad.cinevo.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PersonSearchResponse {
    private List<Person> results;

    public List<Person> getResults() { return results; }

    public static class Person {
        private int id;
        private String name;

        @SerializedName("profile_path")
        private String profilePath;

        @SerializedName("known_for_department")
        private String knownForDepartment;

        public int getId() { return id; }
        public String getName() { return name; }
        public String getProfilePath() { return profilePath; }
        public String getKnownForDepartment() { return knownForDepartment; }
    }
}