package com.example.pi_dev.Controllers.Blog;

import com.example.pi_dev.Entities.Blog.Commentaire;
import com.example.pi_dev.Entities.Blog.Post;
import com.example.pi_dev.Entities.Blog.Reaction;
import com.example.pi_dev.Services.Blog.AI_ModerationService;
import com.example.pi_dev.Services.Blog.ModerationResult;
import com.example.pi_dev.Services.Blog.Commentaire_Services;
import com.example.pi_dev.Services.Blog.NotificationService;
import com.example.pi_dev.Services.Blog.Posting_Services;
import com.example.pi_dev.Services.Blog.Reaction_Services;
import com.example.pi_dev.Services.Blog.TranslationService;
import com.example.pi_dev.Services.Blog.SaveService;

import com.example.pi_dev.Entities.Users.User;
import com.example.pi_dev.Utils.Users.UserSession;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BlogController implements Initializable {

    @FXML private TextArea         newPostContent;
    @FXML private TextField        newPostMediaPath;
    @FXML private Label            postErrorLabel;
    @FXML private Button           publishBtn;
    @FXML private ComboBox<String> visibilityCombo;
    @FXML private VBox             postsFeedContainer;
    @FXML private VBox             emptyStateBox;
    @FXML private Label            postCountLabel;
    @FXML private Label            currentUsernameLabel;
    @FXML private ScrollPane       mainScrollPane;
    @FXML private VBox             archiveView;
    @FXML private VBox             archiveFeedContainer;
    @FXML private VBox             archiveEmptyBox;
    @FXML private Label            archiveCountLabel;

    @FXML private VBox             notifPanel;
    @FXML private Label            notifBadge;
    @FXML private VBox             notifListContainer;
    @FXML private Button           notifTabAll;
    @FXML private Button           notifTabUnread;

    private final Posting_Services     postService        = new Posting_Services();
    private final Commentaire_Services commentService     = new Commentaire_Services();
    private final Reaction_Services    reactionService    = new Reaction_Services();
    private final AI_ModerationService moderationService  = new AI_ModerationService();
    private final TranslationService   translationService = new TranslationService();
    private final NotificationService  notificationService = new NotificationService();

    // ═══════════════════════════════════════════
    // Current User — NOW String (UUID)
    // ═══════════════════════════════════════════
    private String currentUserId   = "00000000-0000-0000-0000-000000000002"; // default, will be set by setCurrentUser
    private String currentUsername = "Utilisateur2";

    public void setCurrentUser(String userId, String username) {
        this.currentUserId   = userId;
        this.currentUsername = username;
        if (currentUsernameLabel != null)
            currentUsernameLabel.setText(username);
    }

    private final ObservableList<Post>        postsList         = FXCollections.observableArrayList();
    private final Map<Integer, Integer>       reportCounts      = new HashMap<>();
    private final Map<Integer, Set<String>>   commentReportMap  = new HashMap<>(); // Set<String> for UUID reporters
    private final Set<Integer>                hiddenPostIds     = new HashSet<>();
    private SaveService                       saveService;
    private Set<Integer>                      savedPostIds      = new HashSet<>();
    private final Set<Integer>                aiHiddenPostIds   = new HashSet<>();
    private final Map<Integer, String>        translationCache  = new HashMap<>();
    private final Set<Integer>                translatedPostIds = new HashSet<>();
    private final List<LocalDateTime>         commentTimestamps = new ArrayList<>();

    // ── Notifications (DB-backed) ─────────────────────────────────────
    // We use NotificationService.NotifRecord instead of in-memory Notif.
    // notifications list is loaded from DB for the current user on login / refresh.
    private List<NotificationService.NotifRecord> notifications = new ArrayList<>();
    private boolean showUnreadOnly = false;

    private static final int MAX_COMMENTS_IN_WINDOW = 3;
    private static final int COMMENT_WINDOW_MINUTES = 1;
    private static final int MAX_REPORTS            = 10;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private static final Background AVATAR_BG_LARGE = buildGradientBackground(
            Color.web("#1E40AF"), Color.web("#3B82F6"), 38);
    private static final Background AVATAR_BG_SMALL = buildGradientBackground(
            Color.web("#1E40AF"), Color.web("#3B82F6"), 28);

    private static Background buildGradientBackground(Color from, Color to, double radius) {
        LinearGradient gradient = new LinearGradient(
                0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, from), new Stop(1, to));
        return new Background(new BackgroundFill(
                gradient, new CornerRadii(radius), Insets.EMPTY));
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        User u = UserSession.getInstance().getCurrentUser();
        if (u != null) {
            currentUserId = u.getUserId().toString();
            currentUsername = u.getFullName() != null ? u.getFullName() : u.getEmail();
        }
        currentUsernameLabel.setText(currentUsername);
        visibilityCombo.setItems(FXCollections.observableArrayList("🌐 Public", "🔒 Privé"));
        visibilityCombo.getSelectionModel().selectFirst();
        saveService = new SaveService(currentUserId);
        savedPostIds = saveService.loadSavedPostIds();
        notificationService.ensureTable();
        loadNotificationsFromDB();
        loadPosts();
    }

    // ═══════════════════════════════════════════
    // Data
    // ═══════════════════════════════════════════

    private void loadPosts() {
        postsList.setAll(postService.afficherPosts());
        rebuildFeed();
    }

    private boolean canSeePost(Post post) {
        if (hiddenPostIds.contains(post.getIdPost())) return false;
        if (aiHiddenPostIds.contains(post.getIdPost())) return false;
        String statut = post.getStatut() != null ? post.getStatut().toLowerCase() : "public";
        if (statut.equals("hidden")) return false;
        if (statut.contains("private")) return currentUserId.equals(post.getIdUser()); // FIXED: .equals()
        return true;
    }

    private boolean isOwner(Post post) {
        return currentUserId.equals(post.getIdUser()); // FIXED: .equals()
    }

    private void rebuildFeed() {
        postsFeedContainer.getChildren().clear();
        int visible = 0;
        for (Post post : postsList) {
            if (!canSeePost(post)) continue;
            VBox card = buildPostCard(post, false);
            card.setOpacity(0);
            postsFeedContainer.getChildren().add(card);
            FadeTransition ft = new FadeTransition(Duration.millis(250), card);
            ft.setFromValue(0); ft.setToValue(1); ft.play();
            visible++;
        }
        boolean empty = visible == 0;
        emptyStateBox.setVisible(empty);
        emptyStateBox.setManaged(empty);
        postCountLabel.setText("· " + visible + (visible == 1 ? " publication" : " publications"));
    }

    private void rebuildArchive() {
        archiveFeedContainer.getChildren().clear();
        List<Post> saved = new ArrayList<>();
        for (Post p : postsList)
            if (savedPostIds.contains(p.getIdPost())) saved.add(p);
        for (Post post : saved)
            archiveFeedContainer.getChildren().add(buildPostCard(post, true));
        boolean empty = saved.isEmpty();
        archiveEmptyBox.setVisible(empty);
        archiveEmptyBox.setManaged(empty);
        archiveCountLabel.setText("· " + saved.size() + " sauvegardé(s)");
    }

    // ═══════════════════════════════════════════
    // AI Moderation
    // ═══════════════════════════════════════════

    private void runModerationAsync(Post post, boolean isEdit) {
        showToast("🤖 L'IA analyse votre publication…");
        CompletableFuture
                .supplyAsync(() -> moderationService.moderate(post.getContenu()))
                .thenAccept(result -> Platform.runLater(() -> handleModerationResult(post, result, isEdit)));
    }

    private void handleModerationResult(Post post, ModerationResult result, boolean isEdit) {
        if (result.shouldHide) {
            aiHiddenPostIds.add(post.getIdPost());
            post.setStatut("hidden");
            postService.modifierPost(post);
            rebuildFeed();
            showStyledModerationDialog(result);
        } else {
            showToast(String.format("✅ Vérification IA réussie (%.0f%% de risque)", result.score * 100));
        }
    }

    // ═══════════════════════════════════════════
    // Translation
    // ═══════════════════════════════════════════

    private void handleTranslate(Post post, Label contentLabel, Button translateBtn) {
        int postId = post.getIdPost();
        if (translatedPostIds.contains(postId)) {
            contentLabel.setText(post.getContenu());
            translatedPostIds.remove(postId);
            translateBtn.setText("🌐 Traduire");
            translateBtn.setStyle(getTranslateBtnStyle(false));
            return;
        }
        if (translationCache.containsKey(postId)) {
            contentLabel.setText(translationCache.get(postId));
            translatedPostIds.add(postId);
            translateBtn.setText("↩ Voir l'original");
            translateBtn.setStyle(getTranslateBtnStyle(true));
            return;
        }
        translateBtn.setText("⏳ Traduction…");
        translateBtn.setDisable(true);
        CompletableFuture
                .supplyAsync(() -> translationService.translateAuto(post.getContenu()))
                .thenAccept(result -> Platform.runLater(() -> {
                    translateBtn.setDisable(false);
                    if (!result.success) {
                        translateBtn.setText("🌐 Traduire");
                        translateBtn.setStyle(getTranslateBtnStyle(false));
                        showToast("⚠ Traduction échouée. Réessayez.");
                        return;
                    }
                    translationCache.put(postId, result.translatedText);
                    translatedPostIds.add(postId);
                    FadeTransition ft = new FadeTransition(Duration.millis(200), contentLabel);
                    ft.setFromValue(1); ft.setToValue(0);
                    ft.setOnFinished(e -> {
                        contentLabel.setText(result.translatedText);
                        FadeTransition fi = new FadeTransition(Duration.millis(200), contentLabel);
                        fi.setFromValue(0); fi.setToValue(1); fi.play();
                    });
                    ft.play();
                    translateBtn.setText("↩ Voir l'original");
                    translateBtn.setStyle(getTranslateBtnStyle(true));
                    String langLabel = "fr".equals(result.sourceLang) ? "FR → EN" : "EN → FR";
                    showToast("🌐 Traduit (" + langLabel + ")");
                }));
    }

    private String getTranslateBtnStyle(boolean isActive) {
        return "-fx-background-color:transparent; -fx-background-radius:8;" +
                "-fx-text-fill:" + (isActive ? "#0891B2" : "#6B7280") + "; -fx-font-size:11px;" +
                "-fx-font-family:'Segoe UI',Arial; -fx-padding:6 10 6 10; -fx-cursor:HAND;";
    }

    // ═══════════════════════════════════════════
    // Post Card
    // ═══════════════════════════════════════════

    private VBox buildPostCard(Post post, boolean isArchive) {
        VBox card = new VBox(0);
        String baseStyle =
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),16,0,0,2);";
        String hoverStyle =
                "-fx-background-color:#FFFFFF;" +
                        "-fx-background-radius:14;" +
                        "-fx-effect:dropshadow(gaussian,rgba(30,64,175,0.13),22,0,0,5);";
        card.setStyle(baseStyle);
        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(baseStyle));

        VBox inner = new VBox(11);
        inner.setPadding(new Insets(18, 20, 4, 20));
        card.getChildren().add(inner);

        inner.getChildren().add(buildCardHeader(post, isArchive));

        Label contentLabel = new Label(
                translatedPostIds.contains(post.getIdPost())
                        ? translationCache.getOrDefault(post.getIdPost(), post.getContenu())
                        : post.getContenu());
        contentLabel.setWrapText(true);
        contentLabel.setStyle(
                "-fx-font-size:13px; -fx-text-fill:#374151;" +
                        "-fx-font-family:'Segoe UI',Arial; -fx-line-spacing:2;");
        inner.getChildren().add(contentLabel);

        if (post.getMedia() != null && !post.getMedia().isBlank()) {
            try {
                Node mediaNode = buildMediaNode(post.getMedia());
                if (mediaNode != null) inner.getChildren().add(mediaNode);
            } catch (Exception ex) {
                Label warn = new Label("⚠ Impossible de charger : " + new File(post.getMedia()).getName());
                warn.setStyle("-fx-font-size:11px; -fx-text-fill:#F59E0B;");
                inner.getChildren().add(warn);
            }
        }

        List<Reaction>    reactions   = reactionService.getReactionsByPost(post.getIdPost());
        List<Commentaire> comments    = commentService.getCommentairesByPost(post.getIdPost());
        int               reportCount = reportCounts.getOrDefault(post.getIdPost(), 0);

        HBox stats = new HBox(6);
        stats.setAlignment(Pos.CENTER_LEFT);
        stats.setPadding(new Insets(2, 0, 0, 0));
        stats.getChildren().addAll(statLabel("♥ " + reactions.size()), statLabel("·"), statLabel("💬 " + comments.size()));
        if (reportCount > 0) stats.getChildren().addAll(statLabel("·"), statLabel("⚑ " + reportCount));
        inner.getChildren().add(stats);

        Separator sep = new Separator();
        sep.setPadding(new Insets(3, 0, 0, 0));
        inner.getChildren().add(sep);

        VBox commentsSection = buildCommentsSection(post, comments);
        commentsSection.setVisible(false);
        commentsSection.setManaged(false);

        HBox actionsRow = buildActionsRow(post, reactions, commentsSection, inner, contentLabel, isArchive);
        inner.getChildren().add(actionsRow);
        inner.getChildren().add(commentsSection);

        return card;
    }

    private HBox buildCardHeader(Post post, boolean isArchive) {
        HBox header = new HBox(11);
        header.setAlignment(Pos.CENTER_LEFT);

        String ownerLabel = isOwner(post) ? currentUsername : "Utilisateur";
        Label avatar = new Label(ownerLabel.substring(0, 1).toUpperCase());
        avatar.setMinSize(38, 38); avatar.setMaxSize(38, 38);
        avatar.setAlignment(Pos.CENTER);
        avatar.setBackground(AVATAR_BG_LARGE);
        avatar.setStyle("-fx-text-fill:white; -fx-font-size:14px; -fx-font-weight:bold;");

        VBox meta = new VBox(1);
        Label nameL = new Label(ownerLabel + (isOwner(post) ? " (moi)" : ""));
        nameL.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#111827; -fx-font-family:'Segoe UI',Arial;");

        HBox dateLine = new HBox(6);
        dateLine.setAlignment(Pos.CENTER_LEFT);
        Label dateL = new Label(post.getDateCreation() != null ? post.getDateCreation().format(DATE_FMT) : "");
        dateL.setStyle("-fx-font-size:10px; -fx-text-fill:#9CA3AF;");

        boolean isPrivate = post.getStatut() != null && post.getStatut().toLowerCase().contains("private");
        Label visBadge = new Label(isPrivate ? "🔒 Privé" : "🌐 Public");
        visBadge.setStyle(
                "-fx-background-color:" + (isPrivate ? "#FEF3C7" : "#EFF6FF") + ";" +
                        "-fx-background-radius:20;" +
                        "-fx-text-fill:" + (isPrivate ? "#92400E" : "#1E40AF") + ";" +
                        "-fx-font-size:9px; -fx-padding:2 7 2 7;");
        dateLine.getChildren().addAll(dateL, visBadge);
        meta.getChildren().addAll(nameL, dateLine);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        header.getChildren().addAll(avatar, meta, spacer, buildMoreMenu(post, isArchive));
        return header;
    }

    private MenuButton buildMoreMenu(Post post, boolean isArchive) {
        MenuButton btn = new MenuButton("• • •");
        btn.setStyle(
                "-fx-background-color:transparent; -fx-background-radius:8;" +
                        "-fx-text-fill:#9CA3AF; -fx-font-size:11px;" +
                        "-fx-padding:4 8 4 8; -fx-cursor:HAND; -fx-border-color:transparent;");

        boolean isSaved = savedPostIds.contains(post.getIdPost());
        MenuItem saveItem = new MenuItem(isSaved ? "🗂  Retirer des sauvegardés" : "🗂  Sauvegarder");
        saveItem.setOnAction(e -> {
            boolean nowSaved = saveService.toggle(post.getIdPost());
            savedPostIds = saveService.loadSavedPostIds();
            showToast(nowSaved ? "Publication sauvegardée ✓" : "Retiré des sauvegardés");
            rebuildFeed();
            if (archiveView.isVisible()) rebuildArchive();
        });

        MenuItem hideItem = new MenuItem("🙈  Masquer cette publication");
        hideItem.setOnAction(e -> {
            hiddenPostIds.add(post.getIdPost());
            rebuildFeed();
            if (archiveView.isVisible()) rebuildArchive();
        });

        boolean currentlyPrivate = post.getStatut() != null && post.getStatut().toLowerCase().contains("private");
        MenuItem visItem = new MenuItem(currentlyPrivate ? "🌐  Rendre Public" : "🔒  Rendre Privé");
        visItem.setOnAction(e -> {
            post.setStatut(currentlyPrivate ? "public" : "private");
            postService.modifierPost(post);
            rebuildFeed();
            if (archiveView.isVisible()) rebuildArchive();
        });

        SeparatorMenuItem sep = new SeparatorMenuItem();
        MenuItem reportItem = new MenuItem("⚑  Signaler la publication");
        reportItem.setOnAction(e -> handleReport(post));

        if (isOwner(post)) {
            btn.getItems().addAll(saveItem, visItem);
        } else {
            btn.getItems().addAll(saveItem, hideItem, sep, reportItem);
        }
        return btn;
    }

    private Node buildMediaNode(String mediaPath) {
        if (mediaPath == null || mediaPath.isBlank()) return null;
        String lower = mediaPath.toLowerCase();
        boolean isVideo = lower.endsWith(".mp4") || lower.endsWith(".avi")
                || lower.endsWith(".mov") || lower.endsWith(".mkv")
                || lower.endsWith(".webm") || lower.endsWith(".flv");
        return isVideo ? buildVideoPlayer(mediaPath) : buildImageNode(mediaPath);
    }

    private Node buildImageNode(String path) {
        try {
            File f = new File(path);
            String url = f.exists() ? f.toURI().toString() : path;
            Image img = new Image(url, 660, 0, true, true, true);
            ImageView iv = new ImageView(img);
            iv.setFitWidth(660); iv.setPreserveRatio(true);
            return iv;
        } catch (Exception e) { return null; }
    }

    private Node buildVideoPlayer(String path) {
        try {
            File f = new File(path);
            String url = f.exists() ? f.toURI().toString() : path;
            Media media = new Media(url);
            MediaPlayer player = new MediaPlayer(media);
            MediaView view = new MediaView(player);
            view.setFitWidth(660); view.setPreserveRatio(true);

            Button playPause = new Button("▶");
            playPause.setStyle("-fx-background-color:#1E40AF; -fx-background-radius:8; -fx-text-fill:white; -fx-font-size:13px; -fx-min-width:36; -fx-min-height:32; -fx-cursor:HAND;");

            Slider progress = new Slider(0, 1, 0);
            HBox.setHgrow(progress, Priority.ALWAYS);

            Label timeLabel = new Label("0:00");
            timeLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#9CA3AF;");

            Button muteBtn = new Button("🔊");
            muteBtn.setStyle("-fx-background-color:transparent; -fx-font-size:13px; -fx-cursor:HAND;");

            HBox controls = new HBox(8, playPause, progress, timeLabel, muteBtn);
            controls.setAlignment(Pos.CENTER_LEFT);
            controls.setPadding(new Insets(8, 12, 8, 12));
            controls.setStyle("-fx-background-color:#1F2937;");

            final boolean[] playing = {false};
            playPause.setOnAction(e -> {
                if (playing[0]) { player.pause(); playPause.setText("▶"); }
                else            { player.play();  playPause.setText("⏸"); }
                playing[0] = !playing[0];
            });

            final boolean[] muted = {false};
            muteBtn.setOnAction(e -> {
                muted[0] = !muted[0]; player.setMute(muted[0]);
                muteBtn.setText(muted[0] ? "🔇" : "🔊");
            });

            progress.setOnMousePressed(e -> {
                javafx.util.Duration total = player.getTotalDuration();
                if (total != null) player.seek(javafx.util.Duration.seconds(progress.getValue() * total.toSeconds()));
            });
            progress.setOnMouseDragged(e -> {
                javafx.util.Duration total = player.getTotalDuration();
                if (total != null) player.seek(javafx.util.Duration.seconds(progress.getValue() * total.toSeconds()));
            });

            player.currentTimeProperty().addListener((obs, old, now) -> {
                javafx.util.Duration total = player.getTotalDuration();
                if (total != null && total.toSeconds() > 0) {
                    progress.setValue(now.toSeconds() / total.toSeconds());
                    int secs = (int) now.toSeconds();
                    timeLabel.setText(secs / 60 + ":" + String.format("%02d", secs % 60));
                }
            });

            player.setOnEndOfMedia(() -> { player.seek(javafx.util.Duration.ZERO); playing[0] = false; playPause.setText("▶"); });

            VBox videoCard = new VBox(0, view, controls);
            videoCard.setStyle("-fx-background-color:#000000; -fx-background-radius:10;");
            Rectangle clip = new Rectangle();
            clip.setArcWidth(20); clip.setArcHeight(20);
            clip.widthProperty().bind(videoCard.widthProperty());
            clip.heightProperty().bind(videoCard.heightProperty());
            videoCard.setClip(clip);

            videoCard.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene == null) {
                    player.stop();
                    player.dispose();
                }
            });

            return videoCard;
        } catch (Exception e) {
            Label err = new Label("⚠ Impossible de charger la vidéo : " + new File(path).getName());
            err.setStyle("-fx-font-size:11px; -fx-text-fill:#F59E0B;");
            return err;
        }
    }

    private HBox buildActionsRow(Post post, List<Reaction> reactions,
                                 VBox commentsSection, VBox cardInner,
                                 Label contentLabel, boolean isArchive) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4, 0, 10, 0));

        boolean alreadyReacted = reactionService.getUserReaction(post.getIdPost(), null, currentUserId) != null;
        Button reactBtn = alreadyReacted ? filledButton("♥  " + reactions.size(), "#EF4444")
                : ghostButton("♥  " + reactions.size(), "#EF4444");
        reactBtn.setOnAction(e -> handleReact(post, reactBtn));

        Button commentBtn = ghostButton("💬 Commenter", "#1E40AF");
        commentBtn.setOnAction(e -> {
            boolean showing = commentsSection.isVisible();
            commentsSection.setVisible(!showing);
            commentsSection.setManaged(!showing);
            commentBtn.setText(showing ? "💬 Commenter" : "💬 Masquer");
        });

        boolean alreadyTranslated = translatedPostIds.contains(post.getIdPost());
        Button translateBtn = new Button(alreadyTranslated ? "↩ Voir l'original" : "🌐 Traduire");
        translateBtn.setStyle(getTranslateBtnStyle(alreadyTranslated));
        translateBtn.setOnMouseEntered(e -> {
            if (!translateBtn.isDisabled())
                translateBtn.setStyle(translateBtn.getStyle().replace("-fx-background-color:transparent;", "-fx-background-color:#EFF6FF;"));
        });
        translateBtn.setOnMouseExited(e -> translateBtn.setStyle(getTranslateBtnStyle(translatedPostIds.contains(post.getIdPost()))));
        translateBtn.setOnAction(e -> handleTranslate(post, contentLabel, translateBtn));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(reactBtn, commentBtn, translateBtn, spacer);

        if (isOwner(post)) {
            Button editBtn   = outlineButton("✏ Modifier", "#6B7280");
            Button deleteBtn = outlineButton("🗑 Supprimer", "#EF4444");
            editBtn.setOnAction(e   -> handleEditPost(post, cardInner, contentLabel));
            deleteBtn.setOnAction(e -> handleDeletePost(post));
            row.getChildren().addAll(editBtn, deleteBtn);
        }
        return row;
    }

    // ═══════════════════════════════════════════
    // Comments
    // ═══════════════════════════════════════════

    private VBox buildCommentsSection(Post post, List<Commentaire> comments) {
        VBox section = new VBox(0);
        section.setStyle("-fx-background-color:#F8FAFF; -fx-background-radius:0 0 12 12; -fx-padding:12 20 14 20;");

        Label spamLabel = new Label();
        spamLabel.setStyle("-fx-text-fill:#EF4444; -fx-font-size:11px; -fx-font-family:'Segoe UI',Arial; -fx-padding:0 0 6 0;");
        spamLabel.setVisible(false); spamLabel.setManaged(false);

        // Sort: most reactions first; tie-break by newest date
        List<Commentaire> sorted = new ArrayList<>(comments);
        sorted.sort((a, b) -> {
            int rA = reactionService.getReactionsByCommentaire(a.getIdCommentaire()).size();
            int rB = reactionService.getReactionsByCommentaire(b.getIdCommentaire()).size();
            if (rB != rA) return Integer.compare(rB, rA);
            LocalDateTime dA = a.getDate() != null ? a.getDate() : LocalDateTime.MIN;
            LocalDateTime dB = b.getDate() != null ? b.getDate() : LocalDateTime.MIN;
            return dB.compareTo(dA);
        });

        VBox commentsList = new VBox(4);
        for (Commentaire c : sorted)
            commentsList.getChildren().add(buildCommentItem(c, post, spamLabel));

        HBox addForm = buildAddCommentForm(post, commentsList, null, null, spamLabel);
        section.getChildren().addAll(spamLabel, commentsList, addForm);
        return section;
    }

    private VBox buildCommentItem(Commentaire comment, Post post, Label spamLabel) {
        VBox wrapper = new VBox(0);
        wrapper.setPadding(new Insets(6, 0, 4, 0));

        HBox mainRow = new HBox(10);
        mainRow.setAlignment(Pos.TOP_LEFT);

        boolean isMyComment = currentUserId.equals(comment.getIdUser()); // FIXED: .equals()
        String avatarLetter = isMyComment ? currentUsername.substring(0, 1).toUpperCase() : "U";
        Label avatar = new Label(avatarLetter);
        avatar.setMinSize(32, 32); avatar.setMaxSize(32, 32);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle(isMyComment
                ? "-fx-background-color:#BFDBFE; -fx-background-radius:16; -fx-text-fill:#1E40AF; -fx-font-size:12px; -fx-font-weight:bold;"
                : "-fx-background-color:#DBEAFE; -fx-background-radius:16; -fx-text-fill:#1E40AF; -fx-font-size:12px; -fx-font-weight:bold;");

        VBox rightCol = new VBox(4);
        HBox.setHgrow(rightCol, Priority.ALWAYS);

        VBox bubble = new VBox(3);
        bubble.setStyle("-fx-background-color:#FFFFFF; -fx-background-radius:4 14 14 14; -fx-padding:8 12 8 12; -fx-effect:dropshadow(gaussian,rgba(0,0,0,0.04),5,0,0,1);");

        Label nameL = new Label(isMyComment ? currentUsername + " (moi)" : "Utilisateur");
        nameL.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#111827; -fx-font-family:'Segoe UI',Arial;");

        Label textL = new Label(comment.getContenu());
        textL.setWrapText(true);
        textL.setStyle("-fx-font-size:13px; -fx-text-fill:#374151; -fx-font-family:'Segoe UI',Arial;");
        bubble.getChildren().addAll(nameL, textL);

        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_LEFT);
        actionRow.setPadding(new Insets(3, 0, 0, 4));

        Label dateL = new Label(comment.getDate() != null ? comment.getDate().format(DATE_FMT) : "");
        dateL.setStyle("-fx-font-size:10px; -fx-text-fill:#9CA3AF;");

        List<Reaction> cReacts = reactionService.getReactionsByCommentaire(comment.getIdCommentaire());
        boolean alreadyReactedC = reactionService.getUserReaction(null, comment.getIdCommentaire(), currentUserId) != null;
        Button cReactBtn = linkButton((alreadyReactedC ? "♥ " : "♡ ") + cReacts.size(), alreadyReactedC ? "#EF4444" : "#6B7280");
        cReactBtn.setOnAction(e -> handleCommentReact(comment, cReactBtn));

        List<Commentaire> replies = commentService.getRepliesByCommentaire(comment.getIdCommentaire());
        String replyLabel = replies.isEmpty() ? "↩ Répondre" : "↩ Répondre · " + replies.size();
        Button replyToggle = linkButton(replyLabel, "#1E40AF");

        MenuButton moreBtn = new MenuButton("•••");
        moreBtn.setStyle(
                "-fx-background-color:transparent; -fx-border-color:transparent;" +
                        "-fx-text-fill:#9CA3AF; -fx-font-size:11px; -fx-padding:1 5 1 5; -fx-cursor:HAND;");

        if (isMyComment) {
            MenuItem editItem = new MenuItem("✏  Modifier");
            editItem.setOnAction(e -> {
                TextField editField = new TextField(comment.getContenu());
                editField.setStyle(
                        "-fx-font-size:12px; -fx-background-radius:6;" +
                                "-fx-border-color:#3B82F6; -fx-border-radius:6; -fx-padding:4 8 4 8;");
                int textIdx = bubble.getChildren().indexOf(textL);
                bubble.getChildren().set(textIdx, editField);

                Button confirmBtn = linkButton("✓ Enregistrer", "#22C55E");
                Button cancelBtn  = linkButton("Annuler", "#9CA3AF");
                HBox editBtns = new HBox(8, confirmBtn, cancelBtn);
                editBtns.setPadding(new Insets(4, 0, 0, 0));
                bubble.getChildren().add(editBtns);

                confirmBtn.setOnAction(ev -> {
                    String updated = editField.getText().trim();
                    if (!updated.isEmpty()) {
                        comment.setContenu(updated);
                        commentService.modifierCommentaire(comment);
                        textL.setText(updated);
                    }
                    int fieldIdx = bubble.getChildren().indexOf(editField);
                    if (fieldIdx >= 0) bubble.getChildren().set(fieldIdx, textL);
                    bubble.getChildren().remove(editBtns);
                });
                cancelBtn.setOnAction(ev -> {
                    int fieldIdx = bubble.getChildren().indexOf(editField);
                    if (fieldIdx >= 0) bubble.getChildren().set(fieldIdx, textL);
                    bubble.getChildren().remove(editBtns);
                });
            });

            MenuItem deleteItem = new MenuItem("🗑  Supprimer");
            deleteItem.setStyle("-fx-text-fill:#EF4444;");
            deleteItem.setOnAction(e -> {
                commentService.supprimerCommentaire(comment.getIdCommentaire());
                javafx.scene.Node parent = wrapper.getParent();
                if (parent instanceof VBox) {
                    FadeTransition ft = new FadeTransition(Duration.millis(180), wrapper);
                    ft.setFromValue(1); ft.setToValue(0);
                    ft.setOnFinished(ev -> ((VBox) parent).getChildren().remove(wrapper));
                    ft.play();
                }
            });

            moreBtn.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);

        } else {
            Set<String> reporters = commentReportMap.computeIfAbsent(
                    comment.getIdCommentaire(), k -> new HashSet<>()); // FIXED: Set<String>

            boolean alreadyReported = reporters.contains(currentUserId);
            MenuItem reportItem = new MenuItem(alreadyReported ? "⚑  Déjà signalé" : "⚑  Signaler");
            reportItem.setDisable(alreadyReported);

            reportItem.setOnAction(e -> {
                reporters.add(currentUserId); // FIXED: add String UUID
                int count = reporters.size();
                reportItem.setText("⚑  Déjà signalé");
                reportItem.setDisable(true);

                if (count >= 3) {
                    commentService.supprimerCommentaire(comment.getIdCommentaire());
                    commentReportMap.remove(comment.getIdCommentaire());
                    javafx.scene.Node parent = wrapper.getParent();
                    if (parent instanceof VBox) {
                        FadeTransition ft = new FadeTransition(Duration.millis(200), wrapper);
                        ft.setFromValue(1); ft.setToValue(0);
                        ft.setOnFinished(ev -> ((VBox) parent).getChildren().remove(wrapper));
                        ft.play();
                    }
                    showToast("🗑 Commentaire supprimé après 3 signalements.");
                } else {
                    showToast("Commentaire signalé (" + count + "/3)");
                }
            });

            moreBtn.getItems().add(reportItem);
        }

        actionRow.getChildren().addAll(dateL, cReactBtn, replyToggle, moreBtn);

        VBox replyThread = new VBox(6);
        replyThread.setPadding(new Insets(8, 0, 0, 44));
        replyThread.setVisible(false); replyThread.setManaged(false);

        for (Commentaire reply : replies)
            replyThread.getChildren().add(buildReplyBubble(reply, post, replyThread, replyToggle, spamLabel));

        HBox replyInput = buildAddCommentForm(post, replyThread, comment, replyToggle, spamLabel);
        replyThread.getChildren().add(replyInput);

        replyToggle.setOnAction(e -> {
            boolean showing = replyThread.isVisible();
            replyThread.setVisible(!showing);
            replyThread.setManaged(!showing);
            int count = (int) replyThread.getChildren().stream()
                    .filter(n -> !(n instanceof HBox)).count();
            replyToggle.setText(showing
                    ? (count > 0 ? "↩ Répondre · " + count : "↩ Répondre")
                    : (count > 0 ? "↩ Masquer · " + count  : "↩ Masquer"));
        });

        rightCol.getChildren().addAll(bubble, actionRow, replyThread);
        mainRow.getChildren().addAll(avatar, rightCol);
        wrapper.getChildren().add(mainRow);
        return wrapper;
    }

    private VBox buildReplyBubble(Commentaire reply, Post post, VBox replyThread,
                                  Button parentReplyToggle, Label spamLabel) {
        VBox wrapper = new VBox(0);
        HBox row = new HBox(8);
        row.setAlignment(Pos.TOP_LEFT);

        boolean isMyReply = currentUserId.equals(reply.getIdUser()); // FIXED: .equals()
        String letter = isMyReply ? currentUsername.substring(0, 1).toUpperCase() : "U";
        Label avatar = new Label(letter);
        avatar.setMinSize(26, 26); avatar.setMaxSize(26, 26);
        avatar.setAlignment(Pos.CENTER);
        avatar.setStyle("-fx-background-color:#DBEAFE; -fx-background-radius:13; -fx-text-fill:#1E40AF; -fx-font-size:10px; -fx-font-weight:bold;");

        VBox col = new VBox(2);

        VBox bubble = new VBox(2);
        bubble.setStyle("-fx-background-color:#EFF6FF; -fx-background-radius:4 14 14 14; -fx-padding:7 11 7 11;");
        Label nameL = new Label(isMyReply ? currentUsername + " (moi)" : "Utilisateur");
        nameL.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#1E40AF;");
        Label textL = new Label(reply.getContenu());
        textL.setWrapText(true);
        textL.setStyle("-fx-font-size:12px; -fx-text-fill:#374151;");
        bubble.getChildren().addAll(nameL, textL);

        HBox replyActionRow = new HBox(10);
        replyActionRow.setAlignment(Pos.CENTER_LEFT);
        replyActionRow.setPadding(new Insets(2, 0, 0, 2));

        Label dateL = new Label(reply.getDate() != null ? reply.getDate().format(DATE_FMT) : "");
        dateL.setStyle("-fx-font-size:10px; -fx-text-fill:#9CA3AF;");

        List<Reaction> rReacts = reactionService.getReactionsByCommentaire(reply.getIdCommentaire());
        boolean alreadyReactedR = reactionService.getUserReaction(null, reply.getIdCommentaire(), currentUserId) != null;
        Button rReactBtn = linkButton((alreadyReactedR ? "♥ " : "♡ ") + rReacts.size(), alreadyReactedR ? "#EF4444" : "#6B7280");
        rReactBtn.setOnAction(e -> handleCommentReact(reply, rReactBtn));

        Button replyToReplyBtn = linkButton("↩ Répondre", "#1E40AF");
        replyToReplyBtn.setOnAction(e -> {
            if (!replyThread.getChildren().isEmpty()) {
                javafx.scene.Node last = replyThread.getChildren().get(replyThread.getChildren().size() - 1);
                if (last instanceof HBox) {
                    HBox form = (HBox) last;
                    for (javafx.scene.Node n : form.getChildren()) {
                        if (n instanceof TextField tf) {
                            tf.setText("@" + (isMyReply ? currentUsername : "Utilisateur") + " ");
                            tf.requestFocus();
                            tf.positionCaret(tf.getText().length());
                            replyThread.setVisible(true);
                            replyThread.setManaged(true);
                            int cnt = (int) replyThread.getChildren().stream()
                                    .filter(nd -> !(nd instanceof HBox)).count();
                            parentReplyToggle.setText(cnt > 0 ? "↩ Masquer · " + cnt : "↩ Masquer");
                            break;
                        }
                    }
                }
            }
        });

        if (isMyReply) {
            MenuButton moreBtn = new MenuButton("•••");
            moreBtn.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;" +
                    "-fx-text-fill:#9CA3AF; -fx-font-size:10px; -fx-padding:1 4 1 4; -fx-cursor:HAND;");

            MenuItem editItem = new MenuItem("✏  Modifier");
            editItem.setOnAction(e -> {
                TextField editField = new TextField(reply.getContenu());
                editField.setStyle("-fx-font-size:11px; -fx-background-radius:6;" +
                        "-fx-border-color:#3B82F6; -fx-border-radius:6; -fx-padding:3 7 3 7;");
                int textIdx = bubble.getChildren().indexOf(textL);
                bubble.getChildren().set(textIdx, editField);

                Button confirmBtn = linkButton("✓", "#22C55E");
                Button cancelBtn  = linkButton("✕", "#9CA3AF");
                HBox editBtns = new HBox(6, confirmBtn, cancelBtn);
                bubble.getChildren().add(editBtns);

                confirmBtn.setOnAction(ev -> {
                    String updated = editField.getText().trim();
                    if (!updated.isEmpty()) {
                        reply.setContenu(updated);
                        commentService.modifierCommentaire(reply);
                        textL.setText(updated);
                    }
                    int fi = bubble.getChildren().indexOf(editField);
                    if (fi >= 0) bubble.getChildren().set(fi, textL);
                    bubble.getChildren().remove(editBtns);
                });
                cancelBtn.setOnAction(ev -> {
                    int fi = bubble.getChildren().indexOf(editField);
                    if (fi >= 0) bubble.getChildren().set(fi, textL);
                    bubble.getChildren().remove(editBtns);
                });
            });

            MenuItem deleteItem = new MenuItem("🗑  Supprimer");
            deleteItem.setOnAction(e -> {
                commentService.supprimerCommentaire(reply.getIdCommentaire());
                replyThread.getChildren().remove(wrapper);
                int cnt = (int) replyThread.getChildren().stream()
                        .filter(n -> !(n instanceof HBox)).count();
                parentReplyToggle.setText(cnt > 0 ? "↩ Masquer · " + cnt : "↩ Masquer");
            });

            moreBtn.getItems().addAll(editItem, new SeparatorMenuItem(), deleteItem);
            replyActionRow.getChildren().addAll(dateL, rReactBtn, replyToReplyBtn, moreBtn);
        } else {
            Set<String> reporters = commentReportMap.computeIfAbsent(
                    reply.getIdCommentaire(), k -> new HashSet<>()); // FIXED: Set<String>
            boolean alreadyReported = reporters.contains(currentUserId);
            MenuItem reportItem = new MenuItem(alreadyReported ? "⚑  Déjà signalé" : "⚑  Signaler");
            reportItem.setDisable(alreadyReported);
            reportItem.setOnAction(e -> {
                reporters.add(currentUserId); // FIXED: String UUID
                int count = reporters.size();
                reportItem.setText("⚑  Déjà signalé");
                reportItem.setDisable(true);
                if (count >= 3) {
                    commentService.supprimerCommentaire(reply.getIdCommentaire());
                    replyThread.getChildren().remove(wrapper);
                    int cnt = (int) replyThread.getChildren().stream()
                            .filter(n -> !(n instanceof HBox)).count();
                    parentReplyToggle.setText(cnt > 0 ? "↩ Masquer · " + cnt : "↩ Masquer");
                    showToast("🗑 Réponse supprimée après 3 signalements.");
                } else {
                    showToast("Réponse signalée (" + count + "/3)");
                }
            });

            MenuButton moreBtn = new MenuButton("•••");
            moreBtn.setStyle("-fx-background-color:transparent; -fx-border-color:transparent;" +
                    "-fx-text-fill:#9CA3AF; -fx-font-size:10px; -fx-padding:1 4 1 4; -fx-cursor:HAND;");
            moreBtn.getItems().add(reportItem);
            replyActionRow.getChildren().addAll(dateL, rReactBtn, replyToReplyBtn, moreBtn);
        }

        col.getChildren().addAll(bubble, replyActionRow);
        row.getChildren().addAll(avatar, col);
        wrapper.getChildren().add(row);
        return wrapper;
    }

    private HBox buildAddCommentForm(Post post, VBox targetList,
                                     Commentaire parentComment, Button replyToggle, Label spamLabel) {
        HBox form = new HBox(8);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(parentComment == null ? 10 : 6, 0, 0, 0));

        Label selfAvatar = new Label(currentUsername.substring(0, 1).toUpperCase());
        selfAvatar.setMinSize(28, 28); selfAvatar.setMaxSize(28, 28);
        selfAvatar.setAlignment(Pos.CENTER);
        selfAvatar.setBackground(AVATAR_BG_SMALL);
        selfAvatar.setStyle("-fx-text-fill:white; -fx-font-size:11px;");

        TextField field = new TextField();
        field.setPromptText(parentComment == null ? "Écrire un commentaire…" : "Écrire une réponse…");
        field.setStyle(
                "-fx-font-size:12px; -fx-font-family:'Segoe UI',Arial;" +
                        "-fx-background-color:#FFFFFF; -fx-background-radius:20;" +
                        "-fx-border-color:#BFDBFE; -fx-border-radius:20; -fx-border-width:1.5;" +
                        "-fx-padding:7 12 7 12;");
        HBox.setHgrow(field, Priority.ALWAYS);

        Button sendBtn = new Button("↵");
        sendBtn.setStyle("-fx-background-color:#1E40AF; -fx-background-radius:16; -fx-text-fill:white; -fx-font-size:13px; -fx-min-width:32; -fx-min-height:32; -fx-cursor:HAND;");

        Runnable submit = () -> {
            String txt = field.getText().trim();
            if (txt.isEmpty()) return;
            if (!canPostComment(spamLabel)) return;
            commentTimestamps.add(LocalDateTime.now());
            hideError(spamLabel);

            if (parentComment == null) {
                Commentaire c = new Commentaire(txt, post.getIdPost(), currentUserId);
                commentService.ajouterCommentaire(c);
                VBox item = buildCommentItem(c, post, spamLabel);
                item.setOpacity(0);
                int idx = Math.max(0, targetList.getChildren().size() - 1);
                targetList.getChildren().add(idx, item);
                fadeIn(item);
                // Notify the POST OWNER (not the commenter)
                if (!currentUserId.equals(post.getIdUser())) {
                    pushNotif(NotificationService.NotifType.COMMENT,
                            post.getIdUser(), post.getContenu(), txt);
                }
            } else {
                Commentaire reply = new Commentaire(txt, post.getIdPost(), currentUserId, parentComment.getIdCommentaire());
                commentService.ajouterCommentaire(reply);
                VBox bubbleNode = buildReplyBubble(reply, post, targetList, replyToggle, spamLabel);
                bubbleNode.setOpacity(0);
                int idx = Math.max(0, targetList.getChildren().size() - 1);
                targetList.getChildren().add(idx, bubbleNode);
                fadeIn(bubbleNode);
                if (replyToggle != null) {
                    int cnt = (int) targetList.getChildren().stream()
                            .filter(n -> !(n instanceof HBox)).count();
                    replyToggle.setText("↩ Masquer · " + cnt);
                }
                // Notify the COMMENT OWNER (not the replier)
                if (!currentUserId.equals(parentComment.getIdUser())) {
                    pushNotif(NotificationService.NotifType.REPLY_TO_COMMENT,
                            parentComment.getIdUser(), post.getContenu(), txt);
                }
            }
            field.clear();
        };

        sendBtn.setOnAction(e -> submit.run());
        field.setOnAction(e  -> submit.run());
        form.getChildren().addAll(selfAvatar, field, sendBtn);
        return form;
    }

    // ═══════════════════════════════════════════
    // Spam Protection
    // ═══════════════════════════════════════════

    private boolean canPostComment(Label spamLabel) {
        LocalDateTime now    = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(COMMENT_WINDOW_MINUTES);
        commentTimestamps.removeIf(t -> t.isBefore(cutoff));
        if (commentTimestamps.size() >= MAX_COMMENTS_IN_WINDOW) {
            LocalDateTime oldest    = commentTimestamps.get(0);
            LocalDateTime unblockAt = oldest.plusMinutes(COMMENT_WINDOW_MINUTES);
            long secondsLeft = java.time.Duration.between(now, unblockAt).toSeconds();
            long minsLeft    = secondsLeft / 60;
            long secsLeft    = secondsLeft % 60;
            String timeStr   = minsLeft > 0 ? minsLeft + " min " + secsLeft + " sec" : secsLeft + " sec";
            showError(spamLabel, "⚠ Trop de commentaires ! Attendez " + timeStr + " avant de commenter.");
            shakeNode(spamLabel);
            return false;
        }
        return true;
    }

    // ═══════════════════════════════════════════
    // FXML Handlers
    // ═══════════════════════════════════════════

    @FXML
    private void handlePublishPost() {
        String content = newPostContent.getText().trim();
        String media   = newPostMediaPath.getText().trim();
        if (content.isEmpty()) {
            showError(postErrorLabel, "⚠  Le contenu ne peut pas être vide.");
            shakeNode(newPostContent);
            return;
        }
        hideError(postErrorLabel);
        publishBtn.setDisable(true);
        publishBtn.setText("Publication…");

        String selected = visibilityCombo.getValue();
        String statut = (selected != null && selected.contains("Privé")) ? "private" : "public";

        Post newPost = new Post(content, media.isEmpty() ? null : media, statut, currentUserId);
        postService.ajouterPost(newPost);
        postsList.add(0, newPost);
        handleClearPost();
        rebuildFeed();
        mainScrollPane.setVvalue(0);
        publishBtn.setDisable(false);
        publishBtn.setText("Publier");
        runModerationAsync(newPost, false);
    }

    @FXML
    private void handleClearPost() {
        newPostContent.clear();
        newPostMediaPath.clear();
        visibilityCombo.getSelectionModel().selectFirst();
        hideError(postErrorLabel);
    }

    @FXML
    private void handleBrowseMedia() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Sélectionner une photo ou vidéo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Fichiers médias",
                "*.png","*.jpg","*.jpeg","*.gif","*.bmp","*.webp",
                "*.mp4","*.avi","*.mov","*.mkv","*.webm","*.flv"));
        File file = chooser.showOpenDialog(newPostContent.getScene().getWindow());
        if (file != null) newPostMediaPath.setText(file.getAbsolutePath());
    }

    @FXML private void handleRefresh() { loadPosts(); }

    @FXML
    private void handleShowArchive() {
        rebuildArchive();
        archiveView.setVisible(true); archiveView.setManaged(true);
        mainScrollPane.setVisible(false); mainScrollPane.setManaged(false);
    }

    @FXML
    private void handleShowFeed() {
        archiveView.setVisible(false); archiveView.setManaged(false);
        mainScrollPane.setVisible(true); mainScrollPane.setManaged(true);
    }

    @FXML
    private void handleToggleNotifications() {
        boolean showing = notifPanel.isVisible();
        notifPanel.setVisible(!showing);
        notifPanel.setManaged(!showing);
        if (!showing) {
            loadNotificationsFromDB(); // always reload from DB to get latest
            rebuildNotifList();
        }
    }

    @FXML
    private void handleMarkAllRead() {
        notificationService.markAllRead(currentUserId);
        notifications.forEach(n -> n.read = true);
        updateNotifBadge();
        rebuildNotifList();
    }

    @FXML
    private void handleNotifTabAll() {
        showUnreadOnly = false;
        notifTabAll.setStyle("-fx-background-color:transparent; -fx-text-fill:#1E40AF; -fx-font-size:12px; -fx-font-weight:bold; -fx-cursor:HAND; -fx-border-color:#1E40AF; -fx-border-width:0 0 2 0; -fx-border-radius:0; -fx-background-radius:0; -fx-padding:10 12 8 12;");
        notifTabUnread.setStyle("-fx-background-color:transparent; -fx-text-fill:#6B7280; -fx-font-size:12px; -fx-cursor:HAND; -fx-border-color:transparent; -fx-border-width:0 0 2 0; -fx-border-radius:0; -fx-background-radius:0; -fx-padding:10 12 8 12;");
        rebuildNotifList();
    }

    @FXML
    private void handleNotifTabUnread() {
        showUnreadOnly = true;
        notifTabUnread.setStyle("-fx-background-color:transparent; -fx-text-fill:#1E40AF; -fx-font-size:12px; -fx-font-weight:bold; -fx-cursor:HAND; -fx-border-color:#1E40AF; -fx-border-width:0 0 2 0; -fx-border-radius:0; -fx-background-radius:0; -fx-padding:10 12 8 12;");
        notifTabAll.setStyle("-fx-background-color:transparent; -fx-text-fill:#6B7280; -fx-font-size:12px; -fx-cursor:HAND; -fx-border-color:transparent; -fx-border-width:0 0 2 0; -fx-border-radius:0; -fx-background-radius:0; -fx-padding:10 12 8 12;");
        rebuildNotifList();
    }

    // ═══════════════════════════════════════════
    // Notification Helpers (DB-backed)
    // ═══════════════════════════════════════════

    /** Load the current user's notifications from DB. */
    private void loadNotificationsFromDB() {
        notifications = notificationService.getForUser(currentUserId);
        updateNotifBadge();
    }

    /**
     * Persist a notification for the RECIPIENT (not the actor).
     * recipientUserId = the owner of the post/comment being interacted with.
     * Only call this when recipientUserId != currentUserId.
     */
    private void pushNotif(NotificationService.NotifType type,
                           String recipientUserId,
                           String postPreview, String contentPreview) {
        notificationService.push(type, recipientUserId, currentUsername, postPreview, contentPreview);
        // If we are also the recipient (shouldn't happen due to guard), reload
        if (recipientUserId.equals(currentUserId)) {
            loadNotificationsFromDB();
            if (notifPanel.isVisible()) rebuildNotifList();
        }
        // Otherwise, the recipient will see it when they log in / refresh notifications.
    }

    private void updateNotifBadge() {
        if (notifBadge == null) return;
        long unread = notifications.stream().filter(n -> !n.read).count();
        if (unread > 0) {
            notifBadge.setText(unread > 9 ? "9+" : String.valueOf(unread));
            notifBadge.setVisible(true);
            notifBadge.setManaged(true);
        } else {
            notifBadge.setVisible(false);
            notifBadge.setManaged(false);
        }
    }

    private void rebuildNotifList() {
        notifListContainer.getChildren().clear();
        List<NotificationService.NotifRecord> list = showUnreadOnly
                ? notifications.stream().filter(n -> !n.read).toList()
                : notifications;

        if (list.isEmpty()) {
            Label empty = new Label(showUnreadOnly ? "Aucune notification non lue" : "Aucune notification");
            empty.setStyle("-fx-font-size:12px; -fx-text-fill:#9CA3AF; -fx-font-family:'Segoe UI',Arial; -fx-padding:28 0 28 0;");
            empty.setMaxWidth(Double.MAX_VALUE);
            empty.setAlignment(Pos.CENTER);
            notifListContainer.getChildren().add(empty);
            return;
        }

        for (NotificationService.NotifRecord notif : list) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.TOP_LEFT);
            row.setPadding(new Insets(12, 16, 12, 16));
            row.setStyle(notif.read
                    ? "-fx-background-color:#FFFFFF; -fx-cursor:HAND;"
                    : "-fx-background-color:#EFF6FF; -fx-cursor:HAND;");
            row.setOnMouseEntered(e  -> row.setStyle(row.getStyle().replace("#FFFFFF", "#F9FAFB").replace("#EFF6FF", "#DBEAFE")));
            row.setOnMouseExited(e   -> row.setStyle(notif.read ? "-fx-background-color:#FFFFFF; -fx-cursor:HAND;" : "-fx-background-color:#EFF6FF; -fx-cursor:HAND;"));
            row.setOnMouseClicked(e  -> {
                notif.read = true;
                notificationService.markRead(notif.id);
                updateNotifBadge();
                rebuildNotifList();
            });

            String icon = switch (notif.type) {
                case COMMENT           -> "💬";
                case REPLY_TO_COMMENT  -> "↩";
                case REACTION_POST     -> "♥";
                case REACTION_COMMENT  -> "♥";
            };
            Label iconLabel = new Label(icon);
            iconLabel.setMinSize(36, 36); iconLabel.setMaxSize(36, 36);
            iconLabel.setAlignment(Pos.CENTER);
            iconLabel.setStyle("-fx-background-color:#DBEAFE; -fx-background-radius:18; -fx-font-size:15px;");

            VBox textCol = new VBox(3);
            HBox.setHgrow(textCol, Priority.ALWAYS);

            String verb = switch (notif.type) {
                case COMMENT           -> " a commenté votre publication";
                case REPLY_TO_COMMENT  -> " a répondu à votre commentaire";
                case REACTION_POST     -> " a réagi à votre publication";
                case REACTION_COMMENT  -> " a réagi à votre commentaire";
            };
            Label msgLabel = new Label(notif.actorUsername + verb);
            msgLabel.setWrapText(true);
            msgLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#111827; -fx-font-family:'Segoe UI',Arial;");

            if (notif.postPreview != null && !notif.postPreview.isBlank()) {
                String preview = notif.postPreview.length() > 50
                        ? notif.postPreview.substring(0, 50) + "…"
                        : notif.postPreview;
                Label postLabel = new Label("📄 " + preview);
                postLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#6B7280; -fx-font-family:'Segoe UI',Arial;");
                postLabel.setWrapText(true);
                textCol.getChildren().add(postLabel);
            }

            if (notif.contentPreview != null && !notif.contentPreview.isBlank()) {
                String preview = notif.contentPreview.length() > 60
                        ? notif.contentPreview.substring(0, 60) + "…"
                        : notif.contentPreview;
                Label commentLabel = new Label("« " + preview + " »");
                commentLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#374151; -fx-font-style:italic; -fx-font-family:'Segoe UI',Arial;");
                commentLabel.setWrapText(true);
                textCol.getChildren().add(commentLabel);
            }

            textCol.getChildren().add(0, msgLabel);

            VBox rightCol = new VBox();
            rightCol.setAlignment(Pos.TOP_RIGHT);
            Label dateLabel = new Label(formatNotifDate(notif.date));
            dateLabel.setStyle("-fx-font-size:10px; -fx-text-fill:#9CA3AF; -fx-font-family:'Segoe UI',Arial;");

            if (!notif.read) {
                Label dot = new Label("●");
                dot.setStyle("-fx-text-fill:#1E40AF; -fx-font-size:8px; -fx-padding:4 0 0 0;");
                rightCol.getChildren().addAll(dateLabel, dot);
            } else {
                rightCol.getChildren().add(dateLabel);
            }

            row.getChildren().addAll(iconLabel, textCol, rightCol);
            Separator sep = new Separator();
            sep.setStyle("-fx-padding:0;");
            notifListContainer.getChildren().addAll(row, sep);
        }
    }

    private String formatNotifDate(LocalDateTime date) {
        long minutes = java.time.Duration.between(date, LocalDateTime.now()).toMinutes();
        if (minutes < 1)  return "À l'instant";
        if (minutes < 60) return minutes + " min";
        long hours = minutes / 60;
        if (hours < 24)   return hours + " h";
        return date.format(DateTimeFormatter.ofPattern("dd MMM"));
    }

    // ═══════════════════════════════════════════
    // Card Handlers
    // ═══════════════════════════════════════════

    private void handleReact(Post post, Button btn) {
        Reaction r = new Reaction("LIKE", post.getIdPost(), currentUserId);
        boolean added = reactionService.toggleReaction(r);
        int count = reactionService.getReactionsByPost(post.getIdPost()).size();
        if (added) {
            btn.setText("♥  " + count);
            btn.setStyle("-fx-background-color:#EF4444; -fx-background-radius:8; -fx-text-fill:white; -fx-font-size:12px; -fx-padding:6 12 6 12; -fx-cursor:HAND;");
            // Notify the POST OWNER when someone reacts (not when un-reacting)
            if (!currentUserId.equals(post.getIdUser())) {
                pushNotif(NotificationService.NotifType.REACTION_POST,
                        post.getIdUser(), post.getContenu(), null);
            }
        } else {
            btn.setText("♥  " + count);
            btn.setStyle("-fx-background-color:transparent; -fx-background-radius:8; -fx-text-fill:#EF4444; -fx-font-size:12px; -fx-padding:6 12 6 12; -fx-cursor:HAND;");
        }
    }

    private void handleCommentReact(Commentaire comment, Button btn) {
        Reaction r = new Reaction("LIKE", null, comment.getIdCommentaire(), currentUserId);
        boolean added = reactionService.toggleReaction(r);
        int count = reactionService.getReactionsByCommentaire(comment.getIdCommentaire()).size();
        if (added) {
            btn.setText("♥ " + count);
            btn.setStyle("-fx-background-color:transparent; -fx-text-fill:#EF4444; -fx-font-size:11px; -fx-font-weight:bold; -fx-font-family:'Segoe UI',Arial; -fx-padding:2 6 2 0; -fx-cursor:HAND; -fx-border-color:transparent;");
            // Notify the COMMENT OWNER when someone reacts
            if (!currentUserId.equals(comment.getIdUser())) {
                pushNotif(NotificationService.NotifType.REACTION_COMMENT,
                        comment.getIdUser(), null, comment.getContenu());
            }
        } else {
            btn.setText("♡ " + count);
            btn.setStyle("-fx-background-color:transparent; -fx-text-fill:#6B7280; -fx-font-size:11px; -fx-font-weight:bold; -fx-font-family:'Segoe UI',Arial; -fx-padding:2 6 2 0; -fx-cursor:HAND; -fx-border-color:transparent;");
        }
    }

    private void handleReport(Post post) {
        int count = reportCounts.getOrDefault(post.getIdPost(), 0) + 1;
        reportCounts.put(post.getIdPost(), count);
        if (count > MAX_REPORTS) {
            postService.supprimerPost(post.getIdPost());
            postsList.removeIf(p -> p.getIdPost() == post.getIdPost());
            savedPostIds.remove(post.getIdPost());
            saveService.unsavePost(post.getIdPost());
            rebuildFeed();
            showToast("Publication retirée après " + MAX_REPORTS + "+ signalements.");
        } else {
            showToast("Signalé (" + count + "/" + MAX_REPORTS + ")");
            rebuildFeed();
        }
    }

    private void handleEditPost(Post post, VBox cardInner, Label contentLabel) {
        int idx = cardInner.getChildren().indexOf(contentLabel);
        if (idx < 0) return;
        translatedPostIds.remove(post.getIdPost());
        translationCache.remove(post.getIdPost());

        TextArea area = new TextArea(post.getContenu());
        area.setWrapText(true); area.setPrefRowCount(3);
        area.setStyle("-fx-font-size:13px; -fx-background-color:#F9FAFB; -fx-background-radius:8; -fx-border-color:#3B82F6; -fx-border-radius:8; -fx-border-width:1.5; -fx-padding:9;");

        Button saveBtn = new Button("✓  Enregistrer");
        saveBtn.setStyle("-fx-background-color:#22C55E; -fx-background-radius:8; -fx-text-fill:white; -fx-font-size:12px; -fx-padding:6 14 6 14; -fx-cursor:HAND;");

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#9CA3AF; -fx-font-size:12px; -fx-padding:6 12 6 12; -fx-cursor:HAND;");

        HBox editActions = new HBox(8, cancelBtn, saveBtn);
        editActions.setAlignment(Pos.CENTER_RIGHT);
        VBox editForm = new VBox(8, area, editActions);
        cardInner.getChildren().set(idx, editForm);

        saveBtn.setOnAction(e -> {
            String updated = area.getText().trim();
            if (!updated.isEmpty()) {
                post.setContenu(updated);
                postService.modifierPost(post);
                contentLabel.setText(updated);
                aiHiddenPostIds.remove(post.getIdPost());
                runModerationAsync(post, true);
            }
            cardInner.getChildren().set(idx, contentLabel);
        });
        cancelBtn.setOnAction(e -> cardInner.getChildren().set(idx, contentLabel));
    }

    private void handleDeletePost(Post post) {
        if (!showStyledDeleteDialog()) return;
        postService.supprimerPost(post.getIdPost());
        postsList.removeIf(p -> p.getIdPost() == post.getIdPost());
        savedPostIds.remove(post.getIdPost());
        aiHiddenPostIds.remove(post.getIdPost());
        translationCache.remove(post.getIdPost());
        translatedPostIds.remove(post.getIdPost());
        rebuildFeed();
    }

    // ═══════════════════════════════════════════
    // Styled Dialogs
    // ═══════════════════════════════════════════

    private void showStyledModerationDialog(ModerationResult result) {
        VBox dialogContent = new VBox(16);
        dialogContent.setPadding(new Insets(0, 0, 8, 0));
        dialogContent.setStyle("-fx-background-color:#FFFFFF;");

        HBox titleRow = new HBox(12);
        titleRow.setStyle(
                "-fx-background-color:linear-gradient(135deg,#FEF3C7,#FDE68A);" +
                        "-fx-background-radius:12 12 0 0; -fx-padding:18 20 18 20; -fx-alignment:CENTER_LEFT;");
        Label icon = new Label("🤖");
        icon.setStyle("-fx-font-size:28px;");
        VBox titleCol = new VBox(3);
        Label titleL = new Label("Publication masquée");
        titleL.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#92400E; -fx-font-family:'Segoe UI',Arial;");
        Label subL = new Label("Modération automatique par IA");
        subL.setStyle("-fx-font-size:11px; -fx-text-fill:#B45309; -fx-font-family:'Segoe UI',Arial;");
        titleCol.getChildren().addAll(titleL, subL);
        titleRow.getChildren().addAll(icon, titleCol);

        VBox body = new VBox(10);
        body.setPadding(new Insets(16, 20, 0, 20));

        HBox scoreRow = new HBox(10);
        scoreRow.setStyle("-fx-alignment:CENTER_LEFT;");
        Label scoreLabel = new Label("Score de risque :");
        scoreLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#6B7280; -fx-font-family:'Segoe UI',Arial;");
        StackPane barBg = new StackPane();
        barBg.setMinSize(160, 10); barBg.setMaxHeight(10);
        barBg.setStyle("-fx-background-color:#F3F4F6; -fx-background-radius:5;");
        Region barFill = new Region();
        barFill.setPrefWidth(160 * result.score); barFill.setPrefHeight(10);
        String barColor = result.score >= 0.85 ? "#EF4444" : result.score >= 0.7 ? "#F59E0B" : "#10B981";
        barFill.setStyle("-fx-background-color:" + barColor + "; -fx-background-radius:5;");
        StackPane.setAlignment(barFill, Pos.CENTER_LEFT);
        barBg.getChildren().add(barFill);
        Label pctLabel = new Label(String.format("%.0f%%", result.score * 100));
        pctLabel.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:" + barColor + "; -fx-font-family:'Segoe UI',Arial;");
        scoreRow.getChildren().addAll(scoreLabel, barBg, pctLabel);

        VBox reasonBox = new VBox(4);
        reasonBox.setStyle("-fx-background-color:#FEF3C7; -fx-background-radius:8; -fx-padding:10 14 10 14;");
        Label reasonTitle = new Label("Raison détectée");
        reasonTitle.setStyle("-fx-font-size:10px; -fx-font-weight:bold; -fx-text-fill:#92400E; -fx-font-family:'Segoe UI',Arial;");
        Label reasonText = new Label(result.reason);
        reasonText.setWrapText(true);
        reasonText.setStyle("-fx-font-size:12px; -fx-text-fill:#78350F; -fx-font-family:'Segoe UI',Arial;");
        reasonBox.getChildren().addAll(reasonTitle, reasonText);

        Label infoL = new Label("Cette publication est désormais visible uniquement par les administrateurs.");
        infoL.setWrapText(true);
        infoL.setStyle("-fx-font-size:11px; -fx-text-fill:#9CA3AF; -fx-font-family:'Segoe UI',Arial;");

        body.getChildren().addAll(scoreRow, reasonBox, infoL);
        dialogContent.getChildren().addAll(titleRow, body);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("");
        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().setHeader(null);
        dialog.getDialogPane().setGraphic(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF; -fx-background-radius:12; -fx-padding:0;");
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color:#1E40AF; -fx-background-radius:8; -fx-text-fill:white; -fx-font-size:12px; -fx-font-family:'Segoe UI',Arial; -fx-padding:7 22 7 22; -fx-cursor:HAND;");
        okBtn.setText("Compris");
        dialog.showAndWait();
    }

    private boolean showStyledDeleteDialog() {
        VBox dialogContent = new VBox(0);
        dialogContent.setStyle("-fx-background-color:#FFFFFF;");

        HBox header = new HBox(12);
        header.setStyle(
                "-fx-background-color:linear-gradient(135deg,#FEE2E2,#FECACA);" +
                        "-fx-background-radius:12 12 0 0; -fx-padding:18 20 18 20; -fx-alignment:CENTER_LEFT;");
        Label icon = new Label("🗑");
        icon.setStyle("-fx-font-size:26px;");
        VBox titleCol = new VBox(3);
        Label titleL = new Label("Supprimer la publication");
        titleL.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#991B1B; -fx-font-family:'Segoe UI',Arial;");
        Label subL = new Label("Cette action est irréversible");
        subL.setStyle("-fx-font-size:11px; -fx-text-fill:#DC2626; -fx-font-family:'Segoe UI',Arial;");
        titleCol.getChildren().addAll(titleL, subL);
        header.getChildren().addAll(icon, titleCol);

        VBox body = new VBox(10);
        body.setPadding(new Insets(18, 20, 10, 20));
        Label msg = new Label("Êtes-vous sûr de vouloir supprimer cette publication ? Tous les commentaires et réactions associés seront également supprimés.");
        msg.setWrapText(true);
        msg.setStyle("-fx-font-size:13px; -fx-text-fill:#374151; -fx-font-family:'Segoe UI',Arial;");
        body.getChildren().add(msg);

        dialogContent.getChildren().addAll(header, body);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("");
        dialog.getDialogPane().setContent(dialogContent);
        dialog.getDialogPane().setHeader(null);
        dialog.getDialogPane().setGraphic(null);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF; -fx-background-radius:12; -fx-padding:0;");
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color:#EF4444; -fx-background-radius:8; -fx-text-fill:white; -fx-font-size:12px; -fx-font-family:'Segoe UI',Arial; -fx-padding:7 22 7 22; -fx-cursor:HAND;");
        okBtn.setText("Supprimer");
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#6B7280; -fx-font-size:12px; -fx-font-family:'Segoe UI',Arial; -fx-padding:7 16 7 16; -fx-cursor:HAND;");
        cancelBtn.setText("Annuler");
        var result = dialog.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // ═══════════════════════════════════════════
    // Toast
    // ═══════════════════════════════════════════

    private void showToast(String message) {
        try {
            Label toast = new Label(message);
            toast.setStyle(
                    "-fx-background-color:rgba(17,24,39,0.85);" +
                            "-fx-background-radius:20; -fx-text-fill:white;" +
                            "-fx-font-size:12px; -fx-font-family:'Segoe UI',Arial;" +
                            "-fx-padding:8 20 8 20;");
            toast.setMouseTransparent(true);
            Pane root = (Pane) postsFeedContainer.getScene().getRoot();
            toast.setLayoutX(root.getWidth() / 2 - 120);
            toast.setLayoutY(root.getHeight() - 80);
            root.getChildren().add(toast);
            FadeTransition ft = new FadeTransition(Duration.millis(1600), toast);
            ft.setFromValue(1); ft.setToValue(0);
            ft.setDelay(Duration.millis(1200));
            ft.setOnFinished(e -> root.getChildren().remove(toast));
            ft.play();
        } catch (Exception ignored) {}
    }

    // ═══════════════════════════════════════════
    // UI Helpers
    // ═══════════════════════════════════════════

    private Button ghostButton(String text, String color) {
        Button b = new Button(text);
        String base = "-fx-background-color:transparent; -fx-background-radius:8; -fx-text-fill:" + color + "; -fx-font-size:12px; -fx-font-family:'Segoe UI',Arial; -fx-padding:6 12 6 12; -fx-cursor:HAND;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(base.replace("-fx-background-color:transparent;", "-fx-background-color:#EFF6FF;")));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private Button filledButton(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:" + color + "; -fx-background-radius:8; -fx-text-fill:white; -fx-font-size:12px; -fx-font-family:'Segoe UI',Arial; -fx-padding:6 12 6 12; -fx-cursor:HAND;");
        return b;
    }

    private Button outlineButton(String text, String color) {
        Button b = new Button(text);
        String base = "-fx-background-color:transparent; -fx-background-radius:8; -fx-border-color:#E5E7EB; -fx-border-width:1; -fx-border-radius:8; -fx-text-fill:" + color + "; -fx-font-size:12px; -fx-font-family:'Segoe UI',Arial; -fx-padding:6 12 6 12; -fx-cursor:HAND;";
        b.setStyle(base);
        b.setOnMouseEntered(e -> b.setStyle(base.replace("-fx-background-color:transparent;", "-fx-background-color:#F9FAFB;")));
        b.setOnMouseExited(e  -> b.setStyle(base));
        return b;
    }

    private Button linkButton(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:transparent; -fx-text-fill:" + color + "; -fx-font-size:11px; -fx-font-weight:bold; -fx-font-family:'Segoe UI',Arial; -fx-padding:2 6 2 0; -fx-cursor:HAND; -fx-border-color:transparent;");
        return b;
    }

    private Label statLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:11px; -fx-text-fill:#9CA3AF; -fx-font-family:'Segoe UI',Arial;");
        return l;
    }

    private void showError(Label label, String msg) {
        label.setText(msg); label.setVisible(true); label.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(200), label);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void hideError(Label label) { label.setVisible(false); label.setManaged(false); }

    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(55), node);
        tt.setByX(7); tt.setCycleCount(4); tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0)); tt.play();
    }

    private void fadeIn(Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(220), node);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }
}
