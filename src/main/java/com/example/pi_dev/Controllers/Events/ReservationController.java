package com.example.pi_dev.Controllers.Events;

import com.example.pi_dev.Entities.Events.Event;
import com.example.pi_dev.Entities.Events.Reservation;
import com.example.pi_dev.Services.Events.EventService;
import com.example.pi_dev.Services.Events.ReservationService;
import com.example.pi_dev.Services.Events.GoogleCalendarService;
import com.example.pi_dev.Services.Events.EmailService;
import com.example.pi_dev.Utils.Events.Mydatabase;

import java.util.regex.Pattern;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;

public class ReservationController {

    @FXML private Label EventDescription;
    @FXML private Label Eventlieu;
    @FXML private Label datedebut;
    @FXML private Label personCountLabel;
    @FXML private Label prixlabel;
    @FXML private HBox EventImagesContainer;
    @FXML private Button prevImageButton;
    @FXML private Button nextImageButton;
    @FXML private Label imageCounterLabel;
    @FXML private VBox videoContainer;
    @FXML private WebView videoWebView;

    @FXML private TextField idEventField;
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private TextField telephoneField;
    @FXML private TextField nombrePersonnesField;
    @FXML private TextArea demandesp;

    @FXML private Button reserverButton;
    @FXML private Button annulerRES;
    @FXML private Button cataloguebtn;
    @FXML private Button panierButton;

    private EventService eventService = new EventService();
    private ReservationService reservationService = new ReservationService();

    private Event currentEvent;
    private List<javafx.scene.Node> allMediaNodes = new ArrayList<>();
    private int currentImageIndex = 0;

    public static ObservableList<Reservation> panier = FXCollections.observableArrayList();

    private GoogleCalendarService googleCalendarService;
    private EmailService emailService;

    public void initialize() {
        System.out.println("✅ ReservationController initialisé");

        googleCalendarService = new GoogleCalendarService();
        boolean calendarInitialized = googleCalendarService.initialize();

        if (calendarInitialized) {
            System.out.println("📅 Google Calendar Service prêt");
        } else {
            System.out.println("⚠️ Google Calendar Service non disponible");
        }

        emailService = new EmailService();
        boolean emailInitialized = emailService.initialize();

        if (emailInitialized) {
            System.out.println("📧 Email Service prêt");
        } else {
            System.out.println("⚠️ Email Service non disponible");
        }

        nombrePersonnesField.textProperty().addListener((observable, oldValue, newValue) -> {
            updateLabels();
        });

        nombrePersonnesField.setText("1");
    }

