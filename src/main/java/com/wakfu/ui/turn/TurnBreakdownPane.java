package com.wakfu.ui.turn;

import com.wakfu.data.SpellCostProvider;
import com.wakfu.domain.model.SpellStats;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.model.PlayerStats;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
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

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.TOP_LEFT);
        container.setBackground(Background.EMPTY);

        Label title = new Label("Tour " + roundNumber + " - " + playerName);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        container.getChildren().add(title);

        // Header grid
        GridPane header = new GridPane();
        header.setHgap(8);
        header.getColumnConstraints().addAll(
            colFixed(150),
            colGrow(),
            colFixed(80),
            colFixed(60),
            colFixed(40)
        );

        Label hName = new Label("Sort");
        hName.setStyle("-fx-font-weight: bold;");
        Label hDmg = new Label("Dégâts");
        hDmg.setStyle("-fx-font-weight: bold;");
        Label hPct = new Label("%");
        hPct.setStyle("-fx-font-weight: bold;");
        Label hPA = new Label("PA");
        hPA.setStyle("-fx-font-weight: bold;");

        header.add(hName, 0, 0);
        header.add(new Region(), 1, 0);
        header.add(hDmg, 2, 0);
        header.add(hPct, 3, 0);
        header.add(hPA, 4, 0);

        container.getChildren().add(header);

        // Sort spells by damage descending
        var sortedSpells = spells.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue().getTotal(), a.getValue().getTotal()))
            .toList();

        for (Map.Entry<String, SpellStats> entry : sortedSpells) {
            SpellStats spell = entry.getValue();
            int spellTotal = spell.getTotal();
            if (spellTotal == 0) continue;

            double pct = (double) spellTotal / total;
            String spellName = spell.getName();
            Integer cost = SpellCostProvider.getCostFor(null, spellName);

            GridPane row = new GridPane();
            row.setHgap(8);
            row.getColumnConstraints().addAll(
                colFixed(150),
                colGrow(),
                colFixed(80),
                colFixed(60),
                colFixed(40)
            );

            Label name = new Label(spellName);
            name.setMaxWidth(150);
            name.setStyle("-fx-text-overflow: ellipsis;");

            // Bar
            StackPane barPane = new StackPane();
            barPane.setMaxWidth(Double.MAX_VALUE);
            barPane.setPrefHeight(14);

            Region track = new Region();
            track.setPrefHeight(14);
            track.setMaxWidth(Double.MAX_VALUE);
            track.setBackground(bg(Color.rgb(60, 60, 60), 4));

            Region fill = new Region();
            fill.setPrefHeight(14);
            fill.setMaxWidth(Double.MAX_VALUE);
            Color barColor = getDominantElementColor(spell);
            fill.setBackground(bg(barColor, 4));
            StackPane.setAlignment(fill, Pos.CENTER_LEFT);

            track.prefWidthProperty().bind(barPane.widthProperty());
            fill.prefWidthProperty().bind(track.widthProperty().multiply(pct));

            barPane.getChildren().addAll(track, fill);
            GridPane.setHgrow(barPane, Priority.ALWAYS);

            Label dmgLabel = new Label(String.format("%,d", spellTotal));
            dmgLabel.setAlignment(Pos.CENTER_RIGHT);
            dmgLabel.setPrefWidth(80);

            Label pctLabel = new Label(String.format("%.1f%%", pct * 100));
            pctLabel.setAlignment(Pos.CENTER_RIGHT);
            pctLabel.setPrefWidth(60);

            Label paLabel = new Label(cost != null ? cost.toString() : "-");
            paLabel.setAlignment(Pos.CENTER);
            paLabel.setPrefWidth(40);

            row.add(name, 0, 0);
            row.add(barPane, 1, 0);
            row.add(dmgLabel, 2, 0);
            row.add(pctLabel, 3, 0);
            row.add(paLabel, 4, 0);

            container.getChildren().add(row);
        }

        return container;
    }

    private static ColumnConstraints colFixed(double width) {
        ColumnConstraints cc = new ColumnConstraints(width);
        cc.setMinWidth(width);
        cc.setMaxWidth(width);
        return cc;
    }

    private static ColumnConstraints colGrow() {
        ColumnConstraints cc = new ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        return cc;
    }

    private static Background bg(Color c, double radius) {
        return new Background(new BackgroundFill(c, new CornerRadii(radius), Insets.EMPTY));
    }

    private static Color getDominantElementColor(SpellStats spell) {
        Element dominant = null;
        int maxDamage = 0;

        for (Map.Entry<Element, Integer> entry : spell.getDamageByElement().entrySet()) {
            if (entry.getValue() > maxDamage) {
                maxDamage = entry.getValue();
                dominant = entry.getKey();
            }
        }

        return getColorForElement(dominant);
    }

    private static Color getColorForElement(Element elem) {
        if (elem == null) return Color.GRAY;
        return switch (elem) {
            case FEU -> Color.rgb(255, 87, 51);
            case EAU -> Color.rgb(51, 153, 255);
            case TERRE -> Color.rgb(139, 195, 74);
            case AIR -> Color.rgb(255, 235, 59);
            case STASIS -> Color.rgb(138, 43, 226);
            case LUMIERE -> Color.rgb(255, 215, 0);
            default -> Color.LIGHTGRAY;
        };
    }
}

