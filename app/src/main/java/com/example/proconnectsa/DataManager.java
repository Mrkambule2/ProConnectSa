package com.example.proconnectsa;

import com.example.proconnectsa.models.User;

public class DataManager {
    private static DataManager instance;
    private User currentUser;
    private String accessToken;

    private DataManager() {
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public User getCurrentUser() { return currentUser; }
    public void setCurrentUser(User user) { this.currentUser = user; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}
