package com.vedad.cinevo.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.vedad.cinevo.databinding.DialogAboutBinding;
import com.vedad.cinevo.databinding.FragmentHomeBinding;
import com.vedad.cinevo.model.MovieFilter;
import com.vedad.cinevo.ui.details.MovieDetailsActivity;

public class HomeFragment extends Fragment {

    private static final int LOAD_MORE_THRESHOLD = 6;

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private MovieAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(HomeViewModel.class);

        adapter = new MovieAdapter();
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
        binding.recyclerMovies.setLayoutManager(layoutManager);
        binding.recyclerMovies.setAdapter(adapter);

        binding.recyclerMovies.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                int total = layoutManager.getItemCount();
                int lastVisible = layoutManager.findLastVisibleItemPosition();
                if (lastVisible >= total - LOAD_MORE_THRESHOLD) {
                    viewModel.loadNextPage();
                }
            }
        });

        adapter.setOnMovieClickListener(movie -> {
            Intent intent = new Intent(requireContext(), MovieDetailsActivity.class);
            intent.putExtra(MovieDetailsActivity.EXTRA_MOVIE_ID, movie.getId());
            startActivity(intent);
        });

        binding.searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                viewModel.setQuery(query);
                binding.searchView.clearFocus();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    viewModel.setQuery("");
                }
                return true;
            }
        });

        binding.btnFilter.setOnClickListener(v ->
                new FilterBottomSheet().show(getParentFragmentManager(), "filters"));

        binding.btnSettings.setOnClickListener(v -> showAboutDialog());

        binding.tvActiveFilters.setOnClickListener(v -> viewModel.applyFilter(new MovieFilter()));

        viewModel.getQueryLive().observe(getViewLifecycleOwner(), text -> {
            if (!binding.searchView.getQuery().toString().equals(text)) {
                binding.searchView.setQuery(text, false);
            }
        });

        viewModel.getFilterLive().observe(getViewLifecycleOwner(), f -> {
            if (f.isActive()) {
                binding.tvActiveFilters.setText("Filteri: " + f.describe() + "   ✕");
                binding.tvActiveFilters.setVisibility(View.VISIBLE);
            } else {
                binding.tvActiveFilters.setVisibility(View.GONE);
            }
        });

        viewModel.getMovies().observe(getViewLifecycleOwner(), movies -> adapter.setMovies(movies));

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading ->
                binding.progressPage.setVisibility(loading ? View.VISIBLE : View.GONE));

        viewModel.getMessage().observe(getViewLifecycleOwner(), text -> {
            if (text != null) {
                Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        viewModel.loadFirstPageIfNeeded();
    }

    private void showAboutDialog() {
        DialogAboutBinding dialogBinding = DialogAboutBinding.inflate(getLayoutInflater());
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("O aplikaciji")
                .setView(dialogBinding.getRoot())
                .setPositiveButton("U redu", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}