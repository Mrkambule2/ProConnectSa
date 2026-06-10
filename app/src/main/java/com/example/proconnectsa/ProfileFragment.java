package com.example.proconnectsa;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.proconnectsa.databinding.FragmentProfileBinding;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.models.UserRole;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        User user = DataManager.getInstance().getCurrentUser();
        if (user != null) {
            binding.profileName.setText(user.getName());
            binding.profileType.setText(user.getRole().toString() + (user.isVerified() ? " (Verified)" : " (Unverified)"));
            
            binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog(user));

            if (user.getRole() == UserRole.TRADESPERSON) {
                binding.textPortfolioTitle.setVisibility(View.VISIBLE);
                binding.recyclerPortfolio.setVisibility(View.VISIBLE);
                // setup portfolio recycler here
            } else if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN) {
                binding.btnEditProfile.setText("Open Admin Dashboard");
                binding.btnEditProfile.setOnClickListener(v -> {
                    startActivity(new Intent(getContext(), AdminDashboardActivity.class));
                });
            }

            binding.btnLogout.setOnClickListener(v -> {
                DataManager.getInstance().setCurrentUser(null);
                Intent intent = new Intent(getContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        }
    }

    private void showEditProfileDialog(User user) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        com.google.android.material.textfield.TextInputEditText editAbout = dialogView.findViewById(R.id.edit_about);
        com.google.android.material.textfield.TextInputEditText editLocation = dialogView.findViewById(R.id.edit_location);
        com.google.android.material.textfield.TextInputEditText editImage = dialogView.findViewById(R.id.edit_image_url);

        editAbout.setText(user.getDescription());
        editLocation.setText(user.getLocationName());
        editImage.setText(user.getProfileImage());

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String about = editAbout.getText().toString();
                    String loc = editLocation.getText().toString();
                    String img = editImage.getText().toString();

                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("description", about);
                    updates.put("location_name", loc);
                    updates.put("profile_image_url", img);

                    com.example.proconnectsa.network.NetworkClient.getRetrofitClient()
                            .updateProfile("eq." + user.getId(), updates)
                            .enqueue(new retrofit2.Callback<Void>() {
                                @Override
                                public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                                    if (response.isSuccessful()) {
                                        user.setDescription(about);
                                        user.setLocationName(loc);
                                        user.setProfileImage(img);
                                        android.widget.Toast.makeText(getContext(), "Profile updated!", android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                                    android.widget.Toast.makeText(getContext(), "Error: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}