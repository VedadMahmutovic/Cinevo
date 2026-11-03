package com.vedad.cinevo.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SavedMovieDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SavedMovie movie);

    @Query("DELETE FROM saved_movies WHERE id = :id AND listType = :listType")
    void delete(int id, String listType);

    @Query("SELECT * FROM saved_movies WHERE listType = :listType ORDER BY addedAt DESC")
    LiveData<List<SavedMovie>> getByType(String listType);

    @Query("SELECT COUNT(*) FROM saved_movies WHERE id = :id AND listType = :listType")
    LiveData<Integer> countOf(int id, String listType);

    @Query("SELECT * FROM saved_movies ORDER BY addedAt DESC")
    List<SavedMovie> getAll();

    @Query("DELETE FROM saved_movies")
    void deleteAll();

    @Query("SELECT userRating FROM saved_movies WHERE id = :id AND listType = 'WATCHED'")
    LiveData<Integer> ratingOf(int id);

    @Query("UPDATE saved_movies SET directorIds = :directorIds WHERE id = :id")
    void updateDirectors(int id, String directorIds);
}