package com.kyas.wolkandhold.ui.data;

import com.kyas.wolkandhold.ui.data.model.LoggedInUser;

public class UserRepository {

    private LoggedInUser user;
    private static volatile UserRepository instance;

    private UserRepository() {

    }
    public static UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public void saveSession(Long id, String username, String jwt) {
        user = new LoggedInUser(id, username, jwt);
    }
    public void saveSession(LoggedInUser user) {
        this.user = user;
    }

    public LoggedInUser getSession() {
        return user;
    }
    public void clearSession() {
        user = null;
    }

}
