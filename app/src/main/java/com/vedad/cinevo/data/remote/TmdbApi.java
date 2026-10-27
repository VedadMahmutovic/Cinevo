package com.vedad.cinevo.data.remote;

import com.vedad.cinevo.model.MovieDetails;
import com.vedad.cinevo.model.MovieResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface TmdbApi {

    @GET("movie/popular")
    Call<MovieResponse> getPopularMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("page") int page
    );

    @GET("movie/{movie_id}")
    Call<MovieDetails> getMovieDetails(
            @Path("movie_id") int movieId,
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("append_to_response") String appendToResponse
    );

    @GET("search/movie")
    Call<MovieResponse> searchMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("query") String query,
            @Query("page") int page
    );

    @GET("discover/movie")
    Call<MovieResponse> discoverMovies(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("with_genres") String withGenres,
            @Query("sort_by") String sortBy,
            @Query("vote_count.gte") int minVoteCount,
            @Query("page") int page
    );

    @GET("movie/{movie_id}/recommendations")
    Call<MovieResponse> getRecommendations(
            @Path("movie_id") int movieId,
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @Query("page") int page
    );

    @GET("discover/movie")
    Call<MovieResponse> discoverWithFilters(
            @Query("api_key") String apiKey,
            @Query("language") String language,
            @QueryMap Map<String, String> filters,
            @Query("page") int page
    );
}