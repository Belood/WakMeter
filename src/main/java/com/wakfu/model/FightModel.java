package com.wakfu.model;

import com.wakfu.domain.actors.Player;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

/**
 * Représente un combat complet, incluant rounds et statistiques.
 */
public class FightModel {

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private final Map<String, PlayerStats> statsByPlayer = new HashMap<>();
    private final List<RoundModel> rounds = new ArrayList<>();

    private int currentRound = 1;
    private Player currentPlayerTurn;

    // Listeners pour notifier quand le modèle change
    private transient final List<Consumer<FightModel>> listeners = new ArrayList<>();

    public void startRound() {
        rounds.add(new RoundModel(currentRound++));
        System.out.println("[FightModel] 🟣 Round started");
        notifyListeners();
    }

    public void endRound() {
        System.out.println("[FightModel] 🔵 Round ended");
        notifyListeners();
    }

    public void startTurn(Player player) {
        currentPlayerTurn = player;
        System.out.println("[FightModel] ▶ Début du tour : " + player.getName());
        notifyListeners();
    }

    public void endTurn(Player player) {
        if (currentPlayerTurn != null && currentPlayerTurn.equals(player)) {
            System.out.println("[FightModel] ⏹ Fin du tour : " + player.getName());
            currentPlayerTurn = null;
            notifyListeners();
        }
    }

    public Map<String, PlayerStats> getStatsByPlayer() {
        return statsByPlayer;
    }

    public List<RoundModel> getRounds() {
        return rounds;
    }

    public void reset() {
        statsByPlayer.clear();
        rounds.clear();
        currentRound = 1;
        currentPlayerTurn = null;
        notifyListeners();
    }

    // Getters & setters
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
        notifyListeners();
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        notifyListeners();
    }

    // Ajout des getters pour permettre la lecture du timestamp
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    // === Listeners management ===
    public void addListener(Consumer<FightModel> listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(Consumer<FightModel> listener) {
        listeners.remove(listener);
    }

    public void notifyListeners() {
        for (Consumer<FightModel> l : listeners) {
            try {
                l.accept(this);
            } catch (Exception e) {
                System.err.println("[FightModel] Listener error: " + e.getMessage());
            }
        }
    }

    // Getter utilitaires pour sérialisation complète
    public int getCurrentRound() { return currentRound; }
    public Player getCurrentPlayerTurn() { return currentPlayerTurn; }
}
