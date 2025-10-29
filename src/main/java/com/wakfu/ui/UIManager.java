package com.wakfu.ui;

import com.wakfu.domain.actors.Player;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
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

    public UIManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.mainContainer = new VBox(10);
        this.statusLabel = new Label("En attente de combat...");
        this.playerGrid = new GridPane();

        setupUI();
    }

    private void setupUI() {
        mainContainer.setPadding(new Insets(10));
        mainContainer.getChildren().addAll(statusLabel, playerGrid);

        Scene scene = new Scene(mainContainer, 600, 400);
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
     * Rafraîchit l'affichage de la liste des joueurs.
     */
    public void displayPlayerStats(List<Player> players, int totalDamage) {
        Platform.runLater(() -> {
            playerGrid.getChildren().clear();

            int row = 0;
            for (Player p : players) {
                double pct = totalDamage > 0
                        ? (double) p.getTotalDamage() / totalDamage
                        : 0.0;

                PlayerUI playerUI = new PlayerUI(
                        p.getName(),
                        p.getTotalDamage(),
                        pct
                );

                HBox rowBox = playerUI.render();
                playerGrid.add(rowBox, 0, row++);
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