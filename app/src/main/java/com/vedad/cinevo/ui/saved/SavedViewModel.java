package com.vedad.cinevo.ui.saved;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.data.repository.SavedMovieRepository;

import java.util.List;

public class SavedViewModel extends AndroidViewModel {

    private final MutableLiveData<String> selectedType =
            new MutableLiveData<>(SavedMovie.FAVORITE);
    private final LiveData<List<SavedMovie>> saved;

    public SavedViewModel(@NonNull Application application) {
        super(application);
        SavedMovieRepository repository = new SavedMovieRepository(application);
        saved = Transformations.switchMap(selectedType, type -> repository.getSaved(type));
    }

    public LiveData<List<SavedMovie>> getSaved() {
        return saved;
    }

    public String getSelectedType() {
        return selectedType.getValue();
    }

    public void selectType(String type) {
        selectedType.setValue(type);
    }
}