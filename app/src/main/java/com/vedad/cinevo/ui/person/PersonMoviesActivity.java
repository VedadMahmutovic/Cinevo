package com.vedad.cinevo.ui.person;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.vedad.cinevo.BuildConfig;
import com.vedad.cinevo.R;
import com.vedad.cinevo.data.remote.RetrofitClient;
import com.vedad.cinevo.data.repository.SavedMovieRepository;
import com.vedad.cinevo.databinding.ActivityPersonMoviesBinding;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.model.PersonCredits;
import com.vedad.cinevo.ui.details.MovieDetailsActivity;
import com.vedad.cinevo.ui.home.MovieAdapter;
import com.vedad.cinevo.util.AppSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PersonMoviesActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "person_id";
    public static final String EXTRA_NAME = "person_name";
    public static final String EXTRA_PHOTO = "person_photo";
    public static final String EXTRA_ROLE = "person_role";

    public static final String ROLE_DIRECTOR = "director";
    public static final String ROLE_ACTOR = "actor";

    private static final String PHOTO_BASE_URL = "https://image.tmdb.org/t/p/w185";
    private static final int MAX_ACTOR_MOVIES = 60;

    private ActivityPersonMoviesBinding binding;
    private MovieAdapter adapter;
    private int personId;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityPersonMoviesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.personRoot, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        personId = getIntent().getIntExtra(EXTRA_ID, -1);
        role = getIntent().getStringExtra(EXTRA_ROLE);
        if (personId == -1 || role == null) {
            finish();
            return;
        }

        binding.tvPersonName.setText(getIntent().getStringExtra(EXTRA_NAME));
        binding.tvPersonRole.setText(ROLE_DIRECTOR.equals(role)
                ? getString(R.string.person_directed) : getString(R.string.person_known_roles));

        String photo = getIntent().getStringExtra(EXTRA_PHOTO);
        if (photo != null) {
            Glide.with(this).load(PHOTO_BASE_URL + photo).into(binding.imgPerson);
        }

        adapter = new MovieAdapter();
        adapter.observeStatuses(this, new SavedMovieRepository(this).getStatuses());
        binding.recyclerPerson.setLayoutManager(new GridLayoutManager(
                this, new AppSettings(this).getGridColumns()));
        binding.recyclerPerson.setAdapter(adapter);
        adapter.setOnMovieClickListener(movie -> {
            Intent intent = new Intent(this, MovieDetailsActivity.class);
            intent.putExtra(MovieDetailsActivity.EXTRA_MOVIE_ID, movie.getId());
            startActivity(intent);
        });

        binding.btnPersonBack.setOnClickListener(v -> finish());
        binding.btnPersonRetry.setOnClickListener(v -> load());

        load();
    }

    private void load() {
        binding.progressPerson.setVisibility(View.VISIBLE);
        binding.statePerson.setVisibility(View.GONE);

        RetrofitClient.getApi()
                .getPersonMovieCredits(personId, BuildConfig.TMDB_API_KEY, "en-US")
                .enqueue(new Callback<PersonCredits>() {
                    @Override
                    public void onResponse(Call<PersonCredits> call,
                                           Response<PersonCredits> response) {
                        if (isFinishing() || isDestroyed()) return;
                        binding.progressPerson.setVisibility(View.GONE);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Movie> movies = buildMovies(response.body());
                            adapter.setMovies(movies);
                            if (movies.isEmpty()) {
                                showState(getString(R.string.person_no_movies), false);
                            }
                        } else {
                            showState(getString(R.string.error_loading_code, response.code()), true);
                        }
                    }

                    @Override
                    public void onFailure(Call<PersonCredits> call, Throwable t) {
                        if (isFinishing() || isDestroyed()) return;
                        binding.progressPerson.setVisibility(View.GONE);
                        showState(getString(R.string.error_no_internet), true);
                    }
                });
    }

    private void showState(String text, boolean retryable) {
        binding.tvPersonState.setText(text);
        binding.btnPersonRetry.setVisibility(retryable ? View.VISIBLE : View.GONE);
        binding.statePerson.setVisibility(View.VISIBLE);
    }

    private List<Movie> buildMovies(PersonCredits credits) {
        boolean director = ROLE_DIRECTOR.equals(role);
        Map<Integer, Movie> unique = new LinkedHashMap<>();

        if (director) {
            if (credits.getCrew() != null) {
                for (PersonCredits.CreditMovie c : credits.getCrew()) {
                    if ("Director".equals(c.getJob()) && c.getPosterPath() != null) {
                        unique.putIfAbsent(c.getId(), c);
                    }
                }
            }
        } else if (credits.getCast() != null) {
            for (Movie m : credits.getCast()) {
                if (m.getPosterPath() != null) unique.putIfAbsent(m.getId(), m);
            }
        }

        List<Movie> list = new ArrayList<>(unique.values());
        if (director) {
            Collections.sort(list, (a, b) -> dateOf(b).compareTo(dateOf(a)));
        } else {
            Collections.sort(list, (a, b) -> Double.compare(b.getPopularity(), a.getPopularity()));
            if (list.size() > MAX_ACTOR_MOVIES) {
                list = new ArrayList<>(list.subList(0, MAX_ACTOR_MOVIES));
            }
        }
        return list;
    }

    private String dateOf(Movie movie) {
        return movie.getReleaseDate() == null ? "" : movie.getReleaseDate();
    }
}