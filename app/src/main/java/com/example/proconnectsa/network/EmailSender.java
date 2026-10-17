package com.example.proconnectsa.network;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Utility to send emails via Gmail SMTP.
 * Note: Requires 'com.sun.mail:android-mail' and 'com.sun.mail:android-activation' dependencies.
 */
public class EmailSender {

    private static final String SENDER_EMAIL = "muzinkambule789@gmail.com";
    private static final String APP_PASSWORD = "cvil nmlo gpsb hnnb"; // Gmail App Password
    private static final String DISPLAY_NAME = "SAConnect";

    public interface EmailListener {
        void onSuccess();
        void onFailure(String error);
    }

    public static void sendSimpleEmail(String recipientEmail, String subject, String body, EmailListener listener) {
        new SendEmailTask(recipientEmail, subject, body, listener).execute();
    }

    private static class SendEmailTask extends AsyncTask<Void, Void, Boolean> {
        private final String recipientEmail;
        private final String subject;
        private final String body;
        private final EmailListener listener;
        private String errorMessage;

        SendEmailTask(String recipientEmail, String subject, String body, EmailListener listener) {
            this.recipientEmail = recipientEmail;
            this.subject = subject;
            this.body = body;
            this.listener = listener;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    String cleanPass = APP_PASSWORD.replaceAll("\\s+", "");
                    Log.d("EmailSender", "Authenticating as: " + SENDER_EMAIL);
                    return new PasswordAuthentication(SENDER_EMAIL, cleanPass);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL, DISPLAY_NAME));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);
                return true;
            } catch (Exception e) {
                Log.e("EmailSender", "Error sending email", e);
                errorMessage = e.getMessage();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                listener.onSuccess();
            } else {
                listener.onFailure(errorMessage);
            }
        }
    }
}
