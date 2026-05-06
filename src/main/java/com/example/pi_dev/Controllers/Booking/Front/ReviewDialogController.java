package com.example.pi_dev.Controllers.Booking.Front;

import com.example.pi_dev.Services.Booking.AiReviewResult;
import com.example.pi_dev.Services.Booking.AiReviewService;
import com.example.pi_dev.Services.Booking.BookingService;
import com.example.pi_dev.Services.Booking.ReviewService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ReviewDialogController {

    @FXML
    private ChoiceBox<Integer> ratingBox;
    @FXML
    private TextArea commentArea;
    @FXML
    private Label errorLabel;

    // AI result section (hidden until analysis completes)
    @FXML
    private VBox aiResultBox;
    @FXML
    private Label sentimentLabel;
    @FXML
    private Label aiSummaryLabel;
    @FXML
    private ProgressIndicator aiProgress;

    private int placeId;
    private String userId;
    private final ReviewService reviewService = new ReviewService();
    private final BookingService bookingService = new BookingService();

    @FXML
    public void initialize() {
        ratingBox.getItems().addAll(1, 2, 3, 4, 5);
        ratingBox.setValue(5);
        if (errorLabel != null)
            errorLabel.setText("");
        if (aiResultBox != null) {
            aiResultBox.setVisible(false);
            aiResultBox.setManaged(false);
        }
        if (aiProgress != null) {
            aiProgress.setVisible(false);
            aiProgress.setManaged(false);
        }
    }

    /** Must be called before showing the dialog. */
    public void setContext(int placeId, String userId) {
        this.placeId = placeId;
        this.userId = userId;
    }

    @FXML
    private void handleSubmit() {
        if (errorLabel != null)
            errorLabel.setText("");

        // Eligibility check
        try {
            boolean canReview = bookingService.canReview(userId, placeId);
            if (!canReview) {
                if (errorLabel != null)
                    errorLabel.setText(
                            "❌ You can only review this place after your stay is completed or confirmed and past the end date.");
                return;
            }
        } catch (Exception e) {
            if (errorLabel != null)
                errorLabel.setText("Error checking eligibility: " + e.getMessage());
            return;
        }

        Integer rating = ratingBox.getValue();
        if (rating == null) {
            if (errorLabel != null)
                errorLabel.setText("Please select a rating.");
            return;
        }

        String comment = commentArea.getText() != null ? commentArea.getText().trim() : "";

        // 1. Insert review synchronously
        int reviewId;
        try {
            reviewId = reviewService.addReview(placeId, userId, rating, comment);
            reviewService.refreshPlaceRatingStats(placeId);
        } catch (Exception e) {
            if (errorLabel != null)
                errorLabel.setText("Error saving review: " + e.getMessage());
            return;
        }

        // 2. Launch AI analysis in background thread
        showAiProgress(true);
        final String finalComment = comment;
        final int finalReviewId = reviewId;
        final AiReviewService aiService = new AiReviewService();

        Task<AiReviewResult> aiTask = new Task<>() {
            @Override
            protected AiReviewResult call() {
                // Analyse the text (heuristic or Gemini API)
                AiReviewResult result = aiService.analyzeReview(finalComment);
                // Persist sentiment + summary to the DB
                reviewService.analyzeAndUpdateReview(finalReviewId, finalComment);
                return result;
            }
        };

        aiTask.setOnSucceeded(evt -> {
            showAiProgress(false);
            AiReviewResult result = aiTask.getValue();
            displayAiResult(result);
        });

        aiTask.setOnFailed(evt -> {
            showAiProgress(false);
            displayAiResult(new AiReviewResult("NEUTRAL", "Analyse IA indisponible."));
        });

        Thread aiThread = new Thread(aiTask, "ai-review-thread");
        aiThread.setDaemon(true);
        aiThread.start();
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void showAiProgress(boolean show) {
        if (aiProgress != null) {
            aiProgress.setVisible(show);
            aiProgress.setManaged(show);
        }
    }

    private void displayAiResult(AiReviewResult result) {
        if (aiResultBox == null) {
            closeStage();
            return;
        }

        // Sentiment badge color
        String sentiment = result.getSentiment();
        String badgeColor;
        String icon;
        switch (sentiment) {
            case "POSITIVE" -> {
                badgeColor = "#16A34A";
                icon = "😊 ";
            }
            case "NEGATIVE" -> {
                badgeColor = "#DC2626";
                icon = "😞 ";
            }
            default -> {
                badgeColor = "#6B7280";
                icon = "😐 ";
            }
        }

        if (sentimentLabel != null) {
            sentimentLabel.setText(icon + sentiment);
            sentimentLabel.setStyle("-fx-text-fill: white; -fx-background-color: " + badgeColor +
                    "; -fx-background-radius: 12; -fx-padding: 4 12 4 12; -fx-font-weight: bold; -fx-font-size: 13px;");
        }
        if (aiSummaryLabel != null) {
            aiSummaryLabel.setText("📝 " + result.getSummary());
        }

        aiResultBox.setVisible(true);
        aiResultBox.setManaged(true);

        // Auto-close after 3 seconds so user sees the result
        Task<Void> closeTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Thread.sleep(3000);
                return null;
            }
        };
        closeTask.setOnSucceeded(e -> closeStage());
        Thread t = new Thread(closeTask, "review-close-thread");
        t.setDaemon(true);
        t.start();
    }

    private void closeStage() {
        if (ratingBox != null && ratingBox.getScene() != null) {
            ((Stage) ratingBox.getScene().getWindow()).close();
        }
    }
}
