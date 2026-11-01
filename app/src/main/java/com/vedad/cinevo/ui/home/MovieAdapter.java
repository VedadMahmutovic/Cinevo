package com.vedad.cinevo.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vedad.cinevo.R;
import com.vedad.cinevo.data.repository.RuntimeRepository;
import com.vedad.cinevo.databinding.ItemMovieBinding;
import com.vedad.cinevo.model.Movie;
import com.vedad.cinevo.model.MovieDetails;
import com.vedad.cinevo.util.RatingFormatter;
import com.vedad.cinevo.util.RuntimeFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {

    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";

    public interface OnMovieClickListener {
        void onMovieClick(Movie movie);
    }

    private final List<Movie> movies = new ArrayList<>();
    private OnMovieClickListener listener;

    public void setOnMovieClickListener(OnMovieClickListener listener) {
        this.listener = listener;
    }

    public void setMovies(List<Movie> newMovies) {
        final List<Movie> oldList = new ArrayList<>(movies);

        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldList.size();
            }

            @Override
            public int getNewListSize() {
                return newMovies.size();
            }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                return oldList.get(oldPos).getId() == newMovies.get(newPos).getId();
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                return oldList.get(oldPos).getUserRating() == newMovies.get(newPos).getUserRating();
            }
        });

        movies.clear();
        movies.addAll(newMovies);
        diff.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMovieBinding binding = ItemMovieBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        if (fixedItemWidth > 0) {
            ViewGroup.LayoutParams params = binding.getRoot().getLayoutParams();
            params.width = fixedItemWidth;
            binding.getRoot().setLayoutParams(params);
        }
        return new MovieViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movies.get(position);

        holder.binding.tvTitle.setText(movie.getTitle());

        String date = movie.getReleaseDate();
        final String year = date != null && date.length() >= 4 ? date.substring(0, 4) : "";

        holder.boundMovieId = movie.getId();
        if (holder.runtimeCall != null) {
            holder.runtimeCall.cancel();
            holder.runtimeCall = null;
        }

        Integer cached = RuntimeRepository.getCached(movie.getId());
        if (cached != null) {
            holder.binding.tvYear.setText(yearWithRuntime(year, cached));
        } else {
            holder.binding.tvYear.setText(year);
            holder.runtimeCall = RuntimeRepository.fetch(movie.getId(), minutes -> {
                if (holder.boundMovieId == movie.getId()) {
                    holder.binding.tvYear.setText(yearWithRuntime(year, minutes));
                }
            });
        }

        if (movie.getUserRating() > 0) {
            holder.binding.tvRating.setText(
                    "★ " + RatingFormatter.stars(movie.getUserRating()) + "/5");
            holder.binding.tvRating.setBackgroundResource(R.drawable.bg_badge_primary);
        } else {
            holder.binding.tvRating.setText(movie.getVoteAverage() > 0
                    ? String.format(Locale.US, "★ %.1f", movie.getVoteAverage())
                    : "Novo");
            holder.binding.tvRating.setBackgroundResource(R.drawable.bg_badge_dark);
        }

        String url = movie.getPosterPath() != null
                ? IMAGE_BASE_URL + movie.getPosterPath()
                : null;

        Glide.with(holder.binding.getRoot())
                .load(url)
                .placeholder(android.R.color.darker_gray)
                .into(holder.binding.imgPoster);

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onMovieClick(movie);
        });
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        final ItemMovieBinding binding;
        int boundMovieId = -1;
        Call<MovieDetails> runtimeCall;

        MovieViewHolder(ItemMovieBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private int fixedItemWidth = 0;

    public void setFixedItemWidth(int widthPx) {
        this.fixedItemWidth = widthPx;
    }

    private static String yearWithRuntime(String year, int minutes) {
        String runtime = RuntimeFormatter.format(minutes);
        return year.isEmpty() ? runtime : year + " · " + runtime;
    }

    @Override
    public void onViewRecycled(@NonNull MovieViewHolder holder) {
        if (holder.runtimeCall != null) {
            holder.runtimeCall.cancel();
            holder.runtimeCall = null;
        }
    }

    public void setMoviesInstant(List<Movie> newMovies) {
        movies.clear();
        movies.addAll(newMovies);
        notifyDataSetChanged();
    }
}