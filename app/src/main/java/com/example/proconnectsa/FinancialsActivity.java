package com.example.proconnectsa;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proconnectsa.databinding.ActivityFinancialsBinding;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.network.NetworkClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FinancialsActivity extends AppCompatActivity {

    private ActivityFinancialsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFinancialsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.financialsToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        loadCurrentBankDetails();

        binding.layoutPayout.btnSaveBank.setOnClickListener(v -> saveBankDetails());
    }

    private void loadCurrentBankDetails() {
        User user = DataManager.getInstance().getCurrentUser();
    }

    private void saveBankDetails() {
        String holder = binding.layoutPayout.editHolderName.getText().toString();
        String bank = binding.layoutPayout.spinnerBank.getSelectedItem().toString();
        String account = binding.layoutPayout.editAccountNumber.getText().toString();

        if (holder.isEmpty() || account.isEmpty()) {
            Toast.makeText(this, "Please fill all bank details", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = DataManager.getInstance().getCurrentUser();
        Map<String, Object> updates = new HashMap<>();
        updates.put("bank_name", bank);
        updates.put("account_number", account);
        // Add holder name if your table supports it

        NetworkClient.getRetrofitClient().updateProfile("eq." + user.getId(), updates).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FinancialsActivity.this, "Bank details updated!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(FinancialsActivity.this, "Failed to save details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}