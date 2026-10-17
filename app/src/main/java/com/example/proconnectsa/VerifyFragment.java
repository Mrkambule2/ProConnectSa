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

import com.example.proconnectsa.network.EmailSender;
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
        NetworkClient.getRetrofitClient().getProfiles(null, "eq.TRADESPERSON", "eq.false", null, "*").enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<User> pending = new ArrayList<>();
                    for (Map<String, Object> map : response.body()) {
                        User u = new User(
                                (String) map.get("id"),
                                (String) map.get("name"),
                                (String) map.get("email"),
                                UserRole.valueOf((String) map.get("role"))
                        );
                        
                        // Debug logging - check Logcat if values are still null
                        android.util.Log.d("AdminVerify", "Raw Data for " + u.getName() + ": " + map.toString());

                        u.setTradeCategory((String) map.get("trade_category"));
                        u.setIdDocumentUrl((String) map.get("id_document_url"));
                        u.setQualificationDocumentUrl((String) map.get("qualification_document_url"));
                        pending.add(u);
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
            holder.b.userTrade.setText("Category: " + (u.getTradeCategory() != null ? u.getTradeCategory() : "None"));
            
            holder.b.btnViewId.setOnClickListener(v -> {
                if (u.getIdDocumentUrl() != null && !u.getIdDocumentUrl().isEmpty()) {
                    try {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(u.getIdDocumentUrl()));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "No app found to open this document", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "No ID document available", Toast.LENGTH_SHORT).show();
                }
            });

            holder.b.btnViewQual.setOnClickListener(v -> {
                if (u.getQualificationDocumentUrl() != null && !u.getQualificationDocumentUrl().isEmpty()) {
                    try {
                        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(u.getQualificationDocumentUrl()));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "No app found to open this document", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "No qualification document available", Toast.LENGTH_SHORT).show();
                }
            });

            holder.b.btnVerify.setOnClickListener(v -> {
                Map<String, Object> updates = new HashMap<>();
                updates.put("is_verified", true);
                NetworkClient.getRetrofitClient().updateProfile("eq." + u.getId(), updates).enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), u.getName() + " verified!", Toast.LENGTH_SHORT).show();
                            sendVerificationEmail(u.getEmail(), u.getName(), true);
                            setupList();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) { }
                });
            });
            holder.b.btnReject.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Rejected " + u.getName(), Toast.LENGTH_SHORT).show();
                sendVerificationEmail(u.getEmail(), u.getName(), false);
                setupList();
            });
        }

        private void sendVerificationEmail(String email, String name, boolean approved) {
            String subject = approved ? "Account Approved - SAConnect" : "Account Verification Update - SAConnect";
            String body = approved ? 
                "Hello " + name + ",\n\nCongratulations! Your tradesperson account has been approved by the admin. You can now log in and start receiving jobs.\n\nRegards,\nSAConnect Team" :
                "Hello " + name + ",\n\nWe regret to inform you that your tradesperson account verification has been declined. Please ensure your documents are clear and valid.\n\nRegards,\nSAConnect Team";

            EmailSender.sendSimpleEmail(email, subject, body, new EmailSender.EmailListener() {
                @Override public void onSuccess() { android.util.Log.d("Verify", "Email sent"); }
                @Override public void onFailure(String error) { android.util.Log.e("Verify", "Email fail: " + error); }
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