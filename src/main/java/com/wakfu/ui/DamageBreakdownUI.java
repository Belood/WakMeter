package com.wakfu.ui;

import com.wakfu.data.SpellCostProvider;
import com.wakfu.model.PlayerStats;
import com.wakfu.model.SpellStats;
import com.wakfu.domain.abilities.Element;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Map;

/**
 * Fenêtre affichant le breakdown des dégâts d’un joueur,
 * avec barres horizontales colorées selon l’élément du sort.
 */
public class DamageBreakdownUI {

    private final PlayerStats playerStats;
    private final Stage stage;

    public DamageBreakdownUI(PlayerStats playerStats) {
        this.playerStats = playerStats;
        this.stage = new Stage();
    }

    /**
     * Construct a reusable panel (VBox) containing the breakdown UI for embedding in the main window.
     */
    public static Pane buildPanel(PlayerStats playerStats) {
        Map<String, SpellStats> spells = playerStats.getSpells();
        int total = spells.values().stream().mapToInt(SpellStats::getTotal).sum();
        if (total == 0) total = 1;

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.TOP_LEFT);
        container.setBackground(Background.EMPTY);

        Label title = new Label("Répartition des dégâts par sort - " + playerStats.getPlayer().getName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        container.getChildren().add(title);

        // Header (GridPane) same as in setupUI
        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setAlignment(Pos.CENTER_LEFT);
        ColumnConstraints hCol0 = new ColumnConstraints(180);
        hCol0.setMinWidth(180); hCol0.setMaxWidth(180);
        ColumnConstraints hCol1 = new ColumnConstraints(); hCol1.setHgrow(Priority.ALWAYS);
        ColumnConstraints hCol2 = new ColumnConstraints(90); hCol2.setMinWidth(90); hCol2.setMaxWidth(90);
        ColumnConstraints hCol3 = new ColumnConstraints(90); hCol3.setMinWidth(90); hCol3.setMaxWidth(90);
        ColumnConstraints hCol4 = new ColumnConstraints(60); hCol4.setMinWidth(60); hCol4.setMaxWidth(60);
        headerGrid.getColumnConstraints().addAll(java.util.Arrays.asList(hCol0, hCol1, hCol2, hCol3, hCol4));
        Label hName = new Label("Sort"); hName.setPrefWidth(180);
        Label hBar = new Label(""); hBar.setBackground(Background.EMPTY); GridPane.setHgrow(hBar, Priority.ALWAYS);
        Label hDmg = new Label("Dégâts"); hDmg.setPrefWidth(90);
        Label hDmgPerPA = new Label("Dégât/PA"); hDmgPerPA.setPrefWidth(90);
        Label hPct = new Label("% "); hPct.setPrefWidth(60);
        headerGrid.add(hName,0,0); headerGrid.add(hBar,1,0); headerGrid.add(hDmg,2,0); headerGrid.add(hDmgPerPA,3,0); headerGrid.add(hPct,4,0);
        headerGrid.setBackground(Background.EMPTY);
        container.getChildren().add(headerGrid);

        VBox list = new VBox(6);
        list.setBackground(Background.EMPTY);

        int finalTotal = total;
        final String playerClassKey = null;

        spells.values().stream()
                .sorted((a,b) -> Integer.compare(b.getTotal(), a.getTotal()))
                .forEach(sp -> {
                    String spell = sp.getName();
                    int dmg = sp.getTotal();
                    double pct = (double)dmg / finalTotal;

                    Element element = Element.INCONNU;
                    Map<Element,Integer> byElem = sp.getDamageByElement();
                    if (!byElem.isEmpty()) element = byElem.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(Element.INCONNU);
                    String color = getColorForElementStatic(element);

                    GridPane rowGrid = new GridPane();
                    rowGrid.setHgap(10);
                    ColumnConstraints col0 = new ColumnConstraints(180); col0.setMinWidth(180); col0.setMaxWidth(180);
                    ColumnConstraints col1 = new ColumnConstraints(); col1.setHgrow(Priority.ALWAYS);
                    ColumnConstraints col2 = new ColumnConstraints(90); col2.setMinWidth(90); col2.setMaxWidth(90);
                    ColumnConstraints col3 = new ColumnConstraints(90); col3.setMinWidth(90); col3.setMaxWidth(90);
                    ColumnConstraints col4 = new ColumnConstraints(60); col4.setMinWidth(60); col4.setMaxWidth(60);
                    rowGrid.getColumnConstraints().addAll(java.util.Arrays.asList(col0,col1,col2,col3,col4));

                    Label spellName = new Label(spell); spellName.setPrefWidth(180);
                    double pctClamped = Math.max(0.0, Math.min(1.0, pct));
                    Region track = new Region(); track.setPrefHeight(12); track.setBackground(new Background(new BackgroundFill(Color.rgb(255,255,255,0.08), new CornerRadii(6), Insets.EMPTY))); track.setMinWidth(0);
                    Region fill = new Region(); fill.setPrefHeight(12);
                    try { fill.setBackground(new Background(new BackgroundFill(Color.web(color), new CornerRadii(6), Insets.EMPTY))); } catch (Exception ex) { fill.setBackground(new Background(new BackgroundFill(Color.web("#cccccc"), new CornerRadii(6), Insets.EMPTY))); }
                    StackPane barPane = new StackPane(track, fill); barPane.setMinWidth(0); barPane.setMaxWidth(Double.MAX_VALUE); GridPane.setHgrow(barPane, Priority.ALWAYS);
                    track.prefWidthProperty().bind(barPane.widthProperty()); fill.prefWidthProperty().bind(track.widthProperty().multiply(pctClamped)); fill.maxWidthProperty().bind(track.widthProperty().multiply(pctClamped)); fill.setMinWidth(0); StackPane.setAlignment(fill, Pos.CENTER_LEFT);

                    Label dmgLabel = new Label(String.format("%,d", dmg)); dmgLabel.setPrefWidth(90);
                    String dmgPerPaText = "-"; Integer cost = SpellCostProvider.getCostFor(playerClassKey, spell); if (cost != null && cost > 0) dmgPerPaText = String.format("%1$.2f", (double)dmg / cost);
                    Label dmgPerPaLabel = new Label(dmgPerPaText); dmgPerPaLabel.setPrefWidth(90);
                    Label pctLabel = new Label(String.format("%.1f%%", pct * 100)); pctLabel.setPrefWidth(60);

                    rowGrid.add(spellName,0,0); rowGrid.add(barPane,1,0); rowGrid.add(dmgLabel,2,0); rowGrid.add(dmgPerPaLabel,3,0); rowGrid.add(pctLabel,4,0);
                    list.getChildren().add(rowGrid);
                });

        ScrollPane scrollPane = new ScrollPane(list);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scrollPane.setBackground(Background.EMPTY);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);
        container.getChildren().add(scrollPane);

        return container;
    }

    // helper static color getter for buildPanel
    private static String getColorForElementStatic(Element element) {
        return switch (element) {
            case FEU -> "#ff4b4b";
            case EAU -> "#4b8cff";
            case TERRE -> "#4bff6b";
            case AIR -> "#b44bff";
            case LUMIERE -> "#ffd84b";
            case STASIS -> "#9c9c9c";
            default -> "#cccccc";
        };
    }

    /**
     * Affiche la fenêtre du breakdown.
     */
    public void show() {
        stage.show();
    }
}
