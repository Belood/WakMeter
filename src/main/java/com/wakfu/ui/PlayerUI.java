package com.wakfu.ui;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

public class PlayerUI {

    private final String playerName;
    private final int totalDamage;
    private final double percentage;

    public PlayerUI(String playerName, int totalDamage, double percentage) {
        this.playerName = playerName;
        this.totalDamage = totalDamage;
        this.percentage = percentage;
    }

    /**
     * Crée et renvoie la ligne JavaFX correspondante à ce joueur.
     */
    public HBox render() {
        Label nameLabel = new Label(playerName);
        nameLabel.setMinWidth(150);

        ProgressBar bar = new ProgressBar(percentage);
        bar.setPrefWidth(250);

        Label valueLabel = new Label(String.format("%,d (%2.1f%%)", totalDamage, percentage * 100));

        HBox hbox = new HBox(10, nameLabel, bar, valueLabel);
        hbox.setPadding(new Insets(5, 0, 5, 0));

        return hbox;
    }
}