    public void loadEvent(int idEvent) {
        try {
            currentEvent = eventService.findById(idEvent);

            if (currentEvent != null) {
                System.out.println("DEBUG: Événement chargé - ID: " + currentEvent.getId());

                loadEventImages();

                if (EventDescription != null) {
                    String eventDescription = currentEvent.getDescription();
                    String activiteDescription = (currentEvent.getActivite() != null) ? currentEvent.getActivite().getDescription() : null;

                    String descriptionToShow = eventDescription;
                    if (descriptionToShow == null || descriptionToShow.trim().isEmpty()) {
                        descriptionToShow = activiteDescription;
                    }

                    if (descriptionToShow != null && !descriptionToShow.trim().isEmpty()) {
                        EventDescription.setText(descriptionToShow);
                    } else {
                        EventDescription.setText("Aucune description disponible");
                    }
                }

                if (Eventlieu != null) {
                    Eventlieu.setText(currentEvent.getLieu() != null ? currentEvent.getLieu() : "Lieu non spécifié");
                }

                if (datedebut != null) {
                    if (currentEvent.getDateDebut() != null) {
                        datedebut.setText(currentEvent.getDateDebut().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    } else {
                        datedebut.setText("Date non définie");
                    }
                }

                loadYouTubeVideo();

                if (personCountLabel != null) {
                    int placesRestantes = currentEvent.getPlacesDisponibles();
                    personCountLabel.setText(" " + placesRestantes + " places restantes");
                }

                if (demandesp != null) {
                    if (currentEvent.getMaterielsNecessaires() != null && !currentEvent.getMaterielsNecessaires().trim().isEmpty()) {
                        demandesp.setPromptText("Allergies alimentaires, besoins spéciaux, questions...\n\n📋 Matériels requis pour cet événement :\n" + currentEvent.getMaterielsNecessaires());
                        demandesp.setEditable(true);
                        demandesp.setStyle("-fx-border-color: #2D70B3; -fx-background-color: white;");
                    } else {
                        demandesp.setPromptText("Allergies alimentaires, besoins spéciaux, questions...");
                        demandesp.setEditable(true);
                        demandesp.setStyle("-fx-border-color: #2D70B3; -fx-background-color: white;");
                    }
                }

                if (prixlabel != null) {
                    try {
                        int nbPersonnes = Integer.parseInt(nombrePersonnesField.getText().isEmpty() ? "1" : nombrePersonnesField.getText());
                        double prixUnitaire = currentEvent.getPrix().doubleValue();
                        double total = prixUnitaire * nbPersonnes;
                        prixlabel.setText(" " + total + " TND");
                    } catch (NumberFormatException e) {
                        prixlabel.setText(" 0 TND");
                    }
                }

                loadEventPhotos();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadEventImages() {
        try {
            allMediaNodes.clear();
            currentImageIndex = 0;

            if (currentEvent.getImage() != null && !currentEvent.getImage().trim().isEmpty()) {
                try {
                    java.io.File imageFile = new java.io.File(currentEvent.getImage());
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        ImageView imageView = new ImageView(image);
                        imageView.setFitHeight(180);
                        imageView.setFitWidth(180);
                        imageView.setPreserveRatio(true);
                        imageView.setStyle("-fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: #2D70B3; -fx-background-color: white; -fx-padding: 5;");
                        allMediaNodes.add(imageView);
                        System.out.println("DEBUG: Image principale chargée: " + currentEvent.getImage());
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement de l'image principale: " + e.getMessage());
                }
            }

            if (currentEvent.getActivite() != null && currentEvent.getActivite().getImage() != null) {
                String activityImagePath = currentEvent.getActivite().getImage();
                if (!activityImagePath.isEmpty() && !activityImagePath.equals(currentEvent.getImage())) {
                    try {
                        java.io.File imageFile = new java.io.File(activityImagePath);
                        if (imageFile.exists()) {
                            Image image = new Image(imageFile.toURI().toString());
                            ImageView imageView = new ImageView(image);
                            imageView.setFitHeight(180);
                            imageView.setFitWidth(180);
                            imageView.setPreserveRatio(true);
                            imageView.setStyle("-fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: #2D70B3; -fx-background-color: white; -fx-padding: 5;");
                            allMediaNodes.add(imageView);
                            System.out.println("DEBUG: Image activité chargée: " + activityImagePath);
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors du chargement de l'image activité: " + e.getMessage());
                    }
                }
            }

            loadEventPhotos();

            if (!allMediaNodes.isEmpty()) {
                System.out.println("DEBUG: Nombre total de médias chargés: " + allMediaNodes.size());
                updateImageDisplay();
            } else {
                System.out.println("DEBUG: Aucun média à afficher");
            }

        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des images: " + e.getMessage());
        }
    }

    private void loadEventPhotos() {
        try {
            String sql = "SELECT chemin_photo FROM event_photos WHERE id_event = ?";
            java.sql.PreparedStatement ps = Mydatabase.getInstance().getConnextion().prepareStatement(sql);
            ps.setInt(1, currentEvent.getId());
            java.sql.ResultSet rs = ps.executeQuery();

            List<String> photoPaths = new ArrayList<>();
            while (rs.next()) {
                photoPaths.add(rs.getString("chemin_photo"));
            }
            rs.close();
            ps.close();

            for (String photoPath : photoPaths) {
                try {
                    java.io.File photoFile = new java.io.File(photoPath);
                    if (photoFile.exists()) {
                        Image image = new Image(photoFile.toURI().toString());

                        ImageView imageView = new ImageView(image);
                        imageView.setFitHeight(180);
                        imageView.setFitWidth(180);
                        imageView.setPreserveRatio(true);
                        imageView.setStyle("-fx-border-radius: 10; -fx-background-radius: 10; -fx-border-color: #2D70B3; -fx-background-color: white; -fx-padding: 5;");
                        allMediaNodes.add(imageView);

                        System.out.println("DEBUG: Photo supplémentaire chargée: " + photoPath);
                    }
                } catch (Exception e) {
                    System.err.println("Erreur lors du chargement de la photo: " + photoPath + " - " + e.getMessage());
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des photos: " + e.getMessage());
        }
    }

    @FXML
    void previousImage(ActionEvent event) {
        if (currentImageIndex > 0) {
            currentImageIndex--;
            updateImageDisplay();
        }
    }

    @FXML
    void nextImage(ActionEvent event) {
        if (currentImageIndex < allMediaNodes.size() - 1) {
            currentImageIndex++;
            updateImageDisplay();
        }
    }

    private void updateImageDisplay() {
        if (allMediaNodes.isEmpty() || EventImagesContainer == null) {
            return;
        }

        EventImagesContainer.getChildren().clear();

        javafx.scene.Node currentNode = allMediaNodes.get(currentImageIndex);
        EventImagesContainer.getChildren().add(currentNode);

        if (imageCounterLabel != null) {
            imageCounterLabel.setText((currentImageIndex + 1) + " / " + allMediaNodes.size());
        }

        if (prevImageButton != null) {
            prevImageButton.setDisable(currentImageIndex == 0);
        }
        if (nextImageButton != null) {
            nextImageButton.setDisable(currentImageIndex == allMediaNodes.size() - 1);
        }

        System.out.println("DEBUG: Affichage image " + (currentImageIndex + 1) + " / " + allMediaNodes.size());
    }

    private void loadYouTubeVideo() {
        System.out.println("DEBUG: Début loadYouTubeVideo()");

        if (currentEvent != null && currentEvent.getVideoYoutube() != null && !currentEvent.getVideoYoutube().trim().isEmpty() && videoWebView != null && videoContainer != null) {
            try {
                String videoUrl = currentEvent.getVideoYoutube().trim();
                System.out.println("DEBUG: Chargement de la vidéo YouTube dans WebView: " + videoUrl);

                String embedUrl = convertToEmbedUrl(videoUrl);

                if (embedUrl == null || embedUrl.trim().isEmpty()) {
                    System.err.println("DEBUG: URL embed invalide, masquage du conteneur vidéo");
                    videoContainer.setVisible(false);
                    return;
                }

                String htmlContent = "<!DOCTYPE html>\n" +
                        "<html>\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <style>\n" +
                        "        body { margin: 0; padding: 0; background-color: #000; font-family: Arial; width: 400px; height: 280px; display: flex; justify-content: center; align-items: center; }\n" +
                        "        .video-box { width: 380px; height: 260px; background-color: #1a1a1a; border: 2px solid #333; border-radius: 10px; display: flex; flex-direction: column; justify-content: center; align-items: center; text-align: center; color: white; }\n" +
                        "        .icon { font-size: 48px; color: #ff0000; margin-bottom: 15px; }\n" +
                        "        .title { font-size: 18px; font-weight: bold; margin-bottom: 10px; }\n" +
                        "        .desc { font-size: 14px; color: #ccc; margin-bottom: 20px; }\n" +
                        "        .btn { background-color: #ff0000; color: white; padding: 12px 24px; border: none; border-radius: 20px; font-size: 14px; font-weight: bold; cursor: pointer; text-decoration: none; }\n" +
                        "        .btn:hover { background-color: #cc0000; }\n" +
                        "    </style>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "    <div class=\"video-box\">\n" +
                        "        <div class=\"icon\">🎥</div>\n" +
                        "        <div class=\"title\">Vidéo de présentation</div>\n" +
                        "        <div class=\"desc\">Cliquez pour regarder sur YouTube</div>\n" +
                        "        <a href=\"" + videoUrl + "\" target=\"_blank\" class=\"btn\">▶️ YouTube</a>\n" +
                        "    </div>\n" +
                        "</body>\n" +
                        "</html>";

                WebEngine webEngine = videoWebView.getEngine();
                webEngine.loadContent(htmlContent);

                videoContainer.setVisible(true);
                videoContainer.setManaged(true);
                videoWebView.setVisible(true);
                videoWebView.setManaged(true);

                System.out.println("DEBUG: Vidéo YouTube chargée avec succès dans WebView");

            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de la vidéo YouTube: " + e.getMessage());
                e.printStackTrace();
                if (videoContainer != null) {
                    videoContainer.setVisible(false);
                }
            }
        } else {
            System.out.println("DEBUG: Aucune vidéo YouTube à charger ou WebView non initialisé");
            if (videoContainer != null) {
                videoContainer.setVisible(false);
            }
        }
    }

    private String convertToEmbedUrl(String youtubeUrl) {
        String embedUrl = "";

        try {
            if (youtubeUrl.contains("youtube.com/watch?v=")) {
                String videoId = youtubeUrl.substring(youtubeUrl.indexOf("v=") + 2);
                if (videoId.contains("&")) {
                    videoId = videoId.substring(0, videoId.indexOf("&"));
                }
                if (videoId.contains("#")) {
                    videoId = videoId.substring(0, videoId.indexOf("#"));
                }
                embedUrl = "https://www.youtube.com/embed/" + videoId;
            } else if (youtubeUrl.contains("youtu.be/")) {
                String videoId = youtubeUrl.substring(youtubeUrl.indexOf("youtu.be/") + 9);
                if (videoId.contains("?")) {
                    videoId = videoId.substring(0, videoId.indexOf("?"));
                }
                if (videoId.contains("&")) {
                    videoId = videoId.substring(0, videoId.indexOf("&"));
                }
                embedUrl = "https://www.youtube.com/embed/" + videoId;
            } else if (youtubeUrl.contains("youtube.com/embed/")) {
                embedUrl = youtubeUrl;
            } else if (youtubeUrl.contains("youtube.com/shorts/")) {
                String videoId = youtubeUrl.substring(youtubeUrl.indexOf("shorts/") + 7);
                if (videoId.contains("?")) {
                    videoId = videoId.substring(0, videoId.indexOf("?"));
                }
                embedUrl = "https://www.youtube.com/embed/" + videoId;
            }

            if (!embedUrl.isEmpty()) {
                String videoId = embedUrl.substring(embedUrl.lastIndexOf("/") + 1);
                if (videoId.length() < 11 || videoId.matches(".*[^a-zA-Z0-9_-].*")) {
                    System.err.println("ID vidéo YouTube invalide: " + videoId);
                    return "";
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur lors de la conversion de l'URL YouTube: " + e.getMessage());
            return "";
        }

        return embedUrl;
    }

    private boolean validateInput() {
        if (currentEvent == null) {
            showAlert("Veuillez d'abord sélectionner un événement");
            return false;
        }

        if (nomField.getText().isEmpty()) {
            showAlert("Le nom est obligatoire");
            return false;
        }

        if (!Pattern.matches("^[A-Za-z ]+$", nomField.getText())) {
            showAlert("Nom invalide");
            return false;
        }

        if (!Pattern.matches("^[A-Za-z0-9+_.-]+@(.+)$", emailField.getText())) {
            showAlert("Email invalide");
            return false;
        }

        if (!Pattern.matches("^[0-9]{8,15}$", telephoneField.getText())) {
            showAlert("Téléphone invalide");
            return false;
        }

        int nombre;
        try {
            nombre = Integer.parseInt(nombrePersonnesField.getText());
        } catch (NumberFormatException e) {
            showAlert("Nombre invalide");
            return false;
        }

        if (nombre <= 0 || nombre > 12) {
            showAlert("Maximum 12 personnes autorisées");
            return false;
        }

        if (nombre > currentEvent.getPlacesDisponibles()) {
            showAlert("Pas assez de places disponibles");
            return false;
        }

        return true;
    }

    @FXML
    void reserver(ActionEvent event) {
        if (!validateInput()) {
            return;
        }

        try {
            if (currentEvent == null || currentEvent.getId() == 0) {
                showAlert("Aucun événement sélectionné. Veuillez sélectionner un événement avant de réserver.");
                return;
            }

            if (!eventExists(currentEvent.getId())) {
                showAlert("L'événement sélectionné n'est plus disponible. Veuillez rafraîchir la liste des événements et réessayer.");
                return;
            }

            Reservation reservation = new Reservation();
            reservation.setIdEvent(currentEvent.getId());

            reservation.setNom(nomField.getText());
            reservation.setEmail(emailField.getText());
            reservation.setTelephone(telephoneField.getText());

            reservation.setNombrePersonnes(Integer.parseInt(nombrePersonnesField.getText()));
            reservation.setDemandesSpeciales(demandesp.getText() != null && !demandesp.getText().trim().isEmpty() ? demandesp.getText().trim() : "");
            reservation.setEvent(currentEvent);
            reservation.setStatut(Reservation.StatutReservation.CONFIRMEE);

            double prixUnitaire = currentEvent.getPrix().doubleValue();
            int nbPersonnes = Integer.parseInt(nombrePersonnesField.getText());
            double prixTotal = prixUnitaire * nbPersonnes;
            reservation.setPrixTotal(prixTotal);

            reservationService.add(reservation);

            panier.add(reservation);

            showAlert("Réservation effectuée avec succès !");

            afficherQRCode(reservation);

            if (googleCalendarService != null && googleCalendarService.isAvailable()) {
                boolean addedToCalendar = googleCalendarService.addReservationToCalendar(reservation);
                if (addedToCalendar) {
                    System.out.println("📅 Réservation ajoutée automatiquement à Google Calendar");
                    showAlert("Réservation effectuée avec succès !\n📅 L'événement a été ajouté à votre Google Calendar.");
                } else {
                    System.out.println("⚠️ Erreur lors de l'ajout à Google Calendar");
                    showAlert("Réservation effectuée avec succès !\n⚠️ Impossible d'ajouter à Google Calendar.");
                }
            } else {
                showAlert("Réservation effectuée avec succès !");
            }

            if (emailService != null && emailService.isAvailable()) {
                boolean emailSent = emailService.sendReservationConfirmation(reservation);
                if (emailSent) {
                    System.out.println("📧 Email de confirmation envoyé avec lien Google Maps");
                    Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                    infoAlert.setTitle("📧 Email envoyé");
                    infoAlert.setHeaderText(null);
                    infoAlert.setContentText("Un email de confirmation a été envoyé à " + reservation.getEmail() +
                            "\n\n🗺️ L'email contient un lien Google Maps pour vous rendre au lieu de l'événement.");
                    infoAlert.showAndWait();
                } else {
                    System.out.println("⚠️ Erreur lors de l'envoi de l'email");
                    showAlert("Réservation effectuée avec succès !\n⚠️ Impossible d'envoyer l'email de confirmation.");
                }
            }

            fermerFenetre();

        } catch (SQLException e) {
            System.err.println("Erreur lors de la réservation: " + e.getMessage());

            if (e.getMessage().contains("foreign key constraint fails")) {
                showAlert("Erreur : L'événement sélectionné n'est plus disponible. Veuillez rafraîchir la liste des événements et réessayer.");
                fermerFenetre();
            } else {
                showAlert("Erreur lors de la réservation: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            showAlert("Veuillez entrer un nombre valide pour le nombre de personnes");
        }
    }

    private void afficherQRCode(Reservation reservation) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pi_dev/events/QRCode.fxml"));
            Parent root = loader.load();

            QRCodeController qrController = loader.getController();
            qrController.setReservation(reservation);

            Stage stage = new Stage();
            stage.setTitle("QR Code de Réservation");
            stage.setScene(new Scene(root));
            stage.setWidth(500);
            stage.setHeight(600);
            stage.centerOnScreen();
            stage.setResizable(false);
            stage.show();

        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de l'interface QR Code: " + e.getMessage());
            showAlert("Erreur lors de l'ouverture du QR Code");
        }
    }

    @FXML
    void annulerRES(ActionEvent event) {
        goToCatalogue(event);
    }

    @FXML
    void goToCatalogue(ActionEvent event) {
        fermerFenetre();
    }

    @FXML
    void goToPanier(ActionEvent event) {
        fermerFenetre();
    }

    @FXML
    void nombrePersonnesChanged(ActionEvent event) {
        updateLabels();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearFields() {
        nomField.clear();
        emailField.clear();
        telephoneField.clear();
        nombrePersonnesField.clear();
        demandesp.clear();
    }

    private void updateLabels() {
        System.out.println("=== DEBUG: updateLabels() appelé ===");
        System.out.println("currentEvent: " + (currentEvent != null ? "NON NULL" : "NULL"));
        System.out.println("nombrePersonnesField.getText(): '" + nombrePersonnesField.getText() + "'");

        if (currentEvent != null && !nombrePersonnesField.getText().isEmpty()) {
            try {
                int nbPersonnes = Integer.parseInt(nombrePersonnesField.getText());

                System.out.println("currentEvent.getPrix(): " + (currentEvent.getPrix() != null ? currentEvent.getPrix() : "NULL"));

                double prixUnitaire = currentEvent.getPrix().doubleValue();
                double total = prixUnitaire * nbPersonnes;

                System.out.println("prixUnitaire: " + prixUnitaire);
                System.out.println("total calculé: " + total);

                prixlabel.setText(" " + total + " TND");
            } catch (NumberFormatException e) {
                prixlabel.setText("  0 TND");
            }
        } else {
            System.out.println("DEBUG: currentEvent est NULL ou nombrePersonnesField est vide");
        }
    }

    private void fermerFenetre() {
        if (EventDescription != null && EventDescription.getScene() != null) {
            Stage stage = (Stage) EventDescription.getScene().getWindow();
            stage.close();
        }
    }

    private boolean eventExists(int eventId) {
        try {
            String sql = "SELECT COUNT(*) FROM events WHERE id = ?";
            java.sql.PreparedStatement ps = Mydatabase.getInstance().getConnextion().prepareStatement(sql);
            ps.setInt(1, eventId);
            java.sql.ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}