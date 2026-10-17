package com.example.proconnectsa;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.proconnectsa.R;
import com.example.proconnectsa.databinding.FragmentSecondBinding;
import com.example.proconnectsa.models.PaymentTransaction;
import com.example.proconnectsa.models.Tradesperson;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.utils.PaymentManager;
import com.example.proconnectsa.utils.UIUtils;

import java.util.UUID;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private PaymentTransaction pendingPayFastTransaction;

    private final ActivityResultLauncher<Intent> payfastLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && pendingPayFastTransaction != null) {
                    UIUtils.getInstance().showProcessing(getContext(), "Processing PayFast Escrow...");
                    PaymentManager.getInstance().createPayment(getContext(), pendingPayFastTransaction, new PaymentManager.PaymentCallback() {
                        @Override
                        public void onSuccess(String message) {
                            UIUtils.getInstance().showSuccess("PayFast Payment Authorized!\n" + message, null);
                            pendingPayFastTransaction = null;
                        }

                        @Override
                        public void onError(String error) {
                            UIUtils.getInstance().showError("Payment Error", error);
                        }
                    });
                } else {
                    Toast.makeText(getContext(), "PayFast payment was cancelled or not completed.", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            Tradesperson tp = (Tradesperson) getArguments().getSerializable("tradesperson");
            if (tp != null) {
                binding.detailName.setText(tp.getName());
                binding.detailTrade.setText(tp.getTrade() + " • " + tp.getArea());
                binding.detailRating.setRating(tp.getRating());
                binding.detailAbout.setText(tp.getDescription());
                
                com.bumptech.glide.Glide.with(this)
                        .load(tp.getProfileImageUrl())
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .into(binding.detailImage);
            }
        }

        binding.btnBookNow.setOnClickListener(v -> {
            if (getArguments() != null) {
                Tradesperson tp = (Tradesperson) getArguments().getSerializable("tradesperson");
                if (tp != null) {
                    Intent intent = new Intent(getContext(), PostJobActivity.class);
                    intent.putExtra("tradesperson_id", tp.getId());
                    intent.putExtra("tradesperson_name", tp.getName());
                    intent.putExtra("tradesperson_email", tp.getEmail());
                    intent.putExtra("trade_category", tp.getTrade());
                    startActivity(intent);
                }
            }
        });

        binding.btnMessage.setOnClickListener(v -> {
            if (getArguments() != null) {
                Tradesperson tp = (Tradesperson) getArguments().getSerializable("tradesperson");
                if (tp != null) {
                    Intent intent = new Intent(getContext(), ChatActivity.class);
                    intent.putExtra("receiver_id", tp.getId());
                    intent.putExtra("receiver_name", tp.getName());
                    startActivity(intent);
                }
            }
        });

        binding.btnPayProvider.setOnClickListener(v -> {
            if (getArguments() != null) {
                Tradesperson tp = (Tradesperson) getArguments().getSerializable("tradesperson");
                if (tp != null) {
                    showPayDialog(tp);
                }
            }
        });
    }

    private void showPayDialog(Tradesperson tp) {
        User currentUser = DataManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to make a payment", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_make_payment, null);
        android.widget.TextView textRecipient = dialogView.findViewById(R.id.text_payment_recipient);
        android.widget.TextView textJobTitle = dialogView.findViewById(R.id.text_payment_job_title);
        com.google.android.material.textfield.TextInputEditText editAmount = dialogView.findViewById(R.id.edit_payment_amount);
        com.google.android.material.textfield.TextInputEditText editNotes = dialogView.findViewById(R.id.edit_payment_notes);
        android.widget.RadioButton radioTest = dialogView.findViewById(R.id.radio_payfast_test);

        boolean currentTestMode = com.example.proconnectsa.network.PayFastConfig.isTestMode(getContext());
        if (radioTest != null) {
            radioTest.setChecked(currentTestMode);
        }

        textRecipient.setText("Paying: " + tp.getName());
        textJobTitle.setText("Service: " + tp.getTrade());

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Pay via PayFast (Instant EFT / Card)")
                .setView(dialogView)
                .setPositiveButton("Pay with PayFast", (dialog, which) -> {
                    String amountStr = editAmount.getText() != null ? editAmount.getText().toString().trim() : "";
                    String notes = editNotes.getText() != null ? editNotes.getText().toString().trim() : "";

                    if (radioTest != null) {
                        boolean isTest = radioTest.isChecked();
                        com.example.proconnectsa.network.PayFastConfig.setTestMode(getContext(), isTest);
                    }

                    if (amountStr.length() == 0) {
                        Toast.makeText(getContext(), "Please enter payment amount", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double amount = Double.parseDouble(amountStr);
                        if (amount <= 0) {
                            Toast.makeText(getContext(), "Amount must be greater than zero", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        PaymentTransaction pt = new PaymentTransaction();
                        pt.setId(UUID.randomUUID().toString());
                        pt.setClientId(currentUser.getId());
                        pt.setClientName(currentUser.getName());
                        pt.setProviderId(tp.getId());
                        pt.setProviderName(tp.getName());
                        pt.setJobTitle("Direct Booking: " + tp.getTrade());
                        pt.setAmount(amount);
                        pt.setNotes(notes);

                        pendingPayFastTransaction = pt;

                        // Launch PayFast Checkout Activity
                        Intent intent = new Intent(getContext(), PayFastPaymentActivity.class);
                        intent.putExtra("amount", amount);
                        intent.putExtra("item_name", "Payment for " + tp.getTrade() + " (" + tp.getName() + ")");
                        intent.putExtra("m_payment_id", pt.getId());
                        intent.putExtra("first_name", currentUser.getName());
                        intent.putExtra("email", currentUser.getEmail());
                        
                        payfastLauncher.launch(intent);

                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Invalid amount format", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}
