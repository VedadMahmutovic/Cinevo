package com.vedad.cinevo.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.vedad.cinevo.data.local.AppDatabase;
import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.data.local.SavedMovieDao;

import java.util.List;

public class SavedMovieRepository {

    private final SavedMovieDao dao;

    public SavedMovieRepository(Context context) {
        dao = AppDatabase.getInstance(context).savedMovieDao();
    }

    public LiveData<List<SavedMovie>> getSaved(String listType) {
        return dao.getByType(listType);
    }

    public LiveData<Integer> countOf(int id, String listType) {
        return dao.countOf(id, listType);
    }

    public void add(SavedMovie movie) {
        AppDatabase.databaseExecutor.execute(() -> dao.insert(movie));
    }

    public void remove(int id, String listType) {
        AppDatabase.databaseExecutor.execute(() -> dao.delete(id, listType));
    }
}