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
import com.example.proconnectsa.utils.UIUtils;

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
        fetchWorkerEarnings();

        binding.btnViewMap.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getContext(), MapsActivity.class);
            startActivity(intent);
        });

        binding.btnManagePayouts.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getContext(), FinancialsActivity.class);
            startActivity(intent);
        });

        binding.textTotalEarnings.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(getContext(), FinancialsActivity.class);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchWorkerEarnings();
    }

    private void fetchWorkerEarnings() {
        User user = DataManager.getInstance().getCurrentUser();
        if (user == null || getContext() == null) return;

        com.example.proconnectsa.utils.PaymentManager.getInstance().fetchPayments(getContext(), payments -> {
            double approvedTotal = 0.0;
            double pendingTotal = 0.0;

            for (com.example.proconnectsa.models.PaymentTransaction pt : payments) {
                if (user.getId().equals(pt.getProviderId())) {
                    double amt = pt.getAmount() != null ? pt.getAmount() : 0.0;
                    String status = pt.getStatus() != null ? pt.getStatus().toUpperCase() : "";

                    if ("APPROVED".equals(status)) {
                        approvedTotal += amt;
                    } else if ("PENDING_APPROVAL".equals(status)) {
                        pendingTotal += amt;
                    }
                }
            }

            if (binding != null) {
                binding.textTotalEarnings.setText(String.format(java.util.Locale.US, "R %.2f", approvedTotal));
                binding.textPendingEarnings.setText(String.format(java.util.Locale.US, "Pending Admin Escrow: R %.2f", pendingTotal));
            }
        });
    }

    private void setupAvailabilityToggle() {
        binding.switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
            User user = DataManager.getInstance().getCurrentUser();
            if (user == null) return;

            String status = isChecked ? "Online" : "Offline";
            Map<String, Object> updates = new HashMap<>();
            updates.put("is_online", isChecked);

            NetworkClient.getRetrofitClient().updateProfile("eq." + user.getId(), updates).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful() && getContext() != null) {
                        Toast.makeText(getContext(), "You are now " + status, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to update status", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    private void fetchJobLeads() {
        User user = DataManager.getInstance().getCurrentUser();
        if (user == null) return;
        
        binding.workerProgress.setVisibility(View.VISIBLE);
        String providerId = "eq." + user.getId();
        
        // Use a counter or join them to hide progress correctly
        final int[] callsPending = {4}; // 4 network calls

        Runnable decrementAndHide = () -> {
            callsPending[0]--;
            if (callsPending[0] <= 0) binding.workerProgress.setVisibility(View.GONE);
        };
        // 1. Fetch direct bookings (OPEN status)
        NetworkClient.getRetrofitClient().getJobs("eq.OPEN", null, providerId).enqueue(new Callback<List<Job>>() {
            @Override
            public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                decrementAndHide.run();
                if (response.isSuccessful() && response.body() != null) {
                    setupDirectBookings(response.body());
                }
            }
            @Override public void onFailure(Call<List<Job>> call, Throwable t) { decrementAndHide.run(); }
        });

        // 2. Fetch Active Jobs (PENDING status)
        NetworkClient.getRetrofitClient().getJobs("eq.PENDING", null, providerId).enqueue(new Callback<List<Job>>() {
            @Override
            public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                decrementAndHide.run();
                if (response.isSuccessful() && response.body() != null) {
                    setupActiveJobs(response.body());
                }
            }
            @Override public void onFailure(Call<List<Job>> call, Throwable t) { decrementAndHide.run(); }
        });

        // 3. Fetch Completed Jobs (COMPLETED_BY_PROVIDER and CLOSED status)
        NetworkClient.getRetrofitClient().getJobs(null, null, providerId).enqueue(new Callback<List<Job>>() {
            @Override
            public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                decrementAndHide.run();
                if (response.isSuccessful() && response.body() != null) {
                    List<Job> completed = new ArrayList<>();
                    for (Job j : response.body()) {
                        if ("COMPLETED_BY_PROVIDER".equals(j.getStatus()) || "CLOSED".equals(j.getStatus())) {
                            completed.add(j);
                        }
                    }
                    setupCompletedJobs(completed);
                }
            }
            @Override public void onFailure(Call<List<Job>> call, Throwable t) { decrementAndHide.run(); }
        });

        // 4. Fetch general OPEN leads (broadcasted jobs)
        String leadFilter = "eq.OPEN";
        if (user.getTradeCategory() != null) {
            // If the worker has a category, only show jobs in that category
            NetworkClient.getRetrofitClient().getJobs("eq.OPEN", null, null).enqueue(new Callback<List<Job>>() {
                @Override
                public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                    decrementAndHide.run();
                    if (response.isSuccessful() && response.body() != null) {
                        List<Job> leads = new ArrayList<>();
                        for (Job j : response.body()) {
                            if ((j.getProviderId() == null || j.getProviderId().isEmpty()) && 
                                user.getTradeCategory().equals(j.getCategory())) {
                                leads.add(j);
                            }
                        }
                        setupLeadsList(leads);
                    } else {
                        setupLeadsList(new ArrayList<>());
                    }
                }
                @Override
                public void onFailure(Call<List<Job>> call, Throwable t) {
                    decrementAndHide.run();
                    setupLeadsList(new ArrayList<>());
                }
            });
        } else {
            NetworkClient.getRetrofitClient().getJobs("eq.OPEN", null, null).enqueue(new Callback<List<Job>>() {
                @Override
                public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                    decrementAndHide.run();
                    if (response.isSuccessful() && response.body() != null) {
                        // Filter out jobs that are already assigned to someone else
                        List<Job> leads = new ArrayList<>();
                        for (Job j : response.body()) {
                            if (j.getProviderId() == null || j.getProviderId().isEmpty()) {
                                leads.add(j);
                            }
                        }
                        setupLeadsList(leads);
                    } else {
                        setupLeadsList(new ArrayList<>());
                    }
                }
                @Override
                public void onFailure(Call<List<Job>> call, Throwable t) {
                    decrementAndHide.run();
                    setupLeadsList(new ArrayList<>());
                }
            });
        }
    }

    private void setupDirectBookings(List<Job> bookings) {
        if (bookings.isEmpty()) {
            binding.labelDirectBookings.setVisibility(View.GONE);
            binding.recyclerDirectBookings.setVisibility(View.GONE);
            return;
        }
        binding.labelDirectBookings.setVisibility(View.VISIBLE);
        binding.recyclerDirectBookings.setVisibility(View.VISIBLE);
        binding.recyclerDirectBookings.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerDirectBookings.setAdapter(new JobLeadsAdapter(bookings, JobType.DIRECT));
    }

    private void setupActiveJobs(List<Job> jobs) {
        if (jobs.isEmpty()) {
            binding.labelActiveJobs.setVisibility(View.GONE);
            binding.recyclerActiveJobs.setVisibility(View.GONE);
            return;
        }
        binding.labelActiveJobs.setVisibility(View.VISIBLE);
        binding.recyclerActiveJobs.setVisibility(View.VISIBLE);
        binding.recyclerActiveJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerActiveJobs.setAdapter(new JobLeadsAdapter(jobs, JobType.ACTIVE));
    }

    private void setupCompletedJobs(List<Job> jobs) {
        if (jobs.isEmpty()) {
            binding.labelCompletedJobs.setVisibility(View.GONE);
            binding.recyclerCompletedJobs.setVisibility(View.GONE);
            return;
        }
        binding.labelCompletedJobs.setVisibility(View.VISIBLE);
        binding.recyclerCompletedJobs.setVisibility(View.VISIBLE);
        binding.recyclerCompletedJobs.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerCompletedJobs.setAdapter(new JobLeadsAdapter(jobs, JobType.COMPLETED));
    }

    private void setupLeadsList(List<Job> leads) {
        binding.recyclerJobLeads.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerJobLeads.setAdapter(new JobLeadsAdapter(leads, JobType.LEAD));
    }

    private enum JobType { DIRECT, ACTIVE, LEAD, COMPLETED }

    private class JobLeadsAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<JobLeadsAdapter.VH> {
        private final List<Job> list;
        private final JobType type;

        JobLeadsAdapter(List<Job> list, JobType type) { 
            this.list = list; 
            this.type = type;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemJobLeadBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Job job = list.get(position);
            holder.b.leadTitle.setText(job.getTitle());
            holder.b.leadLocation.setText(String.format("📍 %s", job.getLocation()));
            holder.b.leadDescription.setText(job.getDescription());
            
            if (type == JobType.DIRECT) {
                holder.b.btnMessageClient.setVisibility(View.VISIBLE);
                holder.b.btnMessageClient.setOnClickListener(v -> openChatWithClient(job));

                holder.b.btnBid.setText("Accept");
                holder.b.btnBid.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                holder.b.btnBid.setOnClickListener(v -> updateStatus(job, "PENDING", "Booking Accepted!"));
                
                holder.b.btnDecline.setVisibility(View.VISIBLE);
                holder.b.btnDecline.setOnClickListener(v -> updateStatus(job, "DECLINED", "Booking Declined."));
            } else if (type == JobType.ACTIVE) {
                holder.b.btnMessageClient.setVisibility(View.VISIBLE);
                holder.b.btnMessageClient.setOnClickListener(v -> openChatWithClient(job));

                holder.b.btnBid.setText("Mark Completed");
                holder.b.btnBid.setBackgroundColor(getResources().getColor(android.R.color.holo_blue_dark));
                holder.b.btnBid.setOnClickListener(v -> updateStatus(job, "COMPLETED_BY_PROVIDER", "Job marked as completed!"));
                holder.b.btnDecline.setVisibility(View.GONE);
            } else if (type == JobType.COMPLETED) {
                holder.b.btnMessageClient.setVisibility(View.VISIBLE);
                holder.b.btnMessageClient.setOnClickListener(v -> openChatWithClient(job));

                holder.b.btnBid.setText(job.getStatus().equals("CLOSED") ? "Finalized" : "Waiting for Client");
                holder.b.btnBid.setEnabled(false);
                holder.b.btnBid.setAlpha(0.6f);
                holder.b.btnDecline.setVisibility(View.GONE);
            } else {
                holder.b.btnMessageClient.setVisibility(View.GONE);
                holder.b.btnBid.setText("Send Quote");
                holder.b.btnBid.setOnClickListener(v -> showQuoteBuilderDialog(job));
                holder.b.btnDecline.setVisibility(View.GONE);
            }
        }

        private void openChatWithClient(Job job) {
            if (job.getClientId() == null) {
                Toast.makeText(getContext(), "Error: No client ID found for this job", Toast.LENGTH_SHORT).show();
                return;
            }
            android.content.Intent intent = new android.content.Intent(getContext(), ChatActivity.class);
            intent.putExtra("receiver_id", job.getClientId());
            // We don't have the client name in the Job object, but we can pass a generic title or fetch it later
            intent.putExtra("receiver_name", "Client"); 
            startActivity(intent);
        }

        private void updateStatus(Job job, String status, String toastMsg) {
            UIUtils.getInstance().showProcessing(getContext(), "Updating Job...");
            Map<String, String> updates = new HashMap<>();
            updates.put("status", status);
            NetworkClient.getRetrofitClient().updateJobStatus("eq." + job.getId(), updates).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        UIUtils.getInstance().showSuccess(toastMsg, () -> {
                            fetchJobLeads();
                        });
                    } else {
                        UIUtils.getInstance().showError("Update Failed", response.message());
                    }
                }
                @Override public void onFailure(Call<Void> call, Throwable t) {
                    UIUtils.getInstance().showError("Connection Error", t.getMessage());
                }
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