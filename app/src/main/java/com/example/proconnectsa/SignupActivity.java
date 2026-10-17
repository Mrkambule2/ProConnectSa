package com.example.proconnectsa;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.proconnectsa.databinding.ActivitySignupBinding;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.models.UserRole;

import java.io.InputStream;
import java.util.UUID;

import com.example.proconnectsa.network.NetworkClient;
import com.example.proconnectsa.utils.UIUtils;

import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SignupActivity extends AppCompatActivity {

    private ActivitySignupBinding binding;
    private Uri selectedImageUri;
    private Uri selectedIdUri;
    private Uri selectedQualificationUri;
    private java.util.List<String> categoryNames = new java.util.ArrayList<>();

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    Glide.with(this).load(uri).into(binding.imgSignupProfile);
                }
            });

    private final ActivityResultLauncher<String> mGetIdContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedIdUri = uri;
                    binding.txtIdStatus.setText("ID: " + uri.getLastPathSegment());
                }
            });

    private final ActivityResultLauncher<String> mGetQualContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedQualificationUri = uri;
                    binding.txtQualificationStatus.setText("Qual: " + uri.getLastPathSegment());
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnUploadPhoto.setOnClickListener(v -> mGetContent.launch("image/*"));
        binding.btnUploadId.setOnClickListener(v -> mGetIdContent.launch("*/*"));
        binding.btnUploadQualification.setOnClickListener(v -> mGetQualContent.launch("*/*"));

        binding.radioGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_worker) {
                binding.layoutWorkerFields.setVisibility(android.view.View.VISIBLE);
                fetchCategories();
            } else {
                binding.layoutWorkerFields.setVisibility(android.view.View.GONE);
            }
        });

        binding.btnSignup.setOnClickListener(v -> {
            String name = binding.editName.getText().toString().trim();
            String email = binding.editEmail.getText().toString().trim();
            String password = binding.editPassword.getText().toString().trim();
            
            UserRole role = binding.radioWorker.isChecked() ? UserRole.TRADESPERSON : UserRole.CLIENT;

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (role == UserRole.TRADESPERSON) {
                if (selectedIdUri == null || selectedQualificationUri == null) {
                    Toast.makeText(this, "Please upload ID and Qualification documents", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            UIUtils.getInstance().showProcessing(this, "Creating Account...");
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
                    if (response.isSuccessful() && response.body() != null) {
                        Map<String, Object> responseBody = response.body();
                        String userId = null;
                        
                        // Extract User ID and Token from the response
                        String token = (String) responseBody.get("access_token");
                        if (token != null) {
                            DataManager.getInstance().setAccessToken(token);
                        }

                        if (responseBody.containsKey("user")) {
                            Map<String, Object> userMap = (Map<String, Object>) responseBody.get("user");
                            if (userMap != null) userId = (String) userMap.get("id");
                        } else if (responseBody.containsKey("id")) {
                            userId = (String) responseBody.get("id");
                        }
                        
                        if (userId != null) {
                            startUploadSequence(userId, name, email, role);
                        }
                    } else {
                        String errorMsg = "Signup failed";
                        try {
                            if (response.errorBody() != null) {
                                String errorJson = response.errorBody().string();
                                if (errorJson.contains("over_email_send_limit") || 
                                    errorJson.contains("email_limit") || 
                                    errorJson.contains("rate limit exceeded")) {
                                    errorMsg = "Email limit reached. Disable 'Confirm Email' in Supabase Auth settings.";
                                } else {
                                    errorMsg = errorJson;
                                }
                            }
                        } catch (Exception e) { e.printStackTrace(); }
                        UIUtils.getInstance().showError("Signup Failed", errorMsg);
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Log.e("Signup", "Failure: " + t.getMessage(), t);
                    String msg = "Connection Error";
                    if (t instanceof java.net.UnknownHostException) {
                        msg = "Cannot reach server. Check internet or Cold Boot your emulator.";
                    } else if (t instanceof java.net.SocketTimeoutException) {
                        msg = "Server timed out. Please try again.";
                    } else {
                        msg = t.getMessage();
                    }
                    UIUtils.getInstance().showError("Connection Error", msg);
                }
            });
        });

        binding.btnBackHome.setOnClickListener(v -> finish());
    }

    private void fetchCategories() {
        // Clear previous attempts and show loading
        categoryNames.clear();
        categoryNames.add("Loading categories...");
        ArrayAdapter<String> loadingAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryNames);
        binding.spinnerCategory.setAdapter(loadingAdapter);

        NetworkClient.getRetrofitClient().getCategories().enqueue(new Callback<java.util.List<com.example.proconnectsa.models.Category>>() {
            @Override
            public void onResponse(Call<java.util.List<com.example.proconnectsa.models.Category>> call, Response<java.util.List<com.example.proconnectsa.models.Category>> response) {
                categoryNames.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (com.example.proconnectsa.models.Category cat : response.body()) {
                        categoryNames.add(cat.getName());
                    }
                    
                    if (categoryNames.isEmpty()) {
                        categoryNames.add("General");
                        Toast.makeText(SignupActivity.this, "No categories found in database.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String error = "Error " + response.code();
                    try { if (response.errorBody() != null) error = response.errorBody().string(); } catch (Exception ignored) {}
                    Log.e("SignupActivity", "Category fetch failed: " + error);
                    categoryNames.add("General (Default)");
                    Toast.makeText(SignupActivity.this, "Could not load categories. Using default.", Toast.LENGTH_LONG).show();
                }
                
                ArrayAdapter<String> adapter = new ArrayAdapter<>(SignupActivity.this, android.R.layout.simple_spinner_item, categoryNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                binding.spinnerCategory.setAdapter(adapter);
            }

            @Override
            public void onFailure(Call<java.util.List<com.example.proconnectsa.models.Category>> call, Throwable t) {
                Log.e("SignupActivity", "Category fetch network error", t);
                categoryNames.clear();
                categoryNames.add("General (Default)");
                ArrayAdapter<String> adapter = new ArrayAdapter<>(SignupActivity.this, android.R.layout.simple_spinner_item, categoryNames);
                binding.spinnerCategory.setAdapter(adapter);
                Toast.makeText(SignupActivity.this, "Network error. Using default category.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String profileUrl = null, idUrl = null, qualUrl = null;

    private void startUploadSequence(String userId, String name, String email, UserRole role) {
        UIUtils.getInstance().showProcessing(this, "Uploading Documents...");
        
        // Reset URLs for new attempt
        profileUrl = null; idUrl = null; qualUrl = null;

        // 1. Upload Profile Photo
        uploadProfile(userId, () -> {
            // 2. Upload ID (Mandatory for workers)
            uploadDocument(userId, selectedIdUri, "id", urlID -> {
                if (urlID == null && role == UserRole.TRADESPERSON) {
                    runOnUiThread(() -> UIUtils.getInstance().showError("Upload Failed", "Could not upload ID document."));
                    return;
                }
                idUrl = urlID;
                
                // 3. Upload Qualification (Mandatory for workers)
                uploadDocument(userId, selectedQualificationUri, "qual", urlQual -> {
                    if (urlQual == null && role == UserRole.TRADESPERSON) {
                        runOnUiThread(() -> UIUtils.getInstance().showError("Upload Failed", "Could not upload Qualification document."));
                        return;
                    }
                    qualUrl = urlQual;
                    
                    // 4. Finally create the database entry with ALL URLs
                    createProfile(userId, name, email, role);
                });
            });
        });
    }

    private void uploadProfile(String userId, Runnable next) {
        if (selectedImageUri == null) {
            next.run();
            return;
        }
        uploadFile(selectedImageUri, "profile_" + userId + "_" + System.currentTimeMillis() + ".png", "image/png", url -> {
            profileUrl = url;
            next.run();
        });
    }

    private void uploadDocument(String userId, Uri uri, String prefix, java.util.function.Consumer<String> next) {
        if (uri == null) {
            next.accept(null);
            return;
        }
        
        String mimeType = getContentResolver().getType(uri);
        if (mimeType == null) mimeType = "application/octet-stream";
        
        String extension = mimeType.contains("pdf") ? ".pdf" : ".png";
        String fileName = prefix + "_" + userId + "_" + System.currentTimeMillis() + extension;
        
        uploadFile(uri, fileName, mimeType, next);
    }

    private void uploadFile(Uri uri, String fileName, String mimeType, java.util.function.Consumer<String> next) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            inputStream.close();

            RequestBody requestBody = RequestBody.create(MediaType.parse(mimeType), bytes);
            NetworkClient.getRetrofitClient().uploadAvatar(fileName, requestBody).enqueue(new Callback<Map<String, String>>() {
                @Override
                public void onResponse(Call<Map<String, String>> call, Response<Map<String, String>> response) {
                    if (response.isSuccessful()) {
                        String fullUrl = "https://tyyexrmomanepliljnjp.supabase.co/storage/v1/object/public/avatars/" + fileName;
                        Log.d("SignupUpload", "Success: " + fullUrl);
                        next.accept(fullUrl);
                    } else {
                        Log.e("SignupUpload", "Failed: " + response.code());
                        next.accept(null);
                    }
                }
                @Override public void onFailure(Call<Map<String, String>> call, Throwable t) {
                    Log.e("SignupUpload", "Error", t);
                    next.accept(null);
                }
            });
        } catch (Exception e) {
            next.accept(null);
        }
    }

    private void createProfile(String userId, String name, String email, UserRole role) {
        Map<String, Object> updates = new HashMap<>();
        // Note: We don't send 'id' in a PATCH request body usually, 
        // we use it in the URL filter.
        
        updates.put("name", name);
        updates.put("email", email);
        updates.put("role", role.name());
        updates.put("is_verified", false);
        
        if (profileUrl != null) updates.put("profile_image_url", profileUrl);
        
        if (role == UserRole.TRADESPERSON) {
            String selectedCat = binding.spinnerCategory.getSelectedItem() != null ? 
                               binding.spinnerCategory.getSelectedItem().toString() : "General";
            updates.put("trade_category", selectedCat);
            updates.put("id_document_url", idUrl);
            updates.put("qualification_document_url", qualUrl);
            
            Log.d("SignupFinal", "UPDATING Existing Profile - Cat: " + selectedCat + ", ID: " + idUrl + ", Qual: " + qualUrl);
        }

        // We use updateProfile (PATCH) because the Supabase Trigger already created the row
        NetworkClient.getRetrofitClient().updateProfile("eq." + userId, updates).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    UIUtils.getInstance().showSuccess("Signup Successful! Please wait for admin verification.", () -> {
                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                        finish();
                    });
                } else {
                    String errorMsg = "Update Error " + response.code();
                    try { if (response.errorBody() != null) errorMsg = response.errorBody().string(); } catch (Exception e) {}
                    Log.e("SignupFinal", "Error: " + errorMsg);
                    UIUtils.getInstance().showError("Profile Error", "Could not update documents: " + errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e("SignupFinal", "Network Failure", t);
                UIUtils.getInstance().showError("Connection Error", "Registration failed. Check network.");
            }
        });
    }
}