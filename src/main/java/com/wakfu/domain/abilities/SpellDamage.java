package com.wakfu.domain.abilities;

import com.wakfu.domain.event.EventType;

/**
 * Représente la contribution d'un sort (dégâts, soins, bouclier).
 */
public class SpellDamage {

    private final Ability ability;
    private final int damageDealt;
    private final EventType type;

    public SpellDamage(Ability ability, int damageDealt, EventType type) {
        this.ability = ability;
        this.damageDealt = damageDealt;
        this.type = type;
    }

    public Ability getAbility() {
        return ability;
    }

    public int getDamageDealt() {
        return damageDealt;
    }

    public EventType getType() {
        return type;
    }

    @Override
    public String toString() {
        return String.format("%s : %d (%s)", ability.getName(), damageDealt, type);
    }
}