package com.vedad.cinevo.ui.details;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vedad.cinevo.databinding.ItemProviderBinding;
import com.vedad.cinevo.model.MovieDetails;

import java.util.ArrayList;
import java.util.List;

public class ProviderAdapter extends RecyclerView.Adapter<ProviderAdapter.ProviderViewHolder> {

    private static final String LOGO_BASE_URL = "https://image.tmdb.org/t/p/w92";

    private final List<MovieDetails.Provider> providers = new ArrayList<>();

    public void setProviders(List<MovieDetails.Provider> newProviders) {
        providers.clear();
        if (newProviders != null) providers.addAll(newProviders);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProviderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProviderBinding binding = ItemProviderBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ProviderViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProviderViewHolder holder, int position) {
        MovieDetails.Provider provider = providers.get(position);

        String url = provider.getLogoPath() != null
                ? LOGO_BASE_URL + provider.getLogoPath()
                : null;

        holder.binding.imgProvider.setContentDescription(provider.getName());
        Glide.with(holder.binding.getRoot())
                .load(url)
                .placeholder(android.R.color.darker_gray)
                .into(holder.binding.imgProvider);
    }

    @Override
    public int getItemCount() {
        return providers.size();
    }

    static class ProviderViewHolder extends RecyclerView.ViewHolder {
        final ItemProviderBinding binding;

        ProviderViewHolder(ItemProviderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}