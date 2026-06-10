package com.example.proconnectsa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proconnectsa.adapters.CategoryAdapter;
import com.example.proconnectsa.databinding.FragmentManageCategoriesBinding;
import com.example.proconnectsa.models.Category;

import com.example.proconnectsa.network.NetworkClient;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageCategoriesFragment extends Fragment {

    private FragmentManageCategoriesBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentManageCategoriesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        refreshList();

        binding.btnAddCat.setOnClickListener(v -> {
            String name = binding.editCatName.getText().toString().trim();
            if (name.isEmpty()) {
                binding.layoutCatName.setError("Name required");
            } else {
                binding.layoutCatName.setError(null);
                addCategory(new Category(name, android.R.drawable.ic_menu_help));
            }
        });
    }

    private void addCategory(Category category) {
        binding.btnAddCat.setEnabled(false);
        NetworkClient.getRetrofitClient().addCategory(category).enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                binding.btnAddCat.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Category added successfully", Toast.LENGTH_SHORT).show();
                    binding.editCatName.setText("");
                    refreshList();
                } else {
                    String errorMsg = "Failed to add: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show();
                    android.util.Log.e("ManageCategories", errorMsg);
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                binding.btnAddCat.setEnabled(true);
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void refreshList() {
        NetworkClient.getRetrofitClient().getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    binding.recyclerManageCats.setLayoutManager(new LinearLayoutManager(getContext()));
                    binding.recyclerManageCats.setAdapter(new AdminCategoryAdapter(response.body()));
                } else {
                    Toast.makeText(getContext(), "Failed to fetch categories", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(getContext(), "Error fetching categories: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class AdminCategoryAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<AdminCategoryAdapter.VH> {
        private final List<Category> list;
        AdminCategoryAdapter(List<Category> list) { this.list = list; }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_category, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Category cat = list.get(position);
            holder.name.setText(cat.getName());
            holder.icon.setImageResource(cat.getIconResId() != 0 ? cat.getIconResId() : android.R.drawable.ic_menu_help);
            
            holder.delete.setOnClickListener(v -> {
                if (getContext() == null) return;
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                        .setTitle("Delete Category")
                        .setMessage("Are you sure you want to delete " + cat.getName() + "?")
                        .setPositiveButton("Delete", (dialog, which) -> deleteCategory(cat.getName()))
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        private void deleteCategory(String name) {
            NetworkClient.getRetrofitClient().deleteCategory("eq." + name).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Deleted " + name, Toast.LENGTH_SHORT).show();
                        refreshList();
                    }
                }
                @Override
                public void onFailure(Call<Void> call, Throwable t) {}
            });
        }

        @Override
        public int getItemCount() { return list.size(); }

        class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.TextView name;
            android.widget.ImageView icon;
            android.view.View delete;
            VH(android.view.View v) {
                super(v);
                name = v.findViewById(R.id.admin_category_name);
                icon = v.findViewById(R.id.admin_category_icon);
                delete = v.findViewById(R.id.btn_delete_cat);
            }
        }
    }
}