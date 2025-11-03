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
    private final Integer baseCost;      // Coût de base du sort (depuis SortsPA.json)
    private final int paRegained;        // PA regagnés lors du cast
    private final Integer effectivePACost; // Coût effectif = baseCost - paRegained

    public SpellDamage(Ability ability, int damageDealt, EventType type) {
        this(ability, damageDealt, type, null, 0);
    }

    public SpellDamage(Ability ability, int damageDealt, EventType type, Integer baseCost, int paRegained) {
        this.ability = ability;
        this.damageDealt = damageDealt;
        this.type = type != null ? type : EventType.DAMAGE;
        this.element = ability != null ? ability.getElement() : Element.INCONNU;
        this.sourceType = ability != null ? ability.getSourceType() : DamageSourceType.AUTRE;
        this.baseCost = baseCost;
        this.paRegained = paRegained;

        // Calculer le coût effectif
        if (baseCost != null && baseCost > 0) {
            int effective = baseCost - paRegained;
            this.effectivePACost = Math.max(effective, 0); // Ne peut pas être négatif
        } else {
            this.effectivePACost = null;
        }
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

    public Integer getBaseCost() {
        return baseCost;
    }

    public int getPaRegained() {
        return paRegained;
    }

    public Integer getEffectivePACost() {
        return effectivePACost;
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
