package com.vedad.cinevo.ui.details;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vedad.cinevo.databinding.ItemCastRowBinding;
import com.vedad.cinevo.model.MovieDetails;

import java.util.ArrayList;
import java.util.List;

public class CastRowAdapter extends RecyclerView.Adapter<CastRowAdapter.RowViewHolder> {

    private static final String PHOTO_BASE_URL = "https://image.tmdb.org/t/p/w185";

    public interface OnCastClickListener {
        void onCastClick(MovieDetails.Cast member);
    }

    private final List<MovieDetails.Cast> cast = new ArrayList<>();
    private OnCastClickListener listener;

    public void setOnCastClickListener(OnCastClickListener listener) {
        this.listener = listener;
    }

    public void setCast(List<MovieDetails.Cast> newCast) {
        cast.clear();
        cast.addAll(newCast);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCastRowBinding binding = ItemCastRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new RowViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        MovieDetails.Cast member = cast.get(position);

        holder.binding.tvRowName.setText(member.getName());
        holder.binding.tvRowRole.setText(
                member.getCharacter() != null ? member.getCharacter() : "");

        String url = member.getProfilePath() != null
                ? PHOTO_BASE_URL + member.getProfilePath()
                : null;
        Glide.with(holder.binding.getRoot()).load(url).into(holder.binding.imgCastRow);

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onCastClick(member);
        });
    }

    @Override
    public int getItemCount() {
        return cast.size();
    }

    static class RowViewHolder extends RecyclerView.ViewHolder {
        final ItemCastRowBinding binding;

        RowViewHolder(ItemCastRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}