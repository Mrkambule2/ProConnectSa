package com.example.proconnectsa.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.proconnectsa.DataManager;
import com.example.proconnectsa.models.PaymentTransaction;
import com.example.proconnectsa.models.User;
import com.example.proconnectsa.network.EmailSender;
import com.example.proconnectsa.network.NetworkClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentManager {

    private static final String TAG = "PaymentManager";
    private static final String PREF_NAME = "proconnect_payments_pref";
    private static final String KEY_PAYMENTS = "payments_list";

    private static PaymentManager instance;

    public interface PaymentCallback {
        void onSuccess(String message);
        void onError(String error);
    }

    public interface PaymentListCallback {
        void onLoaded(List<PaymentTransaction> payments);
    }

    private PaymentManager() {}

    public static synchronized PaymentManager getInstance() {
        if (instance == null) {
            instance = new PaymentManager();
        }
        return instance;
    }

    private SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // Load local cache
    public List<PaymentTransaction> getLocalPayments(Context context) {
        List<PaymentTransaction> list = new ArrayList<>();
        String jsonStr = getPrefs(context).getString(KEY_PAYMENTS, null);
        if (jsonStr != null) {
            try {
                JSONArray array = new JSONArray(jsonStr);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    PaymentTransaction pt = new PaymentTransaction();
                    pt.setId(obj.optString("id"));
                    pt.setClientId(obj.optString("client_id"));
                    pt.setClientName(obj.optString("client_name"));
                    pt.setProviderId(obj.optString("provider_id"));
                    pt.setProviderName(obj.optString("provider_name"));
                    pt.setJobId(obj.optString("job_id"));
                    pt.setJobTitle(obj.optString("job_title"));
                    pt.setAmount(obj.optDouble("amount", 0.0));
                    pt.setStatus(obj.optString("status"));
                    pt.setCreatedAt(obj.optString("created_at"));
                    pt.setNotes(obj.optString("notes"));
                    pt.setAdminNote(obj.optString("admin_note"));
                    list.add(pt);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed parsing local payments: " + e.getMessage());
            }
        }
        return list;
    }

    // Save local cache
    private void saveLocalPayments(Context context, List<PaymentTransaction> payments) {
        try {
            JSONArray array = new JSONArray();
            for (PaymentTransaction pt : payments) {
                JSONObject obj = new JSONObject();
                obj.put("id", pt.getId());
                obj.put("client_id", pt.getClientId());
                obj.put("client_name", pt.getClientName());
                obj.put("provider_id", pt.getProviderId());
                obj.put("provider_name", pt.getProviderName());
                obj.put("job_id", pt.getJobId());
                obj.put("job_title", pt.getJobTitle());
                obj.put("amount", pt.getAmount());
                obj.put("status", pt.getStatus());
                obj.put("created_at", pt.getCreatedAt());
                obj.put("notes", pt.getNotes());
                obj.put("admin_note", pt.getAdminNote());
                array.put(obj);
            }
            getPrefs(context).edit().putString(KEY_PAYMENTS, array.toString()).apply();
        } catch (Exception e) {
            Log.e(TAG, "Failed saving local payments: " + e.getMessage());
        }
    }

    public void createPayment(Context context, PaymentTransaction payment, PaymentCallback callback) {
        if (payment.getId() == null || payment.getId().trim().isEmpty()) {
            payment.setId(UUID.randomUUID().toString());
        }
        if (payment.getCreatedAt() == null || payment.getCreatedAt().trim().isEmpty()) {
            payment.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date()));
        }
        payment.setStatus("PENDING_APPROVAL");

        // Save locally
        List<PaymentTransaction> localList = getLocalPayments(context);
        localList.add(0, payment);
        saveLocalPayments(context, localList);

        // Send to remote database
        List<Map<String, Object>> body = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        map.put("id", payment.getId());
        map.put("client_id", payment.getClientId());
        map.put("client_name", payment.getClientName());
        map.put("provider_id", payment.getProviderId());
        map.put("provider_name", payment.getProviderName());
        map.put("job_id", payment.getJobId());
        map.put("job_title", payment.getJobTitle());
        map.put("amount", payment.getAmount());
        map.put("status", "PENDING_APPROVAL");
        map.put("created_at", payment.getCreatedAt());
        map.put("notes", payment.getNotes());
        body.add(map);

        NetworkClient.getRetrofitClient().createPayment(body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d(TAG, "createPayment response code: " + response.code());
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "createPayment error: " + t.getMessage());
            }
        });

        // Send confirmation email
        sendPaymentCreatedEmail(payment);

        callback.onSuccess("Payment of R" + String.format(Locale.US, "%.2f", payment.getAmount()) + 
                " submitted for Admin Approval.");
    }

    public void fetchPayments(Context context, PaymentListCallback callback) {
        NetworkClient.getRetrofitClient().getPayments(null, null, null, "created_at.desc")
                .enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override
                    public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                        List<PaymentTransaction> localList = getLocalPayments(context);
                        List<PaymentTransaction> mergedList = new ArrayList<>(localList);

                        if (response.isSuccessful() && response.body() != null) {
                            for (Map<String, Object> m : response.body()) {
                                PaymentTransaction pt = mapToPayment(m);
                                if (pt != null) {
                                    boolean exists = false;
                                    for (PaymentTransaction existing : mergedList) {
                                        if (existing.getId() != null && existing.getId().equals(pt.getId())) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) mergedList.add(pt);
                                }
                            }
                        }
                        saveLocalPayments(context, mergedList);
                        callback.onLoaded(mergedList);
                    }

                    @Override
                    public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                        callback.onLoaded(getLocalPayments(context));
                    }
                });
    }

    public void approvePayment(Context context, PaymentTransaction payment, PaymentCallback callback) {
        payment.setStatus("APPROVED");

        List<PaymentTransaction> list = getLocalPayments(context);
        for (PaymentTransaction pt : list) {
            if (pt.getId().equals(payment.getId())) {
                pt.setStatus("APPROVED");
                break;
            }
        }
        saveLocalPayments(context, list);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "APPROVED");
        NetworkClient.getRetrofitClient().updatePaymentStatus("eq." + payment.getId(), updates).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d(TAG, "approvePayment response: " + response.code());
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "approvePayment error: " + t.getMessage());
            }
        });

        sendPaymentApprovedEmail(payment);

        callback.onSuccess("Payment APPROVED! R" + String.format(Locale.US, "%.2f", payment.getAmount()) +
                " transferred to " + payment.getProviderName() + "'s account.");
    }

    public void declinePayment(Context context, PaymentTransaction payment, String reason, PaymentCallback callback) {
        payment.setStatus("DECLINED");
        payment.setAdminNote(reason);

        List<PaymentTransaction> list = getLocalPayments(context);
        for (PaymentTransaction pt : list) {
            if (pt.getId().equals(payment.getId())) {
                pt.setStatus("DECLINED");
                pt.setAdminNote(reason);
                break;
            }
        }
        saveLocalPayments(context, list);

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "DECLINED");
        if (reason != null && !reason.trim().isEmpty()) {
            updates.put("admin_note", reason);
        }
        NetworkClient.getRetrofitClient().updatePaymentStatus("eq." + payment.getId(), updates).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                Log.d(TAG, "declinePayment response: " + response.code());
            }
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "declinePayment error: " + t.getMessage());
            }
        });

        sendPaymentDeclinedEmail(payment, reason);

        callback.onSuccess("Payment DECLINED! R" + String.format(Locale.US, "%.2f", payment.getAmount()) +
                " paid back to sender account (" + payment.getClientName() + ").");
    }

    private PaymentTransaction mapToPayment(Map<String, Object> map) {
        try {
            PaymentTransaction pt = new PaymentTransaction();
            pt.setId((String) map.get("id"));
            pt.setClientId((String) map.get("client_id"));
            pt.setClientName((String) map.get("client_name"));
            pt.setProviderId((String) map.get("provider_id"));
            pt.setProviderName((String) map.get("provider_name"));
            pt.setJobId((String) map.get("job_id"));
            pt.setJobTitle((String) map.get("job_title"));
            
            Object amt = map.get("amount");
            if (amt instanceof Number) {
                pt.setAmount(((Number) amt).doubleValue());
            }
            pt.setStatus((String) map.get("status"));
            pt.setCreatedAt((String) map.get("created_at"));
            pt.setNotes((String) map.get("notes"));
            pt.setAdminNote((String) map.get("admin_note"));
            return pt;
        } catch (Exception e) {
            Log.e(TAG, "mapToPayment error: " + e.getMessage());
            return null;
        }
    }

    private void sendPaymentCreatedEmail(PaymentTransaction pt) {
        User currentUser = DataManager.getInstance().getCurrentUser();
        String senderEmail = currentUser != null ? currentUser.getEmail() : null;
        if (senderEmail != null) {
            String subject = "Payment Submitted - Pending Admin Approval";
            String body = "Hello " + pt.getClientName() + ",\n\n" +
                    "Your payment of R" + String.format(Locale.US, "%.2f", pt.getAmount()) +
                    " to " + pt.getProviderName() + " has been recorded.\n" +
                    "It is currently PENDING ADMIN APPROVAL before being released to the service provider.\n\n" +
                    "If declined by Admin, the full amount will be refunded directly back to your account.\n\n" +
                    "Regards,\nProConnectSA Financials";
            EmailSender.sendSimpleEmail(senderEmail, subject, body, new EmailSender.EmailListener() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String error) {}
            });
        }
    }

    private void sendPaymentApprovedEmail(PaymentTransaction pt) {
        String subject = "Payment Approved & Transferred - ProConnectSA";
        String bodyClient = "Hello " + pt.getClientName() + ",\n\n" +
                "Your payment of R" + String.format(Locale.US, "%.2f", pt.getAmount()) +
                " to " + pt.getProviderName() + " has been APPROVED by Admin and successfully transferred.\n\n" +
                "Regards,\nProConnectSA Team";
        
        User currentUser = DataManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            EmailSender.sendSimpleEmail(currentUser.getEmail(), subject, bodyClient, new EmailSender.EmailListener() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String error) {}
            });
        }
    }

    private void sendPaymentDeclinedEmail(PaymentTransaction pt, String reason) {
        String subject = "Payment Declined - Refunded to Your Account";
        String reasonStr = (reason != null && !reason.trim().isEmpty()) ? "\nReason: " + reason : "";
        String body = "Hello " + pt.getClientName() + ",\n\n" +
                "Your payment of R" + String.format(Locale.US, "%.2f", pt.getAmount()) +
                " to " + pt.getProviderName() + " has been DECLINED by Admin." + reasonStr + "\n\n" +
                "The full amount of R" + String.format(Locale.US, "%.2f", pt.getAmount()) +
                " has been PAID BACK / REFUNDED to your account.\n\n" +
                "Regards,\nProConnectSA Team";

        User currentUser = DataManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            EmailSender.sendSimpleEmail(currentUser.getEmail(), subject, body, new EmailSender.EmailListener() {
                @Override public void onSuccess() {}
                @Override public void onFailure(String error) {}
            });
        }
    }
}
