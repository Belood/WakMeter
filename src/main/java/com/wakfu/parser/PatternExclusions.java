package com.wakfu.parser;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Centralise les patterns / tokens à exclure des lignes de log avant parsing.
 * Permet d'ajouter ou modifier rapidement les exclusions sans polluer le parser.
 */
public final class PatternExclusions {
    private PatternExclusions() {}

    private static final List<Pattern> EXCLUSIONS = List.of(
            Pattern.compile("\\(Parade !\\)"),
            Pattern.compile("\\(Parade!\\)"),
            Pattern.compile("\\(Lumière\\)"),
            Pattern.compile("\\(Critiques\\)"),
            Pattern.compile("\\(Simple\\)"),
            Pattern.compile("\\(Double\\)")
    );

    /**
     * Nettoie une ligne de log en retirant les tokens connus et en normalisant les espaces.
     */
    public static String clean(String line) {
        if (line == null) return null;
        String out = line;
        for (Pattern p : EXCLUSIONS) {
            out = p.matcher(out).replaceAll("");
        }
        // Normalise les espaces
        out = out.replaceAll("\\s+", " ").trim();
        return out;
    }
}

