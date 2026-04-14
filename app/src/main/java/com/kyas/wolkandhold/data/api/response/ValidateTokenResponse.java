package com.kyas.wolkandhold.data.api.response;

import com.google.gson.annotations.SerializedName;

public class ValidateTokenResponse {

    @SerializedName(value = "username", alternate = {"userName", "name"})
    private String username;

    @SerializedName(value = "userId", alternate = {"id", "user_id"})
    private Long userId;

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
