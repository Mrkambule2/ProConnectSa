package com.example.proconnectsa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proconnectsa.adapters.JobAdapter;
import com.example.proconnectsa.databinding.FragmentFirstBinding;
import com.example.proconnectsa.models.Job;

import java.util.ArrayList;
import java.util.List;

import com.example.proconnectsa.network.NetworkClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fetchJobs();
    }

    private void fetchJobs() {
        com.example.proconnectsa.models.User user = DataManager.getInstance().getCurrentUser();
        String clientId = user != null ? "eq." + user.getId() : null;

        NetworkClient.getRetrofitClient().getJobs(null, clientId).enqueue(new Callback<List<Job>>() {
            @Override
            public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Job> jobs = response.body();
                    JobAdapter adapter = new JobAdapter(jobs);
                    binding.recyclerJobs.setLayoutManager(new LinearLayoutManager(getContext()));
                    binding.recyclerJobs.setAdapter(adapter);

                    if (jobs.isEmpty()) {
                        binding.textNoJobs.setVisibility(View.VISIBLE);
                    } else {
                        binding.textNoJobs.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Job>> call, Throwable t) {
                // Handle error
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}