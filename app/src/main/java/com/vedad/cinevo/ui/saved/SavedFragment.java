package com.vedad.cinevo.ui.saved;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.tabs.TabLayout;
import com.vedad.cinevo.data.local.SavedMovie;
import com.vedad.cinevo.databinding.FragmentSavedBinding;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.ui.details.MovieDetailsActivity;
import com.vedad.cinevo.ui.home.MovieAdapter;
import com.vedad.cinevo.util.AppSettings;

import java.util.ArrayList;
import java.util.List;

public class SavedFragment extends Fragment {

    private static final String[] TYPES = {
            SavedMovie.FAVORITE, SavedMovie.WATCHLIST, SavedMovie.WATCHED
    };

    private FragmentSavedBinding binding;
    private SavedViewModel viewModel;
    private MovieAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSavedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(SavedViewModel.class);

        adapter = new MovieAdapter();
        binding.recyclerSaved.setLayoutManager(new GridLayoutManager(
                requireContext(), new AppSettings(requireContext()).getGridColumns()));
        binding.recyclerSaved.setAdapter(adapter);

        adapter.setOnMovieClickListener(movie -> {
            Intent intent = new Intent(requireContext(), MovieDetailsActivity.class);
            intent.putExtra(MovieDetailsActivity.EXTRA_MOVIE_ID, movie.getId());
            startActivity(intent);
        });

        int startTab = 0;
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(viewModel.getSelectedType())) startTab = i;
        }
        TabLayout.Tab tab = binding.tabLayout.getTabAt(startTab);
        if (tab != null) tab.select();

        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewModel.selectType(TYPES[tab.getPosition()]);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        viewModel.getSaved().observe(getViewLifecycleOwner(), list -> {
            List<Movie> movies = new ArrayList<>();
            for (SavedMovie s : list) {
                Movie m = new Movie(s.id, s.title, s.overview,
                        s.posterPath, s.voteAverage, s.releaseDate);
                m.setUserRating(s.userRating);
                movies.add(m);
            }
            adapter.setMovies(movies);

            binding.tvEmpty.setText(emptyMessage(viewModel.getSelectedType()));
            binding.tvEmpty.setVisibility(movies.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private String emptyMessage(String type) {
        if (SavedMovie.WATCHLIST.equals(type)) return "Lista za gledanje je prazna.";
        if (SavedMovie.WATCHED.equals(type)) return "Još nisi ocijenio nijedan film.";
        return "Još nemaš favorita.";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}