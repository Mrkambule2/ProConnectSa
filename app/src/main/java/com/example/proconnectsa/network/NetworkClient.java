package com.example.proconnectsa.network;

import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import com.example.proconnectsa.DataManager;

public class NetworkClient {

    private static final String BASE_URL = "https://tyyexrmomanepliljnjp.supabase.co/";
    private static final String API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InR5eWV4cm1vbWFuZXBsaWxqbmpwIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg1MzM5MTcsImV4cCI6MjA5NDEwOTkxN30.FQmNF0rtRxwb5JWGa_v9st7KwhhhowI5OfDhPPjCK3E";

    private static Retrofit retrofit = null;

    public static SupabaseService getRetrofitClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder requestBuilder = original.newBuilder()
                                .header("apikey", API_KEY);

                        if (original.header("Content-Type") == null) {
                            requestBuilder.header("Content-Type", "application/json");
                        }

                        String userToken = DataManager.getInstance().getAccessToken();
                        if (userToken != null && !userToken.isEmpty()) {
                            // If we have a user JWT, we MUST use it for all requests
                            requestBuilder.header("Authorization", "Bearer " + userToken);
                        } else {
                            // If not logged in, we use the anon key as a Bearer token
                            requestBuilder.header("Authorization", "Bearer " + API_KEY);
                        }

                        // Required for Supabase REST to return updated data
                        if (original.url().encodedPath().contains("/rest/v1/")) {
                            if (!"GET".equals(original.method())) {
                                requestBuilder.header("Prefer", "return=representation");
                            }
                        }
                        
                        return chain.proceed(requestBuilder.build());
                    })
                    .addInterceptor(logging);

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
