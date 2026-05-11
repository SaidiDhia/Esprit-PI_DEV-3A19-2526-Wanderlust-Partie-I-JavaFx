package com.example.pi_dev.Services.Users;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import com.example.pi_dev.common.ApiConfiguration;

public class EmailService implements IEmailService {

    private static final String APP_PASSWORD_LINK = "https://myaccount.google.com/apppasswords";
    private static final String TWO_STEP_LINK = "https://myaccount.google.com/security";

    /** Use password from central ApiConfiguration */
    private static String getPassword() {
        return ApiConfiguration.SMTP_PASSWORD.replaceAll("\\s+", "");
    }

    @Override
    public boolean sendVerificationCode(String email, String userName, String code) {
        String subject = "Wanderlust - Verification Code";
        String body = "Hello " + userName + ",\n\n" +
                "Your verification code is: " + code + "\n" +
                "This code will expire in 10 minutes.\n\n" +
                "If you did not request this code, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Wanderlust Team";

        try {
            sendEmail(email, subject, body);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send verification code: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendPasswordResetEmail(String email, String userName, String resetLink) {
        String subject = "Wanderlust - Password Reset";
        String body = "Hello " + userName + ",\n\n" +
                "Click the link below to reset your password:\n" +
                resetLink + "\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "Best regards,\n" +
                "Wanderlust Team";

        try {
            sendEmail(email, subject, body);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendWelcomeEmail(String email, String userName) {
        String subject = "Welcome to Wanderlust!";
        String body = "Hello " + userName + ",\n\n" +
                "Welcome to Wanderlust! We're excited to have you on board.\n\n" +
                "Start exploring amazing activities and creating unforgettable memories.\n\n" +
                "Best regards,\n" +
                "Wanderlust Team";

        try {
            sendEmail(email, subject, body);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send welcome email: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean sendEmail(String toEmail, String subject, String body) {
        String password = getPassword();
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", ApiConfiguration.SMTP_HOST);
        props.put("mail.smtp.port", String.valueOf(ApiConfiguration.SMTP_PORT));
        props.put("mail.smtp.ssl.trust", ApiConfiguration.SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(ApiConfiguration.SMTP_USERNAME, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(ApiConfiguration.SMTP_USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("Email sent successfully to " + toEmail);
            return true;

        } catch (AuthenticationFailedException e) {
            String msg = "Gmail rejected the login. Create a NEW App Password: " + APP_PASSWORD_LINK + " (enable 2-Step Verification first: " + TWO_STEP_LINK + "). You can put the new password in a file named mail.password in the project folder (no spaces).";
            System.err.println(msg);
            return false;
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Failed to send email: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean isConfigured() {
        return true;
    }
}
