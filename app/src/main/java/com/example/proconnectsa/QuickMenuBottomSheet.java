package com.example.proconnectsa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class QuickMenuBottomSheet extends BottomSheetDialogFragment {

    public interface OnMenuSelectedListener {
        void onMenuSelected(int itemId);
    }

    private OnMenuSelectedListener listener;

    public QuickMenuBottomSheet(OnMenuSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_quick_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.menu_home).setOnClickListener(v -> {
            listener.onMenuSelected(R.id.HomeFragment);
            dismiss();
        });

        view.findViewById(R.id.menu_messages).setOnClickListener(v -> {
            listener.onMenuSelected(R.id.MessagesFragment);
            dismiss();
        });

        view.findViewById(R.id.menu_dashboard).setOnClickListener(v -> {
            listener.onMenuSelected(R.id.WorkerDashboardFragment);
            dismiss();
        });

        view.findViewById(R.id.menu_profile).setOnClickListener(v -> {
            listener.onMenuSelected(R.id.ProfileFragment);
            dismiss();
        });
    }
}