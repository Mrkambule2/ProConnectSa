package com.example.proconnectsa.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Job implements Serializable {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("status")
    private String status;

    @SerializedName("date")
    private String date;

    @SerializedName("location")
    private String location;

    @SerializedName("provider")
    private String provider;

    @SerializedName("client_id")
    private String clientId;

    @SerializedName("description")
    private String description;

    @SerializedName("category")
    private String category;

    @SerializedName("budget")
    private Double budget;

    public Job(String title, String status, String location, String provider) {
        this.title = title;
        this.status = status;
        this.location = location;
        this.provider = provider;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getStatus() { return status; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getLocation() { return location; }
    public String getProvider() { return provider; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }
}