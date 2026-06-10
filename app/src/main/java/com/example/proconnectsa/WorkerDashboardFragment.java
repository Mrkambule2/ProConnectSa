package com.example.proconnectsa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proconnectsa.databinding.FragmentWorkerDashboardBinding;
import com.example.proconnectsa.databinding.ItemJobLeadBinding;
import com.example.proconnectsa.models.Job;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.network.NetworkClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WorkerDashboardFragment extends Fragment {

    private FragmentWorkerDashboardBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWorkerDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupAvailabilityToggle();
        fetchJobLeads();

        binding.btnViewMap.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getContext(), MapsActivity.class);
            startActivity(intent);
        });

        binding.textTotalEarnings.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getContext(), FinancialsActivity.class);
            startActivity(intent);
        });
    }

    private void setupAvailabilityToggle() {
        binding.switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "Online" : "Offline";
            
            User user = DataManager.getInstance().getCurrentUser();
            Map<String, Object> updates = new HashMap<>();
            updates.put("is_online", isChecked);

            NetworkClient.getRetrofitClient().updateProfile("eq." + user.getId(), updates).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "You are now " + status, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Toast.makeText(getContext(), "Failed to update status", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void fetchJobLeads() {
        NetworkClient.getRetrofitClient().getJobs("eq.OPEN", null).enqueue(new Callback<List<Job>>() {
            @Override
            public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    setupLeadsList(response.body());
                } else {
                    Toast.makeText(getContext(), "Failed to fetch jobs", Toast.LENGTH_SHORT).show();
                    setupLeadsList(new ArrayList<>());
                }
            }

            @Override
            public void onFailure(Call<List<Job>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                setupLeadsList(new ArrayList<>());
            }
        });
    }


    private void setupLeadsList(List<Job> leads) {
        binding.recyclerJobLeads.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerJobLeads.setAdapter(new JobLeadsAdapter(leads));
    }

    private class JobLeadsAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<JobLeadsAdapter.VH> {
        private final List<Job> list;
        JobLeadsAdapter(List<Job> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemJobLeadBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Job job = list.get(position);
            holder.b.leadTitle.setText(job.getTitle());
            holder.b.leadLocation.setText("📍 " + job.getLocation());
            holder.b.btnBid.setOnClickListener(v -> {
                showQuoteBuilderDialog(job);
            });
        }

        private void showQuoteBuilderDialog(Job job) {
            com.google.android.material.dialog.MaterialAlertDialogBuilder builder = new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext());
            builder.setTitle("Create Quote for " + job.getTitle());
            
            View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_quote_builder, null);
            builder.setView(view);

            builder.setPositiveButton("Send Quote", (dialog, which) -> {
                Toast.makeText(getContext(), "Quote sent to " + job.getProvider(), Toast.LENGTH_SHORT).show();
            });
            builder.setNegativeButton("Cancel", null);
            builder.show();
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemJobLeadBinding b;
            VH(ItemJobLeadBinding b) { super(b.getRoot()); this.b = b; }
        }
    }
}