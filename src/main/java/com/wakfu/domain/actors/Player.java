package com.wakfu.domain.actors;

import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.abilities.SpellDamage;
import com.wakfu.domain.event.EventType;

import java.util.*;

/**
 * Représente un joueur (hérite de Fighter).
 */
public class Player extends Fighter {

    /** Liste complète des actions du joueur (dégâts, soins, boucliers). */
    private final List<SpellDamage> spells = new ArrayList<>();

    /** Classe du joueur (Iop, Eniripsa, etc.) */
    private PlayerClass playerClass;

    /** Registre des dégâts par sort (utilisé pour le breakdown UI). */
    private final Map<String, Integer> damageByAbility = new HashMap<>();

    public Player(String name, long id, FighterType type) {
        super(name, id, type);
    }

    // ============================================================
    // 🔹 Gestion des actions (dégâts, soins, boucliers)
    // ============================================================

    public void addSpellDamage(Ability ability, int value) {
        spells.add(new SpellDamage(ability, value, EventType.DAMAGE));
        addDamage(ability.getName(), value);
    }

    public void addSpellHeal(Ability ability, int value) {
        spells.add(new SpellDamage(ability, value, EventType.HEAL));
    }

    public void addSpellShield(Ability ability, int value) {
        spells.add(new SpellDamage(ability, value, EventType.SHIELD));
    }

    // ============================================================
    // 🔹 Dégâts cumulés
    // ============================================================

    /**
     * Ajoute ou cumule les dégâts pour un sort donné.
     */
    public void addDamage(String abilityName, int value) {
        if (abilityName == null || abilityName.isBlank()) return;
        damageByAbility.merge(abilityName, value, Integer::sum);
    }

    /**
     * Renvoie la somme totale des dégâts infligés.
     */
    public int getTotalDamage() {
        return spells.stream()
                .filter(s -> s.getType() == EventType.DAMAGE)
                .mapToInt(SpellDamage::getDamageDealt)
                .sum();
    }

    /**
     * Renvoie la somme totale des soins prodigués.
     */
    public int getTotalHeal() {
        return spells.stream()
                .filter(s -> s.getType() == EventType.HEAL)
                .mapToInt(SpellDamage::getDamageDealt)
                .sum();
    }

    /**
     * Renvoie la somme totale des boucliers générés.
     */
    public int getTotalShield() {
        return spells.stream()
                .filter(s -> s.getType() == EventType.SHIELD)
                .mapToInt(SpellDamage::getDamageDealt)
                .sum();
    }

    // ============================================================
    // 🔹 Getters / Setters
    // ============================================================

    public List<SpellDamage> getSpells() {
        return Collections.unmodifiableList(spells);
    }

    public Map<String, Integer> getDamageByAbility() {
        return Collections.unmodifiableMap(damageByAbility);
    }

    public void setPlayerClass(PlayerClass playerClass) {
        this.playerClass = playerClass;
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    @Override
    public FighterType getType() {
        return FighterType.PLAYER;
    }

    @Override
    public String toString() {
        return String.format("%s [%s] - %d dégâts", getName(), getType(), getTotalDamage());
    }
}
