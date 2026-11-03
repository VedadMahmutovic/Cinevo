package com.vedad.cinevo.ui.details;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.data.repository.SavedMovieRepository;
import com.vedad.cinevo.model.MovieDetails;

import java.util.ArrayList;
import java.util.List;

public class MovieDetailsViewModel extends AndroidViewModel {

    private final SavedMovieRepository repository;

    private List<MovieDetails.Cast> fullCast = new ArrayList<>();

    public MovieDetailsViewModel(@NonNull Application application) {
        super(application);
        repository = new SavedMovieRepository(application);
    }

    public LiveData<Integer> countOf(int movieId, String listType) {
        return repository.countOf(movieId, listType);
    }

    public void add(SavedMovie movie) {
        repository.add(movie);
    }

    public void remove(int movieId, String listType) {
        repository.remove(movieId, listType);
    }

    public LiveData<Integer> ratingOf(int movieId) {
        return repository.ratingOf(movieId);
    }

    public void setFullCast(List<MovieDetails.Cast> cast) {
        fullCast = new ArrayList<>(cast);
    }

    public List<MovieDetails.Cast> getFullCast() {
        return fullCast;
    }
}