package com.example.proconnectsa;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proconnectsa.databinding.ActivityPostJobBinding;
import com.example.proconnectsa.models.Category;
import com.example.proconnectsa.models.Job;
import com.example.proconnectsa.network.NetworkClient;
import java.util.ArrayList;
import java.util.List;
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

        fetchCategories();

        binding.btnSubmitJob.setOnClickListener(v -> submitJob());
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
                    for (Category cat : response.body()) {
                        categoryNames.add(cat.getName());
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(PostJobActivity.this, 
                            android.R.layout.simple_spinner_item, categoryNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.spinnerJobCategory.setAdapter(adapter);
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(PostJobActivity.this, "Failed to load categories", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitJob() {
        String title = binding.editJobTitle.getText().toString();
        String location = binding.editJobLocation.getText().toString();
        String description = binding.editJobDescription.getText().toString();
        String category = binding.spinnerJobCategory.getSelectedItem() != null ? 
                binding.spinnerJobCategory.getSelectedItem().toString() : "";

        if (title.isEmpty() || location.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.proconnectsa.models.User currentUser = DataManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in to post a job", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = currentUser.getId();

        Job job = new Job(title, "OPEN", location, null);
        job.setDescription(description);
        job.setCategory(category);
        job.setClientId(currentUserId);
        job.setDate(new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(new java.util.Date()));
        
        try {
            String budgetStr = binding.editJobBudget.getText().toString();
            if (!budgetStr.isEmpty()) {
                job.setBudget(Double.parseDouble(budgetStr));
            }
        } catch (Exception e) { e.printStackTrace(); }

        NetworkClient.getRetrofitClient().createJob(job).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(PostJobActivity.this, "Job posted successfully!", Toast.LENGTH_SHORT).show();
                    // Go back to main/home
                    finish();
                } else {
                    Toast.makeText(PostJobActivity.this, "Error posting job", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(PostJobActivity.this, "Network failure", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
