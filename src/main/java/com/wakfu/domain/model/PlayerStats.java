package com.wakfu.domain.model;

import com.wakfu.domain.actors.Player;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.event.CombatEvent;

import java.util.*;

/**
 * Statistiques cumulées d’un joueur avec breakdown par sort et élément.
 */
public class PlayerStats {

    private final Player player;
    private final Map<String, SpellStats> spells = new LinkedHashMap<>();

    private int totalDamage = 0;
    private int totalHeal = 0;
    private int totalShield = 0;

    public PlayerStats(Player player) {
        this.player = player;
    }

    public void addDamage(CombatEvent event) {
        int val = event.getValue();
        Element element = event.getElement();
        totalDamage += val;

        spells
            .computeIfAbsent(event.getAbility().getName(), SpellStats::new)
            .addDamage(element, val);
    }

    public void addHeal(CombatEvent event) {
        totalHeal += event.getValue();
    }

    public void addShield(CombatEvent event) {
        totalShield += event.getValue();
    }

    public int getTotalDamage() {
        return totalDamage;
    }

    public Map<String, SpellStats> getSpells() {
        return Collections.unmodifiableMap(spells);
    }

    public Player getPlayer() {
        return player;
    }

    public int getTotalHeal() {
        return totalHeal;
    }

    public int getTotalShield() {
        return totalShield;
    }

    public int getGlobalTotal() {
        return totalDamage + totalHeal + totalShield;
    }
}
