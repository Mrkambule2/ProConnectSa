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

    public Tradesperson() {}

    public Tradesperson(String name, String trade, String area, float rating, String profileImageUrl) {
        this.name = name;
        this.trade = trade;
        this.area = area;
        this.rating = rating;
        this.profileImageUrl = profileImageUrl;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getTrade() { return trade; }
    public String getArea() { return area; }
    public float getRating() { return rating; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
