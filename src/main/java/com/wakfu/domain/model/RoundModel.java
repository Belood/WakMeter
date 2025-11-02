package com.wakfu.domain.model;

import com.wakfu.domain.actors.Player;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RoundModel {

    private final int roundNumber;
    private final LocalDateTime startTime;
    private LocalDateTime endTime;

    private final Map<String, PlayerStats> playerStatsByRound = new LinkedHashMap<>();

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

    public PlayerStats getOrCreatePlayerStats(String playerName) {
        return playerStatsByRound.computeIfAbsent(playerName,
            name -> new PlayerStats(new Player(name, -1, Player.FighterType.PLAYER)));
    }

    public Map<String, PlayerStats> getPlayerStatsByRound() {
        return Collections.unmodifiableMap(playerStatsByRound);
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
