package com.example.proconnectsa.network;

import com.example.proconnectsa.models.Category;
import com.example.proconnectsa.models.Job;
import com.example.proconnectsa.models.Message;
import com.example.proconnectsa.models.Tradesperson;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Query;

public interface SupabaseService {

    @POST("auth/v1/signup")
    Call<Map<String, Object>> signUp(@Body Map<String, Object> body);

    @POST("auth/v1/token?grant_type=password")
    Call<Map<String, Object>> signIn(@Body Map<String, String> body);

    @POST("auth/v1/recover")
    Call<Void> recoverPassword(@Body Map<String, String> body);

    @POST("auth/v1/verify")
    Call<Map<String, Object>> verifyOTP(@Body Map<String, String> body);

    @PUT("auth/v1/user")
    Call<Void> updatePassword(
            @retrofit2.http.Header("Authorization") String token,
            @Body Map<String, String> body
    );

    @GET("rest/v1/profiles")
    Call<List<Map<String, Object>>> getProfiles(
            @Query("id") String id,
            @Query("role") String role,
            @Query("is_verified") String isVerified,
            @Query("trade_category") String tradeCategory,
            @Query("select") String select
    );

    @GET("rest/v1/profiles")
    Call<List<Map<String, Object>>> getProfilesByEmail(
            @Query("email") String emailFilter,
            @Query("select") String select
    );

    @PATCH("rest/v1/profiles")
    Call<Void> updateProfile(@Query("id") String id, @Body Map<String, Object> updates);

    @retrofit2.http.DELETE("rest/v1/profiles")
    Call<Void> deleteProfile(@Query("id") String id);

    @POST("rest/v1/profiles?on_conflict=id")
    @retrofit2.http.Headers("Prefer: resolution=merge-duplicates")
    Call<Void> upsertProfile(@Body List<Map<String, Object>> profile);

    @GET("rest/v1/categories?select=*")
    Call<List<Category>> getCategories();

    @POST("rest/v1/categories")
    Call<List<Category>> addCategory(@Body Category category);

    @retrofit2.http.DELETE("rest/v1/categories")
    Call<Void> deleteCategory(@Query("name") String nameFilter);

    @GET("rest/v1/jobs")
    Call<List<Job>> getJobs(
            @Query("status") String status,
            @Query("client_id") String clientId,
            @Query("provider_id") String providerId
    );

    @POST("rest/v1/jobs")
    Call<Void> createJob(@Body List<Map<String, Object>> jobs);

    @PATCH("rest/v1/jobs")
    Call<Void> updateJobStatus(@Query("id") String id, @Body Map<String, String> updates);

    @GET("rest/v1/profiles?role=eq.TRADESPERSON")
    Call<List<Tradesperson>> getTradespeople();

    @GET("rest/v1/messages")
    Call<List<Message>> getMessages(
            @Query("or") String orFilter,
            @Query("order") String order
    );

    @POST("rest/v1/messages")
    Call<List<Message>> sendMessage(@Body List<Message> message);

    @POST("rest/v1/messages")
    Call<List<Message>> sendMessageRaw(@Body List<Map<String, Object>> message);

    @retrofit2.http.DELETE("rest/v1/messages")
    Call<Void> deleteMessage(@Query("id") String id);

    @POST("storage/v1/object/avatars/{path}")
    Call<Map<String, String>> uploadAvatar(
            @retrofit2.http.Path("path") String path,
            @Body okhttp3.RequestBody file
    );

    @GET("rest/v1/payments")
    Call<List<Map<String, Object>>> getPayments(
            @Query("status") String status,
            @Query("client_id") String clientId,
            @Query("provider_id") String providerId,
            @Query("order") String order
    );

    @POST("rest/v1/payments")
    Call<Void> createPayment(@Body List<Map<String, Object>> payments);

    @PATCH("rest/v1/payments")
    Call<Void> updatePaymentStatus(@Query("id") String id, @Body Map<String, Object> updates);
}
