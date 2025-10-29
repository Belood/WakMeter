package com.wakfu.parser;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour valider la détection correcte
 * des événements de combat dans les logs Wakfu.
 */
public class LogPatternsTest {

    @Test
    void testStartCombat() {
        String line = "INFO 00:49:15,273 [AWT-EventQueue-0] (aWl:47) - CREATION DU COMBAT";
        assertTrue(LogPatterns.START_COMBAT.matcher(line).find(), "Début de combat non détecté");
    }

    @Test
    void testEndCombat() {
        String line = "INFO 00:49:36,187 [AWT-EventQueue-0] (aVi:92) - [FIGHT] End fight with id 1552013926";
        assertTrue(LogPatterns.END_COMBAT.matcher(line).find(), "Fin de combat non détectée");
    }

    @Test
    void testPlayerJoin() {
        String line = "INFO 00:49:15,277 [AWT-EventQueue-0] (eRL:1407) - [_FL_] fightId=1552013926 Portailier breed : 18 [10482754] isControlledByAI=false obstacleId : -1 join the fight at {Point3 : (-2, -14, 0)}";
        Matcher m = LogPatterns.PLAYER_JOIN.matcher(line);
        assertTrue(m.find(), "Entrée joueur non détectée");
        assertEquals("Portailier", m.group(1).trim());
        assertEquals("10482754", m.group(2).trim());
        assertEquals("false", m.group(3).trim());
    }

    @Test
    void testCastSpell() {
        String line = "INFO 00:49:25,064 [AWT-EventQueue-0] (aOC:174) - [Information (combat)] Portailier lance le sort Pulsation (Critiques)";
        Matcher m = LogPatterns.CAST_SPELL.matcher(line);
        assertTrue(m.find(), "Lancement de sort non détecté");
        assertEquals("Portailier", m.group(1).trim());
        assertEquals("Pulsation", m.group(2).trim());
    }

    @Test
    void testDamage() {
        String line = "INFO 00:49:26,483 [AWT-EventQueue-0] (aOC:174) - [Information (combat)] Sac à patates: -5 726 PV (Eau)";
        Matcher m = LogPatterns.DAMAGE.matcher(line);
        assertTrue(m.find(), "Dégât non détecté");
        assertEquals("Sac à patates", m.group(1).trim());
        assertEquals("5 726".replaceAll("[^0-9]", ""), m.group(2).replaceAll("[^0-9]", ""));
        assertEquals("Eau", m.group(3).trim());
    }

    @Test
    void testHeal() {
        String line = "INFO 23:21:52,728 [AWT-EventQueue-0] (aOC:174) - [Information (combat)] Hurdy Gurdy: +1 PV (Neutre)";
        Matcher m = LogPatterns.HEAL.matcher(line);
        assertTrue(m.find(), "Soin non détecté");
        assertEquals("Hurdy Gurdy", m.group(1).trim());
        assertEquals("1", m.group(2).replaceAll("[^0-9]", ""));
        assertEquals("Neutre", m.group(3).trim());
    }

    @Test
    void testShield() {
        String line = "INFO 23:11:12,945 [AWT-EventQueue-0] (aOC:174) - [Information (combat)] Hurdy Gurdy: 6 752 Armure (Bouclier Volcan)";
        Matcher m = LogPatterns.SHIELD.matcher(line);
        assertTrue(m.find(), "Bouclier non détecté");
        assertEquals("Hurdy Gurdy", m.group(1).trim());
        assertEquals("6752", m.group(2).replaceAll("[^0-9]", ""));
        assertEquals("Bouclier Volcan", m.group(3).trim());
    }
}
