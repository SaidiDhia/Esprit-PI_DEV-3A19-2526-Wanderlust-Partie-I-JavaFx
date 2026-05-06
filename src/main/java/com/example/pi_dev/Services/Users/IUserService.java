package com.example.pi_dev.Services.Users;

import com.example.pi_dev.Entities.Users.User;
import com.google.zxing.WriterException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public interface IUserService {
    User register(User user);
    String login(String email, String password); // Returns JWT
    User getUserById(UUID userId);
    List<User> getAllUsers();
    void updateUser(User user);
    void deleteUser(UUID userId);

    // Two-Factor Authentication
    byte[] enableTwoFactor(UUID userId) throws IOException, WriterException, SQLException;
    boolean verifyTwoFactor(UUID userId, int code);

    // Password Reset
    void initiatePasswordReset(String email);
    boolean resetPassword(String token, String newPassword);
}
