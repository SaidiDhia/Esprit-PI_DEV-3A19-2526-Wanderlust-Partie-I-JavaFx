package com.example.pi_dev.Session;

public class Session {

    private static String currentUserId;

    private Session() {}

    public static void login(String userId) {
        currentUserId = userId;
    }

    public static String getCurrentUserId() {
        return currentUserId;
    }
}
