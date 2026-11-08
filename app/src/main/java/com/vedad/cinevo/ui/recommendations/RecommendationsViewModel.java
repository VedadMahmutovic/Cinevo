package com.vedad.cinevo.ui.recommendations;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.vedad.cinevo.R;
import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.data.repository.RecommendationRepository;
import com.vedad.cinevo.data.repository.SavedMovieRepository;
import com.vedad.cinevo.model.Movie;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecommendationsViewModel extends AndroidViewModel {

    private final RecommendationRepository repository;
    private final SavedMovieRepository savedRepository;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<List<Movie>> recommendations = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public RecommendationsViewModel(@NonNull Application application) {
        super(application);
        repository = new RecommendationRepository(application);
        savedRepository = new SavedMovieRepository(application);
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
                error.postValue(getApplication().getString(R.string.error_no_internet));
            } finally {
                loading.postValue(false);
            }
        });
    }

    public void dismiss(Movie movie) {
        List<Movie> current = recommendations.getValue();
        if (current == null) return;

        List<Movie> updated = new ArrayList<>(current);
        updated.remove(movie);
        recommendations.setValue(updated);

        SavedMovie row = new SavedMovie();
        row.id = movie.getId();
        row.listType = SavedMovie.DISMISSED;
        row.title = movie.getTitle();
        row.posterPath = movie.getPosterPath();
        row.voteAverage = movie.getVoteAverage();
        row.releaseDate = movie.getReleaseDate();
        row.overview = movie.getOverview();
        row.genreIds = TextUtils.join(",", movie.getGenreIds());
        row.addedAt = System.currentTimeMillis();
        savedRepository.add(row);
    }

    public void undoDismiss(Movie movie, int position) {
        savedRepository.remove(movie.getId(), SavedMovie.DISMISSED);

        List<Movie> current = recommendations.getValue();
        if (current == null) return;

        List<Movie> updated = new ArrayList<>(current);
        updated.add(Math.max(0, Math.min(position, updated.size())), movie);
        recommendations.setValue(updated);
    }

    @Override
    protected void onCleared() {
        executor.shutdown();
    }
}