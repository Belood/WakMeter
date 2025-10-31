package com.wakfu.parser;

import java.util.regex.Pattern;

/**
 * Motifs regex précis pour différencier les dégâts directs et indirects.
 */
public final class LogPatterns {

    private LogPatterns() {}

    /** Début du combat */
    public static final Pattern START_COMBAT = Pattern.compile("CREATION DU COMBAT");

    /** Fin du combat */
    public static final Pattern END_COMBAT = Pattern.compile("\\[FIGHT\\]\\s+End fight with id");

    /** Entrée d’un combattant */
    public static final Pattern PLAYER_JOIN = Pattern.compile(
            "fightId=\\d+\\s+([^\\[]+)\\s+breed\\s*:\\s*\\d+\\s*\\[(-?\\d+)\\]\\s*isControlledByAI=(true|false)"
    );

    /** Lancement de sort */
    public static final Pattern CAST_SPELL = Pattern.compile(
            "\\[Information \\(combat\\)\\]\\s+([^:]+)\\s+lance le sort\\s+([^\\(\\[]+)"
    );

    // === DÉGÂTS DIRECTS ===
    public static final Pattern DAMAGE_DIRECT = Pattern.compile(
            "(?U)\\[Information \\(combat\\)\\]\\s+([^:]+):\\s*[−–-]?\\s*([\\d\\s\\p{Zs}]+)\\s*PV\\s*\\(([^)]+)\\)\\s*$"
    );

    // === DÉGÂTS INDIRECTS ===
    public static final Pattern DAMAGE_INDIRECT = Pattern.compile(
            "(?U)\\[Information \\(combat\\)\\]\\s+([^:]+):\\s*[−–-]?\\s*([\\d\\s\\p{Zs}]+)\\s*PV\\s*((?:\\([^)]*\\)\\s*){2,})"
    );


    /** Soins */
    public static final Pattern HEAL = Pattern.compile(
            "(?U)\\[Information \\(combat\\)\\]\\s+([^:]+):\\s*\\+\\s*([\\d\\s\\p{Zs}]+)\\s*PV\\s*\\(([^)]+)\\)"
    );

    /** Armures */
    public static final Pattern SHIELD = Pattern.compile(
            "(?U)\\[Information \\(combat\\)\\]\\s+([^:]+):\\s*([\\d\\s\\p{Zs}]+)\\s*Armure\\s*\\(([^)]+)\\)"
    );
}
