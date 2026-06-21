package com.example.proconnectsa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proconnectsa.databinding.ActivitySignupBinding;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.models.UserRole;

import java.util.UUID;

import com.example.proconnectsa.network.NetworkClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnSignup.setOnClickListener(v -> {
            String name = binding.editName.getText().toString();
            String email = binding.editEmail.getText().toString();
            String password = binding.editPassword.getText().toString();
            
            UserRole role = binding.radioWorker.isChecked() ? UserRole.TRADESPERSON : UserRole.CLIENT;

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            binding.btnSignup.setEnabled(false);
            Map<String, Object> body = new HashMap<>();
            body.put("email", email);
            body.put("password", password);
            
            // Adding metadata - this often fixes Supabase 500 trigger errors
            Map<String, String> metadata = new HashMap<>();
            metadata.put("name", name);
            metadata.put("role", role.name());
            body.put("data", metadata);

            NetworkClient.getRetrofitClient().signUp(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    binding.btnSignup.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> responseBody = response.body();
                        String userId = null;
                        
                        // Extract User ID from the response
                        if (responseBody.containsKey("user")) {
                            Map<String, Object> userMap = (Map<String, Object>) responseBody.get("user");
                            if (userMap != null) userId = (String) userMap.get("id");
                        } else if (responseBody.containsKey("id")) {
                            userId = (String) responseBody.get("id");
                        }
                        
                        if (userId != null) {
                            // Explicitly create profile to ensure it exists
                            createProfile(userId, name, email, role);
                        }
                    } else {
                        String errorMsg = "Signup failed";
                        try {
                            if (response.errorBody() != null) {
                                String errorJson = response.errorBody().string();
                                if (errorJson.contains("over_email_send_limit") || 
                                    errorJson.contains("email_limit") || 
                                    errorJson.contains("rate limit exceeded")) {
                                    errorMsg = "Email limit reached. To fix this:\n1. Go to Supabase Dashboard\n2. Auth -> Providers -> Email\n3. Disable 'Confirm Email'\n4. Save changes and try again.";
                                } else {
                                    errorMsg = errorJson;
                                }
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                        Toast.makeText(SignupActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    binding.btnSignup.setEnabled(true);
                    Toast.makeText(SignupActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    android.util.Log.e("SignupError", "Error: ", t);
                }
            });
        });

        binding.btnBackHome.setOnClickListener(v -> finish());
    }

    private void createProfile(String userId, String name, String email, UserRole role) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", userId);
        profile.put("name", name);
        profile.put("email", email);
        profile.put("role", role.name());
        profile.put("is_verified", false);

        NetworkClient.getRetrofitClient().createProfile(profile).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                // 201 Created or 409 Conflict (already exists via trigger) are both fine
                if (response.isSuccessful() || response.code() == 409) {
                    User newUser = new User(userId, name, email, role);
                    DataManager.getInstance().setCurrentUser(newUser);
                    Toast.makeText(SignupActivity.this, "Signup successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupActivity.this, MainActivity.class));
                    finish();
                } else {
                    String errorMsg = "Failed to create profile";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(SignupActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    android.util.Log.e("SignupError", errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Even on failure, try to proceed if it's just a network issue after auth success
                Toast.makeText(SignupActivity.this, "Profile creation error, but account created. Try logging in.", Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }
}