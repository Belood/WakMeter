package com.wakfu.service;

import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.abilities.DamageSourceType;
import com.wakfu.domain.actors.Fighter;
import com.wakfu.domain.actors.Player;
import com.wakfu.domain.event.CombatEvent;
import com.wakfu.domain.event.EventType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère le cumul des dégâts, soins et boucliers pour chaque joueur,
 * avec un breakdown par sort, élément et type de source.
 */
public class DamageCalculator {

    // --- Statistiques globales ---
    private final Map<String, Player> playerStats = new ConcurrentHashMap<>();
    private final Map<Element, Integer> totalByElement = new EnumMap<>(Element.class);

    // Pour éviter le double comptage
    private final Set<Integer> processedEvents = Collections.synchronizedSet(new HashSet<>());

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
        if (event.getType() != EventType.DAMAGE) return; // on ignore les soins et boucliers

        int hash = event.hashCode();
        if (processedEvents.contains(hash)) return; // évite doublon
        processedEvents.add(hash);

        String playerName = event.getCaster().getName();
        if (playerName == null || playerName.isBlank() || event.getCaster().getType() == Fighter.FighterType.ENEMY) return;

        // Regroupe tous les dégâts indirects sous un joueur "Indirect"
        if (event.getSourceType() == DamageSourceType.INDIRECT) {
            playerName = "Indirect";
        }

        Player player = playerStats.computeIfAbsent(
                playerName,
                name -> new Player(name, event.getCaster().getId(), event.getCaster().getType())
        );

        Ability ability = event.getAbility();
        int value = Math.max(event.getValue(), 0);
        Element element = event.getElement();

        // Mise à jour du breakdown du joueur
        player.addSpellDamage(ability, value);

        // Mise à jour du total par élément
        addElementalDamage(element, value);
    }

    /**
     * Ajoute des dégâts à l'élément concerné (Feu, Eau, etc.)
     */
    private void addElementalDamage(Element element, int value) {
        if (element == null) element = Element.INCONNU;
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
     * Réinitialise toutes les statistiques.
     */
    public synchronized void reset() {
        playerStats.clear();
        processedEvents.clear();
        totalByElement.replaceAll((e, v) -> 0);
    }
}
