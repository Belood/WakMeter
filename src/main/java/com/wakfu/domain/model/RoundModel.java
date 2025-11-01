package com.wakfu.domain.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Représente un round (tour complet de tous les joueurs).
 */
public class RoundModel {

    private final int roundNumber;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;

    private final Map<String, Integer> damageByPlayer = new LinkedHashMap<>();

    public RoundModel(int roundNumber) {
        this.roundNumber = roundNumber;
        this.startTime = LocalDateTime.now();
    }

    public void end() {
        this.endTime = LocalDateTime.now();
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void addDamage(String playerName, int value) {
        damageByPlayer.merge(playerName, value, Integer::sum);
    }

    public Map<String, Integer> getDamageByPlayer() {
        return damageByPlayer;
    }

    // Getters pour sérialisation
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
