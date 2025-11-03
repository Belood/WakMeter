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

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.TOP_LEFT);
        container.setBackground(Background.EMPTY);

        Label title = new Label("Repartition des degats - " + playerName);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        container.getChildren().add(title);

        // Calculate dynamic column width using UIUtils
        double spellNameColumnWidth = UIUtils.calculateDynamicColumnWidth(spells.keySet());

        // Header grid
        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setAlignment(Pos.CENTER_LEFT);

        // Use UIUtils to create column constraints
        ColumnConstraints[] headerColumns = UIUtils.createBreakdownColumns(spellNameColumnWidth);
        headerGrid.getColumnConstraints().addAll(java.util.Arrays.asList(headerColumns));

        Label hName = new Label("Sort");
        hName.setPrefWidth(60);
        Label hBar = new Label("");
        hBar.setBackground(Background.EMPTY);
        GridPane.setHgrow(hBar, Priority.ALWAYS);
        Label hDmg = new Label("Degats");
        hDmg.setPrefWidth(50);
        Label hDmgPerPA = new Label("Degat/PA");
        hDmgPerPA.setPrefWidth(60);
        Label hPct = new Label("%");
        hPct.setPrefWidth(40);

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
        final String playerClassKey = null;

        // Sort spells by damage descending
        spells.values().stream()
            .sorted((a, b) -> Integer.compare(b.getTotal(), a.getTotal()))
            .forEach(sp -> {
                String spellName = sp.getName();
                int dmg = sp.getTotal();
                double pct = (double) dmg / finalTotal;

                // Get dominant element using UIUtils
                Element element = UIUtils.getDominantElement(sp.getDamageByElement());

                // Create row grid
                GridPane rowGrid = new GridPane();
                rowGrid.setHgap(10);

                // Use UIUtils to create column constraints
                ColumnConstraints[] rowColumns = UIUtils.createBreakdownColumns(spellNameColumnWidth);
                rowGrid.getColumnConstraints().addAll(java.util.Arrays.asList(rowColumns));

                Label spellLabel = new Label(spellName);
                spellLabel.setPrefWidth(spellNameColumnWidth);

                // Create progress bar using UIUtils
                StackPane barPane = UIUtils.createProgressBar(pct, element, 12, 6);
                GridPane.setHgrow(barPane, Priority.ALWAYS);

                Label dmgLabel = new Label(String.format("%,d", dmg));
                dmgLabel.setPrefWidth(50);

                // Calculate Degat/PA
                String dmgPerPaText = "-";
                Integer cost = SpellCostProvider.getCostFor(playerClassKey, spellName);
                int castCount = sp.getCastCount();
                if (cost != null && cost > 0 && castCount > 0) {
                    dmgPerPaText = String.format("%.2f", (double) dmg / (cost * castCount));
                }
                Label dmgPerPaLabel = new Label(dmgPerPaText);
                dmgPerPaLabel.setPrefWidth(70);

                Label pctLabel = new Label(String.format("%.1f%%", pct * 100));
                pctLabel.setPrefWidth(60);

                rowGrid.add(spellLabel, 0, 0);
                rowGrid.add(barPane, 1, 0);
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

