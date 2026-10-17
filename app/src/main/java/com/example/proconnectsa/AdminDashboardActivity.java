package com.example.proconnectsa;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.bumptech.glide.Glide;
import com.example.proconnectsa.databinding.ActivityAdminDashboardBinding;
import com.example.proconnectsa.models.Category;
import com.example.proconnectsa.models.Job;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.models.UserRole;
import com.example.proconnectsa.network.NetworkClient;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminDashboardActivity extends AppCompatActivity {

    private ActivityAdminDashboardBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        setSupportActionBar(binding.adminToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        User admin = DataManager.getInstance().getCurrentUser();
        if (admin != null && admin.getProfileImage() != null && !admin.getProfileImage().isEmpty()) {
            Glide.with(this).load(admin.getProfileImage()).circleCrop().into(binding.adminToolbarProfile);
        }
        binding.adminToolbarProfile.setOnClickListener(v -> binding.adminViewPager.setCurrentItem(5));

        setupViewPager();
        setupBottomNav();
        fetchStats();
    }

    private void setupBottomNav() {
        binding.adminBottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.VerifyFragment) {
                binding.adminViewPager.setCurrentItem(0);
                return true;
            } else if (id == R.id.HomeFragment) {
                binding.adminViewPager.setCurrentItem(1); // Categories
                return true;
            } else if (id == R.id.WorkerDashboardFragment) {
                binding.adminViewPager.setCurrentItem(2); // Jobs
                return true;
            } else if (id == R.id.ProfileFragment) {
                binding.adminViewPager.setCurrentItem(5); // My Profile (Me)
                return true;
            }
            return false;
        });
    }

    private void setupViewPager() {
        int tabCount = 6; // Verify, Categories, Jobs, Payments, Users, My Profile

        binding.adminViewPager.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0: return new VerifyFragment();
                    case 1: return new ManageCategoriesFragment();
                    case 2: return new ManageJobsFragment();
                    case 3: return new ManagePaymentsFragment();
                    case 4: return new ManageUsersFragment();
                    case 5: return new ProfileFragment();
                    default: return new Fragment();
                }
            }

            @Override
            public int getItemCount() {
                return tabCount;
            }
        });

        new TabLayoutMediator(binding.adminTabs, binding.adminViewPager, (tab, position) -> {
            switch (position) {
                case 0: tab.setText("Verify"); break;
                case 1: tab.setText("Categories"); break;
                case 2: tab.setText("Jobs"); break;
                case 3: tab.setText("Payments"); break;
                case 4: tab.setText("Users"); break;
                case 5: tab.setText("Me"); break;
            }
        }).attach();
    }

    private void fetchStats() {
        binding.textTotalUsers.setText("...");
        binding.textTotalProfessionals.setText("...");
        binding.textTotalStaff.setText("...");
        binding.textTotalJobs.setText("...");
        binding.textTotalCategories.setText("...");

        // Fetch Users Count
        NetworkClient.getRetrofitClient().getProfiles(null, null, null, null, "*").enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int total = response.body().size();
                    int pros = 0;
                    int staff = 0;
                    for (Map<String, Object> u : response.body()) {
                        String role = (String) u.get("role");
                        if (UserRole.TRADESPERSON.name().equals(role)) pros++;
                        if (UserRole.ADMIN.name().equals(role) || UserRole.SUPER_ADMIN.name().equals(role)) staff++;
                    }
                    binding.textTotalUsers.setText(String.valueOf(total));
                    binding.textTotalProfessionals.setText(String.valueOf(pros));
                    binding.textTotalStaff.setText(String.valueOf(staff));
                }
            }
            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });

        // Fetch Jobs Count
        NetworkClient.getRetrofitClient().getJobs(null, null, null).enqueue(new Callback<List<Job>>() {
            @Override
            public void onResponse(Call<List<Job>> call, Response<List<Job>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    binding.textTotalJobs.setText(String.valueOf(response.body().size()));
                }
            }
            @Override
            public void onFailure(Call<List<Job>> call, Throwable t) {}
        });

        // Fetch Categories Count
        NetworkClient.getRetrofitClient().getCategories().enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    binding.textTotalCategories.setText(String.valueOf(response.body().size()));
                }
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {}
        });
    }
}
