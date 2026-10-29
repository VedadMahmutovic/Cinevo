package com.vedad.cinevo.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.vedad.cinevo.databinding.FragmentHomeBinding;
import com.vedad.cinevo.model.MovieFilter;
import com.vedad.cinevo.ui.details.MovieDetailsActivity;
import com.vedad.cinevo.ui.settings.SettingsActivity;
import com.vedad.cinevo.util.AppSettings;

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
        AppSettings settings = new AppSettings(requireContext());

        adapter = new MovieAdapter();
        GridLayoutManager layoutManager =
                new GridLayoutManager(requireContext(), settings.getGridColumns());
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

        binding.btnFilter.setOnClickListener(v -> openFilters());

        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        binding.btnGrid.setOnClickListener(v -> {
            int next = layoutManager.getSpanCount() == 2 ? 3 : 2;
            layoutManager.setSpanCount(next);
            settings.setGridColumns(next);
        });

        binding.btnSort.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), binding.btnSort);
            popup.getMenu().add(0, 1, 0, MovieFilter.sortLabel(MovieFilter.SORT_POPULARITY));
            popup.getMenu().add(0, 2, 1, MovieFilter.sortLabel(MovieFilter.SORT_RATING));
            popup.getMenu().add(0, 3, 2, MovieFilter.sortLabel(MovieFilter.SORT_DATE));
            popup.getMenu().add(0, 4, 3, MovieFilter.sortLabel(MovieFilter.SORT_DATE_ASC));
            popup.setOnMenuItemClickListener(item -> {
                switch (item.getItemId()) {
                    case 2:
                        viewModel.setSort(MovieFilter.SORT_RATING);
                        break;
                    case 3:
                        viewModel.setSort(MovieFilter.SORT_DATE);
                        break;
                    case 4:
                        viewModel.setSort(MovieFilter.SORT_DATE_ASC);
                        break;
                    default:
                        viewModel.setSort(MovieFilter.SORT_POPULARITY);
                }
                return true;
            });
            popup.show();
        });

        binding.chipActiveFilters.setOnClickListener(v -> openFilters());
        binding.chipActiveFilters.setOnCloseIconClickListener(v -> viewModel.clearFilters());

        viewModel.getQueryLive().observe(getViewLifecycleOwner(), text -> {
            if (!binding.searchView.getQuery().toString().equals(text)) {
                binding.searchView.setQuery(text, false);
            }
        });

        viewModel.getFilterLive().observe(getViewLifecycleOwner(), f -> {
            binding.btnSort.setText("Sortiraj: " + MovieFilter.sortLabel(f.sortBy) + " ▾");
            if (f.hasFilters()) {
                binding.chipActiveFilters.setText("Filteri: " + f.describe());
                binding.chipActiveFilters.setVisibility(View.VISIBLE);
            } else {
                binding.chipActiveFilters.setVisibility(View.GONE);
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

    private void openFilters() {
        new FilterBottomSheet().show(getParentFragmentManager(), "filters");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}