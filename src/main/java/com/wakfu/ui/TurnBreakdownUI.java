package com.wakfu.ui;

import com.wakfu.domain.model.FightModel;
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
public class TurnBreakdownUI {

    private final Stage stage;
    private final VBox content;
    private final ComboBox<String> playerFilter;
    private FightModel model;
    private final Map<String, javafx.scene.paint.Color> playerColors = new ConcurrentHashMap<>();

    public TurnBreakdownUI(Stage owner) {
        this.stage = new Stage();
        this.content = new VBox(8);
        this.playerFilter = new ComboBox<>();
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
        stage.setTitle("Turn Breakdown");
    }

    public void show() {
        stage.show();
    }

    public void update(FightModel model) {
        this.model = model;
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

    private void refreshContent() {
        Platform.runLater(() -> {
            content.getChildren().clear();
            if (model == null) return;
            String selected = playerFilter.getValue();

            // For each round, show a sub-section with damages per player
            for (RoundModel round : model.getRounds()) {
                VBox roundBox = new VBox(6);
                roundBox.setPadding(new Insets(6));
                Label rTitle = new Label("Round " + round.getRoundNumber());
                rTitle.setStyle("-fx-font-weight:bold;");
                roundBox.getChildren().add(rTitle);

                GridPane grid = new GridPane();
                grid.setHgap(10);
                grid.setVgap(6);

                ColumnConstraints col0 = new ColumnConstraints(200);
                col0.setMinWidth(200); col0.setMaxWidth(200);
                ColumnConstraints col1 = new ColumnConstraints(); col1.setHgrow(Priority.ALWAYS);
                ColumnConstraints col2 = new ColumnConstraints(100); col2.setMinWidth(100); col2.setMaxWidth(100);
                ColumnConstraints col3 = new ColumnConstraints(60); col3.setMinWidth(60); col3.setMaxWidth(60);
                grid.getColumnConstraints().addAll(java.util.Arrays.asList(col0, col1, col2, col3));

                int row = 0;
                Map<String, Integer> damages = round.getDamageByPlayer();
                int totalRound = damages.values().stream().mapToInt(Integer::intValue).sum();
                if (totalRound == 0) totalRound = 1;

                // sort players by damage desc
                List<Map.Entry<String, Integer>> entries = damages.entrySet().stream()
                        .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                        .toList();

                for (Map.Entry<String, Integer> e : entries) {
                    String player = e.getKey();
                    if (selected != null && !selected.equals(player)) continue;
                    int dmg = e.getValue();
                    double pct = (double) dmg / totalRound;

                    Label name = new Label(player);
                    name.setPrefWidth(200);

                    // Track + Fill
                    Region track = new Region();
                    track.getStyleClass().add("bar-track");
                    track.setPrefHeight(12);
                    track.setMinWidth(0);

                    Region fill = new Region();
                    fill.getStyleClass().add("bar-fill");
                    fill.setPrefHeight(12);
                    javafx.scene.paint.Color color = getColorForPlayer(player);
                    fill.setBackground(new Background(new BackgroundFill(color.deriveColor(0,1.0,1.0,0.85), new CornerRadii(6), Insets.EMPTY)));
                    fill.setMinWidth(0);

                    StackPane barPane = new StackPane(track, fill);
                    barPane.setMinWidth(0);
                    barPane.setMaxWidth(Double.MAX_VALUE);
                    GridPane.setHgrow(barPane, Priority.ALWAYS);

                    // Bind widths
                    track.prefWidthProperty().bind(barPane.widthProperty());
                    fill.prefWidthProperty().bind(track.widthProperty().multiply(Math.max(0.0, Math.min(1.0, pct))));
                    fill.maxWidthProperty().bind(track.widthProperty().multiply(Math.max(0.0, Math.min(1.0, pct))));
                    StackPane.setAlignment(fill, Pos.CENTER_LEFT);

                    Label val = new Label(String.format("%,d", dmg));
                    val.setPrefWidth(100);
                    Label pctLabel = new Label(String.format("%1$.1f%%", pct * 100));
                    pctLabel.getStyleClass().add("percent-label");
                    pctLabel.setPrefWidth(60);

                    grid.add(name, 0, row);
                    grid.add(barPane, 1, row);
                    grid.add(val, 2, row);
                    grid.add(pctLabel, 3, row);
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
