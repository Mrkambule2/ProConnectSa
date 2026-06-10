package com.example.proconnectsa;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.proconnectsa.databinding.ActivityMainBinding;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.models.UserRole;

import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            binding.appBar.setPadding(0, systemBars.top, 0, 0);
            binding.bottomNavigation.setPadding(0, 0, 0, systemBars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        setSupportActionBar(binding.toolbar);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        
        setupBottomNav(navController);

        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.HomeFragment, R.id.VerifyFragment, R.id.ProfileFragment, R.id.WorkerDashboardFragment)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        User currentUser = DataManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getRole() == UserRole.CLIENT) {
            binding.fab.setVisibility(View.VISIBLE);
            binding.fab.setOnClickListener(view -> {
                Intent intent = new Intent(MainActivity.this, PostJobActivity.class);
                startActivity(intent);
            });
        } else {
            binding.fab.setVisibility(View.GONE);
        }
    }

    private void setupBottomNav(NavController navController) {
        User user = DataManager.getInstance().getCurrentUser();
        Menu menu = binding.bottomNavigation.getMenu();
        
        boolean isAdmin = user != null && (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN);
        
        menu.findItem(R.id.VerifyFragment).setVisible(isAdmin);
        menu.findItem(R.id.WorkerDashboardFragment).setVisible(user != null && user.getRole() == UserRole.TRADESPERSON);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        User user = DataManager.getInstance().getCurrentUser();
        
        MenuItem workerItem = menu.findItem(R.id.action_worker_dashboard);
        if (workerItem != null) {
            workerItem.setVisible(user != null && user.getRole() == UserRole.TRADESPERSON);
        }

        MenuItem catItem = menu.findItem(R.id.action_manage_categories);
        if (catItem != null) {
            catItem.setVisible(user != null && (user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN));
        }

        MenuItem usersItem = menu.findItem(R.id.action_manage_users);
        if (usersItem != null) {
            usersItem.setVisible(user != null && user.getRole() == UserRole.SUPER_ADMIN);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        NavController navController = ((NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main)).getNavController();

        if (id == android.R.id.home) {
            return navController.navigateUp() || super.onOptionsItemSelected(item);
        }

        if (id == R.id.action_worker_dashboard) {
            navController.navigate(R.id.WorkerDashboardFragment);
            return true;
        }

        if (id == R.id.action_manage_categories) {
            navController.navigate(R.id.ManageCategoriesFragment);
            return true;
        }

        if (id == R.id.action_manage_users) {
            navController.navigate(R.id.ManageUsersFragment);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        NavController navController = navHostFragment.getNavController();
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
