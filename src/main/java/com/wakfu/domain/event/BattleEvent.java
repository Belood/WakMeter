package com.wakfu.domain.event;

/**
 * Représente le début ou la fin d’un combat.
 */
public class BattleEvent extends LogEvent {

    public enum BattleState {
        START,
        END
    }

    private BattleState state;

    public BattleEvent(LocalDateTime timestamp, BattleState state) {
        super(timestamp);
        this.state = state;
    }

    @Override
    public LogEventType getEventType() {
        return LogEventType.BATTLE;
    }

    public BattleState getState() {
        return state;
    }

    @Override
    public String toString() {
        return String.format("[%s] Combat %s", timestamp, state);
    }
}