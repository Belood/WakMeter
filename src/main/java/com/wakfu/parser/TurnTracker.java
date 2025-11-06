package com.wakfu.parser;

import com.wakfu.domain.event.BattleEvent;
import com.wakfu.service.EventProcessor;

import java.time.LocalDateTime;
import java.util.*;

public class TurnTracker {
    private String currentPlayerTurn;
    private int roundNumber = 1;
    private final Set<String> playersThisRound = new HashSet<>();
    private final Set<String> playersKO = new HashSet<>();
    private String firstPlayerThisRound;
    private final List<String> turnOrderThisRound = new ArrayList<>();
    private long lastTurnStartTime = 0;
    private long lastActivityTime = 0;

    private static final int MIN_PLAYERS_FOR_ROUND = 2;

    private final EventProcessor eventProcessor;
    private final FighterRegistry fighterRegistry;

    public TurnTracker(EventProcessor eventProcessor, FighterRegistry fighterRegistry) {
        this.eventProcessor = eventProcessor;
        this.fighterRegistry = fighterRegistry;
    }

    public void markPlayerKO(String playerName) {
        playersKO.add(playerName);
        System.out.printf("[Parser] %s est KO (ne jouera plus ce combat)%n", playerName);

        if (playerName.equals(currentPlayerTurn)) {
            System.out.printf("[Parser] Tour de %s terminé (KO)%n", currentPlayerTurn);
            currentPlayerTurn = null;
        }
    }

    public void revivePlayer(String playerName) {
        if (playersKO.remove(playerName)) {
            System.out.printf("[Parser] %s est ressuscité (peut rejouer)%n", playerName);
        }
    }

    public void endCurrentTurn() {
        if (currentPlayerTurn != null) {
            System.out.printf("[Parser] Tour de %s terminé%n", currentPlayerTurn);
            currentPlayerTurn = null;
        }
    }

    public void detectTurnStart(String playerName) {
        long currentTime = System.currentTimeMillis();
        lastActivityTime = currentTime;

        if (playersKO.contains(playerName)) {
            System.out.printf("[Parser] IGNORED cast from KO player: %s%n", playerName);
            return;
        }

        if (currentPlayerTurn == null || !currentPlayerTurn.equals(playerName)) {
            if (playersThisRound.isEmpty()) {
                firstPlayerThisRound = playerName;
                turnOrderThisRound.clear();
                eventProcessor.process(new BattleEvent(LocalDateTime.now(),
                        BattleEvent.BattleState.ROUND_START, roundNumber));
                System.out.println("[Round] >>> ROUND " + roundNumber + " START <<<");
            }

            if (currentPlayerTurn != null && !currentPlayerTurn.equals(playerName)) {
                eventProcessor.process(new BattleEvent(LocalDateTime.now(),
                        BattleEvent.BattleState.END_TURN, currentPlayerTurn));
                System.out.println("[Turn] <<< " + currentPlayerTurn + " END TURN <<<");
            }

            currentPlayerTurn = playerName;
            playersThisRound.add(playerName);
            turnOrderThisRound.add(playerName);
            lastTurnStartTime = currentTime;

            eventProcessor.process(new BattleEvent(LocalDateTime.now(),
                    BattleEvent.BattleState.START_TURN, playerName));
            System.out.println("[Turn] >>> " + playerName + " START TURN <<<");

            checkRoundEnd(playerName);
        }
    }

    private void checkRoundEnd(String playerName) {
        boolean shouldEndRound = false;
        String endReason = "";

        if (firstPlayerThisRound != null
                && playerName.equals(firstPlayerThisRound)
                && playersThisRound.size() >= MIN_PLAYERS_FOR_ROUND) {
            shouldEndRound = true;
            endReason = "first player returned";
        } else if (turnOrderThisRound.size() >= 2) {
            int lastIdx = turnOrderThisRound.size() - 1;
            String lastPlayer = turnOrderThisRound.get(lastIdx);
            String secondLastPlayer = turnOrderThisRound.get(lastIdx - 1);

            if (lastPlayer.equals(secondLastPlayer) && playersThisRound.size() >= MIN_PLAYERS_FOR_ROUND) {
                shouldEndRound = true;
                endReason = "player played twice consecutively";
            }
        } else if (playersThisRound.size() >= fighterRegistry.countActivePlayers(playersKO)
                && fighterRegistry.countActivePlayers(playersKO) >= MIN_PLAYERS_FOR_ROUND) {
            shouldEndRound = true;
            endReason = "all active players have played";
        }

        if (shouldEndRound) {
            eventProcessor.process(new BattleEvent(LocalDateTime.now(),
                    BattleEvent.BattleState.ROUND_END, roundNumber));
            System.out.println("[Round] <<< ROUND " + roundNumber + " END (" + endReason + ") <<<");
            System.out.println("[Round] Players who played: " + playersThisRound);
            System.out.println("[Round] Turn order: " + turnOrderThisRound);

            roundNumber++;
            playersThisRound.clear();
            turnOrderThisRound.clear();
            firstPlayerThisRound = playerName;
        }
    }

    public void reset() {
        currentPlayerTurn = null;
        firstPlayerThisRound = null;
        playersThisRound.clear();
        playersKO.clear();
        turnOrderThisRound.clear();
        roundNumber = 1;
        lastTurnStartTime = 0;
        lastActivityTime = 0;
    }

    public boolean isPlayerKO(String playerName) {
        return playersKO.contains(playerName);
    }
}

