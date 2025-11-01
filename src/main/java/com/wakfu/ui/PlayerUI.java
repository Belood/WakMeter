package com.wakfu.ui;

import com.wakfu.domain.actors.Player;
import com.wakfu.model.PlayerStats;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class PlayerUI {

    private final PlayerStats playerStats;
    private final int totalDamage;
    private final double percentage;
    private final java.util.function.Consumer<PlayerStats> onBreakdownRequested;
    private final javafx.scene.paint.Color barColor;

    // note: barColor parameter is accepted for API compatibility but not stored because
    // the player bars intentionally have no visible fill per UX requirement.
    public PlayerUI(PlayerStats playerStats, double percentage, Color barColor) {
        this(playerStats, percentage, barColor, null);
    }

    public PlayerUI(PlayerStats playerStats, double percentage, Color barColor, java.util.function.Consumer<PlayerStats> onBreakdownRequested) {
        this.playerStats = playerStats;
        this.totalDamage = playerStats.getTotalDamage();
        this.percentage = percentage;
        this.onBreakdownRequested = onBreakdownRequested;
        this.barColor = barColor == null ? Color.web("#4b8cff") : barColor;
    }

    /**
     * Crée et renvoie la ligne JavaFX correspondante à ce joueur.
     * Layout via GridPane : [Name (fixed)] [Bar (HGrow ALWAYS)] [Value (fixed)] [Pct (fixed)] [🔍 (fixed)]
     */
    public HBox render() {
        Player player = playerStats.getPlayer();

        GridPane rowGrid = new GridPane();
        rowGrid.setHgap(10);
        rowGrid.setAlignment(Pos.CENTER_LEFT);

        ColumnConstraints col0 = new ColumnConstraints(150);
        col0.setMinWidth(150); col0.setMaxWidth(150);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setHgrow(Priority.ALWAYS);
        ColumnConstraints col2 = new ColumnConstraints(100); col2.setMinWidth(100); col2.setMaxWidth(100);
        ColumnConstraints col3 = new ColumnConstraints(70); col3.setMinWidth(70); col3.setMaxWidth(70);
        ColumnConstraints col4 = new ColumnConstraints(36); col4.setMinWidth(36); col4.setMaxWidth(36);
        rowGrid.getColumnConstraints().addAll(java.util.Arrays.asList(col0, col1, col2, col3, col4));

        Label nameLabel = new Label(player.getName());
        nameLabel.setPrefWidth(150); nameLabel.setMinWidth(150); nameLabel.setMaxWidth(150);
        nameLabel.setAlignment(Pos.CENTER_LEFT);

        // Track + Fill
        Region track = new Region();
        track.setPrefHeight(16);
        track.setBackground(new Background(new BackgroundFill(Color.rgb(255,255,255,0.04), new CornerRadii(8), Insets.EMPTY)));
        track.setMinWidth(0);
        // subtle border using player's color to provide a visual cue (fill remains transparent)
        track.setBorder(new Border(new BorderStroke(
                barColor.deriveColor(0, 1.0, 1.0, 0.35),
                BorderStrokeStyle.SOLID,
                new CornerRadii(8),
                new BorderWidths(2)
        )));

        Region fill = new Region();
        fill.setPrefHeight(16);
        // Visible fill using player color with some opacity
        javafx.scene.paint.Color fillColor = barColor.deriveColor(0, 1.0, 1.0, 0.85);
        fill.setBackground(new Background(new BackgroundFill(fillColor, new CornerRadii(8), Insets.EMPTY)));
        fill.setMinWidth(0);

        StackPane barPane = new StackPane(track, fill);
        barPane.setMinWidth(0);
        barPane.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(barPane, Priority.ALWAYS);

        // Bind widths so that fill width = track.width * percentage
        track.prefWidthProperty().bind(barPane.widthProperty());
        fill.prefWidthProperty().bind(track.widthProperty().multiply(Math.max(0.0, Math.min(1.0, percentage))));
        fill.maxWidthProperty().bind(track.widthProperty().multiply(Math.max(0.0, Math.min(1.0, percentage))));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        Label valueLabel = new Label(String.format("%,d", totalDamage));
        valueLabel.setPrefWidth(100); valueLabel.setMinWidth(100); valueLabel.setMaxWidth(100);
        valueLabel.setAlignment(Pos.CENTER_RIGHT);

        Label pctLabel = new Label(String.format("%2.1f%%", percentage * 100));
        pctLabel.setPrefWidth(70); pctLabel.setMinWidth(70); pctLabel.setMaxWidth(70);
        pctLabel.setAlignment(Pos.CENTER_RIGHT);

        Button breakdown = new Button("🔍");
        breakdown.setTooltip(new Tooltip("Breakdown"));
        breakdown.setMinWidth(36);
        breakdown.setOnAction(event -> {
            System.identityHashCode(event);
            if (onBreakdownRequested != null) {
                try { onBreakdownRequested.accept(playerStats); } catch (Exception ignored) {}
            } else {
                new DamageBreakdownUI(playerStats).show();
            }
        });

        rowGrid.add(nameLabel, 0, 0);
        rowGrid.add(barPane, 1, 0);
        rowGrid.add(valueLabel, 2, 0);
        rowGrid.add(pctLabel, 3, 0);
        rowGrid.add(breakdown, 4, 0);

        HBox wrapper = new HBox(rowGrid);
        wrapper.setPadding(new Insets(5, 0, 5, 0));
        wrapper.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(rowGrid, Priority.ALWAYS);

        return wrapper;
    }
}