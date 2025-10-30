package com.wakfu.domain.abilities;

import com.wakfu.domain.event.EventType;

import java.util.Objects;

/**
 * Représente une instance de dégâts, de soin ou de bouclier
 * associée à une capacité (Ability).
 */
public class SpellDamage {

    private final Ability ability;
    private final int damageDealt;
    private final EventType type;
    private final Element element;
    private final DamageSourceType sourceType;

    public SpellDamage(Ability ability, int damageDealt, EventType type) {
        this.ability = ability;
        this.damageDealt = damageDealt;
        this.type = type != null ? type : EventType.DAMAGE;
        this.element = ability != null ? ability.getElement() : Element.INCONNU;
        this.sourceType = ability != null ? ability.getSourceType() : DamageSourceType.AUTRE;
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

    public Element getElement() {
        return element;
    }

    public DamageSourceType getSourceType() {
        return sourceType;
    }

    @Override
    public String toString() {
        return String.format("%s - %s : %d (%s, %s)",
                ability != null ? ability.getName() : "Inconnu",
                type,
                damageDealt,
                element,
                sourceType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpellDamage that)) return false;
        return damageDealt == that.damageDealt
                && Objects.equals(ability, that.ability)
                && type == that.type
                && element == that.element
                && sourceType == that.sourceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ability, damageDealt, type, element, sourceType);
    }
}
