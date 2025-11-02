package com.wakfu.ui;

import com.wakfu.data.SpellCostProvider;
import com.wakfu.domain.model.SpellStats;
import com.wakfu.domain.abilities.Element;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Map;

/**
 * BreakdownPane renders damage breakdown statistics in a reusable Pane format.
 * This component displays spell damage breakdown with colored bars and percentages.
 */
public class BreakdownPane {

    private BreakdownPane() {
        // Utility class - no instantiation
    }

    /**
     * Builds a panel showing damage breakdown for a player.
     * Designed to be embedded in MainUI's right pane.
     */
    public static Pane buildPanel(Object playerStatsObj) {
        Map<String, Object> spellsRaw = null;
        String playerName = "Joueur";

        try {
            var getSpells = playerStatsObj.getClass().getMethod("getSpells");
            Object spellsObj = getSpells.invoke(playerStatsObj);
            if (spellsObj instanceof Map<?, ?>) {
                Map<?, ?> tmp = (Map<?, ?>) spellsObj;
                spellsRaw = new java.util.LinkedHashMap<>();
                for (Map.Entry<?, ?> e : tmp.entrySet()) {
                    String key = e.getKey() == null ? null : e.getKey().toString();
                    spellsRaw.put(key, e.getValue());
                }
            }
        } catch (Exception ignored) {
        }

        try {
            var getPlayer = playerStatsObj.getClass().getMethod("getPlayer");
            Object playerObj = getPlayer.invoke(playerStatsObj);
            if (playerObj != null) {
                var getName = playerObj.getClass().getMethod("getName");
                Object nameObj = getName.invoke(playerObj);
                if (nameObj != null) playerName = nameObj.toString();
            }
        } catch (Exception ignored) {
        }

        Map<String, SpellStats> spells = new java.util.LinkedHashMap<>();
        if (spellsRaw != null) {
            for (Map.Entry<String, Object> en : spellsRaw.entrySet()) {
                Object v = en.getValue();
                if (v == null) continue;
                if (v instanceof SpellStats ss) {
                    spells.put(en.getKey(), ss);
                    continue;
                }
                try {
                    var cls = v.getClass();
                    var mGetName = cls.getMethod("getName");
                    var mGetByElem = cls.getMethod("getDamageByElement");
                    Object nameObj = mGetName.invoke(v);
                    Object byElemObj = mGetByElem.invoke(v);
                    String name = nameObj == null ? en.getKey() : nameObj.toString();
                    @SuppressWarnings("unchecked")
                    Map<Element, Integer> byElem = byElemObj instanceof Map ? (Map<Element, Integer>) byElemObj : null;
                    SpellStats newSs = new SpellStats(name);
                    if (byElem != null) {
                        for (Map.Entry<Element, Integer> be : byElem.entrySet()) {
                            if (be.getKey() != null && be.getValue() != null)
                                newSs.addDamage(be.getKey(), be.getValue());
                        }
                    }
                    spells.put(en.getKey(), newSs);
                } catch (Exception ignored) {
                }
            }
        }

        int total = spells.values().stream().mapToInt(SpellStats::getTotal).sum();
        if (total == 0) total = 1;

        VBox container = new VBox(10);
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.TOP_LEFT);
        container.setBackground(Background.EMPTY);

        Label title = new Label("Répartition des dégâts - " + playerName);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        container.getChildren().add(title);

        // Header grid
        GridPane headerGrid = new GridPane();
        headerGrid.setHgap(10);
        headerGrid.setAlignment(Pos.CENTER_LEFT);
        ColumnConstraints hCol0 = new ColumnConstraints(60);
        hCol0.setMinWidth(60);
        hCol0.setMaxWidth(60);
        ColumnConstraints hCol1 = new ColumnConstraints();
        hCol1.setHgrow(Priority.ALWAYS);
        ColumnConstraints hCol2 = new ColumnConstraints(50);
        hCol2.setMinWidth(50);
        hCol2.setMaxWidth(50);
        ColumnConstraints hCol3 = new ColumnConstraints(60);
        hCol3.setMinWidth(60);
        hCol3.setMaxWidth(60);
        ColumnConstraints hCol4 = new ColumnConstraints(40);
        hCol4.setMinWidth(40);
        hCol4.setMaxWidth(40);
        headerGrid.getColumnConstraints().addAll(java.util.Arrays.asList(hCol0, hCol1, hCol2, hCol3, hCol4));

