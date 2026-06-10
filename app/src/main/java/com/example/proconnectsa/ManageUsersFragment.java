package com.example.proconnectsa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.proconnectsa.databinding.FragmentManageUsersBinding;
import com.example.proconnectsa.databinding.ItemUserManageBinding;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.models.UserRole;
import com.example.proconnectsa.network.NetworkClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageUsersFragment extends Fragment {

    private FragmentManageUsersBinding binding;
    private List<User> allUsers = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentManageUsersBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        binding.editSearchUsers.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { 
                applyFilters();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        binding.chipGroupRoles.setOnCheckedStateChangeListener((group, checkedIds) -> {
            applyFilters();
        });

        refreshList();
    }

    private void applyFilters() {
        String query = binding.editSearchUsers.getText().toString().toLowerCase();
        int checkedId = binding.chipGroupRoles.getCheckedChipId();
        
        List<User> filtered = new ArrayList<>();
        for (User u : allUsers) {
            boolean matchesSearch = u.getName().toLowerCase().contains(query) || 
                                    u.getEmail().toLowerCase().contains(query);
            
            boolean matchesRole = true;
            if (checkedId == R.id.chip_role_clients) {
                matchesRole = u.getRole() == UserRole.CLIENT;
            } else if (checkedId == R.id.chip_role_pros) {
                matchesRole = u.getRole() == UserRole.TRADESPERSON;
            } else if (checkedId == R.id.chip_role_staff) {
                matchesRole = u.getRole() == UserRole.ADMIN || u.getRole() == UserRole.SUPER_ADMIN;
            }

            if (matchesSearch && matchesRole) {
                filtered.add(u);
            }
        }
        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerUsers.setAdapter(new UserManageAdapter(filtered));
    }

    private void filterUsers(String query) {
        applyFilters();
    }

    private void refreshList() {
        NetworkClient.getRetrofitClient().getProfiles(null, null, null, null).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allUsers.clear();
                    for (Map<String, Object> map : response.body()) {
                        User user = new User(
                                (String) map.get("id"),
                                (String) map.get("name"),
                                (String) map.get("email"),
                                UserRole.valueOf((String) map.get("role"))
                        );
                        user.setProfileImage((String) map.get("profile_image_url"));
                        user.setDescription((String) map.get("description"));
                        user.setLocationName((String) map.get("location_name"));
                        
                        Object isVerified = map.get("is_verified");
                        if (isVerified instanceof Boolean) user.setVerified((Boolean) isVerified);
                        else if (isVerified instanceof Number) user.setVerified(((Number) isVerified).intValue() == 1);
                        
                        allUsers.add(user);
                    }
                    applyFilters();
                }
            }
            @Override public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) { }
        });
    }

    private class UserManageAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<UserManageAdapter.VH> {
        private final List<User> list;
        UserManageAdapter(List<User> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemUserManageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            User u = list.get(position);
            holder.b.manageUserName.setText(u.getName());
            holder.b.manageUserEmail.setText(u.getEmail());
            holder.b.manageUserRoleChip.setText(u.getRole().name());
            
            Glide.with(holder.itemView.getContext())
                    .load(u.getProfileImage())
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .into(holder.b.manageUserImage);

            if (u.getRole() == UserRole.SUPER_ADMIN) {
                holder.b.btnMakeAdmin.setVisibility(View.GONE);
                holder.b.btnDeleteUser.setVisibility(View.GONE);
            } else {
                boolean isAdmin = u.getRole() == UserRole.ADMIN;
                holder.b.btnMakeAdmin.setText(isAdmin ? "Demote" : "Promote");
                
                holder.b.btnMakeAdmin.setOnClickListener(v -> {
                    UserRole newRole = isAdmin ? UserRole.CLIENT : UserRole.ADMIN;
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("role", newRole.name());
                    NetworkClient.getRetrofitClient().updateProfile("eq." + u.getId(), updates).enqueue(new Callback<Void>() {
                        @Override public void onResponse(Call<Void> call, Response<Void> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(getContext(), u.getName() + " updated", Toast.LENGTH_SHORT).show();
                                refreshList();
                            }
                        }
                        @Override public void onFailure(Call<Void> call, Throwable t) {}
                    });
                });

                holder.b.btnDeleteUser.setOnClickListener(v -> {
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                            .setTitle("Delete User")
                            .setMessage("Remove " + u.getName() + "?")
                            .setPositiveButton("Delete", (dialog, which) -> {
                                NetworkClient.getRetrofitClient().deleteProfile("eq." + u.getId()).enqueue(new Callback<Void>() {
                                    @Override public void onResponse(Call<Void> call, Response<Void> response) {
                                        if (response.isSuccessful()) refreshList();
                                    }
                                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                                });
                            }).show();
                });
            }

            holder.b.btnViewDetails.setOnClickListener(v -> {
                String details = "ID: " + u.getId() + "\nRole: " + u.getRole() + "\nVerified: " + u.isVerified();
                if (u.getLocationName() != null) details += "\nLocation: " + u.getLocationName();
                
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                        .setTitle(u.getName())
                        .setMessage(details)
                        .setPositiveButton("Close", null)
                        .show();
            });
        }

        @Override public int getItemCount() { return list.size(); }
        class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemUserManageBinding b;
            VH(ItemUserManageBinding b) { super(b.getRoot()); this.b = b; }
        }
    }
}
