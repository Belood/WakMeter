package com.wakfu.domain.event;

import java.time.LocalDateTime;

/**
 * Représente le début ou la fin d’un combat.
 */
public class BattleEvent extends LogEvent {

    public enum BattleState {
        START,
        END,
        START_TURN,
        END_TURN,
        ROUND_START,
        ROUND_END
    }

    private final BattleState state;
    private final String playerName;   // Nom du joueur concerné (si applicable)
    private final int roundNumber;     // Numéro du round (si applicable)

    // --- Constructeur générique (combat start/end) ---
    public BattleEvent(LocalDateTime timestamp, BattleState state) {
        this(timestamp, state, null, 0);
    }

    // --- Constructeur pour les tours ---
    public BattleEvent(LocalDateTime timestamp, BattleState state, String playerName) {
        this(timestamp, state, playerName, 0);
    }

    // --- Constructeur pour les rounds ---
    public BattleEvent(LocalDateTime timestamp, BattleState state, int roundNumber) {
        this(timestamp, state, null, roundNumber);
    }

    // --- Constructeur complet ---
    public BattleEvent(LocalDateTime timestamp, BattleState state, String playerName, int roundNumber) {
        super(timestamp);
        this.state = state;
        this.playerName = playerName;
        this.roundNumber = roundNumber;
    }

    @Override
    public LogEventType getEventType() {
        return LogEventType.BATTLE;
    }

    public BattleState getState() {
        return state;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    @Override
    public String toString() {
        return "[BattleEvent] " + state +
                (playerName != null ? " - Player: " + playerName : "") +
                (roundNumber > 0 ? " - Round: " + roundNumber : "");
    }

}