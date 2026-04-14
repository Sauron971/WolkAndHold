package com.kyas.wolkandhold.ui.data.model;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser {

    private Long userId;
    private String displayName;

    private String jwtToken;

    public LoggedInUser(Long userId, String displayName, String jwtToken) {
        this.userId = userId;
        this.displayName = displayName;
        this.jwtToken = jwtToken;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return "LoggedInUser{" +
                "userId=" + userId +
                ", displayName='" + displayName + '\'' +
                ", jwtToken='" + jwtToken + '\'' +
                '}';
    }
}