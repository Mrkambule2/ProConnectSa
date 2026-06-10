package com.example.proconnectsa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proconnectsa.databinding.FragmentVerifyTradesBinding;
import com.example.proconnectsa.databinding.ItemVerifyUserBinding;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.models.UserRole;

import java.util.ArrayList;
import java.util.List;

import com.example.proconnectsa.network.NetworkClient;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VerifyFragment extends Fragment {

    private FragmentVerifyTradesBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentVerifyTradesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupList();
    }

    private void setupList() {
        NetworkClient.getRetrofitClient().getProfiles(null, "eq.TRADESPERSON", "eq.false", null).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> pending = new ArrayList<>();
                    for (Map<String, Object> map : response.body()) {
                        pending.add(new User(
                                (String) map.get("id"),
                                (String) map.get("name"),
                                (String) map.get("email"),
                                UserRole.valueOf((String) map.get("role"))
                        ));
                    }

                    if (pending.isEmpty()) {
                        binding.textEmptyVerify.setVisibility(View.VISIBLE);
                    } else {
                        binding.textEmptyVerify.setVisibility(View.GONE);
                    }

                    binding.recyclerVerify.setLayoutManager(new LinearLayoutManager(getContext()));
                    binding.recyclerVerify.setAdapter(new VerifyAdapter(pending));
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                // Handle error
            }
        });
    }

    private class VerifyAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<VerifyAdapter.VH> {
        private final List<User> list;

        VerifyAdapter(List<User> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemVerifyUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            User u = list.get(position);
            holder.b.userName.setText(u.getName());
            holder.b.userEmail.setText(u.getEmail());
            
            // Set a random avatar color or similar if needed
            // holder.b.imgUserAvatar.setColorFilter(...)

            holder.b.btnVerify.setOnClickListener(v -> {
                Map<String, Object> updates = new HashMap<>();
                updates.put("is_verified", true);
                NetworkClient.getRetrofitClient().updateProfile("eq." + u.getId(), updates).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), u.getName() + " verified!", Toast.LENGTH_SHORT).show();
                            setupList();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) { }
                });
            });
            holder.b.btnReject.setOnClickListener(v -> {
                // For modern feel, let's just toast for now or implement a delete
                Toast.makeText(getContext(), "Rejected " + u.getName(), Toast.LENGTH_SHORT).show();
                setupList();
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemVerifyUserBinding b;
            VH(ItemVerifyUserBinding b) { super(b.getRoot()); this.b = b; }
        }
    }
}