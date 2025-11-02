package com.wakfu.ui.turn;

import com.wakfu.domain.model.FightModel;
import com.wakfu.domain.model.PlayerStats;
import com.wakfu.domain.model.RoundModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Fenêtre affichant les dégâts par round (par joueur) avec possibilité de filtrer par joueur.
 */
public class TurnDetailsPane {

    @FunctionalInterface
    public interface OnBreakdownCallback {
        void onBreakdown(int roundNumber, PlayerStats stats);
    }

    private final Stage stage;
    private final VBox content;
    private final ComboBox<String> playerFilter;
    private FightModel model;
    private Map<String, javafx.scene.paint.Color> playerColors = new ConcurrentHashMap<>();
    private final OnBreakdownCallback onBreakdownCallback;

    public TurnDetailsPane(Stage owner, OnBreakdownCallback callback) {
        this.stage = new Stage();
        this.content = new VBox(8);
        this.playerFilter = new ComboBox<>();
        this.onBreakdownCallback = callback;
        setupUI(owner);
    }

    private void setupUI(Stage owner) {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));

        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Détails par tour");
        title.setStyle("-fx-font-weight:bold; -fx-font-size:14px;");

        playerFilter.setPromptText("Filtrer par joueur");
        playerFilter.setOnAction(e -> { System.identityHashCode(e); refreshContent(); });

        top.getChildren().addAll(title, playerFilter);
        root.setTop(top);

        ScrollPane sc = new ScrollPane(content);
        sc.setFitToWidth(true);
        sc.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(sc);

        Scene scene = new Scene(root, 520, 600);
        var css = getClass().getResource("/dark-theme.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
        stage.initOwner(owner);
        stage.setTitle("Details par tours");
    }

    public void show() {
        stage.show();
    }

    public void update(FightModel model, Map<String, javafx.scene.paint.Color> colors) {
        this.model = model;
        this.playerColors = colors != null ? colors : new ConcurrentHashMap<>();
        // populate player filter
        List<String> players = model.getStatsByPlayer().values().stream()
                .map(ps -> ps.getPlayer().getName())
                .distinct()
                .collect(Collectors.toList());
        Platform.runLater(() -> {
            playerFilter.getItems().setAll(players);
            refreshContent();
        });
    }

    public VBox getContent() {
        return content;
    }

    private void refreshContent() {
        Platform.runLater(() -> {
            content.getChildren().clear();
            if (model == null) return;
            String selected = playerFilter.getValue();

            // For each round, show a sub-section with damages per player
            for (RoundModel round : model.getRounds()) {
                VBox roundBox = new VBox(6);
                roundBox.setPadding(new Insets(6));
                Label rTitle = new Label("Tour " + round.getRoundNumber());
                rTitle.setStyle("-fx-font-weight:bold;");
                roundBox.getChildren().add(rTitle);

                GridPane grid = new GridPane();
                grid.setHgap(5);
                grid.setVgap(3);

                ColumnConstraints col0 = new ColumnConstraints(90);
                col0.setMinWidth(90); col0.setMaxWidth(90);
                ColumnConstraints col1 = new ColumnConstraints(); col1.setHgrow(Priority.ALWAYS);
                ColumnConstraints col2 = new ColumnConstraints(60); col2.setMinWidth(60); col2.setMaxWidth(60);
                ColumnConstraints col3 = new ColumnConstraints(60); col3.setMinWidth(60); col3.setMaxWidth(60);
                ColumnConstraints col4 = new ColumnConstraints(48); col4.setMinWidth(48); col4.setMaxWidth(48);
                grid.getColumnConstraints().addAll(java.util.Arrays.asList(col0, col1, col2, col3, col4));

                int row = 0;
                Map<String, PlayerStats> playerStats = round.getPlayerStatsByRound();
                int totalRound = playerStats.values().stream()
                        .mapToInt(PlayerStats::getTotalDamage)
                        .sum();
                if (totalRound == 0) totalRound = 1;

                // sort players by damage desc
                List<Map.Entry<String, PlayerStats>> entries = playerStats.entrySet().stream()
                        .sorted((a,b) -> Integer.compare(b.getValue().getTotalDamage(), a.getValue().getTotalDamage()))
                        .toList();

                final int currentRound = round.getRoundNumber();
                for (Map.Entry<String, PlayerStats> e : entries) {
                    String player = e.getKey();
                    if (selected != null && !selected.equals(player)) continue;
                    PlayerStats stats = e.getValue();
                    int dmg = stats.getTotalDamage();
                    double pct = (double) dmg / totalRound;

                    Label name = new Label(player);
                    name.setPrefWidth(90);
                    name.setAlignment(Pos.CENTER_LEFT);

                    // Bar container with proportional width
                    HBox barContainer = new HBox();
                    barContainer.setMinWidth(0);
                    barContainer.setMaxWidth(Double.MAX_VALUE);
                    barContainer.setAlignment(Pos.CENTER_LEFT);

                    // Track + Fill
                    StackPane bar = new StackPane();
                    bar.setMinWidth(0);
                    bar.setMaxWidth(Double.MAX_VALUE);
                    bar.setPrefHeight(12);

                    Region track = new Region();
                    track.setMinWidth(0);
                    track.setPrefHeight(12);
                    track.setBackground(new Background(new BackgroundFill(Color.rgb(0, 0, 0, 0.10), new CornerRadii(6), Insets.EMPTY)));

                    Region fill = new Region();
                    fill.setMinWidth(0);
                    fill.setPrefHeight(12);
                    javafx.scene.paint.Color color = getColorForPlayer(player);
                    fill.setBackground(new Background(new BackgroundFill(color, new CornerRadii(6), Insets.EMPTY)));
                    StackPane.setAlignment(fill, Pos.CENTER_LEFT);

                    track.prefWidthProperty().bind(bar.widthProperty());
                    fill.prefWidthProperty().bind(track.widthProperty());

                    bar.getChildren().addAll(track, fill);
                    HBox.setHgrow(bar, Priority.NEVER);
                    bar.prefWidthProperty().bind(barContainer.widthProperty().multiply(pct));

                    barContainer.getChildren().add(bar);
                    GridPane.setHgrow(barContainer, Priority.ALWAYS);

                    Label val = new Label(String.format("%,d", dmg));
                    val.setPrefWidth(60);
                    val.setAlignment(Pos.CENTER_RIGHT);

                    Label pctLabel = new Label(String.format("%.1f%%", pct * 100));
                    pctLabel.setPrefWidth(60);
                    pctLabel.setAlignment(Pos.CENTER_RIGHT);

                    javafx.scene.control.Button details = new javafx.scene.control.Button("🔍");
                    details.setTooltip(new javafx.scene.control.Tooltip("Breakdown"));
                    details.setMinWidth(48);
                    details.setOnAction(evt -> {
                        if (onBreakdownCallback != null) {
                            onBreakdownCallback.onBreakdown(currentRound, stats);
                        }
                    });

                    grid.add(name, 0, row);
                    grid.add(barContainer, 1, row);
                    grid.add(val, 2, row);
                    grid.add(pctLabel, 3, row);
                    grid.add(details, 4, row);
                    row++;
                }

                roundBox.getChildren().add(grid);
                content.getChildren().add(roundBox);
            }
        });
    }

    private Color getColorForPlayer(String player) {
        return playerColors.computeIfAbsent(player, p -> {
            int hash = Math.abs(p.hashCode());
            double hue = hash % 360;
            double saturation = 0.7 + (hash % 30) / 100.0;
            double brightness = 0.9 - (hash % 30) / 100.0;
            return Color.hsb(hue, saturation, brightness);
        });
    }
}
