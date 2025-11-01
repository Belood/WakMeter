package com.wakfu.ui;

import com.wakfu.config.UserSettings;
import com.wakfu.domain.actors.Fighter;
import com.wakfu.service.DamageCalculator;
import com.wakfu.model.FightModel;
import com.wakfu.storage.FightHistoryManager;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Interface principale affichant les dégâts totaux des joueurs.
 */
public class UIManager {

    private final Stage primaryStage;
    private final VBox mainContainer;
    private final Label statusLabel;    // affiche le dossier de logs / messages
    private final Label appStatusLabel; // nouveau : affiche l'état de l'application juste sous le header
    private final GridPane playerGrid;
    // External breakdown stage (docked to the right of main window)
    private Stage breakdownStage = null;
    // Turn breakdown UI (per-round details)
    private TurnBreakdownUI turnBreakdownUI = null;
    // Keep last model to feed TurnBreakdownUI
    private com.wakfu.model.FightModel lastModel = null;

    // === Nouveaux éléments ===
    private final Button refreshButton; // remplace resetButton
    private final CheckBox autoResetCheck;
    private final Button selectLogsFolderButton;
    private final DamageCalculator damageCalculator;

    // Historique
    private final CheckBox historyCheck;
    private final Button clearHistoryButton;
    // Per-player colors for the session
    private final Map<String, javafx.scene.paint.Color> playerColors = new ConcurrentHashMap<>();

    // Callback appelée quand l'utilisateur choisit un dossier
    private Consumer<String> onLogFolderSelected;
    // Callback when history checkbox changes
    private Consumer<Boolean> onHistoryChanged;
    // Callback when auto-reset checkbox changes
    private Consumer<Boolean> onAutoResetChanged;

    public UIManager(Stage primaryStage, DamageCalculator damageCalculator) {
        this.primaryStage = primaryStage;
        this.damageCalculator = damageCalculator;
        this.mainContainer = new VBox(10);
        this.statusLabel = new Label("En attente de combat...");
        this.appStatusLabel = new Label("App Status: \"Actif\"");
        this.playerGrid = new GridPane();

        this.refreshButton = new Button("🔄");
        this.autoResetCheck = new CheckBox("Auto-reset");
        this.selectLogsFolderButton = new Button("📁");
        this.historyCheck = new CheckBox("Historique");
        this.clearHistoryButton = new Button("✖");

        setupUI();
    }

    public void setOnLogFolderSelected(Consumer<String> callback) {
        this.onLogFolderSelected = callback;
    }

    public void setOnHistoryChanged(Consumer<Boolean> callback) {
        this.onHistoryChanged = callback;
    }

    /**
     * Called by the application to be notified when the Auto-reset checkbox changes.
     */
    public void setOnAutoResetChanged(Consumer<Boolean> callback) {
        this.onAutoResetChanged = callback;
    }

