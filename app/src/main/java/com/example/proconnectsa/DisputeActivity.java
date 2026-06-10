package com.example.proconnectsa;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proconnectsa.databinding.ActivityDisputeBinding;

public class DisputeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityDisputeBinding binding = ActivityDisputeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnSubmitDispute.setOnClickListener(v -> {
            Toast.makeText(this, "Dispute filed. An admin will review the chat logs and evidence.", Toast.LENGTH_LONG).show();
            finish();
        });

        binding.btnAttachEvidence.setOnClickListener(v -> {
            Toast.makeText(this, "Camera opened for evidence photos...", Toast.LENGTH_SHORT).show();
        });
    }
}