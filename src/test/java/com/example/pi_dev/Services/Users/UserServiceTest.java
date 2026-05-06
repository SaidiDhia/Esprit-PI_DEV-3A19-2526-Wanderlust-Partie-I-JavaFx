package com.example.pi_dev.Services.Users;

import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Repositories.Users.UserRepository;
import com.example.pi_dev.enums.RoleEnum;
import com.example.pi_dev.enums.TFAMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private TwoFactorService twoFactorService;
    @Mock
    private PasswordResetService passwordResetService;
    @Mock
    private EmailService emailService;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        userService = new UserService();

        // Use reflection to inject mocks into the private final fields
        injectMock("userRepository", userRepository);
        injectMock("twoFactorService", twoFactorService);
        injectMock("passwordResetService", passwordResetService);
        injectMock("emailService", emailService);
    }

    private void injectMock(String fieldName, Object mock) throws Exception {
        Field field = UserService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(userService, mock);
    }

    @Test
    void register() throws Exception {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPasswordHash("password");
        user.setRole(RoleEnum.PARTICIPANT);

        User result = userService.register(user);

        assertNotNull(result);
        assertNotEquals("password", result.getPasswordHash()); // Password should be hashed
        verify(userRepository, times(1)).create(any(User.class));
    }

    @Test
    void login() throws Exception {
        String email = "test@example.com";
        String password = "password";
        String passwordHash = userService.register(new User(UUID.randomUUID(), email, password, "Test User", "123", true, RoleEnum.PARTICIPANT, null, null, null)).getPasswordHash();
        
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordHash);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        String token = userService.login(email, password);

        assertNotNull(token);
    }

    @Test
    void getUserById() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        User result = userService.getUserById(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
    }

    @Test
    void getUserByEmail() throws Exception {
        String email = "test@example.com";
        User user = new User();
        user.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        User result = userService.getUserByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
    }

    @Test
    void getAllUsers() throws Exception {
        when(userRepository.findAll()).thenReturn(Collections.singletonList(new User()));

        List<User> users = userService.getAllUsers();

        assertNotNull(users);
        assertFalse(users.isEmpty());
    }

    @Test
    void updateUser() throws Exception {
        User user = new User();
        userService.updateUser(user);
        verify(userRepository, times(1)).update(user);
    }

    @Test
    void deleteUser() throws Exception {
        UUID userId = UUID.randomUUID();
        userService.deleteUser(userId);
        verify(userRepository, times(1)).delete(userId);
    }

    @Test
    void setupTwoFactorQR() throws Exception {
        UUID userId = UUID.randomUUID();
        String secret = "SECRET";
        when(twoFactorService.generateSecretKey()).thenReturn(secret);
        when(twoFactorService.getGoogleAuthenticatorBarCode(anyString(), anyString(), anyString())).thenReturn("barcode");
        when(twoFactorService.generateQRCodeImage(anyString())).thenReturn(new byte[]{1, 2, 3});

        byte[] qrCode = userService.setupTwoFactorQR(userId);

        assertNotNull(qrCode);
        verify(userRepository).saveTfaSecret(userId, secret);
    }

    @Test
    void verifyTwoFactor() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setTfaMethod(TFAMethod.QR);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.getTfaSecret(userId)).thenReturn("SECRET");
        when(twoFactorService.validateCode("SECRET", 123456)).thenReturn(true);

        boolean result = userService.verifyTwoFactor(userId, 123456);

        assertTrue(result);
    }

    @Test
    void finalizeTwoFactorSetup() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.finalizeTwoFactorSetup(userId, TFAMethod.QR);

        assertEquals(TFAMethod.QR, user.getTfaMethod());
        verify(userRepository).update(user);
    }

    @Test
    void initiatePasswordReset() {
        String email = "test@example.com";
        userService.initiatePasswordReset(email);
        verify(passwordResetService).initiatePasswordReset(email);
    }

    @Test
    void changePassword() throws Exception {
        UUID userId = UUID.randomUUID();
        String oldPassword = "oldPassword";
        String newPassword = "newPassword";
        
        User user = new User();
        user.setUserId(userId);
        user.setPasswordHash(userService.register(new User(UUID.randomUUID(), "test@example.com", oldPassword, "Test User", "123", true, RoleEnum.PARTICIPANT, null, null, null)).getPasswordHash());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean result = userService.changePassword(userId, oldPassword, newPassword);

        assertTrue(result);
        assertNull(user.getTfaMethod()); // Should be disabled
        verify(userRepository).update(user);
    }

    @Test
    void setupTwoFactorEmail() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setEmail("test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.setupTwoFactorEmail(userId);

        verify(emailService).sendEmail(eq("test@example.com"), anyString(), anyString());
        verify(userRepository).saveTfaSecret(eq(userId), contains("EMAIL:"));
    }

    @Test
    void sendTwoFactorCodeEmail() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setEmail("test@example.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.sendTwoFactorCodeEmail(userId);

        verify(emailService).sendEmail(eq("test@example.com"), anyString(), anyString());
        verify(userRepository).saveTfaSecret(eq(userId), contains("EMAIL:"));
    }

    @Test
    void setupTwoFactorFace() throws Exception {
        UUID userId = UUID.randomUUID();
        byte[] imageBytes = new byte[]{1, 2, 3};

        userService.setupTwoFactorFace(userId, imageBytes);

        verify(userRepository).saveTfaSecret(eq(userId), contains("FACE:"));
    }

    @Test
    void resetPassword() throws Exception {
        String token = "token";
        String newPassword = "newPassword";
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setUserId(userId);

        when(passwordResetService.resetPassword(eq(token), anyString())).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        boolean result = userService.resetPassword(token, newPassword);

        assertTrue(result);
        assertNull(user.getTfaMethod());
        verify(userRepository).update(user);
    }

    @Test
    void adminUpdatePassword() throws Exception {
        UUID userId = UUID.randomUUID();
        String newPassword = "newPassword";
        User user = new User();
        user.setUserId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.adminUpdatePassword(userId, newPassword);

        assertNotEquals(newPassword, user.getPasswordHash());
        assertNull(user.getTfaMethod());
        verify(userRepository).update(user);
    }
}