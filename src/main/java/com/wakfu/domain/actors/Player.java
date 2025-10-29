package com.wakfu.domain.actors;

import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.abilities.SpellDamage;
import com.wakfu.domain.event.EventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un joueur (hérite de Fighter).
 */
public class Player extends Fighter {

    private final List<SpellDamage> spells = new ArrayList<>();
    private PlayerClass playerClass;

    public Player(String name, long id, FighterType type) {
        super(name, id, type);
    }

    // --- Gestion des dégâts / soins / boucliers ---
    public void addSpellDamage(Ability ability, int value) {
        spells.add(new SpellDamage(ability, value, EventType.DAMAGE));
    }

    public void addSpellHeal(Ability ability, int value) {
        spells.add(new SpellDamage(ability, value, EventType.HEAL));
    }

    public void addSpellShield(Ability ability, int value) {
        spells.add(new SpellDamage(ability, value, EventType.SHIELD));
    }

    public int getTotalDamage() {
        return spells.stream()
                .filter(s -> s.getType() == EventType.DAMAGE)
                .mapToInt(SpellDamage::getDamageDealt)
                .sum();
    }

    public int getTotalHeal() {
        return spells.stream()
                .filter(s -> s.getType() == EventType.HEAL)
                .mapToInt(SpellDamage::getDamageDealt)
                .sum();
    }

    public int getTotalShield() {
        return spells.stream()
                .filter(s -> s.getType() == EventType.SHIELD)
                .mapToInt(SpellDamage::getDamageDealt)
                .sum();
    }

    public List<SpellDamage> getSpells() {
        return spells;
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
}