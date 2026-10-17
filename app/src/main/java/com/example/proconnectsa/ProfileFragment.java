package com.example.proconnectsa;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.proconnectsa.databinding.FragmentProfileBinding;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.models.UserRole;
import com.example.proconnectsa.network.NetworkClient;
import com.example.proconnectsa.utils.UIUtils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private Uri selectedImageUri;
    private com.google.android.material.imageview.ShapeableImageView dialogPreview;

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    if (dialogPreview != null) {
                        Glide.with(this).load(uri).into(dialogPreview);
                    }
                }
            });

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

            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                Glide.with(this)
                        .load(user.getProfileImage())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(binding.profileImageLarge);
            }
            
            binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog(user));

            binding.btnFinancials.setOnClickListener(v -> {
                startActivity(new Intent(getContext(), FinancialsActivity.class));
            });

            binding.btnWorkerDashboard.setOnClickListener(v -> {
                androidx.navigation.fragment.NavHostFragment.findNavController(ProfileFragment.this)
                        .navigate(R.id.WorkerDashboardFragment);
            });

            if (user.getRole() == UserRole.TRADESPERSON) {
                binding.textPortfolioTitle.setVisibility(View.VISIBLE);
                binding.recyclerPortfolio.setVisibility(View.VISIBLE);
                // setup portfolio recycler here
            } else if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN) {
                binding.btnAdminDashboard.setVisibility(View.VISIBLE);
                binding.btnAdminDashboard.setOnClickListener(v -> {
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
        View loadingContainer = dialogView.findViewById(R.id.loading_container);
        dialogPreview = dialogView.findViewById(R.id.img_profile_preview);
        com.google.android.material.button.MaterialButton btnSelectImage = dialogView.findViewById(R.id.btn_select_image);

        editAbout.setText(user.getDescription());
        editLocation.setText(user.getLocationName());
        editImage.setText(user.getProfileImage());
        
        if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
            Glide.with(this).load(user.getProfileImage()).into(dialogPreview);
        }

        btnSelectImage.setOnClickListener(v -> mGetContent.launch("image/*"));

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                .setView(dialogView)
                .setTitle("Edit Profile")
                .setPositiveButton("Save", null) // Set to null to override later
                .setNegativeButton("Cancel", null)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            com.google.android.material.button.MaterialButton saveBtn = (com.google.android.material.button.MaterialButton) dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            saveBtn.setOnClickListener(v -> {
                String about = editAbout.getText().toString();
                String loc = editLocation.getText().toString();
                
                loadingContainer.setVisibility(View.VISIBLE);
                saveBtn.setEnabled(false);

                Map<String, Object> updates = new HashMap<>();
                updates.put("description", about);
                updates.put("location_name", loc);

                if (selectedImageUri != null) {
                    uploadImageAndSave(user, updates, about, loc, dialog, loadingContainer, saveBtn);
                } else {
                    String imgUrl = editImage.getText().toString();
                    updates.put("profile_image_url", imgUrl);
                    performUpdate(user, updates, about, loc, imgUrl, dialog, loadingContainer, saveBtn);
                }
            });
        });

        dialog.show();
    }

    private void uploadImageAndSave(User user, Map<String, Object> updates, String about, String loc, androidx.appcompat.app.AlertDialog dialog, View loading, View saveBtn) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(selectedImageUri);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            String fileName = "profile_" + user.getId() + "_" + System.currentTimeMillis() + ".png";
            RequestBody requestBody = RequestBody.create(MediaType.parse("image/png"), bytes);

            NetworkClient.getRetrofitClient().uploadAvatar(fileName, requestBody).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful()) {
                        String imageUrl = "https://tyyexrmomanepliljnjp.supabase.co/storage/v1/object/public/avatars/" + fileName;
                        updates.put("profile_image_url", imageUrl);
                        performUpdate(user, updates, about, loc, imageUrl, dialog, loading, saveBtn);
                    } else {
                        loading.setVisibility(View.GONE);
                        saveBtn.setEnabled(true);
                        String errorMsg = response.message();
                        try { if (response.errorBody() != null) errorMsg = response.errorBody().string(); } catch (Exception e) {}
                        
                        if (errorMsg.contains("row-level security")) {
                            errorMsg = "Permission Denied: Please enable RLS policies for the 'avatars' bucket in Supabase.";
                        }
                        Toast.makeText(getContext(), "Upload failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    loading.setVisibility(View.GONE);
                    saveBtn.setEnabled(true);
                    Toast.makeText(getContext(), "Upload error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            loading.setVisibility(View.GONE);
            saveBtn.setEnabled(true);
            Toast.makeText(getContext(), "Error reading file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void performUpdate(User user, Map<String, Object> updates, String about, String loc, String imgUrl, androidx.appcompat.app.AlertDialog dialog, View loading, View saveBtn) {
        // Show global processing for the final stage
        UIUtils.getInstance().showProcessing(getContext(), "Finalizing Profile...");
        
        NetworkClient.getRetrofitClient()
                .updateProfile("eq." + user.getId(), updates)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            user.setDescription(about);
                            user.setLocationName(loc);
                            user.setProfileImage(imgUrl);
                            Glide.with(ProfileFragment.this).load(imgUrl).into(binding.profileImageLarge);
                            
                            UIUtils.getInstance().showSuccess("Profile Updated!", () -> {
                                dialog.dismiss();
                            });
                        } else {
                            loading.setVisibility(View.GONE);
                            saveBtn.setEnabled(true);
                            UIUtils.getInstance().showError("Update Failed", response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        loading.setVisibility(View.GONE);
                        saveBtn.setEnabled(true);
                        UIUtils.getInstance().showError("Network Error", t.getMessage());
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}