package com.wakfu.ui;

import com.wakfu.domain.actors.Fighter;
import com.wakfu.domain.actors.Player;
import com.wakfu.service.DamageCalculator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

/**
 * Interface principale affichant les dégâts totaux des joueurs.
 */
public class UIManager {

    private final Stage primaryStage;
    private final VBox mainContainer;
    private final Label statusLabel;
    private final GridPane playerGrid;

    // === Nouveaux éléments ===
    private final Button resetButton;
    private final CheckBox autoResetCheck;
    private final DamageCalculator damageCalculator;

    private boolean autoResetEnabled = false;

    public UIManager(Stage primaryStage, DamageCalculator damageCalculator) {
        this.primaryStage = primaryStage;
        this.damageCalculator = damageCalculator;
        this.mainContainer = new VBox(10);
        this.statusLabel = new Label("En attente de combat...");
        this.playerGrid = new GridPane();

        this.resetButton = new Button("Reset");
        this.autoResetCheck = new CheckBox("Auto-reset au début de combat");

        setupUI();
    }

    private void setupUI() {
        // --- HEADER ---
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10));

        resetButton.setOnAction(e -> {
            resetData();
        });
        autoResetCheck.selectedProperty().addListener((obs, oldVal, newVal) -> autoResetEnabled = newVal);

        header.getChildren().addAll(resetButton, autoResetCheck, statusLabel);

        // --- Conteneur principal ---
        mainContainer.setPadding(new Insets(10));
        mainContainer.getChildren().addAll(header, playerGrid);

        Scene scene = new Scene(mainContainer, 700, 450);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Wakfu Damage Meter");
        primaryStage.show();
    }

    /**
     * Affiche un message simple (état ou notification).
     */
    public void showMessage(String title, String message) {
        Platform.runLater(() -> statusLabel.setText(title + " - " + message));
    }

    /**
     * Réinitialise toutes les données du Damage Calculator.
     */
    private void resetData() {
        if (damageCalculator != null) {
            damageCalculator.reset();
        }
        Platform.runLater(() -> {
            playerGrid.getChildren().clear();
            statusLabel.setText("En attente de combat...");
        });
    }

    /**
     * Réinitialisation automatique si activée.
     */
    public void handleCombatStart() {
        if (autoResetEnabled) {
            Platform.runLater(this::resetData);
        }
    }

    /**
     * Rafraîchit l'affichage de la liste des joueurs (ignore les ennemis).
     */
    public void displayPlayerStats(List<Player> players, int totalDamage) {
        Platform.runLater(() -> {
            playerGrid.getChildren().clear();

            int row = 0;
            for (Player p : players) {
                 if (p.getType() == Fighter.FighterType.PLAYER){
                    double pct = totalDamage > 0
                            ? (double) p.getTotalDamage() / totalDamage
                            : 0.0;

                    PlayerUI playerUI = new PlayerUI(
                            p.getName(),
                            p.getTotalDamage(),
                            pct
                    );

                    HBox rowBox = playerUI.render();

                    // --- Bouton Breakdown ---
                    Button breakdownButton = new Button("Breakdown");
                    breakdownButton.setOnAction(e -> new DamageBreakdownUI(p).show());

                    rowBox.getChildren().add(breakdownButton);
                    rowBox.setSpacing(10);

                    playerGrid.add(rowBox, 0, row++);
                }
            }
        });
    }

    /**
     * Affiche un message d'erreur simple.
     */
    public void showError(String title, String message) {
        Platform.runLater(() -> statusLabel.setText("❌ " + title + ": " + message));
    }
}
