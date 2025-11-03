package com.wakfu.ui.util;

import com.wakfu.domain.abilities.Element;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Map;

/**
 * Classe utilitaire statique pour centraliser le code UI réutilisable.
 * Contient des méthodes pour la construction de colonnes, calculs de taille,
 * création de backgrounds, et gestion des couleurs.
 */
public class UIUtils {

    private UIUtils() {
        // Utility class - no instantiation
    }

    // ===== Column Constraints =====

    /**
     * Crée une contrainte de colonne avec largeur fixe.
     */
    public static ColumnConstraints createFixedColumn(double width) {
        ColumnConstraints cc = new ColumnConstraints(width);
        cc.setMinWidth(width);
        cc.setMaxWidth(width);
        return cc;
    }

    /**
     * Crée une contrainte de colonne flexible avec largeur définie.
     */
    public static ColumnConstraints createFlexibleColumn(double width, double minWidth, double maxWidth) {
        ColumnConstraints cc = new ColumnConstraints(width);
        cc.setMinWidth(minWidth);
        cc.setMaxWidth(maxWidth);
        cc.setHgrow(Priority.ALWAYS);
        return cc;
    }

    /**
     * Crée une contrainte de colonne qui peut grandir.
     */
    public static ColumnConstraints createGrowColumn() {
        ColumnConstraints cc = new ColumnConstraints();
        cc.setHgrow(Priority.ALWAYS);
        return cc;
    }

    // ===== Dynamic Width Calculation =====

    /**
     * Calcule la largeur dynamique pour une colonne basée sur le texte le plus long.
     * @param texts Map ou collection de textes
     * @param minWidth Largeur minimale
     * @param pixelsPerChar Pixels approximatifs par caractère (défaut: 7)
     * @param maxWidth Largeur maximale
     * @return Largeur calculée
     */
    public static double calculateDynamicColumnWidth(Iterable<String> texts, double minWidth, double pixelsPerChar, double maxWidth) {
        int maxLength = 0;
        for (String text : texts) {
            if (text != null && text.length() > maxLength) {
                maxLength = text.length();
            }
        }
        double calculatedWidth = Math.max(minWidth, maxLength * pixelsPerChar);
        return Math.min(calculatedWidth, maxWidth);
    }

    /**
     * Calcule la largeur dynamique pour une colonne basée sur le texte le plus long (valeurs par défaut).
     */
    public static double calculateDynamicColumnWidth(Iterable<String> texts) {
        return calculateDynamicColumnWidth(texts, 80, 7, 300);
    }

    // ===== Background Creation =====

    /**
     * Crée un Background avec une couleur et un rayon de coins.
     */
    public static Background createBackground(Color color, double radius) {
        return new Background(new BackgroundFill(color, new CornerRadii(radius), Insets.EMPTY));
    }

    /**
     * Crée un Background avec une couleur (sans coins arrondis).
     */
    public static Background createBackground(Color color) {
        return createBackground(color, 0);
    }

    // ===== Color Management =====

    /**
     * Retourne la couleur hexadécimale pour un élément.
     * Couleurs cohérentes entre TotalBreakdownPane et TurnBreakdownPane.
     */
    public static String getElementColorHex(Element element) {
        if (element == null) return "#cccccc";
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
     * Retourne la couleur JavaFX Color pour un élément.
     */
    public static Color getElementColor(Element element) {
        return Color.web(getElementColorHex(element));
    }

    // ===== Utility Methods =====

    /**
     * Clamp une valeur entre 0 et 1.
     */
    public static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    /**
     * Crée un style CSS pour un fond coloré avec rayon.
     */
    public static String createBackgroundStyle(String colorHex, double radius) {
        return String.format("-fx-background-color: %s; -fx-background-radius: %.1f;", colorHex, radius);
    }

    // ===== Grid Setup Helpers =====

    /**
     * Configuration standard des colonnes pour le breakdown pane.
     * @param spellNameWidth Largeur dynamique pour la colonne du nom du sort
     * @return Array de ColumnConstraints [spellName, bar, damage, dmgPerPA, percent]
     */
    public static ColumnConstraints[] createBreakdownColumns(double spellNameWidth) {
        return new ColumnConstraints[] {
            createFlexibleColumn(spellNameWidth, 80, 300),  // Spell name
            createGrowColumn(),                              // Bar
            createFixedColumn(50),                           // Damage
            createFixedColumn(60),                           // Dmg/PA
            createFixedColumn(40)                            // Percent
        };
    }

    /**
     * Configuration standard des colonnes pour le player damage pane.
     * @return Array de ColumnConstraints [name, bar, value, percent, button]
     */
    public static ColumnConstraints[] createPlayerDamageColumns() {
        return new ColumnConstraints[] {
            createFixedColumn(90),     // Name
            createGrowColumn(),         // Bar container
            createFixedColumn(60),      // Value
            createFixedColumn(60),      // Percent
            createFixedColumn(48)       // Button
        };
    }

    /**
     * Extrait l'élément dominant d'une map de dégâts par élément.
     */
    public static Element getDominantElement(Map<Element, Integer> damageByElement) {
        if (damageByElement == null || damageByElement.isEmpty()) {
            return Element.INCONNU;
        }
        return damageByElement.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(Element.INCONNU);
    }

    // ===== Bar Creation Helpers =====

    /**
     * Crée une barre de progression standard (track + fill).
     * @param pct Pourcentage (0..1)
     * @param color Couleur de la barre
     * @param height Hauteur de la barre
     * @param radius Rayon des coins
     * @return StackPane contenant la barre
     */
    public static StackPane createProgressBar(double pct, Color color, double height, double radius) {
        double pctClamped = clamp01(pct);

        Region track = new Region();
        track.setPrefHeight(height);
        track.setBackground(createBackground(Color.rgb(255, 255, 255, 0.08), radius));
        track.setMinWidth(0);

        Region fill = new Region();
        fill.setPrefHeight(height);
        fill.setBackground(createBackground(color, radius));
        fill.setMinWidth(0);

        StackPane barPane = new StackPane(track, fill);
        barPane.setMinWidth(0);
        barPane.setMaxWidth(Double.MAX_VALUE);

        track.prefWidthProperty().bind(barPane.widthProperty());
        fill.prefWidthProperty().bind(track.widthProperty().multiply(pctClamped));
        fill.maxWidthProperty().bind(track.widthProperty().multiply(pctClamped));
        StackPane.setAlignment(fill, javafx.geometry.Pos.CENTER_LEFT);

        return barPane;
    }

    /**
     * Crée une barre de progression avec couleur d'élément.
     */
    public static StackPane createProgressBar(double pct, Element element, double height, double radius) {
        return createProgressBar(pct, getElementColor(element), height, radius);
    }

    /**
     * Crée une barre de progression avec paramètres par défaut (hauteur 12, radius 6).
     */
    public static StackPane createProgressBar(double pct, Color color) {
        return createProgressBar(pct, color, 12, 6);
    }
}

