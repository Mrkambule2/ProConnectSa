package com.example.proconnectsa;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proconnectsa.databinding.ActivityPostJobBinding;
import com.example.proconnectsa.models.Category;
import com.example.proconnectsa.models.Job;
import com.example.proconnectsa.network.NetworkClient;
import com.example.proconnectsa.network.EmailSender;
import com.example.proconnectsa.utils.UIUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostJobActivity extends AppCompatActivity {

    private ActivityPostJobBinding binding;
    private List<String> categoryNames = new ArrayList<>();

    private final ActivityResultLauncher<Intent> locationPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String address = result.getData().getStringExtra("address");
                    if (address != null) {
                        binding.editJobLocation.setText(address);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPostJobBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String providerName = getIntent().getStringExtra("tradesperson_name");
        if (providerName != null) {
            binding.textPostJobHeader.setText("Direct Booking Request");
            binding.textBookingProvider.setText(String.format("Booking: %s", providerName));
            binding.btnSubmitJob.setText("Confirm Booking");
        } else {
            binding.textBookingProvider.setText("Booking: Open Request");
        }

        fetchCategories();

        binding.btnSubmitJob.setOnClickListener(v -> submitJob());
        binding.btnCloseBooking.setOnClickListener(v -> finish());
        
        binding.btnPickLocation.setOnClickListener(v -> {
            Intent intent = new Intent(this, MapsActivity.class);
            intent.putExtra("pick_mode", true);
            locationPickerLauncher.launch(intent);
        });
    }

    private void fetchCategories() {
        NetworkClient.getRetrofitClient().getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    categoryNames.clear();
                    for (Category cat : response.body()) {
                        categoryNames.add(cat.getName());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(PostJobActivity.this, 
                            android.R.layout.simple_spinner_item, categoryNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.spinnerJobCategory.setAdapter(adapter);

                    String extraCategory = getIntent().getStringExtra("trade_category");
                    if (extraCategory != null) {
                        int pos = categoryNames.indexOf(extraCategory);
                        if (pos >= 0) {
                            binding.spinnerJobCategory.setSelection(pos);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Log.e("PostJob", "Cat fetch failed", t);
            }
        });
    }

    private void submitJob() {
        String title = binding.editJobTitle.getText().toString().trim();
        String location = binding.editJobLocation.getText().toString().trim();
        String description = binding.editJobDescription.getText().toString().trim();
        String category = binding.spinnerJobCategory.getSelectedItem() != null ? 
                binding.spinnerJobCategory.getSelectedItem().toString() : "";

        if (title.isEmpty() || location.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.proconnectsa.models.User currentUser = DataManager.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getId() == null) {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String currentUserId = currentUser.getId();
        String providerId = getIntent().getStringExtra("tradesperson_id");
        String providerEmail = getIntent().getStringExtra("tradesperson_email");

        java.util.Map<String, Object> jobData = new java.util.HashMap<>();
        jobData.put("title", title);
        jobData.put("status", "OPEN"); 
        jobData.put("location", location);
        jobData.put("description", description);
        jobData.put("category", category);
        jobData.put("client_id", currentUserId);
        jobData.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(new java.util.Date()));
        
        if (providerId != null && !providerId.isEmpty()) {
            jobData.put("provider_id", providerId);
        }

        try {
            String budgetStr = binding.editJobBudget.getText().toString().trim();
            if (!budgetStr.isEmpty()) {
                jobData.put("budget", Double.parseDouble(budgetStr));
            }
        } catch (Exception e) { e.printStackTrace(); }

        binding.btnSubmitJob.setEnabled(false);
        UIUtils.getInstance().showProcessing(this, "Confirming Booking...");
        
        List<Map<String, Object>> jobsList = new ArrayList<>();
        jobsList.add(jobData);

        NetworkClient.getRetrofitClient().createJob(jobsList).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    UIUtils.getInstance().showSuccess("Booking Confirmed!", () -> {
                        if (providerEmail != null && !providerEmail.isEmpty()) {
                            sendBookingEmail(providerEmail, title, description);
                        }
                        finish();
                    });
                } else {
                    binding.btnSubmitJob.setEnabled(true);
                    String errorDetail = "Error: " + response.code();
                    try { if (response.errorBody() != null) errorDetail = response.errorBody().string(); } catch (Exception e) {}
                    UIUtils.getInstance().showError("Booking Failed", errorDetail);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                binding.btnSubmitJob.setEnabled(true);
                UIUtils.getInstance().showError("Connection Error", "Please check your internet.");
            }
        });
    }

    private void sendBookingEmail(String toEmail, String jobTitle, String jobDesc) {
        String subject = "New Direct Booking: " + jobTitle;
        String body = "Hello,\n\nYou have been specifically booked for a new job via ProConnectSA!\n\n" +
                "Job Title: " + jobTitle + "\n" +
                "Description: " + jobDesc + "\n\n" +
                "Regards,\nProConnectSA Team";

        EmailSender.sendSimpleEmail(toEmail, subject, body, new EmailSender.EmailListener() {
            @Override
            public void onSuccess() {
                Log.d("PostJob", "Email sent to " + toEmail);
            }
            @Override
            public void onFailure(String error) {
                Log.e("PostJob", "Email failed: " + error);
            }
        });
    }
}
