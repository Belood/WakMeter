package com.wakfu.service;

import com.wakfu.domain.actors.Player;
import com.wakfu.domain.event.BattleEvent;
import com.wakfu.domain.event.CombatEvent;
import com.wakfu.domain.event.LogEvent;
import com.wakfu.data.MessageProvider;
import com.wakfu.domain.model.FightModel;
import com.wakfu.domain.model.PlayerStats;
import com.wakfu.storage.FightHistoryManager;

import java.util.function.Consumer;

public class EventProcessor {

    private final FightModel currentFight = new FightModel();

    // Hook pour que l'UI/DamageCalculator s'abonne au modèle
    public void addModelListener(Consumer<FightModel> listener) {
        currentFight.addListener(listener);
    }

    public void removeModelListener(Consumer<FightModel> listener) {
        currentFight.removeListener(listener);
    }

    // Status listener pour messages texte destinés à l'UI
    private Consumer<String> statusListener;

    public void addStatusListener(Consumer<String> listener) {
        this.statusListener = listener;
    }

    private void fireStatus(String message) {
        if (statusListener != null) {
            try { statusListener.accept(message); } catch (Exception ignored) {}
        }
    }

    // Flag pour savoir si la sauvegarde d'historique est activée
    private volatile boolean historyEnabled = false;

    public void setHistoryEnabled(boolean enabled) {
        this.historyEnabled = enabled;
    }

    public boolean isHistoryEnabled() {
        return historyEnabled;
    }

    public void onEvent(LogEvent event) {
        process(event);
    }

    public void process(LogEvent event) {
        if (event instanceof BattleEvent) {
            handleBattleEvent((BattleEvent) event);
        } else if (event instanceof CombatEvent) {
            handleCombatEvent((CombatEvent) event);
        }
    }

    // Hook appelé au démarrage d'un combat (START) pour permettre à l'UI/damageCalculator
    // d'effectuer un reset avant que les listeners soient notifiés.
    private Runnable onBattleStart;

    public void setOnBattleStart(Runnable onBattleStart) { this.onBattleStart = onBattleStart; }

    // Hook appelé au démarrage d'un combat (uniquement au START)
    private void handleBattleEvent(BattleEvent event) {
        switch (event.getState()) {
            case START: {
                currentFight.reset();
                currentFight.setStartTime(event.getTimestamp());
                // Signaler le status avant le reset hook
                fireStatus(MessageProvider.combatInProgress());
                // Appeler le hook de démarrage de combat (si configuré) afin que l'UI et
                // le calculator puissent se resetter avant de recevoir les notifications du modèle.
                if (onBattleStart != null) {
                    try { onBattleStart.run(); } catch (Exception ignored) {}
                }
                // Après le reset des composants, notifier les listeners du modèle
                currentFight.notifyListeners();
                break;
            }
            case END: {
                currentFight.setEndTime(event.getTimestamp());
                currentFight.notifyListeners();
                // Sauvegarde automatique du combat uniquement si activée
                if (historyEnabled) {
                    try { FightHistoryManager.saveFight(currentFight); } catch (Exception ignored) {}
                }
                fireStatus(MessageProvider.waitingCombat());
                break;
            }
            case ROUND_START: {
                currentFight.startRound();
                break;
            }
            case ROUND_END: {
                currentFight.endRound();
                break;
            }
            case START_TURN: {
                if (event.getPlayerName() != null) {
                    Player p = new Player(event.getPlayerName(), -1, Player.FighterType.PLAYER);
                    currentFight.startTurn(p);
                }
                break;
            }
            case END_TURN: {
                if (event.getPlayerName() != null) {
                    Player p = new Player(event.getPlayerName(), -1, Player.FighterType.PLAYER);
                    currentFight.endTurn(p);
                }
                break;
            }
        }
    }

    private void handleCombatEvent(CombatEvent event) {
        // caster may be NPC; only process if caster is Player
        if (event.getCaster() instanceof Player) {
            Player caster = (Player) event.getCaster();

            // Récupère ou crée le PlayerStats correspondant au niveau du combat
            PlayerStats stats = currentFight.getStatsByPlayer()
                    .computeIfAbsent(caster.getName(), name -> new PlayerStats(caster));

            // Également update les stats du round courant si un round est actif
            com.wakfu.domain.model.RoundModel currentRound = currentFight.getCurrentRoundModel();
            PlayerStats roundStats = null;
            if (currentRound != null) {
                roundStats = currentRound.getOrCreatePlayerStats(caster.getName());
            }

            switch (event.getType()) {
                case DAMAGE:
                    stats.addDamage(event);
                    if (roundStats != null) roundStats.addDamage(event);
                    break;
                case HEAL:
                    stats.addHeal(event);
                    if (roundStats != null) roundStats.addHeal(event);
                    break;
                case SHIELD:
                    stats.addShield(event);
                    if (roundStats != null) roundStats.addShield(event);
                    break;
                default:
                    break;
            }

            // Notify after updating stats
            currentFight.notifyListeners();
        }
    }

    public FightModel getCurrentFight() {
        return currentFight;
    }
}