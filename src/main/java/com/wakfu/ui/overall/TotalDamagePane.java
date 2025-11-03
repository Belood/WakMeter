package com.wakfu.ui.overall;

import com.wakfu.domain.actors.Player;
import com.wakfu.domain.model.PlayerStats;
import com.wakfu.ui.util.UIUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.Consumer;

public class TotalDamagePane {

    private final PlayerStats stats;
    private final double pct;                 // 0..1 percentage dans la barre principale
    private final double damagePercentage;   // 0..1 percentage du track de dégâts
    private final Color barColor;
    private final Color trackColor;
    private final Consumer<PlayerStats> onBreakdown;

    public TotalDamagePane(PlayerStats stats, double percentage, Color barColor, Consumer<PlayerStats> onBreakdownRequested) {
        this(stats, percentage, barColor, percentage, onBreakdownRequested);
    }

    public TotalDamagePane(PlayerStats stats, double percentage, Color barColor, double damagePercentage, Consumer<PlayerStats> onBreakdownRequested) {
        this(stats, percentage, barColor, barColor, damagePercentage, onBreakdownRequested);
    }

    public TotalDamagePane(PlayerStats stats, double percentage, Color barColor, Color trackColor, double damagePercentage, Consumer<PlayerStats> onBreakdownRequested) {
        this.stats = stats;
        this.pct = UIUtils.clamp01(percentage);
        this.damagePercentage = UIUtils.clamp01(damagePercentage);
        this.barColor = barColor == null ? Color.web("#4b8cff") : barColor;
        this.trackColor = trackColor == null ? Color.web("#4b8cff") : trackColor;
        this.onBreakdown = onBreakdownRequested;
    }

    /** Layout : [Name (fixed)] [Bar (HGrow ALWAYS)] [Value (fixed)] [Pct (fixed)] [🔍 (fixed)] */
    public HBox render() {
        Player p = stats.getPlayer();

        GridPane row = new GridPane();
        row.setHgap(5);
        row.setAlignment(Pos.CENTER_LEFT);

        // Use UIUtils to create column constraints
        ColumnConstraints[] columns = UIUtils.createPlayerDamageColumns();
        row.getColumnConstraints().addAll(List.of(columns));

        // [Name]
        Label name = labelLeft(p.getName(), 90);

        // [Bar container] — holds the bar with width proportional to player damage
        HBox barContainer = new HBox();
        barContainer.setMinWidth(0);
        barContainer.setMaxWidth(Double.MAX_VALUE);
        barContainer.setAlignment(Pos.CENTER_LEFT);
        barContainer.setSpacing(5);

        // [Bar] — track (background) + damageTrack (pourcentage de dégâts) + fill (percentage principal)
        StackPane bar = new StackPane();
        bar.setMinWidth(0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(12);

        Region track = new Region();
        track.setMinWidth(0);
        track.setPrefHeight(8);
        track.setBackground(bg(Color.rgb(0, 0, 0, 0.10), 6));

        Region damageTrack = new Region();
        damageTrack.setMinWidth(0);
        damageTrack.setPrefHeight(8);
        damageTrack.setBackground(bg(trackColor, 6));
        damageTrack.setOpacity(0.35);
        StackPane.setAlignment(damageTrack, Pos.CENTER_LEFT);

        Region fill = new Region();
        fill.setMinWidth(0);
        fill.setPrefHeight(8);
        fill.setBackground(bg(barColor, 6));
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        // largeur de la barre liée au conteneur
        track.prefWidthProperty().bind(bar.widthProperty());
        damageTrack.prefWidthProperty().bind(track.widthProperty().multiply(damagePercentage));
        // Fill takes 100% of the bar width (since pct now controls the bar width itself)
        fill.prefWidthProperty().bind(track.widthProperty());

        bar.getChildren().addAll(track, damageTrack, fill);

        // Set the bar width to be proportional to the damage percentage
        HBox.setHgrow(bar, Priority.NEVER);
        bar.prefWidthProperty().bind(barContainer.widthProperty().multiply(pct));

        barContainer.getChildren().add(bar);
        GridPane.setHgrow(barContainer, Priority.ALWAYS);

        // [Value]
        Label value = labelRight(String.format("%,d", stats.getTotalDamage()), 90);

        // [%]
        Label percent = labelRight(String.format("%.1f%%", pct * 100), 70);

        // [🔍]
        Button details = new Button("🔍");
        details.setTooltip(new Tooltip("Breakdown"));
        details.setMinWidth(48);
        details.setOnAction(e -> {
            if (onBreakdown != null) {
                onBreakdown.accept(stats);
            }
        });

        // Ajout d'une marge droite de 10 après le bouton details
        GridPane.setMargin(details, new Insets(0, 10, 0, 0));

        // Placement colonnes
        row.add(name,        0, 0);
        row.add(barContainer, 1, 0);
        row.add(value,       2, 0);
        row.add(percent,     3, 0);
        row.add(details,     4, 0);

        HBox wrapper = new HBox(row);
        wrapper.setAlignment(Pos.CENTER_LEFT);
        wrapper.setPadding(new Insets(2, 0, 2, 15));
        HBox.setHgrow(row, Priority.ALWAYS);
        return wrapper;
    }

    /* ---------- Helpers compacts ---------- */

    private static Label labelLeft(String text, double w) {
        Label l = new Label(text);
        l.setPrefWidth(w);
        l.setAlignment(Pos.CENTER_LEFT);
        return l;
    }

    private static Label labelRight(String text, double w) {
        Label l = new Label(text);
        l.setPrefWidth(w);
        l.setAlignment(Pos.CENTER_RIGHT);
        return l;
    }

    private static Background bg(Color c, double radius) {
        return UIUtils.createBackground(c, radius);
    }
}