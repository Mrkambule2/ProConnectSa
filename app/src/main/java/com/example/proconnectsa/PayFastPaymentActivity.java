package com.example.proconnectsa;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proconnectsa.databinding.ActivityPayfastPaymentBinding;
import com.example.proconnectsa.network.PayFastConfig;

import java.util.Locale;

public class PayFastPaymentActivity extends AppCompatActivity {

    private static final String TAG = "PayFastPayment";
    private ActivityPayfastPaymentBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPayfastPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.payfastToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupWebView();
        initiatePayFastPayment();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings webSettings = binding.payfastWebview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);

        binding.payfastWebview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                binding.payfastProgress.setVisibility(View.VISIBLE);

                Log.d(TAG, "Navigating URL: " + url);

                if (url.contains(PayFastConfig.RETURN_URL) || url.contains("payfast/success") || url.contains("finish")) {
                    Toast.makeText(PayFastPaymentActivity.this, "PayFast Payment Successful!", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                } else if (url.contains(PayFastConfig.CANCEL_URL) || url.contains("payfast/cancel")) {
                    Toast.makeText(PayFastPaymentActivity.this, "PayFast Payment Cancelled", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                binding.payfastProgress.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "shouldOverrideUrlLoading: " + url);

                if (url.contains(PayFastConfig.RETURN_URL) || url.contains("payfast/success")) {
                    Toast.makeText(PayFastPaymentActivity.this, "PayFast Payment Authorized!", Toast.LENGTH_LONG).show();
                    setResult(RESULT_OK);
                    finish();
                    return true;
                } else if (url.contains(PayFastConfig.CANCEL_URL) || url.contains("payfast/cancel")) {
                    Toast.makeText(PayFastPaymentActivity.this, "PayFast Payment Cancelled", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish();
                    return true;
                }
                return false;
            }
        });
    }

    private void initiatePayFastPayment() {
        Intent intent = getIntent();
        double amount = intent.getDoubleExtra("amount", 0.0);
        String itemName = intent.getStringExtra("item_name");
        String paymentId = intent.getStringExtra("m_payment_id");
        String firstName = intent.getStringExtra("first_name");
        String lastName = intent.getStringExtra("last_name");
        String email = intent.getStringExtra("email");

        if (itemName == null) itemName = "ProConnectSA Service";
        if (firstName == null || firstName.isEmpty()) firstName = "ProConnect";
        if (lastName == null || lastName.isEmpty()) lastName = "Client";
        if (email == null) email = "client@proconnectsa.co.za";

        try {
            boolean isTest = PayFastConfig.isTestMode(this);
            String merchantId = PayFastConfig.getMerchantId(this);
            String merchantKey = PayFastConfig.getMerchantKey(this);
            String processUrl = PayFastConfig.getProcessUrl(this);

            Log.d(TAG, "Initiating PayFast. TestMode: " + isTest + ", MerchantID: " + merchantId + ", URL: " + processUrl);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setSubtitle(isTest ? "Sandbox Test Mode" : "Live Production Mode");
            }

            String htmlForm = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"></head>" +
                    "<body onload=\"document.forms['payfast'].submit()\">" +
                    "<div style=\"text-align: center; margin-top: 50px; font-family: sans-serif;\">" +
                    "<h3>Connecting to PayFast Secure Gateway...</h3>" +
                    "<p>Please wait...</p>" +
                    "</div>" +
                    "<form name=\"payfast\" action=\"" + processUrl + "\" method=\"post\">" +
                    "<input type=\"hidden\" name=\"merchant_id\" value=\"" + merchantId.trim() + "\"/>" +
                    "<input type=\"hidden\" name=\"merchant_key\" value=\"" + merchantKey.trim() + "\"/>" +
                    "<input type=\"hidden\" name=\"return_url\" value=\"" + PayFastConfig.RETURN_URL + "\"/>" +
                    "<input type=\"hidden\" name=\"cancel_url\" value=\"" + PayFastConfig.CANCEL_URL + "\"/>" +
                    "<input type=\"hidden\" name=\"notify_url\" value=\"" + PayFastConfig.NOTIFY_URL + "\"/>" +
                    "<input type=\"hidden\" name=\"name_first\" value=\"" + firstName.trim() + "\"/>" +
                    "<input type=\"hidden\" name=\"name_last\" value=\"" + lastName.trim() + "\"/>" +
                    "<input type=\"hidden\" name=\"email_address\" value=\"" + email.trim() + "\"/>" +
                    "<input type=\"hidden\" name=\"m_payment_id\" value=\"" + (paymentId != null ? paymentId.trim() : "1001") + "\"/>" +
                    "<input type=\"hidden\" name=\"amount\" value=\"" + String.format(Locale.US, "%.2f", amount) + "\"/>" +
                    "<input type=\"hidden\" name=\"item_name\" value=\"" + itemName.trim() + "\"/>" +
                    "</form></body></html>";

            binding.payfastWebview.loadDataWithBaseURL("https://sandbox.payfast.co.za", htmlForm, "text/html", "UTF-8", null);

        } catch (Exception e) {
            Log.e(TAG, "Error initiating PayFast request: " + e.getMessage());
            Toast.makeText(this, "Error initializing payment: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        if (PayFastConfig.isTestMode(this)) {
            getMenuInflater().inflate(R.menu.menu_payfast_test, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_approve_test_payment) {
            Toast.makeText(this, "Test Sandbox Payment Authorized!", Toast.LENGTH_LONG).show();
            setResult(RESULT_OK);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
