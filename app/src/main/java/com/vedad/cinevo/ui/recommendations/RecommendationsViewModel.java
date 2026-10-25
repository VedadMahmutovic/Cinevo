package com.vedad.cinevo.ui.recommendations;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.vedad.cinevo.data.repository.RecommendationRepository;
import com.vedad.cinevo.model.Movie;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecommendationsViewModel extends AndroidViewModel {

    private final RecommendationRepository repository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<Movie>> recommendations = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public RecommendationsViewModel(@NonNull Application application) {
        super(application);
        repository = new RecommendationRepository(application);
    }

    public LiveData<List<Movie>> getRecommendations() { return recommendations; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    public void load() {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        loading.setValue(true);
        error.setValue(null);

        executor.execute(() -> {
            try {
                recommendations.postValue(repository.compute());
            } catch (IOException e) {
                error.postValue("Nema veze s internetom");
            } finally {
                loading.postValue(false);
            }
        });
    }

    @Override
    protected void onCleared() {
        executor.shutdown();
    }
}