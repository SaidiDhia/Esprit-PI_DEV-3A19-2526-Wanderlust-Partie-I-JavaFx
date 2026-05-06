package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Entities.Events.Reservation;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class QRCodeController {

    @FXML private Label reservationInfoLabel;
    @FXML private ImageView qrCodeImageView;
    @FXML private Button imprimerButton;
    @FXML private Button sauvegarderButton;
    @FXML private Button fermerButton;

    private Reservation currentReservation;
    private BufferedImage qrCodeImage;

    public void initialize() {
    }

    public void setReservation(Reservation reservation) {
        this.currentReservation = reservation;
        generateQRCode();
        updateReservationInfo();
    }

    private void generateQRCode() {
        try {
            String qrContent = createQRContent();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, 300, 300, hints);

            qrCodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            Image fxImage = convertToFxImage(qrCodeImage);
            qrCodeImageView.setImage(fxImage);

            System.out.println("QR Code généré avec succès");

        } catch (WriterException e) {
            System.err.println("Erreur lors de la génération du QR code: " + e.getMessage());
            showAlert("Erreur lors de la génération du QR code");
        }
    }

    private Image convertToFxImage(BufferedImage bufferedImage) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "PNG", bos);
            byte[] imageBytes = bos.toByteArray();
            return new Image(new ByteArrayInputStream(imageBytes));
        } catch (IOException e) {
            System.err.println("Erreur lors de la conversion d'image: " + e.getMessage());
            return null;
        }
    }

    private String createQRContent() {
        if (currentReservation == null) {
            return "Réservation invalide";
        }

        return createReservationUrl();
    }

    private String createReservationUrl() {
        StringBuilder info = new StringBuilder();
        info.append("🎫 RÉSERVATION WANDERLUST\n");
        info.append("ID: #").append(currentReservation.getId()).append("\n");
        info.append("Nom: ").append(currentReservation.getNom()).append("\n");
        info.append("Email: ").append(currentReservation.getEmail()).append("\n");
        info.append("Téléphone: ").append(currentReservation.getTelephone()).append("\n");

        if (currentReservation.getEvent() != null && currentReservation.getEvent().getActivite() != null) {
            info.append("Événement: ").append(currentReservation.getEvent().getActivite().getTitre()).append("\n");
            info.append("Lieu: ").append(currentReservation.getEvent().getLieu()).append("\n");
            if (currentReservation.getEvent().getDateDebut() != null) {
                info.append("Date: ").append(currentReservation.getEvent().getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
            }
        }

        info.append("Personnes: ").append(currentReservation.getNombrePersonnes()).append("\n");
        info.append("Prix: ").append(currentReservation.getPrixTotal()).append(" TND\n");
        info.append("🌍 wanderlust-tunisie.com");

        return info.toString();
    }

    private void updateReservationInfo() {
        if (currentReservation == null) {
            reservationInfoLabel.setText("Aucune information de réservation disponible");
            return;
        }

        String info = String.format(
                "Réservation #%d\n%s\n%s personnes\n%s TND",
                currentReservation.getId(),
                currentReservation.getEvent().getActivite().getTitre(),
                currentReservation.getNombrePersonnes(),
                currentReservation.getPrixTotal()
        );

        reservationInfoLabel.setText(info);
    }

    @FXML
    void imprimerQRCode(ActionEvent event) {
        if (qrCodeImage == null) {
            showAlert("Aucun QR code à imprimer");
            return;
        }

        try {
            File tempFile = File.createTempFile("qrcode_reservation_", ".png");
            ImageIO.write(qrCodeImage, "PNG", tempFile);

            showAlert("QR Code prêt pour impression\nFichier: " + tempFile.getAbsolutePath());

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(tempFile);
            }

        } catch (IOException e) {
            System.err.println("Erreur lors de l'impression: " + e.getMessage());
            showAlert("Erreur lors de l'impression du QR code");
        }
    }

    @FXML
    void sauvegarderQRCode(ActionEvent event) {
        if (qrCodeImage == null) {
            showAlert("Aucun QR code à sauvegarder");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sauvegarder le QR Code");
        fileChooser.setInitialFileName("qrcode_reservation_" + (currentReservation != null ? currentReservation.getId() : "unknown") + ".png");

        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
                "Fichiers PNG (*.png)", "*.png"
        );
        fileChooser.getExtensionFilters().add(extFilter);

        File selectedFile = fileChooser.showSaveDialog(null);

        if (selectedFile != null) {
            try {
                ImageIO.write(qrCodeImage, "PNG", selectedFile);
                showAlert("QR Code sauvegardé avec succès\nFichier: " + selectedFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
                showAlert("Erreur lors de la sauvegarde du QR code");
            }
        }
    }

    @FXML
    void fermer(ActionEvent event) {
        Stage stage = (Stage) fermerButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("QR Code");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}