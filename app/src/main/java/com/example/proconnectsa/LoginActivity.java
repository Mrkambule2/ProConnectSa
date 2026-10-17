package com.example.proconnectsa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proconnectsa.databinding.ActivityLoginBinding;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.models.UserRole;

import com.example.proconnectsa.network.NetworkClient;
import com.example.proconnectsa.utils.UIUtils;

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
            String email = binding.editEmail.getText().toString().trim();
            String password = binding.editPassword.getText().toString().trim();

            if (email.length() == 0 || password.length() == 0) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            UIUtils.getInstance().showProcessing(this, "Authenticating...");

            // Step 1: Check if email exists
            NetworkClient.getRetrofitClient().getProfilesByEmail("eq." + email, "*").enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isEmpty()) {
                        UIUtils.getInstance().showError("Authentication Failed", "Incorrect email address. No account found with this email.");
                    } else {
                        attemptPasswordSignIn(email, password);
                    }
                }

                @Override
                public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                    attemptPasswordSignIn(email, password);
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
            showForgotPasswordDialog();
        });
    }

    private void attemptPasswordSignIn(String email, String password) {
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("password", password);

        NetworkClient.getRetrofitClient().signIn(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> responseBody = response.body();
                        if (responseBody.containsKey("access_token")) {
                            String token = (String) responseBody.get("access_token");
                            DataManager.getInstance().setAccessToken(token);

                            if (responseBody.containsKey("user")) {
                                Map<String, Object> userMap = (Map<String, Object>) responseBody.get("user");
                                fetchUserProfile((String) userMap.get("id"));
                            } else {
                                UIUtils.getInstance().showError("Login Error", "User data missing from response.");
                            }
                        } else {
                            UIUtils.getInstance().showError("Login Error", "Invalid response from server.");
                        }
                    } catch (Exception e) {
                        UIUtils.getInstance().showError("Login Error", "Failed to parse login response.");
                        Log.e("Login", "Error", e);
                    }
                } else {
                    String errorMsg = "Incorrect password.";
                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            if (errorJson.contains("Email not confirmed")) {
                                errorMsg = "Please verify your email first.";
                            }
                        }
                    } catch (Exception ignored) {}
                    UIUtils.getInstance().showError("Authentication Failed", errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("Login", "Failure: " + t.getMessage(), t);
                String msg = "Connection Error";
                if (t instanceof java.net.UnknownHostException) {
                    msg = "Cannot reach server. Please check your internet connection.";
                } else if (t instanceof java.net.SocketTimeoutException) {
                    msg = "Server timed out. Please try again.";
                } else {
                    msg = t.getMessage();
                }
                UIUtils.getInstance().showError("Connection Error", msg);
            }
        });
    }

    private void showForgotPasswordDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(android.R.layout.select_dialog_item, null);
        android.widget.EditText emailInput = new android.widget.EditText(this);
        emailInput.setHint("Enter your registered email");
        emailInput.setPadding(40, 40, 40, 40);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email to receive a reset code.")
                .setView(emailInput)
                .setPositiveButton("Send Code", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (!email.isEmpty()) {
                        sendResetCode(email);
                    } else {
                        showStatus("Please enter an email", true);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void sendResetCode(String email) {
        showStatus("Sending code...", false);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);

        NetworkClient.getRetrofitClient().recoverPassword(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showStatus("Code sent to your email!", false);
                    showVerifyOTPDialog(email);
                } else {
                    showStatus("Error: Make sure you're editing the 'Reset Password' template in Supabase.", true);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showStatus("Network error: " + t.getMessage(), true);
            }
        });
    }

    private void showVerifyOTPDialog(String email) {
        android.widget.EditText otpInput = new android.widget.EditText(this);
        otpInput.setHint("Enter 8-digit code");
        otpInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT); // Changed to text as some tokens may contain letters
        otpInput.setPadding(40, 40, 40, 40);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Verify Code")
                .setMessage("Enter the code sent to " + email)
                .setView(otpInput)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String otp = otpInput.getText().toString().trim();
                    verifyCode(email, otp);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String recoveryToken;

    private void verifyCode(String email, String otp) {
        showStatus("Verifying...", false);
        Map<String, String> body = new HashMap<>();
        body.put("email", email);
        body.put("token", otp);
        body.put("type", "recovery"); // Added type to body as well

        NetworkClient.getRetrofitClient().verifyOTP(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    recoveryToken = (String) response.body().get("access_token");
                    android.util.Log.d("ResetPassword", "Token received: " + (recoveryToken != null));
                    showStatus("Code verified! Set new password.", false);
                    showNewPasswordDialog();
                } else {
                    String errorMsg = "Invalid code.";
                    try {
                        if (response.errorBody() != null) {
                            String errorJson = response.errorBody().string();
                            // Parse simple Supabase error if possible
                            if (errorJson.contains("message")) {
                                errorMsg = errorJson;
                            }
                        }
                    } catch (Exception e) {}
                    showStatus("Verification failed: " + errorMsg, true);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                showStatus("Error: " + t.getMessage(), true);
            }
        });
    }

    private void showNewPasswordDialog() {
        android.widget.EditText passInput = new android.widget.EditText(this);
        passInput.setHint("Enter new password");
        passInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passInput.setPadding(40, 40, 40, 40);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("New Password")
                .setMessage("Enter your new secure password.")
                .setView(passInput)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newPass = passInput.getText().toString().trim();
                    updatePassword(newPass);
                })
                .setCancelable(false)
                .show();
    }

    private void updatePassword(String newPassword) {
        if (recoveryToken == null) {
            showStatus("Session expired. Please start over.", true);
            return;
        }

        showStatus("Updating password...", false);
        Map<String, String> body = new HashMap<>();
        body.put("password", newPassword.trim());

        NetworkClient.getRetrofitClient().updatePassword("Bearer " + recoveryToken, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    showStatus("Password updated! You can now login.", false);
                    recoveryToken = null;
                } else {
                    String error = "Update failed.";
                    try {
                        if (response.errorBody() != null) {
                            error = response.errorBody().string();
                        }
                    } catch (Exception e) {}
                    showStatus("Error: " + error, true);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                showStatus("Error updating password.", true);
            }
        });
    }

    private void showStatus(String message, boolean isError) {
        binding.textStatusMessage.setVisibility(android.view.View.VISIBLE);
        binding.textStatusMessage.setText(message);
        binding.textStatusMessage.setTextColor(isError ? android.graphics.Color.RED : android.graphics.Color.GREEN);
    }

    private void fetchUserProfile(String userId) {
        NetworkClient.getRetrofitClient().getProfiles("eq." + userId, null, null, null, "*").enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    try {
                        Map<String, Object> profile = response.body().get(0);
                        String roleStr = (String) profile.get("role");
                        UserRole role = UserRole.CLIENT; // Default
                        if (roleStr != null) {
                            try { role = UserRole.valueOf(roleStr); } catch (Exception ignored) {}
                        }

                        User user = new User(
                                (String) profile.get("id"),
                                (String) profile.get("name"),
                                (String) profile.get("email"),
                                role
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
                        user.setTradeCategory((String) profile.get("trade_category"));
                        user.setIdDocumentUrl((String) profile.get("id_document_url"));
                        user.setQualificationDocumentUrl((String) profile.get("qualification_document_url"));

                        // Verification check for Tradespeople
                        if (user.getRole() == UserRole.TRADESPERSON && !user.isVerified()) {
                            DataManager.getInstance().setAccessToken(null); // Clear token
                            UIUtils.getInstance().showError("Pending Verification", 
                                "Your account is waiting for verification from the admin. You will be able to log in once your profile is approved.");
                            return;
                        }
                        
                        DataManager.getInstance().setCurrentUser(user);
                        
                        UIUtils.getInstance().showSuccess("Welcome " + user.getName(), () -> {
                            if (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN) {
                                startActivity(new Intent(LoginActivity.this, AdminDashboardActivity.class));
                            } else {
                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                            }
                            finish();
                        });
                    } catch (Exception e) {
                        UIUtils.getInstance().showError("Profile Error", "Failed to process profile data.");
                        Log.e("Login", "Profile error", e);
                    }
                } else {
                    UIUtils.getInstance().showError("Login Error", "Profile not found.");
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                UIUtils.getInstance().showError("Error", "Failed to fetch profile: " + t.getMessage());
            }
        });
    }
}
