package com.example.pi_dev.Services.Users;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "587";
    private static final String USERNAME = "jacerk15@gmail.com";
    private static final String DEFAULT_PASSWORD = "fmzm mkzm ljly vygh";

    private static final String APP_PASSWORD_LINK = "https://myaccount.google.com/apppasswords";
    private static final String TWO_STEP_LINK = "https://myaccount.google.com/security";

    /** Use password from file mail.password (current dir or user home) if it exists, else default. No spaces. */
    private static String getPassword() {
        for (String base : new String[]{System.getProperty("user.dir"), System.getProperty("user.home")}) {
            if (base == null) continue;
            Path path = Paths.get(base, "mail.password");
            if (Files.isRegularFile(path)) {
                try {
                    String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8).trim().replaceAll("\\s+", "");
                    if (!content.isEmpty()) return content;
                } catch (Exception e) {
                    // ignore, use default
                }
            }
        }
        return DEFAULT_PASSWORD.replaceAll("\\s+", "");
    }

    public void sendEmail(String toEmail, String subject, String body) {
        String password = getPassword();
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.ssl.trust", SMTP_HOST);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(USERNAME, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(USERNAME));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            System.out.println("Email sent successfully to " + toEmail);

        } catch (AuthenticationFailedException e) {
            String msg = "Gmail rejected the login. Create a NEW App Password: " + APP_PASSWORD_LINK + " (enable 2-Step Verification first: " + TWO_STEP_LINK + "). You can put the new password in a file named mail.password in the project folder (no spaces).";
            System.err.println(msg);
            throw new RuntimeException(msg, e);
        } catch (MessagingException e) {
            e.printStackTrace();
            System.err.println("Failed to send email: " + e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        }
    }
}
