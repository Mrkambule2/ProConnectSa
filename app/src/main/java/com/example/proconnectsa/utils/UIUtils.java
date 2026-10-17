package com.example.proconnectsa.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.proconnectsa.R;

import java.lang.ref.WeakReference;

public class UIUtils {

    private Dialog currentDialog;
    private WeakReference<Activity> activityRef;
    private static UIUtils instance;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static synchronized UIUtils getInstance() {
        if (instance == null) {
            instance = new UIUtils();
        }
        return instance;
    }

    public void showProcessing(Context context, String message) {
        if (!(context instanceof Activity)) return;
        final Activity activity = (Activity) context;
        activityRef = new WeakReference<>(activity);
        
        mainHandler.post(() -> {
            if (isDead()) return;
            
            // Close any existing dialog safely
            try {
                if (currentDialog != null && currentDialog.isShowing()) {
                    currentDialog.dismiss();
                }
            } catch (Exception ignored) {}

            try {
                currentDialog = new Dialog(activity);
                currentDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                View view = LayoutInflater.from(activity).inflate(R.layout.dialog_feedback, null);
                currentDialog.setContentView(view);
                
                Window window = currentDialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    window.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);
                }
                
                TextView msgText = view.findViewById(R.id.feedback_message);
                if (msgText != null) msgText.setText(message);
                
                currentDialog.setCancelable(false);
                currentDialog.show();
            } catch (Exception e) {
                Log.e("UIUtils", "Failed to show dialog: " + e.getMessage());
            }
        });
    }

    public void showSuccess(String message, Runnable onComplete) {
        update(true, message, null, onComplete);
    }

    public void showError(String message, String detail) {
        update(false, message, detail, null);
    }

    private void update(boolean success, String message, String detail, Runnable onComplete) {
        mainHandler.post(() -> {
            if (currentDialog == null || !currentDialog.isShowing() || isDead()) {
                Activity act = activityRef != null ? activityRef.get() : null;
                if (act != null) {
                    if (!success) {
                        Toast.makeText(act, message + (detail != null ? "\n" + detail : ""), Toast.LENGTH_LONG).show();
                    } else if (onComplete != null) {
                        onComplete.run();
                    }
                }
                return;
            }

            try {
                ProgressBar progress = currentDialog.findViewById(R.id.feedback_progress);
                ImageView icon = currentDialog.findViewById(R.id.feedback_icon);
                TextView msgText = currentDialog.findViewById(R.id.feedback_message);
                TextView subMsgText = currentDialog.findViewById(R.id.feedback_submessage);

                if (progress != null) progress.setVisibility(View.GONE);
                if (icon != null) {
                    icon.setVisibility(View.VISIBLE);
                    icon.setImageResource(success ? android.R.drawable.checkbox_on_background : android.R.drawable.ic_delete);
                    icon.setColorFilter(Color.parseColor(success ? "#4CAF50" : "#F44336"));
                }

                if (msgText != null) msgText.setText(message);
                if (subMsgText != null && detail != null) {
                    subMsgText.setVisibility(View.VISIBLE);
                    subMsgText.setText(detail);
                }

                if (success) {
                    mainHandler.postDelayed(() -> {
                        hideDialog();
                        if (onComplete != null) onComplete.run();
                    }, 1500);
                } else {
                    currentDialog.setCancelable(true);
                    currentDialog.setCanceledOnTouchOutside(true);
                }
            } catch (Exception e) {
                hideDialog();
                if (onComplete != null && success) onComplete.run();
            }
        });
    }

    public void hideDialog() {
        mainHandler.post(() -> {
            try {
                if (currentDialog != null && currentDialog.isShowing()) {
                    currentDialog.dismiss();
                }
            } catch (Exception ignored) {}
            currentDialog = null;
        });
    }

    private boolean isDead() {
        if (activityRef == null) return true;
        Activity a = activityRef.get();
        return a == null || a.isFinishing() || a.isDestroyed();
    }
}
