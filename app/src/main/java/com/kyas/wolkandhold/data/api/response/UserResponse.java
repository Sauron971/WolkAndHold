package com.kyas.wolkandhold.data.api.response;

import com.google.gson.annotations.SerializedName;

public class UserResponse {

    @SerializedName(value = "userId", alternate = {"id", "user_id"})
    private long id;

    private String username;

    private double lat;

    private double lon;
    private boolean isCapture;

    public boolean isCapture() {
        return isCapture;
    }

    public void setCapture(boolean capture) {
        isCapture = capture;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public long getUserId() {
        return id;
    }

    public void setUserId(long userId) {
        this.id = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
