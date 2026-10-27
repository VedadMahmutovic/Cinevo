package com.vedad.cinevo.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vedad.cinevo.databinding.ItemMovieBinding;
import com.vedad.cinevo.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
                return true;
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
        return new MovieViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movies.get(position);

        holder.binding.tvTitle.setText(movie.getTitle());
        holder.binding.tvRating.setText(
                String.format(Locale.US, "★ %.1f", movie.getVoteAverage()));

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

        MovieViewHolder(ItemMovieBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}