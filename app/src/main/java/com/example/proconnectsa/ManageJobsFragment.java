package com.example.proconnectsa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proconnectsa.databinding.FragmentManageJobsBinding;
import com.example.proconnectsa.databinding.ItemJobAdminBinding;
import com.example.proconnectsa.models.Job;
import com.example.proconnectsa.network.NetworkClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageJobsFragment extends Fragment {

    private FragmentManageJobsBinding binding;
    private List<Job> allJobs = new java.util.ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentManageJobsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.editSearchJobs.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterJobs(s.toString()); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        refreshJobs();
    }

    private void filterJobs(String query) {
        List<Job> filtered = new java.util.ArrayList<>();
        for (Job j : allJobs) {
            if (j.getTitle().toLowerCase().contains(query.toLowerCase()) || 
                j.getStatus().toLowerCase().contains(query.toLowerCase())) {
                filtered.add(j);
            }
        }
        binding.recyclerAllJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerAllJobs.setAdapter(new AdminJobAdapter(filtered));
    }

    private void refreshJobs() {
        NetworkClient.getRetrofitClient().getJobs(null, null).enqueue(new Callback<List<Job>>() {
            @Override
            public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allJobs = response.body();
                    filterJobs(binding.editSearchJobs.getText().toString());
                }
            }
            @Override public void onFailure(Call<List<Job>> call, Throwable t) {
                Toast.makeText(getContext(), "Failed to load system jobs", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class AdminJobAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<AdminJobAdapter.VH> {
        private final List<Job> list;
        AdminJobAdapter(List<Job> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemJobAdminBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Job j = list.get(position);
            holder.b.adminJobTitle.setText(j.getTitle());
            holder.b.adminJobClient.setText("Client ID: " + j.getClientId());
            holder.b.adminJobStatusChip.setText(j.getStatus());
            holder.b.adminJobLocation.setText(j.getLocation());

            holder.b.btnAdminJobDetails.setOnClickListener(v -> {
                String details = "Category: " + j.getCategory() + "\nBudget: R" + j.getBudget() + "\nDescription: " + j.getDescription();
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                        .setTitle(j.getTitle())
                        .setMessage(details)
                        .setPositiveButton("Close", null)
                        .show();
            });

            holder.b.btnAdminJobCancel.setOnClickListener(v -> {
                Map<String, String> updates = new HashMap<>();
                updates.put("status", "CANCELLED");
                NetworkClient.getRetrofitClient().updateJobStatus("eq." + j.getId(), updates).enqueue(new Callback<Void>() {
                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) refreshJobs();
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
            });
        }

        @Override public int getItemCount() { return list.size(); }
        class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemJobAdminBinding b;
            VH(ItemJobAdminBinding b) { super(b.getRoot()); this.b = b; }
        }
    }
}
