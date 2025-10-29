package com.wakfu.ui;

import com.wakfu.domain.actors.Player;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Map;

/**
 * Fenêtre affichant le breakdown des dégâts d’un joueur,
 * avec barres horizontales et pourcentages.
 */
public class DamageBreakdownUI {

    private final Player player;
    private final Stage stage;

    public DamageBreakdownUI(Player player) {
        this.player = player;
        this.stage = new Stage();
        setupUI();
    }

    private void setupUI() {
        stage.setTitle("Breakdown - " + player.getName());

        Map<String, Integer> damageMap = player.getDamageByAbility();
        int total = damageMap.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) total = 1;

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.TOP_LEFT);

        Label title = new Label("Répartition des dégâts par sort");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        container.getChildren().add(title);

        VBox list = new VBox(6);

        int finalTotal = total;
        damageMap.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(entry -> {
                    String spell = entry.getKey();
                    int dmg = entry.getValue();
                    double pct = (double) dmg / finalTotal;

                    HBox row = new HBox(10);
                    row.setAlignment(Pos.CENTER_LEFT);

                    Label spellName = new Label(spell);
                    spellName.setPrefWidth(180);

                    ProgressBar bar = new ProgressBar(pct);
                    bar.setPrefWidth(250);
                    bar.setStyle("-fx-accent: #ff6b6b;");

                    Label dmgLabel = new Label(String.format("%,d", dmg));
                    dmgLabel.setPrefWidth(90);

                    Label pctLabel = new Label(String.format("%.1f%%", pct * 100));
                    pctLabel.setPrefWidth(60);

                    row.getChildren().addAll(spellName, bar, dmgLabel, pctLabel);
                    list.getChildren().add(row);
                });

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);

        container.getChildren().add(scrollPane);

        Scene scene = new Scene(container, 600, 500);
        stage.setScene(scene);
        stage.setAlwaysOnTop(true);
    }

    /**
     * Affiche la fenêtre du breakdown.
     */
    public void show() {
        stage.show();
    }
}
