package com.example.proconnectsa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import androidx.navigation.fragment.NavHostFragment;

import com.example.proconnectsa.adapters.CategoryAdapter;
import com.example.proconnectsa.adapters.TradespersonAdapter;
import com.example.proconnectsa.databinding.FragmentHomeBinding;
import com.example.proconnectsa.models.Category;
import com.example.proconnectsa.models.Tradesperson;

import java.util.ArrayList;
import java.util.List;

import com.example.proconnectsa.network.NetworkClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private String selectedCategory = null;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fetchCategories();
        fetchFeaturedProfiles();
    }

    private void fetchCategories() {
        NetworkClient.getRetrofitClient().getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Category> categories = response.body();
                    if (categories.isEmpty()) {
                        Toast.makeText(getContext(), "No categories found. Add some in Admin panel.", Toast.LENGTH_SHORT).show();
                    }
                    CategoryAdapter adapter = new CategoryAdapter(categories, category -> {
                        if (category.getName().equals(selectedCategory)) {
                            selectedCategory = null; // Toggle off
                            Toast.makeText(getContext(), "Filter cleared", Toast.LENGTH_SHORT).show();
                        } else {
                            selectedCategory = category.getName();
                            Toast.makeText(getContext(), "Filtering by: " + selectedCategory, Toast.LENGTH_SHORT).show();
                        }
                        fetchFeaturedProfiles();
                    });
                    binding.recyclerCategories.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
                    binding.recyclerCategories.setAdapter(adapter);
                } else {
                    Toast.makeText(getContext(), "Failed to load categories: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                Toast.makeText(getContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchFeaturedProfiles() {
        String catFilter = selectedCategory != null ? "eq." + selectedCategory : null;
        NetworkClient.getRetrofitClient().getProfiles(null, "eq.TRADESPERSON", null, catFilter).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Tradesperson> profiles = new ArrayList<>();
                    for (Map<String, Object> map : response.body()) {
                        String name = map.get("name") != null ? (String) map.get("name") : "Anonymous";
                        String category = map.get("trade_category") != null ? (String) map.get("trade_category") : "General";
                        String location = map.get("location_name") != null ? (String) map.get("location_name") : "South Africa";
                        String description = (String) map.get("description");
                        
                        Tradesperson tp = new Tradesperson(
                                name,
                                category,
                                location,
                                5.0f,
                                (String) map.get("profile_image_url")
                        );
                        tp.setDescription(description != null ? description : "No description available.");
                        profiles.add(tp);
                    }
                    
                    if (profiles.isEmpty()) {
                        Toast.makeText(getContext(), "No providers found in your area.", Toast.LENGTH_SHORT).show();
                    }
                    
                    TradespersonAdapter adapter = new TradespersonAdapter(profiles, profile -> {
                        Bundle bundle = new Bundle();
                        bundle.putSerializable("tradesperson", profile);
                        NavHostFragment.findNavController(HomeFragment.this)
                                .navigate(R.id.action_HomeFragment_to_SecondFragment, bundle);
                    });
                    binding.recyclerProfiles.setLayoutManager(new LinearLayoutManager(getContext()));
                    binding.recyclerProfiles.setAdapter(adapter);
                } else {
                    Toast.makeText(getContext(), "Failed to load providers: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(getContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}