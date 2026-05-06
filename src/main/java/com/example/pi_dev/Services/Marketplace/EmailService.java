package com.example.pi_dev.Services.Marketplace;

import com.example.pi_dev.Entities.Marketplace.DeliveryAddress;
import com.example.pi_dev.Entities.Marketplace.FactureProduct;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * EmailService — sends via Gmail SMTP directly.
 * 100% inbox delivery — no third party servers involved.
 *
 * SETUP (one time, 1 minute):
 *  1. Go to https://myaccount.google.com/apppasswords
 *  2. App name: "WonderLust" → Create
 *  3. Copy the 16-char password → paste in GMAIL_APP_PASSWORD below
 *
 * pom.xml dependency needed:
 *  <dependency>
 *      <groupId>com.sun.mail</groupId>
 *      <artifactId>javax.mail</artifactId>
 *      <version>1.6.2</version>
 *  </dependency>
 */
public class EmailService {

    private static final String GMAIL_ADDRESS      = "wonderlustt01@gmail.com";
    private static final String GMAIL_APP_PASSWORD = "fgam fifi ibfm gyws";
    private static final String FROM_NAME          = "WonderLust";

    // ---- Public API ---------------------------------------------------------

    public void sendCashOrderConfirmation(String toEmail, DeliveryAddress da,
                                          List<FactureProduct> items, float total, int orderId) {
        if (!isValidEmail(toEmail)) return;
        send(
                toEmail,
                da.getFullName(),
                "Your WonderLust order #" + orderId,
                buildBody(da, items, total, orderId,
                        "Thank you for shopping with WonderLust! Your order has been received " +
                                "and is being prepared. You will pay when it arrives at your door. " +
                                "Expected delivery within 48 hours.")
        );
    }

    public void sendOnlineOrderConfirmation(String toEmail, DeliveryAddress da,
                                            List<FactureProduct> items, float total, int orderId) {
        if (!isValidEmail(toEmail)) return;
        send(
                toEmail,
                da.getFullName(),
                "Your WonderLust order #" + orderId,
                buildBody(da, items, total, orderId,
                        "Thank you for shopping with WonderLust! Your order is confirmed " +
                                "and is being prepared. Expected delivery within 48 hours.")
        );
    }

    // ---- Build plain text body ----------------------------------------------

    private String buildBody(DeliveryAddress da, List<FactureProduct> items,
                             float total, int orderId, String message) {
        String date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
        StringBuilder sb = new StringBuilder();

        sb.append("Bonjour ").append(da.getFullName()).append(",\n\n");
        sb.append(message).append("\n\n");
        sb.append("Order Number: ").append(orderId).append("\n");
        sb.append("Date: ").append(date).append("\n\n");
        sb.append("Items:\n");
        for (FactureProduct fp : items) {
            sb.append("- ").append(fp.getProductTitle())
                    .append(" (Quantity: ").append(fp.getQuantity()).append("): ")
                    .append(String.format("%.3f TND", fp.getPrice() * fp.getQuantity()))
                    .append("\n");
        }
        sb.append("\nTotal: ").append(String.format("%.3f TND", total)).append("\n\n");
        sb.append("Delivery address:\n");
        sb.append(da.getAddress()).append(", ").append(da.getCity());
        if (!da.getPostalCode().isEmpty()) sb.append(" ").append(da.getPostalCode());
        sb.append("\n\n");
        sb.append("You can track and download your invoice anytime from the WonderLust app.\n\n");
        sb.append("If you have any questions, feel free to reply to this email.\n\n");
        sb.append(FROM_NAME);

        return sb.toString();
    }

    // ---- Gmail SMTP send ----------------------------------------------------

    private void send(String toEmail, String toName, String subject, String body) {
        // Run in background — never block JavaFX UI thread
        new Thread(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.auth",            "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host",            "smtp.gmail.com");
                props.put("mail.smtp.port",            "587");
                props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(GMAIL_ADDRESS, GMAIL_APP_PASSWORD);
                    }
                });

                Message msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(GMAIL_ADDRESS, FROM_NAME));
                msg.setRecipient(Message.RecipientType.TO,
                        new InternetAddress(toEmail, toName));
                msg.setSubject(subject);
                msg.setText(body);
                msg.setSentDate(new Date());

                Transport.send(msg);
                System.out.println("Email sent to: " + toEmail);

            } catch (Exception e) {
                // Never crash the app if email fails — just log it
                System.err.println("Email failed: " + e.getMessage());
            }
        }, "email-sender").start();
    }

    // ---- Helper -------------------------------------------------------------

    private boolean isValidEmail(String email) {
        return email != null
                && !email.trim().isEmpty()
                && email.matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");
    }
}