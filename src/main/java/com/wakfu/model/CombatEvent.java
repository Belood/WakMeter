package com.wakfu.model;

public class CombatEvent {

    private LocalDateTime timestamp;
    private Fighter caster;
    private Fighter target;
    private Ability ability;

    // Exemple : dégâts infligés, soins, KO, etc.
    private EventType type;
    private int value;  // nombre de dégâts, soins, etc.

    public CombatEvent(LocalDateTime timestamp, Fighter caster, Fighter target,
                       Ability ability, EventType type, int value) {
        this.timestamp = timestamp;
        this.caster = caster;
        this.target = target;
        this.ability = ability;
        this.type = type;
        this.value = value;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public Fighter getCaster() { return caster; }
    public Fighter getTarget() { return target; }
    public Ability getAbility() { return ability; }
    public EventType getType() { return type; }
    public int getValue() { return value; }

    @Override
    public String toString() {
        return String.format("[%s] %s -> %s (%s : %d)",
                timestamp,
                caster != null ? caster.getName() : "?",
                target != null ? target.getName() : "?",
                ability != null ? ability.getName() : "?",
                value);
    }
}