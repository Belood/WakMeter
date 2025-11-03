package com.wakfu.ui.overall;

import com.wakfu.data.SpellCostProvider;
import com.wakfu.domain.model.SpellStats;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.model.PlayerStats;
import com.wakfu.ui.util.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Map;

/**
 * TotalBreakdownPane renders damage breakdown statistics in a reusable Pane format.
 * This component displays spell damage breakdown with colored bars and percentages.
 */
public class TotalBreakdownPane {

    private TotalBreakdownPane() {
        // Utility class - no instantiation
    }

    /**
     * Builds a panel showing damage breakdown for a player.
     * Designed to be embedded in MainUI's right pane.
     */
    public static Pane buildPanel(PlayerStats playerStats) {
        Map<String, SpellStats> spells = new java.util.LinkedHashMap<>();
        String playerName = "Joueur";

        // Extract player name
        try {
            if (playerStats != null && playerStats.getPlayer() != null) {
                playerName = playerStats.getPlayer().getName();
            }
        } catch (Exception ignored) {
        }

        // Extract spells
        try {
            if (playerStats != null) {
                Map<String, SpellStats> rawSpells = playerStats.getSpells();
                if (rawSpells != null) {
                    spells = rawSpells;
                }
            }
        } catch (Exception ignored) {
        }

        int total = spells.values().stream().mapToInt(SpellStats::getTotal).sum();
        if (total == 0) total = 1;

        // Trouver le sort avec le plus de dégâts
        int maxDamage = spells.values().stream()
            .mapToInt(SpellStats::getTotal)
            .max()
            .orElse(1);

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.TOP_LEFT);
        container.setBackground(Background.EMPTY);

        Label title = new Label("Répartition des degats - " + playerName);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        container.getChildren().add(title);

        // Calculate dynamic column width using UIUtils
        double spellNameColumnWidth = UIUtils.calculateDynamicColumnWidth(spells.keySet());

        // Header grid
        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(2);
        headerGrid.setAlignment(Pos.CENTER_LEFT);

        // Use UIUtils to create column constraints
        ColumnConstraints[] headerColumns = UIUtils.createBreakdownColumns(spellNameColumnWidth);
        headerGrid.getColumnConstraints().addAll(java.util.Arrays.asList(headerColumns));

        Label hName = new Label("Sort");
        Label hBar = new Label("");
        hBar.setBackground(Background.EMPTY);
        GridPane.setHgrow(hBar, Priority.ALWAYS);
        Label hDmg = new Label("Degats");
        Label hDmgPerPA = new Label("Degat/PA");
        Label hPct = new Label("%");

        headerGrid.add(hName, 0, 0);
        headerGrid.add(hBar, 1, 0);
        headerGrid.add(hDmg, 2, 0);
        headerGrid.add(hDmgPerPA, 3, 0);
        headerGrid.add(hPct, 4, 0);
        headerGrid.setBackground(Background.EMPTY);
        container.getChildren().add(headerGrid);

        VBox list = new VBox(6);
        list.setBackground(Background.EMPTY);

        final int finalTotal = total;
        final int finalMaxDamage = maxDamage;

        // Sort spells by damage descending
        spells.values().stream()
            .sorted((a, b) -> Integer.compare(b.getTotal(), a.getTotal()))
            .forEach(sp -> {
                String spellName = sp.getName();
                int dmg = sp.getTotal();
                double pct = (double) dmg / finalTotal;
                double barPct = (double) dmg / finalMaxDamage; // Proportionnel au max

                // Get dominant element using UIUtils
                Element element = UIUtils.getDominantElement(sp.getDamageByElement());

                // Create row grid
                GridPane rowGrid = new GridPane();
                rowGrid.setHgap(2);

                // Use UIUtils to create column constraints
                ColumnConstraints[] rowColumns = UIUtils.createBreakdownColumns(spellNameColumnWidth);
                rowGrid.getColumnConstraints().addAll(java.util.Arrays.asList(rowColumns));

                Label spellLabel = new Label(spellName);
                spellLabel.setAlignment(Pos.CENTER_LEFT);

                // Bar container avec largeur proportionnelle
                HBox barContainer = new HBox();
                barContainer.setMinWidth(0);
                barContainer.setMaxWidth(Double.MAX_VALUE);
                barContainer.setAlignment(Pos.CENTER_LEFT);

                StackPane barPane = new StackPane();
                barPane.setMinWidth(0);
                barPane.setMaxWidth(Double.MAX_VALUE);
                barPane.setPrefHeight(12);

                Region track = new Region();
                track.setMinWidth(0);
                track.setPrefHeight(12);
                track.setBackground(UIUtils.createBackground(Color.rgb(0, 0, 0, 0.10), 6));

                Region fill = new Region();
                fill.setMinWidth(0);
                fill.setPrefHeight(12);
                fill.setBackground(UIUtils.createBackground(UIUtils.getElementColor(element), 6));
                StackPane.setAlignment(fill, Pos.CENTER_LEFT);

                track.prefWidthProperty().bind(barPane.widthProperty());
                fill.prefWidthProperty().bind(track.widthProperty());

                barPane.getChildren().addAll(track, fill);
                HBox.setHgrow(barPane, Priority.NEVER);
                barPane.prefWidthProperty().bind(barContainer.widthProperty().multiply(barPct));

                barContainer.getChildren().add(barPane);
                GridPane.setHgrow(barContainer, Priority.ALWAYS);

                Label dmgLabel = new Label(String.format("%,d", dmg));
                dmgLabel.setAlignment(Pos.CENTER_RIGHT);

                // Calculate Degat/PA using effectivePACost
                String dmgPerPaText = "-";
                Integer effectiveCost = sp.getEffectivePACost();
                if (effectiveCost != null && effectiveCost > 0) {
                    dmgPerPaText = String.format("%.2f", (double) dmg / effectiveCost);
                }
                Label dmgPerPaLabel = new Label(dmgPerPaText);
                dmgPerPaLabel.setAlignment(Pos.CENTER_RIGHT);

                Label pctLabel = new Label(String.format("%.1f%%", pct * 100));
                pctLabel.setAlignment(Pos.CENTER_RIGHT);

                rowGrid.add(spellLabel, 0, 0);
                rowGrid.add(barContainer, 1, 0);
                rowGrid.add(dmgLabel, 2, 0);
                rowGrid.add(dmgPerPaLabel, 3, 0);
                rowGrid.add(pctLabel, 4, 0);
                list.getChildren().add(rowGrid);
            });

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setBackground(Background.EMPTY);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        container.getChildren().add(scrollPane);

        return container;
    }
}