        Label hName = new Label("Sort");
        hName.setPrefWidth(60);
        Label hBar = new Label("");
        hBar.setBackground(Background.EMPTY);
        GridPane.setHgrow(hBar, Priority.ALWAYS);
        Label hDmg = new Label("Dégâts");
        hDmg.setPrefWidth(50);
        Label hDmgPerPA = new Label("Dégât/PA");
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

        int finalTotal = total;
        final String playerClassKey = null;

        spells.values().stream()
                .sorted((a, b) -> Integer.compare(b.getTotal(), a.getTotal()))
                .forEach(sp -> {
                    String spell = sp.getName();
                    int dmg = sp.getTotal();
                    double pct = (double) dmg / finalTotal;

                    Element element = Element.INCONNU;
                    Map<Element, Integer> byElem = sp.getDamageByElement();
                    if (!byElem.isEmpty())
                        element = byElem.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(Element.INCONNU);
                    String color = getColorForElement(element);

                    GridPane rowGrid = new GridPane();
                    rowGrid.setHgap(10);
                    ColumnConstraints col0 = new ColumnConstraints(60);
                    col0.setMinWidth(60);
                    col0.setMaxWidth(60);
                    ColumnConstraints col1 = new ColumnConstraints();
                    col1.setHgrow(Priority.ALWAYS);
                    ColumnConstraints col2 = new ColumnConstraints(50);
                    col2.setMinWidth(50);
                    col2.setMaxWidth(50);
                    ColumnConstraints col3 = new ColumnConstraints(60);
                    col3.setMinWidth(60);
                    col3.setMaxWidth(60);
                    ColumnConstraints col4 = new ColumnConstraints(40);
                    col4.setMinWidth(40);
                    col4.setMaxWidth(40);
                    rowGrid.getColumnConstraints().addAll(java.util.Arrays.asList(col0, col1, col2, col3, col4));

                    Label spellName = new Label(spell);
                    spellName.setPrefWidth(80);
                    double pctClamped = Math.max(0.0, Math.min(1.0, pct));

                    Region track = new Region();
                    track.setPrefHeight(12);
                    track.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255, 0.08), new CornerRadii(6), Insets.EMPTY)));
                    track.setMinWidth(0);

                    Region fill = new Region();
                    fill.setPrefHeight(12);
                    try {
                        fill.setBackground(new Background(new BackgroundFill(Color.web(color), new CornerRadii(6), Insets.EMPTY)));
                    } catch (Exception ex) {
                        fill.setBackground(new Background(new BackgroundFill(Color.web("#2b2b2b"), new CornerRadii(6), Insets.EMPTY)));
                    }

                    StackPane barPane = new StackPane(track, fill);
                    barPane.setMinWidth(0);
                    barPane.setMaxWidth(Double.MAX_VALUE);
                    GridPane.setHgrow(barPane, Priority.ALWAYS);
                    track.prefWidthProperty().bind(barPane.widthProperty());
                    fill.prefWidthProperty().bind(track.widthProperty().multiply(pctClamped));
                    fill.maxWidthProperty().bind(track.widthProperty().multiply(pctClamped));
                    fill.setMinWidth(0);
                    StackPane.setAlignment(fill, Pos.CENTER_LEFT);

                    Label dmgLabel = new Label(String.format("%,d", dmg));
                    dmgLabel.setPrefWidth(50);
                    String dmgPerPaText = "-";
                    Integer cost = SpellCostProvider.getCostFor(playerClassKey, spell);
                    int castCount = sp.getCastCount();
                    if (cost != null && cost > 0 && castCount > 0) {
                        dmgPerPaText = String.format("%1$.2f", (double) dmg / (cost * castCount));
                    }
                    Label dmgPerPaLabel = new Label(dmgPerPaText);
                    dmgPerPaLabel.setPrefWidth(70);
                    Label pctLabel = new Label(String.format("%.1f%%", pct * 100));
                    pctLabel.setPrefWidth(60);

                    rowGrid.add(spellName, 0, 0);
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

    /**
     * Returns the color code for a given element.
     */
    private static String getColorForElement(Element element) {
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
}

