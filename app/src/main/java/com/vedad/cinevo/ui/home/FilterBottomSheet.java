package com.vedad.cinevo.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.vedad.cinevo.databinding.BottomSheetFilterBinding;
import com.vedad.cinevo.model.MovieFilter;
import com.vedad.cinevo.util.GenreCatalog;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FilterBottomSheet extends BottomSheetDialogFragment {

    private static final int YEAR_MIN = 1950;
    private static final int RUNTIME_MAX = 240;

    private BottomSheetFilterBinding binding;
    private HomeViewModel viewModel;
    private int yearMax;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetFilterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);
        MovieFilter current = viewModel.getFilter();
        yearMax = Calendar.getInstance().get(Calendar.YEAR);

        setupGenres(current);
        setupYears(current);
        setupRating(current);
        setupRuntime(current);
        setupSort(current);
        updateLabels();

        binding.btnReset.setOnClickListener(v -> {
            viewModel.applyFilter(new MovieFilter());
            dismiss();
        });
        binding.btnApply.setOnClickListener(v -> applyFilter());
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetBehavior<?> behavior = ((BottomSheetDialog) getDialog()).getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }
    private void setupGenres(MovieFilter current) {
        for (Map.Entry<Integer, String> entry : GenreCatalog.all().entrySet()) {
            Chip chip = new Chip(requireContext());
            chip.setText(entry.getValue());
            chip.setTag(entry.getKey());
            chip.setCheckable(true);
            chip.setCheckedIconVisible(true);
            chip.setChecked(current.genreIds.contains(entry.getKey()));
            binding.chipGenres.addView(chip);
        }
    }

    private void setupYears(MovieFilter current) {
        binding.sliderYears.setValueFrom(YEAR_MIN);
        binding.sliderYears.setValueTo(yearMax);
        float from = current.yearFrom != null ? current.yearFrom : YEAR_MIN;
        float to = current.yearTo != null ? current.yearTo : yearMax;
        binding.sliderYears.setValues(from, to);
        binding.sliderYears.addOnChangeListener((slider, value, fromUser) -> updateLabels());
    }

    private void setupRating(MovieFilter current) {
        binding.sliderRating.setValue(current.minRating);
        binding.sliderRating.addOnChangeListener((slider, value, fromUser) -> updateLabels());
    }

    private void setupRuntime(MovieFilter current) {
        float min = current.runtimeMin != null ? current.runtimeMin : 0;
        float max = current.runtimeMax != null ? current.runtimeMax : RUNTIME_MAX;
        binding.sliderRuntime.setValues(min, max);
        binding.sliderRuntime.addOnChangeListener((slider, value, fromUser) -> updateLabels());
    }

    private void setupSort(MovieFilter current) {
        if (MovieFilter.SORT_RATING.equals(current.sortBy)) {
            binding.chipSortRating.setChecked(true);
        } else if (MovieFilter.SORT_DATE.equals(current.sortBy)) {
            binding.chipSortDate.setChecked(true);
        } else {
            binding.chipSortPopularity.setChecked(true);
        }
    }
    private void updateLabels() {
        List<Float> years = binding.sliderYears.getValues();
        binding.tvYears.setText(String.format(Locale.US, "Godina izdanja: %d – %d",
                Math.round(years.get(0)), Math.round(years.get(1))));

        float rating = binding.sliderRating.getValue();
        binding.tvRating.setText(rating <= 0f
                ? "Minimalna ocjena: bilo koja"
                : String.format(Locale.US, "Minimalna ocjena: %.1f+", rating));

        List<Float> runtime = binding.sliderRuntime.getValues();
        int rMin = Math.round(runtime.get(0));
        int rMax = Math.round(runtime.get(1));
        if (rMin == 0 && rMax == RUNTIME_MAX) {
            binding.tvRuntime.setText("Trajanje: bilo koje");
        } else {
            binding.tvRuntime.setText(String.format(Locale.US, "Trajanje: %d – %s min",
                    rMin, rMax == RUNTIME_MAX ? RUNTIME_MAX + "+" : String.valueOf(rMax)));
        }
    }
    private void applyFilter() {
        MovieFilter f = new MovieFilter();

        for (int i = 0; i < binding.chipGenres.getChildCount(); i++) {
            Chip chip = (Chip) binding.chipGenres.getChildAt(i);
            if (chip.isChecked()) {
                f.genreIds.add((Integer) chip.getTag());
            }
        }

        List<Float> years = binding.sliderYears.getValues();
        int yFrom = Math.round(years.get(0));
        int yTo = Math.round(years.get(1));
        f.yearFrom = yFrom > YEAR_MIN ? yFrom : null;
        f.yearTo = yTo < yearMax ? yTo : null;

        f.minRating = binding.sliderRating.getValue();

        List<Float> runtime = binding.sliderRuntime.getValues();
        int rMin = Math.round(runtime.get(0));
        int rMax = Math.round(runtime.get(1));
        f.runtimeMin = rMin > 0 ? rMin : null;
        f.runtimeMax = rMax < RUNTIME_MAX ? rMax : null;

        int checkedSort = binding.chipSort.getCheckedChipId();
        if (checkedSort == binding.chipSortRating.getId()) {
            f.sortBy = MovieFilter.SORT_RATING;
        } else if (checkedSort == binding.chipSortDate.getId()) {
            f.sortBy = MovieFilter.SORT_DATE;
        } else {
            f.sortBy = MovieFilter.SORT_POPULARITY;
        }

        viewModel.applyFilter(f);
        dismiss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}