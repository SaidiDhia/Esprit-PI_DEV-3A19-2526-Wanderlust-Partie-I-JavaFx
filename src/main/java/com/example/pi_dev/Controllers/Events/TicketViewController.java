package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Entities.Events.Reservation;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;

import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

public class TicketViewController {

    @FXML private ImageView eventImageView;
    @FXML private Label titleLabel;
    @FXML private Label dateLabel;
    @FXML private Label lieuLabel;
    @FXML private Label subLieuLabel;
    @FXML private Label passagerLabel;
    @FXML private Label ticketIdLabel;
    @FXML private Label participantsLabel;
    @FXML private Label totalLabel;
    @FXML private ImageView qrCodeImageView;

    private Reservation reservation;

    public void setTicketData(Reservation res) {
        this.reservation = res;
        
        titleLabel.setText(res.getEvent() != null ? res.getEvent().getLieu() : "ariana");
        lieuLabel.setText(res.getEvent() != null ? res.getEvent().getLieu() : "ariana");
        subLieuLabel.setText(res.getNomComplet() != null ? res.getNomComplet() : "chaima");
        passagerLabel.setText(res.getNomComplet() != null ? res.getNomComplet() : "chaima");
        
        if (res.getEvent() != null && res.getEvent().getDateDebut() != null) {
            dateLabel.setText(res.getEvent().getDateDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        
        ticketIdLabel.setText(String.format("WDL-%05d-2026", res.getId()));
        participantsLabel.setText(res.getNombrePersonnes() + " personne(s)");
        totalLabel.setText(String.format("%.2f TND", res.getPrixTotal() != null ? res.getPrixTotal() : 0.0));
        
        generateQRCode(String.format("TICKET-%d-%s", res.getId(), res.getNomComplet()));
    }

    private void generateQRCode(String data) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            qrCodeImageView.setImage(SwingFXUtils.toFXImage(bufferedImage, null));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.close();
    }

    @FXML
    private void handleDownloadPDF(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le Ticket PDF");
        fileChooser.setInitialFileName("Ticket_Wanderlust_" + reservation.getId() + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        
        File file = fileChooser.showSaveDialog(((javafx.scene.Node) event.getSource()).getScene().getWindow());
        
        if (file != null) {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();
                document.add(new Paragraph("WANDERLUST E-TICKET"));
                document.add(new Paragraph("----------------------------"));
                document.add(new Paragraph("Evenement: " + titleLabel.getText()));
                document.add(new Paragraph("Date: " + dateLabel.getText()));
                document.add(new Paragraph("Lieu: " + lieuLabel.getText()));
                document.add(new Paragraph("Passager: " + passagerLabel.getText()));
                document.add(new Paragraph("Ticket ID: " + ticketIdLabel.getText()));
                document.add(new Paragraph("Participants: " + participantsLabel.getText()));
                document.add(new Paragraph("Total: " + totalLabel.getText()));
                document.add(new Paragraph("----------------------------"));
                document.add(new Paragraph("Ceci est un ticket electronique valide."));
                document.close();
                System.out.println("PDF généré: " + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
