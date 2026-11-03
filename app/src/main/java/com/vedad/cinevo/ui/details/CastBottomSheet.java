package com.vedad.cinevo.ui.details;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.vedad.cinevo.databinding.BottomSheetCastBinding;
import com.vedad.cinevo.model.MovieDetails;
import com.vedad.cinevo.ui.person.PersonMoviesActivity;

import java.util.List;

public class CastBottomSheet extends BottomSheetDialogFragment {

    private BottomSheetCastBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = BottomSheetCastBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MovieDetailsViewModel viewModel =
                new ViewModelProvider(requireActivity()).get(MovieDetailsViewModel.class);
        List<MovieDetails.Cast> cast = viewModel.getFullCast();

        binding.tvCastTitle.setText("Glumci (" + cast.size() + ")");

        CastRowAdapter adapter = new CastRowAdapter();
        binding.recyclerFullCast.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerFullCast.setAdapter(adapter);
        adapter.setCast(cast);

        adapter.setOnCastClickListener(member -> {
            Intent intent = new Intent(requireContext(), PersonMoviesActivity.class);
            intent.putExtra(PersonMoviesActivity.EXTRA_ID, member.getId());
            intent.putExtra(PersonMoviesActivity.EXTRA_NAME, member.getName());
            intent.putExtra(PersonMoviesActivity.EXTRA_PHOTO, member.getProfilePath());
            intent.putExtra(PersonMoviesActivity.EXTRA_ROLE, PersonMoviesActivity.ROLE_ACTOR);
            startActivity(intent);
            dismiss();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetBehavior<?> behavior = ((BottomSheetDialog) getDialog()).getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}