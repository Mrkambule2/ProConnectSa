package com.example.proconnectsa.models;

import com.google.gson.annotations.SerializedName;

public class Message {
    @SerializedName("id")
    private String id;
    
    @SerializedName("sender_id")
    private String senderId;
    
    @SerializedName("text")
    private String text;
    
    @SerializedName("timestamp")
    private long timestamp;
    
    @SerializedName("image_url")
    private String imageUrl;

    public Message(String senderId, String text, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderId() { return senderId; }
    public String getText() { return text; }
    public long getTimestamp() { return timestamp; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
