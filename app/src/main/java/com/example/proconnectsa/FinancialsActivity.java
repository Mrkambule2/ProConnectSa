package com.example.proconnectsa;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proconnectsa.databinding.ActivityFinancialsBinding;
import com.example.proconnectsa.databinding.ItemTransactionBinding;
import com.example.proconnectsa.models.PaymentTransaction;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.models.UserRole;
import com.example.proconnectsa.network.NetworkClient;
import com.example.proconnectsa.utils.PaymentManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FinancialsActivity extends AppCompatActivity {

    private ActivityFinancialsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFinancialsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.financialsToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        loadCurrentBankDetails();
        loadTransactionsAndBalances();
        setupPayFastToggle();

        binding.layoutPayout.btnSaveBank.setOnClickListener(v -> saveBankDetails());
        binding.layoutPayout.btnReturnHome.setOnClickListener(v -> finish());
    }

    private void setupPayFastToggle() {
        boolean isTestMode = com.example.proconnectsa.network.PayFastConfig.isTestMode(this);
        binding.switchPayfastTestMode.setChecked(isTestMode);
        
        binding.switchPayfastTestMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            com.example.proconnectsa.network.PayFastConfig.setTestMode(FinancialsActivity.this, isChecked);
            String status = isChecked ? "PayFast TEST Mode Enabled (Sandbox)" : "PayFast LIVE Production Mode Enabled (Real Money)";
            Toast.makeText(FinancialsActivity.this, status, Toast.LENGTH_LONG).show();
        });
    }

    private void loadCurrentBankDetails() {
        User user = DataManager.getInstance().getCurrentUser();
        if (user == null) return;

        NetworkClient.getRetrofitClient().getProfiles("eq." + user.getId(), null, null, null, "*").enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Map<String, Object> map = response.body().get(0);
                    String bankName = (String) map.get("bank_name");
                    String accountNum = (String) map.get("account_number");

                    if (bankName != null) {
                        String[] banks = getResources().getStringArray(R.array.sa_banks);
                        for (int i = 0; i < banks.length; i++) {
                            if (banks[i].equalsIgnoreCase(bankName)) {
                                binding.layoutPayout.spinnerBank.setSelection(i);
                                break;
                            }
                        }
                    }
                    if (accountNum != null) {
                        binding.layoutPayout.editAccountNumber.setText(accountNum);
                    }
                    if (user.getName() != null) {
                        binding.layoutPayout.editHolderName.setText(user.getName());
                    }
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    private void loadTransactionsAndBalances() {
        User user = DataManager.getInstance().getCurrentUser();
        if (user == null) return;

        PaymentManager.getInstance().fetchPayments(this, payments -> {
            List<PaymentTransaction> userTransactions = new ArrayList<>();
            double clearCash = 0.0;
            double pendingEscrow = 0.0;

            boolean isProvider = (user.getRole() == UserRole.TRADESPERSON);

            for (PaymentTransaction pt : payments) {
                boolean isClientSender = user.getId().equals(pt.getClientId());
                boolean isProviderRecipient = user.getId().equals(pt.getProviderId());

                if (isClientSender || isProviderRecipient || user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.SUPER_ADMIN) {
                    userTransactions.add(pt);

                    double amt = pt.getAmount() != null ? pt.getAmount() : 0.0;
                    String status = pt.getStatus() != null ? pt.getStatus().toUpperCase() : "PENDING_APPROVAL";

                    if (isProvider) {
                        if ("APPROVED".equals(status)) {
                            clearCash += amt;
                        } else if ("PENDING_APPROVAL".equals(status)) {
                            pendingEscrow += amt;
                        }
                    } else { // Client or Admin view
                        if ("PENDING_APPROVAL".equals(status)) {
                            pendingEscrow += amt;
                        } else if ("DECLINED".equals(status) && isClientSender) {
                            // Paid back / refunded to client account
                            clearCash += amt;
                        }
                    }
                }
            }

            binding.textClearCash.setText(String.format(Locale.US, "R %.2f", clearCash));
            binding.textPendingEscrow.setText(String.format(Locale.US, "R %.2f", pendingEscrow));

            if (userTransactions.isEmpty()) {
                binding.textEmptyTransactions.setVisibility(View.VISIBLE);
            } else {
                binding.textEmptyTransactions.setVisibility(View.GONE);
            }

            binding.recyclerTransactions.setLayoutManager(new LinearLayoutManager(this));
            binding.recyclerTransactions.setAdapter(new TransactionAdapter(userTransactions, user.getId()));
        });
    }

    private void saveBankDetails() {
        String holder = binding.layoutPayout.editHolderName.getText() != null ? 
                binding.layoutPayout.editHolderName.getText().toString().trim() : "";
        String bank = binding.layoutPayout.spinnerBank.getSelectedItem() != null ? 
                binding.layoutPayout.spinnerBank.getSelectedItem().toString() : "";
        String account = binding.layoutPayout.editAccountNumber.getText() != null ? 
                binding.layoutPayout.editAccountNumber.getText().toString().trim() : "";

        if (holder.length() == 0 || account.length() == 0) {
            Toast.makeText(this, "Please fill all bank details", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = DataManager.getInstance().getCurrentUser();
        if (user == null) return;

        com.example.proconnectsa.utils.UIUtils.getInstance().showProcessing(this, "Saving Bank Details...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("bank_name", bank);
        updates.put("account_number", account);

        NetworkClient.getRetrofitClient().updateProfile("eq." + user.getId(), updates).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    com.example.proconnectsa.utils.UIUtils.getInstance().showSuccess("Bank details saved successfully!", () -> finish());
                } else {
                    com.example.proconnectsa.utils.UIUtils.getInstance().showError("Save Failed", response.message());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                com.example.proconnectsa.utils.UIUtils.getInstance().showError("Connection Error", t.getMessage());
            }
        });
    }

    private class TransactionAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<TransactionAdapter.VH> {
        private final List<PaymentTransaction> list;
        private final String currentUserId;

        TransactionAdapter(List<PaymentTransaction> list, String currentUserId) {
            this.list = list;
            this.currentUserId = currentUserId;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemTransactionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            PaymentTransaction pt = list.get(position);

            boolean isSender = currentUserId.equals(pt.getClientId());
            String partyName = isSender ? 
                    "To: " + (pt.getProviderName() != null ? pt.getProviderName() : "Service Provider") :
                    "From: " + (pt.getClientName() != null ? pt.getClientName() : "Client");

            holder.b.txPartyName.setText(partyName);
            holder.b.txAmount.setText(String.format(Locale.US, "R %.2f", pt.getAmount()));
            holder.b.txDate.setText(pt.getCreatedAt() != null ? pt.getCreatedAt() : "");
            holder.b.txNote.setText("Details: " + (pt.getNotes() != null && !pt.getNotes().isEmpty() ? pt.getNotes() : "Direct Transfer"));

            String status = pt.getStatus() != null ? pt.getStatus().toUpperCase() : "PENDING_APPROVAL";

            if ("APPROVED".equals(status)) {
                holder.b.txStatus.setText("APPROVED & TRANSFERRED");
                holder.b.txStatus.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_green_dark));
                holder.b.txRefundInfo.setVisibility(View.GONE);
            } else if ("DECLINED".equals(status)) {
                holder.b.txStatus.setText("DECLINED BY ADMIN");
                holder.b.txStatus.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_red_dark));
                
                if (isSender) {
                    holder.b.txRefundInfo.setVisibility(View.VISIBLE);
                    holder.b.txRefundInfo.setText("✓ Refunded / Paid back to your account");
                } else {
                    holder.b.txRefundInfo.setVisibility(View.VISIBLE);
                    holder.b.txRefundInfo.setText("Payment was declined and refunded to sender");
                }
            } else {
                holder.b.txStatus.setText("PENDING ADMIN APPROVAL");
                holder.b.txStatus.setTextColor(androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_orange_dark));
                holder.b.txRefundInfo.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ItemTransactionBinding b;
            VH(ItemTransactionBinding b) {
                super(b.getRoot());
                this.b = b;
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
