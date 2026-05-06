package com.example.pi_dev.Utils.Booking;
import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Utils.Users.UserSession;
public class Session {
    public static String currentUserId;

    static {
        update();
    }

    public static void update() {
        User user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUserId().toString();
        } else {
            currentUserId = null;
        }
    }

    public static boolean isAdmin() {
        User user = UserSession.getInstance().getCurrentUser();
        return user != null && user.getRole() == RoleEnum.ADMIN;
    }

    public static boolean isHost() {
        User user = UserSession.getInstance().getCurrentUser();
        return user != null && user.getRole() == RoleEnum.HOST;
    }
}
