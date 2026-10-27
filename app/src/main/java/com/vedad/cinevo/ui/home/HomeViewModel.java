package com.vedad.cinevo.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.vedad.cinevo.BuildConfig;
import com.vedad.cinevo.data.remote.RetrofitClient;
import com.vedad.cinevo.data.remote.TmdbApi;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.model.MovieResponse;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeViewModel extends ViewModel {

    private static final String LANGUAGE = "en-US";
    private static final int TMDB_MAX_PAGES = 500;

    private final TmdbApi api = RetrofitClient.getApi();

    private final MutableLiveData<List<Movie>> movies = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    private final List<Movie> allMovies = new ArrayList<>();
    private final Set<Integer> seenIds = new HashSet<>();

    private String query = "";
    private int page = 0;
    private int totalPages = 1;
    private boolean isLoading = false;
    private Call<MovieResponse> currentCall;

    public LiveData<List<Movie>> getMovies() { return movies; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getMessage() { return message; }
    public String getQuery() { return query; }

    public void clearMessage() {
        message.setValue(null);
    }

    public void loadFirstPageIfNeeded() {
        if (page == 0 && !isLoading) {
            loadNextPage();
        }
    }
    public void setQuery(String newQuery) {
        String q = newQuery == null ? "" : newQuery.trim();
        if (q.equals(query)) return;
        query = q;
        reload();
    }

    private void reload() {
        if (currentCall != null) currentCall.cancel();
        allMovies.clear();
        seenIds.clear();
        movies.setValue(new ArrayList<>());
        page = 0;
        totalPages = 1;
        isLoading = false;
        loading.setValue(false);
        loadNextPage();
    }

    public void loadNextPage() {
        if (isLoading || page >= totalPages) return;

        isLoading = true;
        loading.setValue(true);
        final int requestedPage = page + 1;

        Call<MovieResponse> call = query.isEmpty()
                ? api.getPopularMovies(BuildConfig.TMDB_API_KEY, LANGUAGE, requestedPage)
                : api.searchMovies(BuildConfig.TMDB_API_KEY, LANGUAGE, query, requestedPage);
        currentCall = call;

        call.enqueue(new Callback<MovieResponse>() {
            @Override
            public void onResponse(Call<MovieResponse> call, Response<MovieResponse> response) {
                if (call.isCanceled()) return;
                isLoading = false;
                loading.setValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    MovieResponse body = response.body();
                    page = requestedPage;
                    totalPages = Math.min(body.getTotalPages(), TMDB_MAX_PAGES);

                    if (body.getResults() != null) {
                        for (Movie m : body.getResults()) {
                            if (seenIds.add(m.getId())) {
                                allMovies.add(m);
                            }
                        }
                    }
                    movies.setValue(new ArrayList<>(allMovies));

                    if (allMovies.isEmpty() && !query.isEmpty()) {
                        message.setValue("Nema rezultata za: " + query);
                    }
                } else {
                    message.setValue("Greška: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<MovieResponse> call, Throwable t) {
                if (call.isCanceled()) return;
                isLoading = false;
                loading.setValue(false);
                message.setValue("Nema veze s internetom");
            }
        });
    }

    @Override
    protected void onCleared() {
        if (currentCall != null) currentCall.cancel();
    }
}