package com.example.pi_dev.Controllers.Blog;

import com.example.pi_dev.Entities.Blog.Commentaire;
import com.example.pi_dev.Entities.Blog.Post;
import com.example.pi_dev.Entities.Blog.Reaction;
import com.example.pi_dev.Services.Blog.Commentaire_Services;
import com.example.pi_dev.Services.Blog.Posting_Services;
import com.example.pi_dev.Services.Blog.Reaction_Services;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.util.Duration;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminDashboardBlogController implements Initializable {

    // ── Nav Stats ──────────────────────────────────────────────────
    @FXML private Label statPostsLabel;
    @FXML private Label statCommentsLabel;
    @FXML private Label statReactionsLabel;

    // ── Sidebar tabs ───────────────────────────────────────────────
    @FXML private Button tabPostsBtn;
    @FXML private Button tabCommentsBtn;
    @FXML private Button tabReactionsBtn;

    // ── Tab panes ──────────────────────────────────────────────────
    @FXML private VBox postsTab;
    @FXML private VBox commentsTab;
    @FXML private VBox reactionsTab;

    // ── Posts ──────────────────────────────────────────────────────
    @FXML private Label    postsCountBadge;
    @FXML private TextField postSearchField;
    @FXML private VBox     postsTableContainer;

    // ── Comments ───────────────────────────────────────────────────
    @FXML private Label    commentsCountBadge;
    @FXML private TextField commentSearchField;
    @FXML private VBox     commentsTableContainer;

    // ── Reactions ──────────────────────────────────────────────────
    @FXML private Label    reactionsCountBadge;
    @FXML private TextField reactionSearchField;
    @FXML private VBox     reactionsTableContainer;

    // ── Toast ──────────────────────────────────────────────────────
    @FXML private StackPane toastPane;

    // ── Services ───────────────────────────────────────────────────
    private final Posting_Services     postService     = new Posting_Services();
    private final Commentaire_Services commentService  = new Commentaire_Services();
    private final Reaction_Services    reactionService = new Reaction_Services();

    // ── Data caches ────────────────────────────────────────────────
    private List<Post>        allPosts;
    private List<Commentaire> allComments;
    private List<Reaction>    allReactions;

    // ── Validation constants ───────────────────────────────────────
    private static final int POST_MIN_LENGTH     = 5;
    private static final int POST_MAX_LENGTH     = 2000;
    private static final int COMMENT_MIN_LENGTH  = 2;
    private static final int COMMENT_MAX_LENGTH  = 500;
    private static final String UUID_REGEX =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private static final Background AVATAR_BG = new Background(new BackgroundFill(
            new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#1E40AF")), new Stop(1, Color.web("#3B82F6"))),
            new CornerRadii(20), Insets.EMPTY));

    // ══════════════════════════════════════════════════════════════
    // Init
    // ══════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadAll();
    }

    private void loadAll() {
        allPosts     = postService.afficherPosts();
        allComments  = commentService.afficherCommentaires();
        allReactions = reactionService.afficherReactions();

        statPostsLabel.setText(String.valueOf(allPosts.size()));
        statCommentsLabel.setText(String.valueOf(allComments.size()));
        statReactionsLabel.setText(String.valueOf(allReactions.size()));

        renderPostsTable(allPosts);
        renderCommentsTable(allComments);
        renderReactionsTable(allReactions);
    }

    // ══════════════════════════════════════════════════════════════
    // Tab switching
    // ══════════════════════════════════════════════════════════════

    private static final String TAB_ACTIVE =
            "-fx-background-color:#2D59C7; -fx-background-radius:0;" +
                    "-fx-text-fill:#FFFFFF; -fx-font-size:13px;" +
                    "-fx-font-family:'Segoe UI',Arial; -fx-alignment:CENTER_LEFT;" +
                    "-fx-padding:12 20 12 20; -fx-cursor:HAND;" +
                    "-fx-border-color:#60A5FA; -fx-border-width:0 0 0 3; -fx-border-radius:0;";

    private static final String TAB_INACTIVE =
            "-fx-background-color:transparent; -fx-background-radius:0;" +
                    "-fx-text-fill:#BFDBFE; -fx-font-size:13px;" +
                    "-fx-font-family:'Segoe UI',Arial; -fx-alignment:CENTER_LEFT;" +
                    "-fx-padding:12 20 12 20; -fx-cursor:HAND;" +
                    "-fx-border-color:transparent; -fx-border-width:0 0 0 3; -fx-border-radius:0;";

    @FXML private void handleTabPosts() {
        showTab(postsTab, tabPostsBtn);
        hideTab(commentsTab, tabCommentsBtn);
        hideTab(reactionsTab, tabReactionsBtn);
    }

    @FXML private void handleTabComments() {
        hideTab(postsTab, tabPostsBtn);
        showTab(commentsTab, tabCommentsBtn);
        hideTab(reactionsTab, tabReactionsBtn);
    }

    @FXML private void handleTabReactions() {
        hideTab(postsTab, tabPostsBtn);
        hideTab(commentsTab, tabCommentsBtn);
        showTab(reactionsTab, tabReactionsBtn);
    }

    private void showTab(VBox tab, Button btn) {
        tab.setVisible(true);
        tab.setManaged(true);
        btn.setStyle(TAB_ACTIVE);
    }

    private void hideTab(VBox tab, Button btn) {
        tab.setVisible(false);
        tab.setManaged(false);
        btn.setStyle(TAB_INACTIVE);
    }

    @FXML private void handleRefreshAll() {
        loadAll();
        showToast("✓ Données actualisées");
    }

    // ══════════════════════════════════════════════════════════════
    // POSTS TABLE
    // ══════════════════════════════════════════════════════════════

    private void renderPostsTable(List<Post> posts) {
        postsTableContainer.getChildren().clear();
        postsCountBadge.setText(String.valueOf(posts.size()));

        if (posts.isEmpty()) {
            postsTableContainer.getChildren().add(emptyRow("Aucune publication trouvée"));
            return;
        }

        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            HBox row = buildPostRow(post, i);
            postsTableContainer.getChildren().add(row);
            if (i < posts.size() - 1) {
                Separator sep = new Separator();
                sep.setStyle("-fx-padding:0; -fx-background-color:#F3F4F6;");
                postsTableContainer.getChildren().add(sep);
            }
        }
    }

    private HBox buildPostRow(Post post, int index) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        String baseStyle = index % 2 == 0
                ? "-fx-background-color:#FFFFFF;"
                : "-fx-background-color:#FAFBFF;";
        row.setStyle(baseStyle);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#EFF6FF;"));
        row.setOnMouseExited(e  -> row.setStyle(baseStyle));

        Label idL = new Label("#" + post.getIdPost());
        idL.setMinWidth(50);
        idL.setStyle("-fx-font-size:11px; -fx-text-fill:#9CA3AF; -fx-font-family:'Segoe UI',Arial;");

        String preview = post.getContenu() != null && post.getContenu().length() > 60
                ? post.getContenu().substring(0, 60) + "…"
                : (post.getContenu() != null ? post.getContenu() : "—");
        Label contentL = new Label(preview);
        contentL.setStyle("-fx-font-size:12px; -fx-text-fill:#374151; -fx-font-family:'Segoe UI',Arial;");
        HBox.setHgrow(contentL, Priority.ALWAYS);

        String uid = post.getIdUser() != null
                ? post.getIdUser().substring(0, Math.min(8, post.getIdUser().length())) + "…"
                : "—";
        Label userL = new Label(uid);
        userL.setMinWidth(130);
        userL.setStyle("-fx-font-size:11px; -fx-text-fill:#6B7280; -fx-font-family:'Segoe UI',Arial;");

        boolean isPrivate = post.getStatut() != null && post.getStatut().toLowerCase().contains("private");
        boolean isHidden  = post.getStatut() != null && post.getStatut().toLowerCase().equals("hidden");
        String badgeColor = isHidden ? "#FEE2E2" : isPrivate ? "#FEF3C7" : "#DCFCE7";
        String textColor  = isHidden ? "#991B1B" : isPrivate ? "#92400E" : "#166534";
        String statusText = isHidden ? "Masqué" : isPrivate ? "Privé" : "Public";
        Label statusL = new Label(statusText);
        statusL.setMinWidth(80);
        statusL.setStyle("-fx-background-color:" + badgeColor + "; -fx-background-radius:12;" +
                "-fx-text-fill:" + textColor + "; -fx-font-size:10px;" +
                "-fx-font-weight:bold; -fx-padding:3 8 3 8;");

        Label dateL = new Label(post.getDateCreation() != null ? post.getDateCreation().format(DATE_FMT) : "—");
        dateL.setMinWidth(130);
        dateL.setStyle("-fx-font-size:11px; -fx-text-fill:#9CA3AF; -fx-font-family:'Segoe UI',Arial;");

        HBox actions = new HBox(6);
        actions.setMinWidth(120);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = actionBtn("✏", "#3B82F6");
        editBtn.setOnAction(e -> openEditPostDialog(post));

        Button toggleBtn = actionBtn(isPrivate ? "🌐" : "🔒", "#6B7280");
        toggleBtn.setTooltip(new Tooltip(isPrivate ? "Rendre public" : "Rendre privé"));
        toggleBtn.setOnAction(e -> {
            post.setStatut(isPrivate ? "public" : "private");
            postService.modifierPost(post);
            loadAll();
            showToast("Statut mis à jour");
        });

        Button deleteBtn = actionBtn("🗑", "#EF4444");
        deleteBtn.setOnAction(e -> confirmDelete("cette publication", () -> {
            postService.supprimerPost(post.getIdPost());
            loadAll();
            showToast("Publication supprimée");
        }));

        actions.getChildren().addAll(editBtn, toggleBtn, deleteBtn);
        row.getChildren().addAll(idL, contentL, userL, statusL, dateL, actions);
        return row;
    }

    // ══════════════════════════════════════════════════════════════
    // COMMENTS TABLE
    // ══════════════════════════════════════════════════════════════

    private void renderCommentsTable(List<Commentaire> comments) {
        commentsTableContainer.getChildren().clear();
        commentsCountBadge.setText(String.valueOf(comments.size()));

        if (comments.isEmpty()) {
            commentsTableContainer.getChildren().add(emptyRow("Aucun commentaire trouvé"));
            return;
        }

        for (int i = 0; i < comments.size(); i++) {
            Commentaire c = comments.get(i);
            HBox row = buildCommentRow(c, i);
            commentsTableContainer.getChildren().add(row);
            if (i < comments.size() - 1) {
                Separator sep = new Separator();
                sep.setStyle("-fx-padding:0; -fx-background-color:#F3F4F6;");
                commentsTableContainer.getChildren().add(sep);
            }
        }
    }

    private HBox buildCommentRow(Commentaire c, int index) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        String baseStyle = index % 2 == 0
                ? "-fx-background-color:#FFFFFF;"
                : "-fx-background-color:#FAFBFF;";
        row.setStyle(baseStyle);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#EFF6FF;"));
        row.setOnMouseExited(e  -> row.setStyle(baseStyle));

        Label idL = new Label("#" + c.getIdCommentaire());
        idL.setMinWidth(50);
        idL.setStyle("-fx-font-size:11px; -fx-text-fill:#9CA3AF;");

        String preview = c.getContenu() != null && c.getContenu().length() > 55
                ? c.getContenu().substring(0, 55) + "…"
                : (c.getContenu() != null ? c.getContenu() : "—");
        Label contentL = new Label(preview);
        contentL.setStyle("-fx-font-size:12px; -fx-text-fill:#374151; -fx-font-family:'Segoe UI',Arial;");
        HBox.setHgrow(contentL, Priority.ALWAYS);

        String uid = c.getIdUser() != null
                ? c.getIdUser().substring(0, Math.min(8, c.getIdUser().length())) + "…"
                : "—";
        Label userL = new Label(uid);
        userL.setMinWidth(130);
        userL.setStyle("-fx-font-size:11px; -fx-text-fill:#6B7280;");

        Label postIdL = new Label(String.valueOf(c.getIdPost()));
        postIdL.setMinWidth(70);
        postIdL.setStyle("-fx-font-size:11px; -fx-text-fill:#6B7280;");

        Label replyL;
        if (c.getIdParent() != null) {
            replyL = new Label("↩ #" + c.getIdParent());
            replyL.setStyle("-fx-background-color:#EFF6FF; -fx-background-radius:8;" +
                    "-fx-text-fill:#1E40AF; -fx-font-size:10px; -fx-padding:2 6 2 6;");
        } else {
            replyL = new Label("—");
            replyL.setStyle("-fx-font-size:11px; -fx-text-fill:#D1D5DB;");
        }
        replyL.setMinWidth(70);

        Label dateL = new Label(c.getDate() != null ? c.getDate().format(DATE_FMT) : "—");
        dateL.setMinWidth(130);
        dateL.setStyle("-fx-font-size:11px; -fx-text-fill:#9CA3AF;");

        HBox actions = new HBox(6);
        actions.setMinWidth(100);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button editBtn = actionBtn("✏", "#3B82F6");
        editBtn.setOnAction(e -> openEditCommentDialog(c));

        Button deleteBtn = actionBtn("🗑", "#EF4444");
        deleteBtn.setOnAction(e -> confirmDelete("ce commentaire", () -> {
            commentService.supprimerCommentaire(c.getIdCommentaire());
            loadAll();
            showToast("Commentaire supprimé");
        }));

        actions.getChildren().addAll(editBtn, deleteBtn);
        row.getChildren().addAll(idL, contentL, userL, postIdL, replyL, dateL, actions);
        return row;
    }

    // ══════════════════════════════════════════════════════════════
    // REACTIONS TABLE
    // ══════════════════════════════════════════════════════════════

    private void renderReactionsTable(List<Reaction> reactions) {
        reactionsTableContainer.getChildren().clear();
        reactionsCountBadge.setText(String.valueOf(reactions.size()));

        if (reactions.isEmpty()) {
            reactionsTableContainer.getChildren().add(emptyRow("Aucune réaction trouvée"));
            return;
        }

        for (int i = 0; i < reactions.size(); i++) {
            Reaction r = reactions.get(i);
            HBox row = buildReactionRow(r, i);
            reactionsTableContainer.getChildren().add(row);
            if (i < reactions.size() - 1) {
                Separator sep = new Separator();
                sep.setStyle("-fx-padding:0; -fx-background-color:#F3F4F6;");
                reactionsTableContainer.getChildren().add(sep);
            }
        }
    }

    private HBox buildReactionRow(Reaction r, int index) {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 16, 12, 16));
        String baseStyle = index % 2 == 0
                ? "-fx-background-color:#FFFFFF;"
                : "-fx-background-color:#FAFBFF;";
        row.setStyle(baseStyle);
        row.setOnMouseEntered(e -> row.setStyle("-fx-background-color:#EFF6FF;"));
        row.setOnMouseExited(e  -> row.setStyle(baseStyle));

        Label idL = new Label("#" + r.getIdReaction());
        idL.setMinWidth(60);
        idL.setStyle("-fx-font-size:11px; -fx-text-fill:#9CA3AF;");

        String typeEmoji = "LIKE".equalsIgnoreCase(r.getType()) ? "♥ LIKE" : r.getType();
        Label typeL = new Label(typeEmoji);
        typeL.setMinWidth(90);
        typeL.setStyle("-fx-background-color:#FEE2E2; -fx-background-radius:8;" +
                "-fx-text-fill:#991B1B; -fx-font-size:11px; -fx-font-weight:bold;" +
                "-fx-padding:3 8 3 8;");

        String uid = r.getIdUser() != null
                ? r.getIdUser().substring(0, Math.min(12, r.getIdUser().length())) + "…"
                : "—";
        Label userL = new Label(uid);
        userL.setStyle("-fx-font-size:11px; -fx-text-fill:#6B7280;");
        HBox.setHgrow(userL, Priority.ALWAYS);

        Label postIdL = new Label(r.getIdPost() != null ? String.valueOf(r.getIdPost()) : "—");
        postIdL.setMinWidth(80);
        postIdL.setStyle("-fx-font-size:11px; -fx-text-fill:#6B7280;");

        Label commentIdL = new Label(r.getIdCommentaire() != null ? String.valueOf(r.getIdCommentaire()) : "—");
        commentIdL.setMinWidth(120);
        commentIdL.setStyle("-fx-font-size:11px; -fx-text-fill:#6B7280;");

        Label dateL = new Label(r.getDate() != null ? r.getDate().format(DATE_FMT) : "—");
        dateL.setMinWidth(130);
        dateL.setStyle("-fx-font-size:11px; -fx-text-fill:#9CA3AF;");

        HBox actions = new HBox(6);
        actions.setMinWidth(80);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button deleteBtn = actionBtn("🗑", "#EF4444");
        deleteBtn.setOnAction(e -> confirmDelete("cette réaction", () -> {
            reactionService.supprimerReaction(r.getIdReaction());
            loadAll();
            showToast("Réaction supprimée");
        }));

        actions.getChildren().add(deleteBtn);
        row.getChildren().addAll(idL, typeL, userL, postIdL, commentIdL, dateL, actions);
        return row;
    }

    // ══════════════════════════════════════════════════════════════
    // SEARCH
    // ══════════════════════════════════════════════════════════════

    @FXML private void handlePostSearch() {
        String q = postSearchField.getText().trim().toLowerCase();
        List<Post> filtered = q.isEmpty() ? allPosts : allPosts.stream()
                .filter(p -> (p.getContenu() != null && p.getContenu().toLowerCase().contains(q))
                        || (p.getIdUser() != null && p.getIdUser().toLowerCase().contains(q))
                        || (p.getStatut() != null && p.getStatut().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        renderPostsTable(filtered);
    }

    @FXML private void handleCommentSearch() {
        String q = commentSearchField.getText().trim().toLowerCase();
        List<Commentaire> filtered = q.isEmpty() ? allComments : allComments.stream()
                .filter(c -> (c.getContenu() != null && c.getContenu().toLowerCase().contains(q))
                        || (c.getIdUser() != null && c.getIdUser().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        renderCommentsTable(filtered);
    }

    @FXML private void handleReactionSearch() {
        String q = reactionSearchField.getText().trim().toLowerCase();
        List<Reaction> filtered = q.isEmpty() ? allReactions : allReactions.stream()
                .filter(r -> (r.getType() != null && r.getType().toLowerCase().contains(q))
                        || (r.getIdUser() != null && r.getIdUser().toLowerCase().contains(q)))
                .collect(Collectors.toList());
        renderReactionsTable(filtered);
    }

    // ══════════════════════════════════════════════════════════════
    // NEW POST  — with full validation
    // ══════════════════════════════════════════════════════════════

    @FXML private void handleNewPost() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nouvelle publication");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox content = buildDialogContent("✎  Nouvelle publication", "#1E40AF", "#DBEAFE");

        TextArea contentArea = styledTextArea("Contenu de la publication…");
        TextField userField  = styledField("ID utilisateur (UUID)…");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("public", "private");
        statusCombo.setValue("public");
        statusCombo.setStyle("-fx-font-size:12px; -fx-background-color:#F8FAFF; -fx-background-radius:8;" +
                "-fx-border-color:#BFDBFE; -fx-border-radius:8; -fx-pref-width:180;");
        TextField mediaField = styledField("Chemin média (optionnel)…");

        // ── Compteur caractères en temps réel ─────────────────────
        Label contentCounter = charCounter(contentArea, POST_MAX_LENGTH);
        // ─────────────────────────────────────────────────────────

        // ── Inline validation indicators ──────────────────────────
        Label contentError = inlineError();
        Label userError    = inlineError();
        // ─────────────────────────────────────────────────────────

        VBox form = (VBox) content.getChildren().get(1);
        form.getChildren().addAll(
                fieldRowWithMeta("Contenu", contentArea, contentCounter, contentError),
                fieldRowWithMeta("Utilisateur (UUID)", userField, null, userError),
                fieldRow("Statut", statusCombo),
                fieldRow("Média", mediaField)
        );

        dialog.getDialogPane().setContent(content);
        styleDialogButtons(dialog, "#1E40AF", "Créer");

        // ── Validate on OK ────────────────────────────────────────
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean valid = true;

            String text = contentArea.getText().trim();
            if (text.isEmpty()) {
                showInlineError(contentError, "Le contenu est obligatoire.");
                valid = false;
            } else if (text.length() < POST_MIN_LENGTH) {
                showInlineError(contentError, "Minimum " + POST_MIN_LENGTH + " caractères.");
                valid = false;
            } else if (text.length() > POST_MAX_LENGTH) {
                showInlineError(contentError, "Maximum " + POST_MAX_LENGTH + " caractères.");
                valid = false;
            } else {
                hideInlineError(contentError);
            }

            String uid = userField.getText().trim();
            if (uid.isEmpty()) {
                showInlineError(userError, "L'ID utilisateur est obligatoire.");
                valid = false;
            } else if (!uid.matches(UUID_REGEX)) {
                showInlineError(userError, "Format UUID invalide (ex: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx).");
                valid = false;
            } else {
                hideInlineError(userError);
            }

            if (!valid) event.consume(); // prevent dialog close
        });
        // ─────────────────────────────────────────────────────────

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                String text  = contentArea.getText().trim();
                String uid   = userField.getText().trim();
                String media = mediaField.getText().trim();
                Post p = new Post(text, media.isEmpty() ? null : media, statusCombo.getValue(), uid);
                postService.ajouterPost(p);
                loadAll();
                showToast("✓ Publication créée");
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // EDIT POST  — with full validation
    // ══════════════════════════════════════════════════════════════

    private void openEditPostDialog(Post post) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier la publication");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox content = buildDialogContent("✏  Modifier la publication", "#1E40AF", "#DBEAFE");

        TextArea contentArea = styledTextArea(null);
        contentArea.setText(post.getContenu() != null ? post.getContenu() : "");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("public", "private", "hidden");
        statusCombo.setValue(post.getStatut() != null ? post.getStatut() : "public");
        statusCombo.setStyle("-fx-font-size:12px; -fx-background-color:#F8FAFF; -fx-background-radius:8;" +
                "-fx-border-color:#BFDBFE; -fx-border-radius:8; -fx-pref-width:180;");
        TextField mediaField = styledField(null);
        mediaField.setText(post.getMedia() != null ? post.getMedia() : "");

        // ── Compteur + inline error ────────────────────────────────
        Label contentCounter = charCounter(contentArea, POST_MAX_LENGTH);
        Label contentError   = inlineError();
        // ─────────────────────────────────────────────────────────

        VBox form = (VBox) content.getChildren().get(1);
        form.getChildren().addAll(
                fieldRowWithMeta("Contenu", contentArea, contentCounter, contentError),
                fieldRow("Statut", statusCombo),
                fieldRow("Média", mediaField)
        );

        dialog.getDialogPane().setContent(content);
        styleDialogButtons(dialog, "#1E40AF", "Enregistrer");

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String text = contentArea.getText().trim();
            if (text.isEmpty()) {
                showInlineError(contentError, "Le contenu est obligatoire.");
                event.consume();
            } else if (text.length() < POST_MIN_LENGTH) {
                showInlineError(contentError, "Minimum " + POST_MIN_LENGTH + " caractères.");
                event.consume();
            } else if (text.length() > POST_MAX_LENGTH) {
                showInlineError(contentError, "Maximum " + POST_MAX_LENGTH + " caractères.");
                event.consume();
            } else {
                hideInlineError(contentError);
            }
        });

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                post.setContenu(contentArea.getText().trim());
                post.setStatut(statusCombo.getValue());
                post.setMedia(mediaField.getText().trim().isEmpty() ? null : mediaField.getText().trim());
                postService.modifierPost(post);
                loadAll();
                showToast("✓ Publication mise à jour");
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // EDIT COMMENT  — with full validation
    // ══════════════════════════════════════════════════════════════

    private void openEditCommentDialog(Commentaire c) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le commentaire");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox content = buildDialogContent("✏  Modifier le commentaire", "#1E40AF", "#DBEAFE");

        TextArea contentArea = styledTextArea(null);
        contentArea.setText(c.getContenu() != null ? c.getContenu() : "");

        Label infoL = new Label("Post #" + c.getIdPost()
                + (c.getIdParent() != null ? "  ·  Réponse à #" + c.getIdParent() : ""));
        infoL.setStyle("-fx-font-size:11px; -fx-text-fill:#6B7280; -fx-font-family:'Segoe UI',Arial;");

        // ── Compteur + inline error ────────────────────────────────
        Label contentCounter = charCounter(contentArea, COMMENT_MAX_LENGTH);
        Label contentError   = inlineError();
        // ─────────────────────────────────────────────────────────

        VBox form = (VBox) content.getChildren().get(1);
        form.getChildren().addAll(infoL, fieldRowWithMeta("Contenu", contentArea, contentCounter, contentError));

        dialog.getDialogPane().setContent(content);
        styleDialogButtons(dialog, "#1E40AF", "Enregistrer");

        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            String text = contentArea.getText().trim();
            if (text.isEmpty()) {
                showInlineError(contentError, "Le contenu est obligatoire.");
                event.consume();
            } else if (text.length() < COMMENT_MIN_LENGTH) {
                showInlineError(contentError, "Minimum " + COMMENT_MIN_LENGTH + " caractères.");
                event.consume();
            } else if (text.length() > COMMENT_MAX_LENGTH) {
                showInlineError(contentError, "Maximum " + COMMENT_MAX_LENGTH + " caractères.");
                event.consume();
            } else {
                hideInlineError(contentError);
            }
        });

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                c.setContenu(contentArea.getText().trim());
                commentService.modifierCommentaire(c);
                loadAll();
                showToast("✓ Commentaire mis à jour");
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    // Validation Helpers
    // ══════════════════════════════════════════════════════════════

    /** Real-time character counter bound to a TextArea. */
    private Label charCounter(TextArea area, int max) {
        Label counter = new Label(area.getText().length() + "/" + max);
        counter.setStyle("-fx-font-size:10px; -fx-text-fill:#9CA3AF; -fx-font-family:'Segoe UI',Arial;");
        area.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal.length();
            counter.setText(len + "/" + max);
            counter.setStyle("-fx-font-size:10px; -fx-font-family:'Segoe UI',Arial; -fx-text-fill:"
                    + (len > max ? "#EF4444" : len > max * 0.9 ? "#F59E0B" : "#9CA3AF") + ";");
        });
        return counter;
    }

    /** Creates a hidden inline error label to show under a field. */
    private Label inlineError() {
        Label l = new Label();
        l.setStyle("-fx-font-size:11px; -fx-text-fill:#EF4444; -fx-font-family:'Segoe UI',Arial;");
        l.setVisible(false);
        l.setManaged(false);
        return l;
    }

    private void showInlineError(Label l, String msg) {
        l.setText("⚠ " + msg);
        l.setVisible(true);
        l.setManaged(true);
        FadeTransition ft = new FadeTransition(Duration.millis(180), l);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    private void hideInlineError(Label l) {
        l.setVisible(false);
        l.setManaged(false);
    }

    // ══════════════════════════════════════════════════════════════
    // UI Helpers
    // ══════════════════════════════════════════════════════════════

    private Button actionBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:transparent; -fx-background-radius:6;" +
                "-fx-border-color:" + color + "; -fx-border-radius:6; -fx-border-width:1;" +
                "-fx-text-fill:" + color + "; -fx-font-size:11px;" +
                "-fx-min-width:30; -fx-min-height:26; -fx-cursor:HAND;");
        b.setOnMouseEntered(e -> b.setStyle(b.getStyle().replace("-fx-background-color:transparent;",
                "-fx-background-color:" + color + "22;")));
        b.setOnMouseExited(e  -> b.setStyle(b.getStyle().replace("-fx-background-color:" + color + "22;",
                "-fx-background-color:transparent;")));
        return b;
    }

    private HBox emptyRow(String msg) {
        HBox r = new HBox();
        r.setAlignment(Pos.CENTER);
        r.setPadding(new Insets(32));
        Label l = new Label(msg);
        l.setStyle("-fx-font-size:13px; -fx-text-fill:#9CA3AF; -fx-font-family:'Segoe UI',Arial;");
        r.getChildren().add(l);
        return r;
    }

    private VBox buildDialogContent(String title, String accentColor, String bgLight) {
        VBox content = new VBox(0);
        content.setStyle("-fx-background-color:#FFFFFF; -fx-min-width:480;");

        HBox header = new HBox(12);
        header.setStyle("-fx-background-color:" + bgLight + ";" +
                "-fx-background-radius:12 12 0 0; -fx-padding:18 20 18 20; -fx-alignment:CENTER_LEFT;");
        Label titleL = new Label(title);
        titleL.setStyle("-fx-font-size:15px; -fx-font-weight:bold;" +
                "-fx-text-fill:" + accentColor + "; -fx-font-family:'Segoe UI',Arial;");
        header.getChildren().add(titleL);

        VBox form = new VBox(12);
        form.setPadding(new Insets(20, 20, 10, 20));

        content.getChildren().addAll(header, form);
        return content;
    }

    /** Simple field row without counter/error. */
    private VBox fieldRow(String labelText, Node control) {
        VBox row = new VBox(4);
        Label l = new Label(labelText);
        l.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#6B7280;" +
                "-fx-font-family:'Segoe UI',Arial;");
        row.getChildren().addAll(l, control);
        return row;
    }

    /** Field row with optional counter and inline error labels. */
    private VBox fieldRowWithMeta(String labelText, Node control, Label counter, Label errorLabel) {
        VBox row = new VBox(4);
        Label l = new Label(labelText);
        l.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#6B7280;" +
                "-fx-font-family:'Segoe UI',Arial;");
        row.getChildren().add(l);
        row.getChildren().add(control);
        if (counter != null) {
            HBox metaRow = new HBox();
            metaRow.setAlignment(Pos.CENTER_RIGHT);
            metaRow.getChildren().add(counter);
            row.getChildren().add(metaRow);
        }
        if (errorLabel != null) row.getChildren().add(errorLabel);
        return row;
    }

    private TextArea styledTextArea(String prompt) {
        TextArea ta = new TextArea();
        if (prompt != null) ta.setPromptText(prompt);
        ta.setWrapText(true);
        ta.setPrefRowCount(4);
        ta.setStyle("-fx-font-size:13px; -fx-font-family:'Segoe UI',Arial;" +
                "-fx-background-color:#F8FAFF; -fx-background-radius:8;" +
                "-fx-border-color:#BFDBFE; -fx-border-radius:8; -fx-border-width:1.5; -fx-padding:9;");
        return ta;
    }

    private TextField styledField(String prompt) {
        TextField tf = new TextField();
        if (prompt != null) tf.setPromptText(prompt);
        tf.setStyle("-fx-font-size:12px; -fx-font-family:'Segoe UI',Arial;" +
                "-fx-background-color:#F8FAFF; -fx-background-radius:8;" +
                "-fx-border-color:#BFDBFE; -fx-border-radius:8; -fx-border-width:1.5;" +
                "-fx-padding:8 12 8 12;");
        return tf;
    }

    private void styleDialogButtons(Dialog<?> dialog, String color, String okText) {
        dialog.getDialogPane().setStyle(
                "-fx-background-color:#FFFFFF; -fx-background-radius:12; -fx-padding:0;");
        dialog.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color:" + color + "; -fx-background-radius:8;" +
                        "-fx-text-fill:white; -fx-font-size:12px; -fx-font-family:'Segoe UI',Arial;" +
                        "-fx-padding:7 22 7 22; -fx-cursor:HAND;");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.OK)).setText(okText);
        dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
                "-fx-background-color:transparent; -fx-text-fill:#6B7280;" +
                        "-fx-font-size:12px; -fx-font-family:'Segoe UI',Arial;" +
                        "-fx-padding:7 16 7 16; -fx-cursor:HAND;");
        ((Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Annuler");
    }

    private void confirmDelete(String subject, Runnable onConfirm) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Confirmer la suppression");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        VBox content = new VBox(0);
        content.setStyle("-fx-background-color:#FFFFFF; -fx-min-width:380;");

        HBox header = new HBox(12);
        header.setStyle("-fx-background-color:#FEE2E2; -fx-background-radius:12 12 0 0;" +
                "-fx-padding:18 20 18 20; -fx-alignment:CENTER_LEFT;");
        Label icon = new Label("🗑");
        icon.setStyle("-fx-font-size:24px;");
        VBox col = new VBox(3);
        Label titleL = new Label("Supprimer " + subject);
        titleL.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#991B1B; -fx-font-family:'Segoe UI',Arial;");
        Label subL = new Label("Cette action est irréversible");
        subL.setStyle("-fx-font-size:11px; -fx-text-fill:#DC2626; -fx-font-family:'Segoe UI',Arial;");
        col.getChildren().addAll(titleL, subL);
        header.getChildren().addAll(icon, col);

        VBox body = new VBox();
        body.setPadding(new Insets(16, 20, 8, 20));
        Label msg = new Label("Êtes-vous sûr de vouloir supprimer " + subject + " ?\nTous les éléments associés seront également supprimés.");
        msg.setWrapText(true);
        msg.setStyle("-fx-font-size:13px; -fx-text-fill:#374151; -fx-font-family:'Segoe UI',Arial;");
        body.getChildren().add(msg);

        content.getChildren().addAll(header, body);
        dialog.getDialogPane().setContent(content);

        dialog.getDialogPane().setStyle("-fx-background-color:#FFFFFF; -fx-background-radius:12; -fx-padding:0;");
        Button okBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okBtn.setStyle("-fx-background-color:#EF4444; -fx-background-radius:8; -fx-text-fill:white;" +
                "-fx-font-size:12px; -fx-padding:7 22 7 22; -fx-cursor:HAND;");
        okBtn.setText("Supprimer");
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#6B7280;" +
                "-fx-font-size:12px; -fx-padding:7 16 7 16; -fx-cursor:HAND;");
        cancelBtn.setText("Annuler");

        dialog.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) onConfirm.run();
        });
    }

    private void showToast(String message) {
        Platform.runLater(() -> {
            try {
                Label toast = new Label(message);
                toast.setStyle(
                        "-fx-background-color:rgba(17,24,39,0.88);" +
                                "-fx-background-radius:20; -fx-text-fill:white;" +
                                "-fx-font-size:12px; -fx-font-family:'Segoe UI',Arial;" +
                                "-fx-padding:9 22 9 22;");
                toast.setMouseTransparent(true);
                toastPane.getChildren().add(toast);
                FadeTransition ft = new FadeTransition(Duration.millis(1600), toast);
                ft.setFromValue(1); ft.setToValue(0);
                ft.setDelay(Duration.millis(1400));
                ft.setOnFinished(e -> toastPane.getChildren().remove(toast));
                ft.play();
            } catch (Exception ignored) {}
        });
    }
}
