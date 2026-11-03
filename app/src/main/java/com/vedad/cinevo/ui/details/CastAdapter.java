package com.vedad.cinevo.ui.details;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vedad.cinevo.databinding.ItemCastBinding;
import com.vedad.cinevo.model.MovieDetails;

import java.util.ArrayList;
import java.util.List;

public class CastAdapter extends RecyclerView.Adapter<CastAdapter.CastViewHolder> {

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
    public CastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCastBinding binding = ItemCastBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new CastViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CastViewHolder holder, int position) {
        MovieDetails.Cast member = cast.get(position);

        holder.binding.tvCastName.setText(member.getName());
        holder.binding.tvCastRole.setText(
                member.getCharacter() != null ? member.getCharacter() : "");

        String url = member.getProfilePath() != null
                ? PHOTO_BASE_URL + member.getProfilePath()
                : null;
        Glide.with(holder.binding.getRoot()).load(url).into(holder.binding.imgCast);

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onCastClick(member);
        });
    }

    @Override
    public int getItemCount() {
        return cast.size();
    }

    static class CastViewHolder extends RecyclerView.ViewHolder {
        final ItemCastBinding binding;

        CastViewHolder(ItemCastBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}