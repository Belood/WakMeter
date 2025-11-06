package com.wakfu.ui.turn;
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
 * TurnBreakdownPane renders damage breakdown statistics for a specific round.
 * Similar to BreakdownPane but for per-round data.
 */
public class TurnBreakdownPane {
    private TurnBreakdownPane() {
        // Utility class - no instantiation
    }
    /**
     * Builds a panel showing damage breakdown for a player in a specific round.
     * Designed to be embedded in MainUI's right pane.
     */
    public static Pane buildPanel(int roundNumber, PlayerStats playerStats) {
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
        Label title = new Label("Tour " + roundNumber + " - " + playerName);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        container.getChildren().add(title);
        // Calculate dynamic column width using UIUtils
        double spellNameColumnWidth = UIUtils.calculateDynamicColumnWidth(spells.keySet());
        // Header grid matching TotalBreakdownPane
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
        Label hCasts = new Label("Casts");
        Label hPct = new Label("%");
        headerGrid.add(hName, 0, 0);
        headerGrid.add(hBar, 1, 0);
        headerGrid.add(hDmg, 2, 0);
        headerGrid.add(hDmgPerPA, 3, 0);
        headerGrid.add(hCasts, 4, 0);
        headerGrid.add(hPct, 5, 0);
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
                // Create row grid matching TotalBreakdownPane
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
                // Nombre de casts
                Label castsLabel = new Label(String.valueOf(sp.getCastCount()));
                castsLabel.setAlignment(Pos.CENTER_RIGHT);
                Label pctLabel = new Label(String.format("%.1f%%", pct * 100));
                pctLabel.setAlignment(Pos.CENTER_RIGHT);
                rowGrid.add(spellLabel, 0, 0);
                rowGrid.add(barContainer, 1, 0);
                rowGrid.add(dmgLabel, 2, 0);
                rowGrid.add(dmgPerPaLabel, 3, 0);
                rowGrid.add(castsLabel, 4, 0);
                rowGrid.add(pctLabel, 5, 0);
                list.getChildren().add(rowGrid);
            });
        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setBackground(Background.EMPTY);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        container.getChildren().add(scrollPane);

        // Section Bonus Damages
        Map<String, com.wakfu.domain.model.BonusEffectStats> bonusEffects = new java.util.LinkedHashMap<>();
        try {
            if (playerStats != null) {
                Map<String, com.wakfu.domain.model.BonusEffectStats> rawBonus = playerStats.getBonusEffects();
                if (rawBonus != null && !rawBonus.isEmpty()) {
                    bonusEffects = rawBonus;
                }
            }
        } catch (Exception ignored) {
        }

        if (!bonusEffects.isEmpty()) {
            Label bonusTitle = new Label("Degats Bonus");
            bonusTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 0 0 0;");
            container.getChildren().add(bonusTitle);

            // Calculer le total global (sorts + bonus) pour le calcul du pourcentage
            int totalSpellDamage = spells.values().stream().mapToInt(SpellStats::getTotal).sum();
            int totalBonusDamage = bonusEffects.values().stream().mapToInt(com.wakfu.domain.model.BonusEffectStats::getTotal).sum();
            int grandTotal = totalSpellDamage + totalBonusDamage;
            if (grandTotal == 0) grandTotal = 1; // Éviter division par zéro

            int maxBonusDamage = bonusEffects.values().stream()
                .mapToInt(com.wakfu.domain.model.BonusEffectStats::getTotal)
                .max()
                .orElse(1);

            double bonusNameColumnWidth = UIUtils.calculateDynamicColumnWidth(bonusEffects.keySet());

            GridPane bonusHeaderGrid = new GridPane();
            bonusHeaderGrid.setHgap(2);
            bonusHeaderGrid.setAlignment(Pos.CENTER_LEFT);

            ColumnConstraints[] bonusHeaderColumns = UIUtils.createBreakdownColumns(bonusNameColumnWidth);
            bonusHeaderGrid.getColumnConstraints().addAll(java.util.Arrays.asList(bonusHeaderColumns));

            Label hBonusName = new Label("Effet");
            Label hBonusBar = new Label("");
            hBonusBar.setBackground(Background.EMPTY);
            GridPane.setHgrow(hBonusBar, Priority.ALWAYS);
            Label hBonusDmg = new Label("Degats");
            Label hBonusDmgPerPA = new Label("Degat/PA");
            Label hBonusCasts = new Label("Casts");
            Label hBonusPct = new Label("%");

            bonusHeaderGrid.add(hBonusName, 0, 0);
            bonusHeaderGrid.add(hBonusBar, 1, 0);
            bonusHeaderGrid.add(hBonusDmg, 2, 0);
            bonusHeaderGrid.add(hBonusDmgPerPA, 3, 0);
            bonusHeaderGrid.add(hBonusCasts, 4, 0);
            bonusHeaderGrid.add(hBonusPct, 5, 0);
            bonusHeaderGrid.setBackground(Background.EMPTY);
            container.getChildren().add(bonusHeaderGrid);

            VBox bonusList = new VBox(6);
            bonusList.setBackground(Background.EMPTY);

            final int finalGrandTotal = grandTotal;
            final int finalMaxBonusDamage = maxBonusDamage;

            bonusEffects.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotal(), a.getTotal()))
                .forEach(bonus -> {
                    String effectName = bonus.getEffectName();
                    int dmg = bonus.getTotal();
                    double pct = (double) dmg / finalGrandTotal; // Pourcentage par rapport au total global
                    double barPct = (double) dmg / finalMaxBonusDamage;

                    Element element = UIUtils.getDominantElement(bonus.getDamageByElement());

                    GridPane rowGrid = new GridPane();
                    rowGrid.setHgap(2);

                    ColumnConstraints[] rowColumns = UIUtils.createBreakdownColumns(bonusNameColumnWidth);
                    rowGrid.getColumnConstraints().addAll(java.util.Arrays.asList(rowColumns));

                    Label effectLabel = new Label(effectName);
                    effectLabel.setAlignment(Pos.CENTER_LEFT);

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

                    Label dmgPerPaLabel = new Label("-");
                    dmgPerPaLabel.setAlignment(Pos.CENTER_RIGHT);

                    Label castsLabel = new Label("-");
                    castsLabel.setAlignment(Pos.CENTER_RIGHT);

                    Label pctLabel = new Label(String.format("%.1f%%", pct * 100));
                    pctLabel.setAlignment(Pos.CENTER_RIGHT);

                    rowGrid.add(effectLabel, 0, 0);
                    rowGrid.add(barContainer, 1, 0);
                    rowGrid.add(dmgLabel, 2, 0);
                    rowGrid.add(dmgPerPaLabel, 3, 0);
                    rowGrid.add(castsLabel, 4, 0);
                    rowGrid.add(pctLabel, 5, 0);
                    bonusList.getChildren().add(rowGrid);
                });

            ScrollPane bonusScrollPane = new ScrollPane(bonusList);
            bonusScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
            bonusScrollPane.setBackground(Background.EMPTY);
            bonusScrollPane.setFitToWidth(true);
            bonusScrollPane.setPrefHeight(200);
            container.getChildren().add(bonusScrollPane);
        }

        return container;
    }
}
