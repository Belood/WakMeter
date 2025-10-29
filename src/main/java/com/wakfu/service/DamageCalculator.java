package com.wakfu.service;

import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.actors.Player;
import com.wakfu.domain.event.CombatEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Classe responsable du calcul et de la mise à jour continue
 * des statistiques de combat (dégâts, soins, boucliers).
 */
public class DamageCalculator {

    // --- Données internes ---
    private final Map<String, Player> playerStats = new ConcurrentHashMap<>();

    /**
     * Met à jour les statistiques globales à partir d'un événement de combat.
     * Appelée en temps réel par WakfuMeterApp.
     */
    public synchronized void updateWithEvent(CombatEvent event) {
        if (event == null || event.getCaster() == null) return;

        String playerName = event.getCaster().getName();
        if (playerName == null || playerName.isBlank()) return;

        Player player = playerStats.computeIfAbsent(
                playerName,
                name -> new Player(name, event.getCaster().getId(), event.getCaster().getType())
        );

        Ability ability = event.getAbility();
        int value = event.getValue();

        // Selon le type d'événement, on modifie le total approprié
        switch (event.getType()) {
            case DAMAGE -> addSpellDamage(player, ability, value);
            case HEAL -> addSpellHeal(player, ability, value);
            case SHIELD -> addSpellShield(player, ability, value);
            default -> { /* ignoré pour les autres types */ }
        }
    }

    /**
     * Ajoute des dégâts à un joueur pour un sort.
     */
    private void addSpellDamage(Player player, Ability ability, int dmg) {
        player.addSpellDamage(ability, dmg);
    }

    /**
     * Ajoute un soin à un joueur pour un sort.
     */
    private void addSpellHeal(Player player, Ability ability, int heal) {
        player.addSpellHeal(ability, heal);
    }

    /**
     * Ajoute un bouclier (armure générée) à un joueur pour un sort.
     */
    private void addSpellShield(Player player, Ability ability, int shield) {
        player.addSpellShield(ability, shield);
    }

    /**
     * Retourne la liste complète des joueurs triée par dégâts infligés.
     */
    public List<Player> getPlayers() {
        List<Player> list = new ArrayList<>(playerStats.values());
        list.sort(Comparator.comparingInt(Player::getTotalDamage).reversed());
        return list;
    }

    /**
     * Calcule les dégâts totaux infligés par tous les joueurs.
     */
    public int calculateTotalDamage(List<Player> players) {
        return players.stream().mapToInt(Player::getTotalDamage).sum();
    }
}