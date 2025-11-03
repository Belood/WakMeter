package com.wakfu.ui;

import com.wakfu.data.UserSettings;
import com.wakfu.domain.actors.Fighter;
import com.wakfu.domain.model.PlayerStats;
import com.wakfu.service.DamageCalculator;
import com.wakfu.domain.model.FightModel;
import com.wakfu.storage.FightHistoryManager;
import com.wakfu.ui.overall.TotalBreakdownPane;
import com.wakfu.ui.overall.TotalDamagePane;
import com.wakfu.ui.turn.TurnBreakdownPane;
import com.wakfu.ui.turn.TurnDetailsPane;
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

// Note: PlayerUI is defined in TotalDamagePane.java

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
    private final VBox playersContainer;

    // Turn breakdown UI (per-round details)
    private TurnDetailsPane turnDetailsUI = null;
    // Keep last model to feed TurnBreakdownUI
    private FightModel lastModel = null;

    // Track the currently displayed breakdown panel
    private PlayerStats currentSelectedPlayer = null;
    private Integer currentSelectedRound = null; // Track selected round in Tour mode

    // Current display mode
    private DisplayMode currentMode = DisplayMode.TOTAL;

    // === Controls Elements ===
    private final Button refreshButton;
    private final CheckBox autoResetCheck;
    private final Button selectLogsFolderButton;
    private final DamageCalculator damageCalculator;
    private final Button totalBtn;
    private final Button tourBtn;
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


        this.refreshButton = new Button("🔄");
        this.autoResetCheck = new CheckBox("Auto-reset");
        this.selectLogsFolderButton = new Button("📁");
        this.historyCheck = new CheckBox("Historique");
        this.clearHistoryButton = new Button("✖");
        this.totalBtn = new Button("Total");
        this.tourBtn = new Button("Tour");

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
            selectLogsFolderButton, refreshButton, autoResetCheck,
            historyCheck, clearHistoryButton
        ));

        // Add mode buttons
        mainUI.getModeButtonsBox().getChildren().addAll(totalBtn, tourBtn);
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
                setAppStatus(com.wakfu.data.MessageProvider.logFolderSelected());
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
            if (ok) setAppStatus(com.wakfu.data.MessageProvider.historyCleared());
            else setAppStatus(com.wakfu.data.MessageProvider.historyClearError());
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

        // Total button
        totalBtn.setTooltip(new Tooltip("Mode Total"));
        totalBtn.setOnAction(e -> {
            System.identityHashCode(e);
            switchToTotalMode();
        });

        // Tour button
        tourBtn.setTooltip(new Tooltip("Détails Par tour"));
        tourBtn.setOnAction(e -> {
            System.identityHashCode(e);
            switchToTourMode();
        });

        // Set default mode styling
        updateModeButtons();
    }

    /**
     * Met à jour le label d'état de l'application (affiché sous le header).
     */
    public void setAppStatus(String message) {
        String text = (message == null ? "" : message);
        Platform.runLater(() -> mainUI.setAppStatus(text));
    }

    /**
     * Réinitialise l'affichage UI localement.
     */
    private void resetData() {
        Platform.runLater(() -> {
            playersContainer.getChildren().clear();
            setAppStatus(com.wakfu.data.MessageProvider.waitingCombat());
            // Clear breakdown pane
            mainUI.setBreakdownPanel(null);
            currentSelectedPlayer = null;
            currentSelectedRound = null;
            // Clear turn details if in Tour mode
            if (turnDetailsUI != null) {
                turnDetailsUI.clear();
            }
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
            var sortedPlayers = statsList.stream()
                    .sorted((a, b) -> Integer.compare(b.getTotalDamage(), a.getTotalDamage()))
                    .toList();

            // Find max damage among players only
            int maxDamage = sortedPlayers.stream()
                    .filter(ps -> ps.getPlayer().getType() == Fighter.FighterType.PLAYER)
                    .mapToInt(PlayerStats::getTotalDamage)
                    .max()
                    .orElse(1);

            if (maxDamage == 0) maxDamage = 1;

            final int finalMaxDamage = maxDamage;

            sortedPlayers.forEach(ps -> {
                 var p = ps.getPlayer();
                 if (p.getType() == Fighter.FighterType.PLAYER) {
                     int dmg = ps.getTotalDamage();
                     // pct is now relative to the max damage (highest player gets 100%)
                     double pct = (double) dmg / finalMaxDamage;

                    // assign a consistent random color for this player in the session
                    String playerKey = p.getName();
                    javafx.scene.paint.Color c = playerColors.computeIfAbsent(playerKey, key -> {
                        // generate slightly desaturated random color using the key's hash for determinism
                        double hue = Math.abs(key.hashCode() % 360);
                        return javafx.scene.paint.Color.hsb(hue, 0.65, 0.75);
                    });

                    // pass a callback so breakdown opens/updates in the right pane
                    TotalDamagePane playerUI = new TotalDamagePane(ps, pct, c, this::showBreakdownInRightPane);
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

        // Refresh display based on current mode
        if (currentMode == DisplayMode.TOTAL) {
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
        } else if (currentMode == DisplayMode.TOUR) {
            // Update TurnDetailsPane with new model
            if (turnDetailsUI != null) {
                turnDetailsUI.update(model, playerColors);
            }

            // Auto-refresh the turn breakdown if one is selected
            if (currentSelectedPlayer != null && currentSelectedRound != null) {
                // Find the updated PlayerStats for the selected round/player
                var roundModel = model.getRounds().stream()
                        .filter(r -> r.getRoundNumber() == currentSelectedRound)
                        .findFirst();

                if (roundModel.isPresent()) {
                    var updatedStats = roundModel.get().getPlayerStatsByRound()
                            .get(currentSelectedPlayer.getPlayer().getName());
                    if (updatedStats != null) {
                        currentSelectedPlayer = updatedStats;
                        showTurnBreakdownInRightPane(currentSelectedRound, currentSelectedPlayer);
                    } else {
                        // Player no longer in round, clear the breakdown
                        mainUI.setBreakdownPanel(null);
                        currentSelectedPlayer = null;
                        currentSelectedRound = null;
                    }
                } else {
                    // Round no longer exists, clear the breakdown
                    mainUI.setBreakdownPanel(null);
                    currentSelectedPlayer = null;
                    currentSelectedRound = null;
                }
            }
        }
    }

    /**
     * Affiche un message d'erreur simple.
     */
    public void showError(String title, String message) {
        Platform.runLater(() -> setAppStatus("❌ " + title + ": " + message));
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
                currentSelectedRound = null; // Clear round selection when showing total breakdown
                Pane panel = TotalBreakdownPane.buildPanel(stats);
                mainUI.setBreakdownPanel(panel);
             } catch (Exception e) {
                 showError("Erreur", "Impossible d'afficher le breakdown: " + e.getMessage());
             }
         });
     }

    /**
     * Switch to Total mode (shows player total damages)
     */
    private void switchToTotalMode() {
        if (currentMode == DisplayMode.TOTAL) return;
        currentMode = DisplayMode.TOTAL;
        updateModeButtons();

        Platform.runLater(() -> {
            mainUI.setCenterContent(playersContainer);
            // Refresh display with last model
            if (lastModel != null) {
                refresh(lastModel);
            }
        });
    }

    /**
     * Switch to Tour mode (shows per-round breakdown)
     */
    private void switchToTourMode() {
        if (currentMode == DisplayMode.TOUR) return;
        currentMode = DisplayMode.TOUR;
        updateModeButtons();

        Platform.runLater(() -> {
            if (turnDetailsUI == null) {
                turnDetailsUI = new TurnDetailsPane(primaryStage, this::showTurnBreakdownInRightPane);
            }
            mainUI.setCenterContent(turnDetailsUI.getContent());
            if (lastModel != null) {
                turnDetailsUI.update(lastModel, playerColors);
            }
        });
    }

    /**
     * Update button styles to show which mode is selected
     */
    private void updateModeButtons() {
        Platform.runLater(() -> {
            if (currentMode == DisplayMode.TOTAL) {
                totalBtn.setStyle("-fx-background-color: #4a9eff; -fx-text-fill: white; -fx-font-weight: bold;");
                tourBtn.setStyle("");
            } else {
                totalBtn.setStyle("");
                tourBtn.setStyle("-fx-background-color: #4a9eff; -fx-text-fill: white; -fx-font-weight: bold;");
            }
        });
    }

    /**
     * Shows breakdown for a specific round/player in the right pane
     */
    private void showTurnBreakdownInRightPane(int roundNumber, PlayerStats stats) {
        Platform.runLater(() -> {
            try {
                // Remember the selected player and round for auto-refresh on model updates
                currentSelectedPlayer = stats;
                currentSelectedRound = roundNumber;
                Pane panel = TurnBreakdownPane.buildPanel(roundNumber, stats);
                mainUI.setBreakdownPanel(panel);
            } catch (Exception e) {
                showError("Erreur", "Impossible d'afficher le breakdown du tour: " + e.getMessage());
            }
        });
    }
}
