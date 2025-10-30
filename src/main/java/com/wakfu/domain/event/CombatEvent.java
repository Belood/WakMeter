package com.wakfu.domain.event;

import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.abilities.DamageSourceType;
import com.wakfu.domain.actors.Fighter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Représente un événement de combat : dégâts, soin ou bouclier.
 */
public class CombatEvent extends LogEvent {

    private final Fighter caster;
    private final Fighter target;
    private final Ability ability;
    private final EventType type;
    private final int value;
    private final Element element;
    private final DamageSourceType sourceType;

    public CombatEvent(
            LocalDateTime timestamp,
            Fighter caster,
            Fighter target,
            Ability ability,
            EventType type,
            int value,
            Element element
    ) {
        super(timestamp);
        this.caster = caster;
        this.target = target;
        this.ability = ability;
        this.type = type;
        this.value = value;
        this.element = element != null ? element : Element.INCONNU;
        this.sourceType = ability != null ? ability.getSourceType() : DamageSourceType.AUTRE;
    }

    public Fighter getCaster() {
        return caster;
    }

    public Fighter getTarget() {
        return target;
    }

    public Ability getAbility() {
        return ability;
    }

    public EventType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public Element getElement() {
        return element;
    }

    public DamageSourceType getSourceType() {
        return sourceType;
    }

    @Override
    public String toString() {
        return String.format(
                "[%s] %s → %s : %d (%s, %s)",
                type,
                caster != null ? caster.getName() : "Inconnu",
                target != null ? target.getName() : "Inconnu",
                value,
                element,
                sourceType
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CombatEvent that)) return false;
        return value == that.value
                && Objects.equals(caster, that.caster)
                && Objects.equals(target, that.target)
                && Objects.equals(ability, that.ability)
                && type == that.type
                && element == that.element
                && sourceType == that.sourceType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(caster, target, ability, type, value, element, sourceType);
    }

    @Override
    public LogEventType getEventType() {
        return LogEventType.COMBAT;
    }
}
