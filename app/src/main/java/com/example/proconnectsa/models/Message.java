package com.example.proconnectsa.models;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("id")
    private String id;
    
    @SerializedName("sender_id")
    private String senderId;

    @SerializedName("receiver_id")
    private String receiverId;
    
    @SerializedName("text")
    private String text;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("audio_url")
    private String audioUrl;

    @SerializedName("latitude")
    private Double latitude;

    @SerializedName("longitude")
    private Double longitude;

    public Message(String senderId, String receiverId, String text, String timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderId() { return senderId; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public String getText() { return text; }
    public String getTimestamp() { 
        return timestamp != null ? timestamp : createdAt; 
    }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getAudioUrl() { return audioUrl; }
    public void setAudioUrl(String audioUrl) { this.audioUrl = audioUrl; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
