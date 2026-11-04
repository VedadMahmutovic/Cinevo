package com.vedad.cinevo.ui.home;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.vedad.cinevo.databinding.ItemCastBinding;
import com.vedad.cinevo.model.PersonSearchResponse;

import java.util.ArrayList;
import java.util.List;

public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.PersonViewHolder> {

    private static final String PHOTO_BASE_URL = "https://image.tmdb.org/t/p/w185";

    public interface OnPersonClickListener {
        void onPersonClick(PersonSearchResponse.Person person);
    }

    private final List<PersonSearchResponse.Person> people = new ArrayList<>();
    private OnPersonClickListener listener;

    public void setOnPersonClickListener(OnPersonClickListener listener) {
        this.listener = listener;
    }

    public void setPeople(List<PersonSearchResponse.Person> newPeople) {
        people.clear();
        people.addAll(newPeople);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCastBinding binding = ItemCastBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new PersonViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        PersonSearchResponse.Person person = people.get(position);

        holder.binding.tvCastName.setText(person.getName());
        holder.binding.tvCastRole.setText(
                "Directing".equals(person.getKnownForDepartment()) ? "Reditelj" : "Glumac");

        String url = person.getProfilePath() != null
                ? PHOTO_BASE_URL + person.getProfilePath()
                : null;
        Glide.with(holder.binding.getRoot()).load(url).into(holder.binding.imgCast);

        holder.binding.getRoot().setOnClickListener(v -> {
            if (listener != null) listener.onPersonClick(person);
        });
    }

    @Override
    public int getItemCount() {
        return people.size();
    }

    static class PersonViewHolder extends RecyclerView.ViewHolder {
        final ItemCastBinding binding;

        PersonViewHolder(ItemCastBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}