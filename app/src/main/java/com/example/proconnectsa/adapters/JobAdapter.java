package com.example.proconnectsa.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proconnectsa.DisputeActivity;
import com.example.proconnectsa.databinding.ItemJobBinding;
import com.example.proconnectsa.models.Job;

import java.util.List;

import android.view.View;
import android.widget.Toast;
import com.example.proconnectsa.network.NetworkClient;
import com.example.proconnectsa.utils.UIUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class JobAdapter extends RecyclerView.Adapter<JobAdapter.ViewHolder> {

    private final List<Job> jobs;

    public JobAdapter(List<Job> jobs) {
        this.jobs = jobs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemJobBinding binding = ItemJobBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Job job = jobs.get(position);
        holder.binding.jobTitle.setText(job.getTitle());
        
        String status = job.getStatus();
        holder.binding.jobStatus.setText(status);
        
        if ("COMPLETED_BY_PROVIDER".equals(status)) {
            holder.binding.jobStatus.setText("COMPLETED (Pending Your Confirmation)");
            holder.binding.jobStatus.setBackgroundColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_orange_dark));
            holder.binding.btnConfirmCompletion.setVisibility(View.VISIBLE);
        } else if ("CLOSED".equals(status)) {
            holder.binding.jobStatus.setText("FINALIZED");
            holder.binding.jobStatus.setBackgroundColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark));
            holder.binding.btnConfirmCompletion.setVisibility(View.GONE);
        } else {
            holder.binding.jobStatus.setBackgroundColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_blue_dark));
            holder.binding.btnConfirmCompletion.setVisibility(View.GONE);
        }

        holder.binding.jobDate.setText(String.format("Scheduled: %s", job.getDate()));
        holder.binding.jobProvider.setText(String.format("Provider: %s", job.getProvider()));

        holder.binding.btnConfirmCompletion.setOnClickListener(v -> {
            UIUtils.getInstance().showProcessing(v.getContext(), "Confirming Completion...");
            Map<String, String> updates = new HashMap<>();
            updates.put("status", "CLOSED");
            NetworkClient.getRetrofitClient().updateJobStatus("eq." + job.getId(), updates).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        UIUtils.getInstance().showSuccess("Job Finalized!", () -> {
                            job.setCategory("CLOSED"); // Reusing field for local update
                            holder.binding.btnConfirmCompletion.setVisibility(View.GONE);
                            holder.binding.jobStatus.setText("CLOSED");
                        });
                    } else {
                        UIUtils.getInstance().showError("Action Failed", response.message());
                    }
                }
                @Override public void onFailure(Call<Void> call, Throwable t) {
                    UIUtils.getInstance().showError("Network Error", t.getMessage());
                }
            });
        });
        
        holder.itemView.setOnLongClickListener(v -> {
            Intent intent = new Intent(v.getContext(), DisputeActivity.class);
            v.getContext().startActivity(intent);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return jobs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemJobBinding binding;

        public ViewHolder(ItemJobBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}