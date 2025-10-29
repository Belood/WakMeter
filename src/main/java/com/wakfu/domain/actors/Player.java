package com.wakfu.domain.actors;

import com.wakfu.domain.abilities.SpellDamage;

import java.util.List;

/**
 * Représente un joueur humain.
 */
public class Player extends Fighter {

    private PlayerClass playerClass;
    private List<SpellDamage> spells = new ArrayList<>();

    public Player(String name, long id, PlayerClass playerClass) {
        super(name, id, false);
        this.playerClass = playerClass;
    }

    public PlayerClass getPlayerClass() { return playerClass; }

    public List<SpellDamage> getSpells() { return spells; }

    public void addSpellDamage(SpellDamage dmg) {
        spells.add(dmg);
    }

    @Override
    public FighterType getType() {
        return FighterType.PLAYER;
    }
}