package com.wakfu.parser;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public final class LogPatterns {

    private LogPatterns() {} // Classe utilitaire statique

    // --- Combat Lifecycle ---
    public static final Pattern START_COMBAT =
            Pattern.compile("CREATION DU COMBAT");
    public static final Pattern END_COMBAT =
            Pattern.compile("Combat terminé|\\[FIGHT\\] End fight with id");

    // --- Entities ---
    public static final Pattern PLAYER_JOIN =
            Pattern.compile("fightId=.*? ([\\w'\\-éÉèàïüçÀÈ]+) breed .*?\\[(\\d+)\\] isControlledByAI=(true|false)");

    // --- Actions ---
    public static final Pattern CAST_SPELL =
            Pattern.compile("\\[Information \\(combat\\)\\]\\s*(.+?) lance le sort ([^\\(]+)");
    public static final Pattern DAMAGE =
            Pattern.compile("\\[Information \\(combat\\)\\]\\s*(.+?): -([\\d\\s]+) PV\\s*\\(([^\\)]+)\\)?");
    public static final Pattern KO =
            Pattern.compile("\\[Information \\(combat\\)\\]\\s*(.+?) est KO");
    public static final Pattern INSTANT_KO =
            Pattern.compile("\\[Information \\(combat\\)\\]\\s*(.+?): tombe instantanément KO");

    // --- Elements ---
    public static final String ELEMENT_REGEX = "(Feu|Eau|Air|Terre|Lumière|Stasis)";
}