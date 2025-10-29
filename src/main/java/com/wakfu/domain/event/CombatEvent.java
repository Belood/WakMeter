package com.wakfu.domain.event;

import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.actors.Fighter;

import java.time.LocalDateTime;

public class CombatEvent {

    private Fighter caster;
    private Fighter target;
    private Ability ability;
    private EventType type;
    private int value;

    public CombatEvent(LocalDateTime timestamp,
                       Fighter caster,
                       Fighter target,
                       Ability ability,
                       EventType type,
                       int value) {
        super(timestamp);
        this.caster = caster;
        this.target = target;
        this.ability = ability;
        this.type = type;
        this.value = value;
    }

    @Override
    public LogEventType getEventType() {
        return LogEventType.COMBAT;
    }

    public Fighter getCaster() { return caster; }
    public Fighter getTarget() { return target; }
    public Ability getAbility() { return ability; }
    public EventType getType() { return type; }
    public int getValue() { return value; }
}