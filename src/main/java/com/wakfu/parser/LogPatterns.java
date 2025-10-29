package com.wakfu.parser;

import java.util.regex.Pattern;

/**
 * Regroupe tous les motifs (regex) utilisés pour détecter les différents événements
 * dans le fichier de log Wakfu.
 * Supporte les encodages UTF-8 et les caractères spéciaux utilisés dans les logs (espaces insécables, tirets typographiques, etc.).
 */
public final class LogPatterns {

    private LogPatterns() {}

    // ===============================
    // COMBAT
    // ===============================

    /** Début du combat : "CREATION DU COMBAT" */
    public static final Pattern START_COMBAT = Pattern.compile(
            "CREATION DU COMBAT"
    );

    /** Fin du combat : "End fight with id XXXXX" */
    public static final Pattern END_COMBAT = Pattern.compile(
            "\\[FIGHT\\]\\s+End fight with id"
    );

    // ===============================
    // ENTITÉS (JOUEURS / ENNEMIS)
    // ===============================

    /**
     * Détection d’un combattant rejoignant le combat.
     * Exemple :
     * [_FL_] fightId=1552013926 Sac à patates breed : 2335 [-1706...] isControlledByAI=true ...
     */
    public static final Pattern PLAYER_JOIN = Pattern.compile(
            "fightId=\\d+\\s+([^\\[]+?)\\s+breed\\s*:\\s*\\d+\\s*\\[(-?\\d+)\\]\\s*isControlledByAI=(true|false)"
    );

    // ===============================
    // SORTS ET ACTIONS
    // ===============================

    /**
     * Lancement d’un sort.
     * Exemple :
     * [Information (combat)] Portailier lance le sort Pulsation (Critiques)
     */
    public static final Pattern CAST_SPELL = Pattern.compile(
            "\\[Information \\(combat\\)\\]\\s+([^:]+)\\s+lance le sort\\s+([^\\(\\r\\n]+)"
    );

    /**
     * Dégâts infligés.
     * Exemple :
     * [Information (combat)] Sac à patates: -5 726 PV (Eau)
     * Tolère espaces insécables et caractères parasites comme ? ou  .
     */
    public static final Pattern DAMAGE = Pattern.compile(
            "\\[Information \\(combat\\)\\]\\s+([^:]+):\\s*[-−–]?[\\s\\u00A0\\u202F]*([\\d\\s\\u00A0\\u202F]+)\\s*PV\\s*\\(([^)]+)\\)"
    );

    /**
     * Soins.
     * Exemple :
     * [Information (combat)] Hurdy Gurdy: +1 PV (Neutre)
     */
    public static final Pattern HEAL = Pattern.compile(
            "\\[Information \\(combat\\)\\]\\s+([^:]+):\\s*\\+([\\d  ]+)\\s*PV\\s*\\(([^)]+)\\)"
    );

    /**
     * Boucliers.
     * Exemple :
     * [Information (combat)] Hurdy Gurdy: 6 752 Armure (Bouclier Volcan)
     */
    public static final Pattern SHIELD = Pattern.compile(
            "\\[Information \\(combat\\)\\]\\s+([^:]+):\\s*([\\d  ]+)\\s*Armure\\s*\\(([^)]+)\\)"
    );
}