    private void setupUI() {
        // --- HEADER ---
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));

        // Folder select button (icône)
        selectLogsFolderButton.setTooltip(new Tooltip("Select logs folder"));
        selectLogsFolderButton.setOnAction(e -> {
            System.identityHashCode(e);
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Wakfu logs folder");
            // essayer charger le dernier dossier connu
            UserSettings.loadLogFolder().ifPresent(p -> {
                File f = new File(p);
                if (f.exists() && f.isDirectory()) dc.setInitialDirectory(f);
            });
            File chosen = dc.showDialog(primaryStage);
            if (chosen != null) {
                String path = chosen.getAbsolutePath();
                // sauvegarde
                UserSettings.saveLogFolder(path);
                statusLabel.setText("Logs folder: " + path);
                if (onLogFolderSelected != null) onLogFolderSelected.accept(path);
            }
        });
        selectLogsFolderButton.setMinWidth(36);

        // Refresh button (icône)
        refreshButton.setTooltip(new Tooltip("Refresh / Reset UI"));
        refreshButton.setOnAction(e -> { System.identityHashCode(e); resetData(); });
        refreshButton.setMinWidth(36);

        autoResetCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            // reference unused parameters to avoid static analysis warnings
            System.identityHashCode(obs); System.identityHashCode(oldVal);
            if (onAutoResetChanged != null) {
                try { onAutoResetChanged.accept(newVal); } catch (Exception ignored) {}
            }
        });



        // Historique checkbox + clear button
        clearHistoryButton.setTooltip(new Tooltip("Clear history (fight_history.json)"));
        clearHistoryButton.setOnAction(e -> {
            System.identityHashCode(e);
            boolean ok = FightHistoryManager.clearHistory();
            if (ok) statusLabel.setText("Historique effacé");
            else statusLabel.setText("Erreur lors de l'effacement de l'historique");
        });
        clearHistoryButton.setVisible(false);
        clearHistoryButton.setMinWidth(36);

        historyCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            System.identityHashCode(obs); System.identityHashCode(oldVal);
            clearHistoryButton.setVisible(newVal);
            if (onHistoryChanged != null) {
                try { onHistoryChanged.accept(newVal); } catch (Exception ignored) {}
            }
        });

        // Button: Détails Par tour (icon U+1F522)
        Button turnDetailsBtn = new Button("\uD83D\uDCCA");
        turnDetailsBtn.setTooltip(new Tooltip("Détails Par tour"));
        turnDetailsBtn.setOnAction(e -> {
            System.identityHashCode(e);
            // open or update TurnBreakdownUI
            if (turnBreakdownUI == null) turnBreakdownUI = new TurnBreakdownUI(primaryStage);
            turnBreakdownUI.show();
            if (lastModel != null) turnBreakdownUI.update(lastModel);
        });
        turnDetailsBtn.setMinWidth(36);

        header.getChildren().addAll(selectLogsFolderButton, refreshButton, autoResetCheck, historyCheck, clearHistoryButton, statusLabel, turnDetailsBtn);

        // --- Conteneur principal ---
        mainContainer.setPadding(new Insets(10));
        // Layout central: players list (scrollable)
        HBox center = new HBox(10);
        center.setAlignment(Pos.TOP_LEFT);

        ScrollPane playersScroll = new ScrollPane(playerGrid);
        playersScroll.setFitToWidth(true);
        playersScroll.setBackground(Background.EMPTY);
        playersScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        playersScroll.setPrefViewportHeight(400);

        center.getChildren().addAll(playersScroll);

        // Ajout du header, de l'appStatusLabel, puis du centre (players + breakdown)
        mainContainer.getChildren().addAll(header, appStatusLabel, center);

        Scene scene = new Scene(mainContainer, 700, 450);
        // Applique le thème sombre si disponible
        var css = getClass().getResource("/dark-theme.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            System.err.println("[UIManager] dark-theme.css introuvable dans les resources");
        }
        primaryStage.setScene(scene);
        var iconStream = getClass().getResourceAsStream("/assets/ico.png");
        if (iconStream != null) {
            primaryStage.getIcons().add(new Image(iconStream));
        } else {
            System.err.println("[UIManager] assets/ico.png introuvable dans les resources");
        }
        primaryStage.setTitle("WakMeter");
        primaryStage.show();
        // increase minimum size for the main UI
        primaryStage.setMinWidth(920);
        primaryStage.setMinHeight(520);

        // Si un dossier est déjà configuré, l'afficher
        UserSettings.loadLogFolder().ifPresent(p -> statusLabel.setText("Logs folder: " + p));
    }

    @SuppressWarnings("unused")
    public boolean isHistoryEnabled() { return historyCheck.isSelected(); }

    public void setHistoryChecked(boolean value) { historyCheck.setSelected(value); }

    /**
     * Met à jour le label d'état de l'application (affiché sous le header).
     */
    public void setAppStatus(String message) {
        String text = "App Status: \"" + (message == null ? "" : message) + "\"";
        Platform.runLater(() -> appStatusLabel.setText(text));
    }

    /**
     * Affiche un message simple (état ou notification) dans le header (statusLabel).
     */
    public void showMessage(String title, String message) {
        Platform.runLater(() -> statusLabel.setText(title + " - " + message));
    }

    /**
     * Réinitialise l'affichage UI localement.
     */
    private void resetData() {
        Platform.runLater(() -> {
            playerGrid.getChildren().clear();
            statusLabel.setText("En attente de combat...");
            setAppStatus("Inactif");
            // close external breakdown window if opened
            if (breakdownStage != null) {
                try { breakdownStage.close(); } catch (Exception ignored) {}
                breakdownStage = null;
            }
         });
     }

    /** Public wrapper to reset the UI from outside */
    public void resetUI() { resetData(); }

    /**
     * Rafraîchit l'affichage de la liste des joueurs (ignore les ennemis).
     */
    public void displayPlayerStats(List<com.wakfu.model.PlayerStats> statsList, int totalDamage) {
        Platform.runLater(() -> {
            playerGrid.getChildren().clear();

            java.util.concurrent.atomic.AtomicInteger row = new java.util.concurrent.atomic.AtomicInteger(0);
            // Sort players by damage descending
            statsList.stream()
                    .sorted((a, b) -> Integer.compare(b.getTotalDamage(), a.getTotalDamage()))
                    .forEach(ps -> {
                 var p = ps.getPlayer();
                 if (p.getType() == Fighter.FighterType.PLAYER) {
                     int dmg = ps.getTotalDamage();
                     double pct = totalDamage > 0 ? (double) dmg / totalDamage : 0.0;

                    // assign a consistent random color for this player in the session
                    String playerKey = p.getName();
                    javafx.scene.paint.Color c = playerColors.computeIfAbsent(playerKey, key -> {
                        // generate slightly desaturated random color using the key's hash for determinism
                        double hue = Math.abs(key.hashCode() % 360);
                        return javafx.scene.paint.Color.hsb(hue, 0.65, 0.75);
                    });

                    // pass a callback so breakdown opens/updates an external panel docked to the right
                    PlayerUI playerUI = new PlayerUI(ps, pct, c, this::showOrUpdateBreakdown);
                     HBox rowBox = playerUI.render();

                     playerGrid.add(rowBox, 0, row.getAndIncrement());
                 }
             });
          });
      }

    /**
     * Rafraîchit l'UI à partir du modèle de combat.
     */
    public void refresh(FightModel model) {
        if (model == null) return;

        int totalDamage = damageCalculator.getTotalDamage(model);
        var statsList = model.getStatsByPlayer().values().stream().toList();
        // store last model for external UIs
        this.lastModel = model;
        displayPlayerStats(statsList, totalDamage);
    }

    /**
     * Affiche un message d'erreur simple.
     */
    public void showError(String title, String message) {
        Platform.runLater(() -> statusLabel.setText("❌ " + title + ": " + message));
    }

    /**
     * Show or update the external breakdown Stage docked to the right of the primary stage.
     */
    private void showOrUpdateBreakdown(com.wakfu.model.PlayerStats stats) {
         Platform.runLater(() -> {
             try {
                Pane panel = DamageBreakdownUI.buildPanel(stats);

                if (breakdownStage == null) {
                    // create a BorderPane wrapper so we can add a top bar with a close button
                    BorderPane wrapper = new BorderPane();
                    wrapper.setCenter(panel);

                    // top bar with close button aligned to right
                    HBox topBar = new HBox();
                    topBar.setPadding(new Insets(6, 6, 6, 6));
                    topBar.setAlignment(Pos.CENTER_RIGHT);
                    Button closeBtn = new Button("✖");
                    closeBtn.setTooltip(new Tooltip("Fermer"));
                    closeBtn.setOnAction(ev -> {
                        // reference the event to avoid unused-parameter warnings (no functional impact)
                        System.identityHashCode(ev);
                        try { if (breakdownStage != null) breakdownStage.close(); } catch (Exception ignored) {}
                        breakdownStage = null;
                    });
                    closeBtn.setMinWidth(36);
                    topBar.getChildren().add(closeBtn);
                    wrapper.setTop(topBar);

                    breakdownStage = new Stage();
                    Scene s = new Scene(wrapper, 360.0, primaryStage.getHeight());
                    // apply theme
                    var css = getClass().getResource("/dark-theme.css");
                    if (css != null) s.getStylesheets().add(css.toExternalForm());
                    breakdownStage.setScene(s);
                    breakdownStage.setTitle("Breakdown - " + stats.getPlayer().getName());
                    breakdownStage.setResizable(true);
                    // position next to primary stage
                    breakdownStage.setX(primaryStage.getX() + primaryStage.getWidth());
                    breakdownStage.setY(primaryStage.getY());
                    breakdownStage.setHeight(primaryStage.getHeight());
                    breakdownStage.setOnCloseRequest(ev -> { System.identityHashCode(ev); breakdownStage = null; });

                    // reposition when main stage moves/resizes
                    primaryStage.xProperty().addListener((o,ov,nv) -> {
                        System.identityHashCode(o); System.identityHashCode(ov); System.identityHashCode(nv);
                        if (breakdownStage != null) breakdownStage.setX(primaryStage.getX() + primaryStage.getWidth());
                    });
                    primaryStage.yProperty().addListener((o,ov,nv) -> {
                        System.identityHashCode(o); System.identityHashCode(ov); System.identityHashCode(nv);
                        if (breakdownStage != null) breakdownStage.setY(primaryStage.getY());
                    });
                    primaryStage.widthProperty().addListener((o,ov,nv) -> {
                        System.identityHashCode(o); System.identityHashCode(ov); System.identityHashCode(nv);
                        if (breakdownStage != null) breakdownStage.setX(primaryStage.getX() + primaryStage.getWidth());
                    });
                    primaryStage.heightProperty().addListener((o,ov,nv) -> {
                        System.identityHashCode(o); System.identityHashCode(ov); System.identityHashCode(nv);
                        if (breakdownStage != null) breakdownStage.setHeight(primaryStage.getHeight());
                    });

                    breakdownStage.show();
                } else {
                    // update existing: keep the same wrapper, replace center
                    var root = breakdownStage.getScene().getRoot();
                    if (root instanceof BorderPane) {
                        ((BorderPane) root).setCenter(panel);
                    } else {
                        breakdownStage.getScene().setRoot(panel);
                    }
                    breakdownStage.setTitle("Breakdown - " + stats.getPlayer().getName());
                }
             } catch (Exception e) {
                 // fallback: show in a dialog
                 showError("Erreur", "Impossible d'afficher le breakdown: " + e.getMessage());
             }
         });
     }
}
