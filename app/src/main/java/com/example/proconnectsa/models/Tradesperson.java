package com.example.proconnectsa.models;

import java.io.Serializable;

public class Tradesperson implements Serializable {
    private String id;
    private String name;
    private String trade;
    private String area;
    private float rating;
    private String profileImageUrl;
    private String description;
    private String email;

    public Tradesperson() {}

    public Tradesperson(String id, String name, String trade, String area, float rating, String profileImageUrl) {
        this.id = id;
        this.name = name;
        this.trade = trade;
        this.area = area;
        this.rating = rating;
        this.profileImageUrl = profileImageUrl;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public String getTrade() { return trade; }
    public String getArea() { return area; }
    public float getRating() { return rating; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
