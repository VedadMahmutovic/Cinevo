package com.vedad.cinevo.data.repository;

import com.vedad.cinevo.BuildConfig;
import com.vedad.cinevo.data.remote.RetrofitClient;
import com.vedad.cinevo.model.MovieDetails;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RuntimeRepository {

    public interface Listener {
        void onRuntime(int minutes);
    }

    private static final Map<Integer, Integer> CACHE = new ConcurrentHashMap<>();

    public static Integer getCached(int movieId) {
        return CACHE.get(movieId);
    }

    public static void put(int movieId, int runtime) {
        if (runtime > 0) CACHE.put(movieId, runtime);
    }

    public static Call<MovieDetails> fetch(int movieId, Listener listener) {
        Call<MovieDetails> call = RetrofitClient.getApi()
                .getMovieDetails(movieId, BuildConfig.TMDB_API_KEY, "en-US", null);

        call.enqueue(new Callback<MovieDetails>() {
            @Override
            public void onResponse(Call<MovieDetails> call, Response<MovieDetails> response) {
                if (call.isCanceled()) return;
                if (response.isSuccessful() && response.body() != null
                        && response.body().getRuntime() > 0) {
                    int runtime = response.body().getRuntime();
                    put(movieId, runtime);
                    listener.onRuntime(runtime);
                }
            }

            @Override
            public void onFailure(Call<MovieDetails> call, Throwable t) {
            }
        });
        return call;
    }
}