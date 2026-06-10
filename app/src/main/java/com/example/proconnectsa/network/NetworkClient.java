package com.example.proconnectsa.network;

import java.io.IOException;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkClient {

    private static final String BASE_URL = "https://tyyexrmomanepliljnjp.supabase.co/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InR5eWV4cm1vbWFuZXBsaWxqbmpwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg1MzM5MTcsImV4cCI6MjA5NDEwOTkxN30.FQmNF0rtRxwb5JWGa_v9st7KwhhhowI5OfDhPPjCK3E";

    private static Retrofit retrofit = null;

    public static SupabaseService getRetrofitClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .addInterceptor(logging)
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            
                            // Log the URL for debugging (will show in Logcat)
                            android.util.Log.d("NetworkClient", "Request URL: " + original.url().toString());

                            Request.Builder requestBuilder = original.newBuilder()
                                    .header("apikey", API_KEY)
                                    .header("Authorization", "Bearer " + API_KEY)
                                    .header("Content-Type", "application/json");

                            // Only add Prefer header for REST API POST/PATCH calls
                            if (original.url().encodedPath().contains("/rest/v1/")) {
                                if (!original.method().equals("GET")) {
                                    requestBuilder.header("Prefer", "return=representation");
                                }
                            }

                            return chain.proceed(requestBuilder.build());
                        }
                    });

            // Workaround for SSL Handshake issues on some emulators
            setupUnsafeOkHttpClient(builder);

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(builder.build())
                    .build();
        }
        return retrofit.create(SupabaseService.class);
    }

    private static void setupUnsafeOkHttpClient(OkHttpClient.Builder builder) {
        try {
            final TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}