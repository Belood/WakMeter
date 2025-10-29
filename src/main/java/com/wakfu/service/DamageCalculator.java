package com.wakfu.service;

import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.actors.Player;
import com.wakfu.domain.event.CombatEvent;
import com.wakfu.domain.event.EventType;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère le cumul des dégâts, soins et boucliers pour chaque joueur,
 * avec un breakdown par sort et par élément.
 */
public class DamageCalculator {

    // Map principale : nom du joueur -> Player
    private final Map<String, Player> playerStats = new ConcurrentHashMap<>();

    // Breakdown élémentaire global : Feu, Terre, Eau, Air, Lumière, Stasis
    private final Map<Element, Integer> totalByElement = new EnumMap<>(Element.class);

    public DamageCalculator() {
        for (Element e : Element.values()) {
            totalByElement.put(e, 0);
        }
    }

    /**
     * Met à jour les statistiques à partir d’un événement de combat.
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
        Element element = event.getElement();

        switch (event.getType()) {
            case DAMAGE -> {
                player.addSpellDamage(ability, value);
                addElementalDamage(element, value);
            }
            case HEAL -> player.addSpellHeal(ability, value);
            case SHIELD -> player.addSpellShield(ability, value);
            default -> {}
        }
    }

    /**
     * Ajoute des dégâts à l'élément concerné (Feu, Eau, etc.)
     */
    private void addElementalDamage(Element element, int value) {
        if (element == null) element = Element.UNKNOWN;
        totalByElement.merge(element, value, Integer::sum);
    }

    /**
     * Liste complète des joueurs triés par dégâts infligés.
     */
    public List<Player> getPlayers() {
        List<Player> list = new ArrayList<>(playerStats.values());
        list.sort(Comparator.comparingInt(Player::getTotalDamage).reversed());
        return list;
    }

    /**
     * Dégâts totaux (tous joueurs confondus).
     */
    public int calculateTotalDamage(List<Player> players) {
        return players.stream().mapToInt(Player::getTotalDamage).sum();
    }

    /**
     * Breakdown global par élément.
     */
    public Map<Element, Integer> getElementalBreakdown() {
        return Collections.unmodifiableMap(totalByElement);
    }

    /**
     * Réinitialise toutes les statistiques (début de combat).
     */
    public synchronized void reset() {
        playerStats.clear();
        totalByElement.replaceAll((e, v) -> 0);
    }


}
