package com.kyas.wolkandhold.data.api.response;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {
    @SerializedName(value = "token", alternate = {"accessToken", "jwt"})
    private String token;

    @SerializedName(value = "userId", alternate = {"id", "user_id"})
    private Long userId;

    @SerializedName(value = "username", alternate = {"userName", "name"})
    private String username;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
