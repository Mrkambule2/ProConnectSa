package com.example.proconnectsa;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proconnectsa.R;
import com.example.proconnectsa.databinding.FragmentManagePaymentsBinding;
import com.example.proconnectsa.databinding.ItemPaymentAdminBinding;
import com.example.proconnectsa.models.PaymentTransaction;
import com.example.proconnectsa.utils.PaymentManager;
import com.example.proconnectsa.utils.UIUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ManagePaymentsFragment extends Fragment {

    private FragmentManagePaymentsBinding binding;
    private List<PaymentTransaction> allPayments = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentManagePaymentsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.chipGroupPayments.setOnCheckedStateChangeListener((group, checkedIds) -> applyFilter());

        refreshPayments();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshPayments();
    }

    private void refreshPayments() {
        Context context = getContext();
        if (context == null) return;
        PaymentManager.getInstance().fetchPayments(context, payments -> {
            allPayments = payments;
            applyFilter();
        });
    }

    private void applyFilter() {
        if (binding == null) return;
        
        int checkedId = binding.chipGroupPayments.getCheckedChipId();
        List<PaymentTransaction> filtered = new ArrayList<>();

        for (PaymentTransaction pt : allPayments) {
            String status = pt.getStatus() != null ? pt.getStatus().toUpperCase() : "";
            if (checkedId == R.id.chip_pay_pending) {
                if ("PENDING_APPROVAL".equals(status)) {
                    filtered.add(pt);
                }
            } else if (checkedId == R.id.chip_pay_approved) {
                if ("APPROVED".equals(status)) {
                    filtered.add(pt);
                }
            } else if (checkedId == R.id.chip_pay_declined) {
                if ("DECLINED".equals(status)) {
                    filtered.add(pt);
                }
            }
        }

        if (filtered.isEmpty()) {
            binding.textEmptyPayments.setVisibility(View.VISIBLE);
        } else {
            binding.textEmptyPayments.setVisibility(View.GONE);
        }

        binding.recyclerPayments.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerPayments.setAdapter(new AdminPaymentAdapter(filtered));
    }

    private class AdminPaymentAdapter extends RecyclerView.Adapter<AdminPaymentAdapter.VH> {
        private final List<PaymentTransaction> list;

        AdminPaymentAdapter(List<PaymentTransaction> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(ItemPaymentAdminBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            PaymentTransaction pt = list.get(position);
            Context context = holder.itemView.getContext();

            double amount = pt.getAmount() != null ? pt.getAmount() : 0.0;
            String clientStr = pt.getClientName() != null && pt.getClientName().length() > 0 ? pt.getClientName() : (pt.getClientId() != null ? pt.getClientId() : "Unknown Client");
            String providerStr = pt.getProviderName() != null && pt.getProviderName().length() > 0 ? pt.getProviderName() : (pt.getProviderId() != null ? pt.getProviderId() : "Unknown Provider");
            String notesStr = pt.getNotes() != null && pt.getNotes().length() > 0 ? pt.getNotes() : "Direct Payment";
            String dateStr = pt.getCreatedAt() != null ? pt.getCreatedAt() : "";

            String jobTitleStr = pt.getJobTitle() != null && pt.getJobTitle().length() > 0 ? pt.getJobTitle() : "Direct Booking Service";

            holder.b.paymentAdminAmount.setText(String.format(Locale.US, "R %.2f", amount));
            holder.b.paymentAdminClient.setText("Sender (Client): " + clientStr);
            holder.b.paymentAdminProvider.setText("Recipient (Provider): " + providerStr);
            holder.b.paymentAdminDate.setText("Date: " + dateStr);
            holder.b.paymentAdminNotes.setText("Note: " + notesStr);
            holder.b.paymentAdminJobStatus.setText("Service Linked: " + jobTitleStr);

            String status = pt.getStatus() != null ? pt.getStatus().toUpperCase() : "PENDING_APPROVAL";
            holder.b.paymentAdminStatusChip.setText(status.replace("_", " "));

            if ("APPROVED".equals(status)) {
                int color = ContextCompat.getColor(context, android.R.color.holo_green_light);
                holder.b.paymentAdminStatusChip.setChipBackgroundColor(ColorStateList.valueOf(color));
                holder.b.layoutAdminActions.setVisibility(View.GONE);
                holder.b.paymentAdminReason.setVisibility(View.GONE);
            } else if ("DECLINED".equals(status)) {
                int color = ContextCompat.getColor(context, android.R.color.holo_red_light);
                holder.b.paymentAdminStatusChip.setChipBackgroundColor(ColorStateList.valueOf(color));
                holder.b.layoutAdminActions.setVisibility(View.GONE);
                holder.b.paymentAdminReason.setVisibility(View.VISIBLE);
                
                String reason = pt.getAdminNote() != null && pt.getAdminNote().length() > 0 ? pt.getAdminNote() : "No reason provided";
                holder.b.paymentAdminReason.setText("Admin Action: DECLINED — Paid back to sender account (" + reason + ")");
            } else {
                int color = ContextCompat.getColor(context, android.R.color.holo_orange_light);
                holder.b.paymentAdminStatusChip.setChipBackgroundColor(ColorStateList.valueOf(color));
                holder.b.layoutAdminActions.setVisibility(View.VISIBLE);
                holder.b.paymentAdminReason.setVisibility(View.GONE);
            }

            // Approve Button
            holder.b.btnApprovePayment.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Approve Payment")
                        .setMessage("Approve transfer of R" + String.format(Locale.US, "%.2f", amount) + " to " + providerStr + "'s account?")
                        .setPositiveButton("Approve", (dialog, which) -> {
                            UIUtils.getInstance().showProcessing(context, "Processing Approval...");
                            PaymentManager.getInstance().approvePayment(context, pt, new PaymentManager.PaymentCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    UIUtils.getInstance().showSuccess(message, ManagePaymentsFragment.this::refreshPayments);
                                }

                                @Override
                                public void onError(String error) {
                                    UIUtils.getInstance().showError("Approval Error", error);
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });

            // Decline Button
            holder.b.btnDeclinePayment.setOnClickListener(v -> {
                final TextInputEditText editReason = new TextInputEditText(context);
                editReason.setHint("Reason for declining (optional)");
                
                new MaterialAlertDialogBuilder(context)
                        .setTitle("Decline Payment & Refund Sender")
                        .setMessage("Decline this payment? The full amount of R" + String.format(Locale.US, "%.2f", amount) + 
                                " will be IMMEDIATELY PAID BACK / REFUNDED to the sender (" + clientStr + ").")
                        .setView(editReason)
                        .setPositiveButton("Decline & Refund", (dialog, which) -> {
                            String reason = editReason.getText() != null ? editReason.getText().toString().trim() : "";
                            UIUtils.getInstance().showProcessing(context, "Processing Refund...");
                            PaymentManager.getInstance().declinePayment(context, pt, reason, new PaymentManager.PaymentCallback() {
                                @Override
                                public void onSuccess(String message) {
                                    UIUtils.getInstance().showSuccess(message, ManagePaymentsFragment.this::refreshPayments);
                                }

                                @Override
                                public void onError(String error) {
                                    UIUtils.getInstance().showError("Decline Error", error);
                                }
                            });
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ItemPaymentAdminBinding b;
            VH(ItemPaymentAdminBinding b) {
                super(b.getRoot());
                this.b = b;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
