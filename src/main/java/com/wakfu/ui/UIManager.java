package com.wakfu.ui;

import com.wakfu.data.UserSettings;
import com.wakfu.domain.actors.Fighter;
import com.wakfu.domain.model.PlayerStats;
import com.wakfu.service.DamageCalculator;
import com.wakfu.domain.model.FightModel;
import com.wakfu.storage.FightHistoryManager;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * UIManager acts as the MVC Controller.
 * It manages:
 * - User interactions (button clicks, selections)
 * - Communication with DamageCalculator (model)
 * - Updates to MainUI (view)
 * - Display logic and state management
 */
public class UIManager {

    private final Stage primaryStage;
    private final MainUI mainUI;
    private final Label statusLabel;    // affiche le dossier de logs / messages
    private final VBox playersContainer;

    // Turn breakdown UI (per-round details)
    private TurnBreakdownUI turnBreakdownUI = null;
    // Keep last model to feed TurnBreakdownUI
    private FightModel lastModel = null;

    // Track the currently displayed breakdown panel
    private PlayerStats currentSelectedPlayer = null;

    // === Controls Elements ===
    private final Button refreshButton;
    private final CheckBox autoResetCheck;
    private final Button selectLogsFolderButton;
    private final DamageCalculator damageCalculator;
    private final Button turnDetailsBtn;
    // Historique
    private final CheckBox historyCheck;
    private final Button clearHistoryButton;
    // Per-player colors for the session
    private final Map<String, javafx.scene.paint.Color> playerColors = new ConcurrentHashMap<>();

    // Callbacks
    private Consumer<String> onLogFolderSelected;
    private Consumer<Boolean> onHistoryChanged;
    private Consumer<Boolean> onAutoResetChanged;

    public UIManager(Stage primaryStage, DamageCalculator damageCalculator) {
        this.primaryStage = primaryStage;
        this.damageCalculator = damageCalculator;

        // Initialize MainUI (the view)
        this.mainUI = new MainUI(primaryStage);
        this.playersContainer = mainUI.getCenterContainer();

        this.statusLabel = new Label("En attente de combat...");

        this.refreshButton = new Button("🔄");
        this.autoResetCheck = new CheckBox("Auto-reset");
        this.selectLogsFolderButton = new Button("📁");
        this.historyCheck = new CheckBox("Historique");
        this.clearHistoryButton = new Button("✖");
        this.turnDetailsBtn = new Button("\uD83D\uDCCA");

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

    @SuppressWarnings("unused")
    public boolean isHistoryEnabled() { return historyCheck.isSelected(); }

    public void setHistoryChecked(boolean value) { historyCheck.setSelected(value); }

    private void setupUI() {
        // Setup header buttons with callbacks
        setupHeaderControls();

        // Add header controls to MainUI
        mainUI.addAllToHeader(List.of(
            selectLogsFolderButton, refreshButton, autoResetCheck, turnDetailsBtn,
            statusLabel, historyCheck, clearHistoryButton
        ));

        // Si un dossier est déjà configuré, l'afficher
        UserSettings.loadLogFolder().ifPresent(p -> statusLabel.setText("Logs folder: " + p));
    }

    /**
     * Initializes all header control buttons and their event handlers.
     */
    private void setupHeaderControls() {
        // Folder select button (icône)
        selectLogsFolderButton.setTooltip(new Tooltip("Select logs folder"));
        selectLogsFolderButton.setOnAction(e -> {
            System.identityHashCode(e);
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("Select Wakfu logs folder");
            UserSettings.loadLogFolder().ifPresent(p -> {
                File f = new File(p);
                if (f.exists() && f.isDirectory()) dc.setInitialDirectory(f);
            });
            File chosen = dc.showDialog(primaryStage);
            if (chosen != null) {
                String path = chosen.getAbsolutePath();
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
            System.identityHashCode(obs); System.identityHashCode(oldVal);
            if (onAutoResetChanged != null) {
                try { onAutoResetChanged.accept(newVal); } catch (Exception ignored) {}
            }
        });

        // Clear history button
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

        // Turn details button
        turnDetailsBtn.setTooltip(new Tooltip("Détails Par tour"));
        turnDetailsBtn.setOnAction(e -> {
            System.identityHashCode(e);
            if (turnBreakdownUI == null) turnBreakdownUI = new TurnBreakdownUI(primaryStage);
            turnBreakdownUI.show();
            if (lastModel != null) turnBreakdownUI.update(lastModel);
        });
        turnDetailsBtn.setMinWidth(36);
    }

    /**
     * Met à jour le label d'état de l'application (affiché sous le header).
     */
    public void setAppStatus(String message) {
        String text = (message == null ? "" : message);
        Platform.runLater(() -> mainUI.setAppStatus(text));
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
            playersContainer.getChildren().clear();
            statusLabel.setText("En attente de combat...");
            setAppStatus("En attente de combat...");
            // Clear breakdown pane
            mainUI.setBreakdownPanel(null);
            currentSelectedPlayer = null;
         });
     }

    /** Public wrapper to reset the UI from outside */
    public void resetUI() { resetData(); }

    /**
     * Rafraîchit l'affichage de la liste des joueurs (ignore les ennemis).
     */
    public void displayPlayerStats(List<PlayerStats> statsList, int totalDamage) {
        Platform.runLater(() -> {
            playersContainer.getChildren().clear();

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

                    // pass a callback so breakdown opens/updates in the right pane
                    PlayerUI playerUI = new PlayerUI(ps, pct, c, this::showBreakdownInRightPane);
                    HBox rowBox = playerUI.render();
                    HBox.setHgrow(rowBox, Priority.ALWAYS);
                    playersContainer.getChildren().add(rowBox);
                    row.getAndIncrement();
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

        // Auto-refresh the breakdown pane if a player is currently selected
        if (currentSelectedPlayer != null) {
            // Find the updated PlayerStats for the selected player in the new model
            var updatedStats = statsList.stream()
                    .filter(ps -> ps.getPlayer().getName().equals(currentSelectedPlayer.getPlayer().getName()))
                    .findFirst();

            if (updatedStats.isPresent()) {
                currentSelectedPlayer = updatedStats.get();
                showBreakdownInRightPane(currentSelectedPlayer);
            } else {
                // Player no longer in model, clear the breakdown
                mainUI.setBreakdownPanel(null);
                currentSelectedPlayer = null;
            }
        }
    }

    /**
     * Affiche un message d'erreur simple.
     */
    public void showError(String title, String message) {
        Platform.runLater(() -> statusLabel.setText("❌ " + title + ": " + message));
    }

    /**
     * Shows or updates the breakdown panel in the right pane (MainUI).
     * This replaces the old external Stage with an integrated right pane.
     */
    private void showBreakdownInRightPane(PlayerStats stats) {
         Platform.runLater(() -> {
             try {
                // Remember the selected player for auto-refresh on model updates
                currentSelectedPlayer = stats;
                Pane panel = BreakdownPane.buildPanel(stats);
                mainUI.setBreakdownPanel(panel);
             } catch (Exception e) {
                 showError("Erreur", "Impossible d'afficher le breakdown: " + e.getMessage());
             }
         });
     }
}
