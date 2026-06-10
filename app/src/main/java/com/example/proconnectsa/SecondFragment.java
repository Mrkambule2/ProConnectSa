package com.example.proconnectsa;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.proconnectsa.databinding.FragmentSecondBinding;
import com.google.android.material.snackbar.Snackbar;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            com.example.proconnectsa.models.Tradesperson tp = (com.example.proconnectsa.models.Tradesperson) getArguments().getSerializable("tradesperson");
            if (tp != null) {
                binding.detailName.setText(tp.getName());
                binding.detailTrade.setText(tp.getTrade() + " • " + tp.getArea());
                binding.detailRating.setRating(tp.getRating());
                binding.detailAbout.setText(tp.getDescription());
                
                com.bumptech.glide.Glide.with(this)
                        .load(tp.getProfileImageUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(binding.detailImage);
            }
        }

        binding.btnBookNow.setOnClickListener(v -> {
            Snackbar.make(v, "Booking request sent!", Snackbar.LENGTH_LONG).show();
        });

        binding.btnMessage.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), ChatActivity.class));
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}