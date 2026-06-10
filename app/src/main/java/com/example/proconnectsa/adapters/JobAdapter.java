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
        holder.binding.jobStatus.setText(job.getStatus());
        holder.binding.jobDate.setText(String.format("Scheduled: %s", job.getDate()));
        holder.binding.jobProvider.setText(String.format("Provider: %s", job.getProvider()));
        
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