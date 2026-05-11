package com.example.pi_dev.Services.Users;

import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Repositories.Users.UserRepository;
import com.example.pi_dev.Utils.Users.JwtUtil;
import com.example.pi_dev.enums.TFAMethod;
import com.google.zxing.WriterException;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final TwoFactorService twoFactorService;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;

    public UserService() {
        this.userRepository = new UserRepository();
        this.twoFactorService = new TwoFactorService();
        this.passwordResetService = new PasswordResetService();
        this.emailService = new EmailService();
    }

    @Override
    public User register(User user) {
        if (user.getEmail() != null) {
            user.setEmail(user.getEmail().trim().toLowerCase());
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }

        User existingUser = getUserByEmail(user.getEmail());
        if (existingUser != null) {
            throw new IllegalArgumentException("Email is already registered");
        }

        // Hash password
        user.setPasswordHash(hashPassword(user.getPasswordHash()));
        user.setCreatedAt(LocalDateTime.now());
        // Default active
        if (user.getIsActive() == null)
            user.setIsActive(true);

        try {
            userRepository.create(user);
            return user;
        } catch (SQLIntegrityConstraintViolationException e) {
            String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
            if (message.contains("duplicate") || message.contains("uniq") || message.contains("email")) {
                throw new IllegalArgumentException("Email is already registered");
            }
            throw new RuntimeException("Registration failed due to data constraint", e);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error registering user", e);
        }
    }

    @Override
    public String login(String email, String password) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (verifyPassword(password, user.getPasswordHash())) {
                    return JwtUtil.generateToken(user);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public User getUserById(UUID userId) {
        try {
            return userRepository.findById(userId).orElse(null);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public User getUserByEmail(String email) {
        try {
            return userRepository.findByEmail(email).orElse(null);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<User> getAllUsers() {
        try {
            return userRepository.findAll();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void updateUser(User user) {
        try {
            userRepository.update(user);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteUser(UUID userId) {
        try {
            userRepository.delete(userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // --- Two-Factor Authentication Methods ---

    // 1. QR / Authenticator App
    public byte[] setupTwoFactorQR(UUID userId) throws IOException, WriterException, SQLException {
        // Generate new secret for QR setup
        String secretKey = twoFactorService.generateSecretKey();
        userRepository.saveTfaSecret(userId, secretKey);

        User user = getUserById(userId);
        String email = (user != null) ? user.getEmail() : "user@example.com";
        String barCodeUrl = twoFactorService.getGoogleAuthenticatorBarCode(secretKey, email, "PI_DEV_APP");
        return twoFactorService.generateQRCodeImage(barCodeUrl);
    }

    // 2. Email 2FA
    public void setupTwoFactorEmail(UUID userId) throws SQLException {
        // For email setup, we just verify we can send an email.
        // We might not need to store a "secret" yet, but let's store a marker or temp
        // code.
        // Actually, let's just send a code immediately to verify they own the email.
        sendTwoFactorCodeEmail(userId);
    }

    public void sendTwoFactorCodeEmail(UUID userId) throws SQLException {
        // Generate 6-digit code
        int code = 100000 + new java.util.Random().nextInt(900000);
        long expiry = System.currentTimeMillis() + (5 * 60 * 1000); // 5 mins

        // Store in tfa_secrets as "EMAIL:<code>:<expiry>"
        String secretValue = "EMAIL:" + code + ":" + expiry;
        userRepository.saveTfaSecret(userId, secretValue);

        // Send Email
        User user = getUserById(userId);
        if (user != null) {
            emailService.sendEmail(user.getEmail(), "Your 2FA Code", "Your verification code is: " + code);
        }
    }

    // 3. Face 2FA Setup
    // This is called when the user configures Face ID. It saves the snapshot image.
    public void setupTwoFactorFace(UUID userId, byte[] imageBytes) throws SQLException {
        try {
            String fileName = "face_" + userId + ".png";

            // 1. Save to Java local path
            java.io.File javaDir = new java.io.File("uploads/faces");
            if (!javaDir.exists()) javaDir.mkdirs();
            java.io.File javaFile = new java.io.File(javaDir, fileName);
            java.nio.file.Files.write(javaFile.toPath(), imageBytes);

            // 2. Save to Symfony path
            java.io.File symfonyDir = new java.io.File("C:\\Users\\jacer\\Desktop\\dev\\Esprit-PI_DEV-3A19-2526-Wanderlust - Copie\\public\\uploads\\faces");
            if (!symfonyDir.exists()) symfonyDir.mkdirs();
            java.io.File symfonyFile = new java.io.File(symfonyDir, fileName);
            java.nio.file.Files.write(symfonyFile.toPath(), imageBytes);

            // Copy to Symfony profiles path so website displays it
            java.io.File symfonyDirProf = new java.io.File("C:\\Users\\jacer\\Desktop\\dev\\Esprit-PI_DEV-3A19-2526-Wanderlust - Copie\\public\\uploads\\profiles");
            if (!symfonyDirProf.exists()) symfonyDirProf.mkdirs();
            java.io.File symfonyFileProf = new java.io.File(symfonyDirProf, fileName);
            java.nio.file.Files.write(symfonyFileProf.toPath(), imageBytes);

            // 3. Store relative path identifier
            String secretValue = "FACE:" + fileName;
            userRepository.saveTfaSecret(userId, secretValue);
            
            // 4. Set Face Reference specifically for Symfony User Database
            userRepository.updateFaceReferenceImage(userId, fileName);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save face image", e);
        }
    }

    // Verify Method (Dispatches based on User's TFA Method)
    public boolean verifyTwoFactor(UUID userId, int code) {
        try {
            User user = getUserById(userId);
            if (user == null || user.getTfaMethod() == null)
                return true; // Should not happen if 2FA required

            String storedSecret = userRepository.getTfaSecret(userId);
            if (storedSecret == null)
                return false;

            if (user.getTfaMethod() == TFAMethod.QR) {
                return twoFactorService.validateCode(storedSecret, code);
            } else if (user.getTfaMethod() == TFAMethod.EMAIL) {
                // Parse "EMAIL:<code>:<expiry>"
                if (storedSecret.startsWith("EMAIL:")) {
                    String[] parts = storedSecret.split(":");
                    if (parts.length == 3) {
                        int storedCode = Integer.parseInt(parts[1]);
                        long expiry = Long.parseLong(parts[2]);

                        if (System.currentTimeMillis() > expiry) {
                            System.out.println("Email code expired.");
                            return false;
                        }
                        return code == storedCode;
                    }
                }
            }

            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Face 2FA Verify
     * This is called during Login to verify the newly captured photo against the
     * stored reference photo.
     * It uses the DeepFace API (reference photo = img1, login capture = img2).
     */
    public boolean verifyTwoFactorFace(UUID userId, byte[] capturedImage) {
        try {
            // 1. Get the path of the configured image safely stored during setup
            String storedSecret = userRepository.getTfaSecret(userId);
            if (storedSecret == null || !storedSecret.startsWith("FACE:"))
                return false;
            if (capturedImage == null || capturedImage.length == 0)
                return false;

            String storedFilename = storedSecret.substring(5).trim();

            // Locate file in dual paths or fallback to legacy absolute path
            java.io.File storedFile = new java.io.File("uploads/faces", storedFilename);
            if (!storedFile.exists()) {
                storedFile = new java.io.File("C:\\Users\\jacer\\Desktop\\dev\\Esprit-PI_DEV-3A19-2526-Wanderlust - Copie\\public\\uploads\\faces", storedFilename);
            }
            if (!storedFile.exists()) {
                storedFile = new java.io.File(storedFilename); // legacy absolute path
            }

            if (!storedFile.exists()) {
                System.err.println("Reference face image not found in any path.");
                return false;
            }

            // 3. Send both the stored image and the new snapshot to DeepFace implementation
            DeepFaceService deepFace = new DeepFaceService();
            return deepFace.verify(storedFile.getAbsolutePath(), capturedImage);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Helper to finalize enablement
    public void finalizeTwoFactorSetup(UUID userId, TFAMethod method) {
        try {
            User user = getUserById(userId);
            if (user != null) {
                user.setTfaMethod(method);
                updateUser(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isTwoFactorActive(User user) {
        if (user == null) {
            return false;
        }

        TFAMethod method = user.getTfaMethod();
        if (method == null || method == TFAMethod.NONE) {
            return false;
        }

        try {
            String storedSecret = userRepository.getTfaSecret(user.getUserId());
            if (storedSecret == null || storedSecret.isBlank()) {
                return false;
            }

            if (method == TFAMethod.EMAIL) {
                return storedSecret.startsWith("EMAIL:");
            }

            if (method == TFAMethod.FACE_ID) {
                return storedSecret.startsWith("FACE:");
            }

            return !storedSecret.startsWith("EMAIL:") && !storedSecret.startsWith("FACE:");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean shouldRequireTwoFactor(UUID userId) {
        try {
            return isTwoFactorActive(getUserById(userId));
        } catch (Exception e) {
            return false;
        }
    }

    // Old method for backward compatibility (renamed logic)
    public byte[] enableTwoFactor(UUID userId) throws IOException, WriterException, SQLException {
        return setupTwoFactorQR(userId);
    }

    // --- Password Reset Methods ---

    public void initiatePasswordReset(String email) {
        passwordResetService.initiatePasswordReset(email);
    }

    public boolean resetPassword(String token, String newPassword) {
        String hashedPassword = hashPassword(newPassword);

        // Use the modified resetPassword which returns the userId
        String userIdStr = passwordResetService.resetPassword(token, hashedPassword);

        if (userIdStr != null) {
            // Success! Now disable 2FA
            try {
                UUID userId = UUID.fromString(userIdStr);
                User user = getUserById(userId);
                if (user != null) {
                    user.setTfaMethod(null); // Disable 2FA
                    updateUser(user);
                    System.out.println("2FA disabled for user " + userId + " after password reset.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }

        return false;
    }

    public boolean changePassword(UUID userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (user != null) {
            if (verifyPassword(oldPassword, user.getPasswordHash())) {
                user.setPasswordHash(hashPassword(newPassword));
                // Disable 2FA on password change as requested
                user.setTfaMethod(null);
                updateUser(user);

                // Clear TFA secret just in case
                try {
                    userRepository.saveTfaSecret(userId, null); // Or delete logic if we had it
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                return true;
            }
        }
        return false;
    }

    public void adminUpdatePassword(UUID userId, String newPassword) {
        User user = getUserById(userId);
        if (user != null) {
            user.setPasswordHash(hashPassword(newPassword));
            user.setTfaMethod(null); // Optional: disable 2FA on admin reset
            updateUser(user);
        }
    }

    private String hashPassword(String password) {
        if (password == null)
            throw new IllegalArgumentException("Password cannot be null");
        return PhpPasswordHasher.hashBcrypt(password, 13);
    }

    private boolean verifyPassword(String password, String storedHash) {
        if (storedHash == null || storedHash.isBlank())
            return false;
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            return PhpPasswordHasher.verifyBcrypt(password, storedHash);
        }

        // Fallback: support legacy SHA-256 base64 hashed values
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            String base64 = Base64.getEncoder().encodeToString(hash);
            return base64.equals(storedHash);
        } catch (NoSuchAlgorithmException ex) {
            return false;
        }
    }
}
