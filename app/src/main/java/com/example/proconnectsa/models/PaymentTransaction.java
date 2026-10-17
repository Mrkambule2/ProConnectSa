package com.example.proconnectsa.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class PaymentTransaction implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("client_name")
    private String clientName;

    @SerializedName("provider_id")
    private String providerId;

    @SerializedName("provider_name")
    private String providerName;

    @SerializedName("job_id")
    private String jobId;

    @SerializedName("job_title")
    private String jobTitle;

    @SerializedName("amount")
    private Double amount;

    @SerializedName("status")
    private String status; // "PENDING_APPROVAL", "APPROVED", "DECLINED"

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("notes")
    private String notes;

    @SerializedName("admin_note")
    private String adminNote;

    public PaymentTransaction() {}

    public PaymentTransaction(String id, String clientId, String clientName, String providerId, String providerName, String jobId, String jobTitle, Double amount, String status, String createdAt, String notes) {
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
        this.providerId = providerId;
        this.providerName = providerName;
        this.jobId = jobId;
        this.jobTitle = jobTitle;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getAdminNote() { return adminNote; }
    public void setAdminNote(String adminNote) { this.adminNote = adminNote; }
}
