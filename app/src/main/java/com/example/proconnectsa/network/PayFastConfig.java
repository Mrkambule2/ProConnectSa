package com.example.proconnectsa.network;

import android.content.Context;
import android.content.SharedPreferences;

public class PayFastConfig {

    // Live Production Credentials
    public static final String LIVE_MERCHANT_ID = "21017236";
    public static final String LIVE_MERCHANT_KEY = "a2aqjvtu6k7tw";

    // Personal Sandbox Test Credentials
    public static final String SANDBOX_MERCHANT_ID = "10055581";
    public static final String SANDBOX_MERCHANT_KEY = "wib5s798povnp";

    public static final String SANDBOX_URL = "https://sandbox.payfast.co.za/eng/process";
    public static final String PRODUCTION_URL = "https://www.payfast.co.za/eng/process";

    public static final String RETURN_URL = "https://www.payfast.co.za/return";
    public static final String CANCEL_URL = "https://www.payfast.co.za/cancel";
    public static final String NOTIFY_URL = "https://www.payfast.co.za/notify";

    private static final String PREF_NAME = "payfast_mode_pref";
    private static final String KEY_TEST_MODE = "is_test_mode";

    public static boolean isTestMode(Context context) {
        if (context == null) return true;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_TEST_MODE, true); // Default to true (Test Sandbox Mode)
    }

    public static void setTestMode(Context context, boolean enabled) {
        if (context == null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_TEST_MODE, enabled).apply();
    }

    public static String getMerchantId(Context context) {
        return isTestMode(context) ? SANDBOX_MERCHANT_ID : LIVE_MERCHANT_ID;
    }

    public static String getMerchantKey(Context context) {
        return isTestMode(context) ? SANDBOX_MERCHANT_KEY : LIVE_MERCHANT_KEY;
    }

    public static String getProcessUrl(Context context) {
        return isTestMode(context) ? SANDBOX_URL : PRODUCTION_URL;
    }
}
