package com.vedad.cinevo.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;
import com.vedad.cinevo.R;
import com.vedad.cinevo.data.repository.SavedMovieRepository;
import com.vedad.cinevo.databinding.FragmentHomeBinding;
import com.vedad.cinevo.model.MovieFilter;
import com.vedad.cinevo.ui.details.MovieDetailsActivity;
import com.vedad.cinevo.ui.person.PersonMoviesActivity;
import com.vedad.cinevo.ui.settings.SettingsActivity;
import com.vedad.cinevo.util.AppSettings;

public class HomeFragment extends Fragment {

    private static final int LOAD_MORE_THRESHOLD = 6;
    private static final long SEARCH_DEBOUNCE_MS = 400;
    private static final int MIN_QUERY_LENGTH = 2;

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;
    private MovieAdapter adapter;
    private GridLayoutManager layoutManager;
    private boolean isLoadingNow = false;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private PeopleAdapter peopleAdapter;

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

        setupList(settings);
        setupSearch();
        setupControls(settings);
        observeViewModel();

        viewModel.loadFirstPageIfNeeded();
    }

    private void setupList(AppSettings settings) {
        adapter = new MovieAdapter();
        adapter.observeStatuses(getViewLifecycleOwner(),
                new SavedMovieRepository(requireContext()).getStatuses());
        layoutManager = new GridLayoutManager(requireContext(), settings.getGridColumns());
        binding.recyclerMovies.setLayoutManager(layoutManager);
        binding.recyclerMovies.setAdapter(adapter);

        binding.recyclerMovies.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView rv, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    hideKeyboard();
                }
            }

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

        peopleAdapter = new PeopleAdapter();
        binding.recyclerPeople.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerPeople.setAdapter(peopleAdapter);
        peopleAdapter.setOnPersonClickListener(person -> {
            String role = "Directing".equals(person.getKnownForDepartment())
                    ? PersonMoviesActivity.ROLE_DIRECTOR
                    : PersonMoviesActivity.ROLE_ACTOR;

            Intent intent = new Intent(requireContext(), PersonMoviesActivity.class);
            intent.putExtra(PersonMoviesActivity.EXTRA_ID, person.getId());
            intent.putExtra(PersonMoviesActivity.EXTRA_NAME, person.getName());
            intent.putExtra(PersonMoviesActivity.EXTRA_PHOTO, person.getProfilePath());
            intent.putExtra(PersonMoviesActivity.EXTRA_ROLE, role);
            startActivity(intent);
        });
    }

    private void setupSearch() {
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                binding.btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                cancelPendingSearch();

                String text = s.toString().trim();
                if (text.isEmpty()) {
                    viewModel.setQuery("");
                } else if (text.length() >= MIN_QUERY_LENGTH) {
                    pendingSearch = () -> viewModel.setQuery(text);
                    searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
                }
            }
        });

        binding.editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                cancelPendingSearch();
                viewModel.setQuery(binding.editSearch.getText().toString());
                hideKeyboard();
                return true;
            }
            return false;
        });

        binding.btnClearSearch.setOnClickListener(v -> binding.editSearch.setText(""));
    }

    private void cancelPendingSearch() {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
            pendingSearch = null;
        }
    }

    private void hideKeyboard() {
        if (binding == null) return;
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(binding.editSearch.getWindowToken(), 0);
        binding.editSearch.clearFocus();
    }

    private void setupControls(AppSettings settings) {
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), SettingsActivity.class)));

        binding.btnGrid.setImageResource(
                settings.getGridColumns() == 3 ? R.drawable.ic_grid_3 : R.drawable.ic_grid);

        binding.btnGrid.setOnClickListener(v -> {
            int next = layoutManager.getSpanCount() == 2 ? 3 : 2;
            layoutManager.setSpanCount(next);
            settings.setGridColumns(next);

            animateGridIcon(next == 3);

            binding.recyclerMovies.animate().cancel();
            binding.recyclerMovies.setAlpha(0.4f);
            binding.recyclerMovies.animate().alpha(1f).setDuration(220).start();
        });

        binding.chipFilters.setOnClickListener(v -> openFilters());
        binding.chipFilters.setOnCloseIconClickListener(v -> viewModel.clearFilters());

        binding.chipSort.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), binding.chipSort);
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

        binding.btnRetry.setOnClickListener(v -> viewModel.retry());
    }

    private void openFilters() {
        new FilterBottomSheet().show(getParentFragmentManager(), "filters");
    }

    private void observeViewModel() {
        viewModel.getQueryLive().observe(getViewLifecycleOwner(), text -> {
            if (!binding.editSearch.getText().toString().trim().equals(text)) {
                binding.editSearch.setText(text);
                binding.editSearch.setSelection(text.length());
            }
        });

        viewModel.getFilterLive().observe(getViewLifecycleOwner(), f -> {
            binding.chipSort.setText(MovieFilter.sortShortLabel(f.sortBy) + " ▾");

            int count = f.activeCount();
            binding.chipFilters.setText(count > 0 ? "Filteri · " + count : "Filteri");
            binding.chipFilters.setCloseIconVisible(count > 0);
            int background = count > 0
                    ? MaterialColors.getColor(binding.chipFilters,
                    com.google.android.material.R.attr.colorPrimaryContainer)
                    : Color.TRANSPARENT;
            binding.chipFilters.setChipBackgroundColor(ColorStateList.valueOf(background));
        });

        viewModel.getMovies().observe(getViewLifecycleOwner(), movies -> {
            adapter.setMovies(movies);
            renderLoading();

            Parcelable state = viewModel.getListState();
            if (state != null && !movies.isEmpty()) {
                layoutManager.onRestoreInstanceState(state);
                viewModel.setListState(null);
            }
        });

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            isLoadingNow = loading;
            renderLoading();
        });

        viewModel.getStateMessage().observe(getViewLifecycleOwner(), state -> {
            if (state == null) {
                binding.stateView.setVisibility(View.GONE);
                return;
            }
            binding.tvState.setText(state.text);
            binding.btnRetry.setVisibility(state.retryable ? View.VISIBLE : View.GONE);
            binding.stateView.setVisibility(View.VISIBLE);
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), text -> {
            if (text != null) {
                Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        viewModel.getPeople().observe(getViewLifecycleOwner(), people -> {
            peopleAdapter.setPeople(people);
            binding.sectionPeople.setVisibility(people.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    private void renderLoading() {
        boolean empty = adapter.getItemCount() == 0;
        binding.progressCenter.setVisibility(isLoadingNow && empty ? View.VISIBLE : View.GONE);
        binding.progressPage.setVisibility(isLoadingNow && !empty ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cancelPendingSearch();
        if (layoutManager != null) {
            viewModel.setListState(layoutManager.onSaveInstanceState());
        }
        binding = null;
    }

    private void animateGridIcon(boolean threeColumns) {
        binding.btnGrid.animate().cancel();
        binding.btnGrid.animate()
                .scaleX(0f).scaleY(0f).rotation(90f)
                .setDuration(110)
                .withEndAction(() -> {
                    if (binding == null) return;
                    binding.btnGrid.setImageResource(
                            threeColumns ? R.drawable.ic_grid_3 : R.drawable.ic_grid);
                    binding.btnGrid.setRotation(-90f);
                    binding.btnGrid.animate()
                            .scaleX(1f).scaleY(1f).rotation(0f)
                            .setDuration(150)
                            .start();
                })
                .start();
    }
}