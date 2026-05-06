package com.example.pi_dev.Utils.Users;

import com.example.pi_dev.Entities.Users.User;

public class UserSession {

    private static UserSession instance;
    private User currentUser;
    private String token;

    private UserSession() {}

    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public void login(User user, String token) {
        this.currentUser = user;
        this.token = token;
    }

    public void logout() {
        this.currentUser = null;
        this.token = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getToken() {
        return token;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
}
