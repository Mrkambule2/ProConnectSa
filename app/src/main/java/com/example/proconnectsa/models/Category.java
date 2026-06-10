package com.example.proconnectsa.models;

import com.google.gson.annotations.SerializedName;

public class Category {
    @SerializedName("name")
    private String name;

    @SerializedName("icon_res_id")
    private int iconResId;

    public Category(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }
}