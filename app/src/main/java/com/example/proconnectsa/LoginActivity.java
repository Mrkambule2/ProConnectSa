package com.example.proconnectsa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proconnectsa.databinding.ActivityLoginBinding;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.models.UserRole;

import com.example.proconnectsa.network.NetworkClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.editEmail.getText().toString();
            String password = binding.editPassword.getText().toString();

            if (email.isEmpty() || password.isEmpty()) return;

            binding.btnLogin.setEnabled(false);
            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);

            NetworkClient.getRetrofitClient().signIn(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    binding.btnLogin.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            Map<String, Object> body = response.body();
                            if (body.containsKey("user") && body.get("user") instanceof Map) {
                                Map<String, Object> userMap = (Map<String, Object>) body.get("user");
                                String userId = (String) userMap.get("id");
                                fetchUserProfile(userId);
                            } else {
                                Toast.makeText(LoginActivity.this, "Invalid response format", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(LoginActivity.this, "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String errorMsg = "Login failed";
                        try {
                            if (response.errorBody() != null) {
                                String errorJson = response.errorBody().string();
                                if (errorJson.contains("Email not confirmed")) {
                                    errorMsg = "Please verify your email or disable confirmation in Supabase.";
                                } else {
                                    errorMsg += ": " + errorJson;
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    binding.btnLogin.setEnabled(true);
                    Toast.makeText(LoginActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });

        binding.btnGotoSignup.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
        });

        binding.btnBackHome.setOnClickListener(v -> {
            finish(); // Or go to a specific Home activity
        });

        binding.btnForgotPassword.setOnClickListener(v -> {
            Toast.makeText(this, "Password reset link sent to your email", Toast.LENGTH_SHORT).show();
        });
    }

    private void fetchUserProfile(String userId) {
        NetworkClient.getRetrofitClient().getProfiles("eq." + userId, null, null, null).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    try {
                        Map<String, Object> profile = response.body().get(0);
                        User user = new User(
                                (String) profile.get("id"),
                                (String) profile.get("name"),
                                (String) profile.get("email"),
                                UserRole.valueOf((String) profile.get("role"))
                        );
                        
                        Object isVerified = profile.get("is_verified");
                        if (isVerified instanceof Boolean) {
                            user.setVerified((Boolean) isVerified);
                        } else if (isVerified instanceof Number) {
                            user.setVerified(((Number) isVerified).intValue() == 1);
                        }

                        user.setDescription((String) profile.get("description"));
                        user.setLocationName((String) profile.get("location_name"));
                        user.setProfileImage((String) profile.get("profile_image_url"));
                        
                        DataManager.getInstance().setCurrentUser(user);
                        if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN) {
                            startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                        } else {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        }
                        finish();
                    } catch (Exception e) {
                        Toast.makeText(LoginActivity.this, "Profile error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    String detail = (response.body() != null && response.body().isEmpty()) ? "ID " + userId + " not in profiles" : "Code: " + response.code();
                    Toast.makeText(LoginActivity.this, "Login successful, but profile missing: " + detail, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(LoginActivity.this, "Error fetching profile: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}