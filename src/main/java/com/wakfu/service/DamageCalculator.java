package com.wakfu.service;

import com.wakfu.model.FightModel;
import com.wakfu.model.PlayerStats;
import com.wakfu.model.SpellStats;
import com.wakfu.domain.abilities.Element;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe utilitaire stateless permettant de calculer et agréger
 * les statistiques du modèle de combat pour l'affichage ou l'analyse.
 */
public class DamageCalculator {

    // État cache optionnel (peut être nul)
    private FightModel lastModel;

    /**
     * Calcule les dégâts totaux infligés par tous les joueurs.
     */
    public int getTotalDamage(FightModel fight) {
        if (fight == null) return 0;
        return fight.getStatsByPlayer().values().stream()
                .mapToInt(PlayerStats::getTotalDamage)
                .sum();
    }

    /**
     * Retourne les joueurs triés par dégâts infligés (du plus haut au plus bas).
     */
    @SuppressWarnings("unused")
    public List<PlayerStats> getPlayersByDamage(FightModel fight) {
        if (fight == null) return List.of();
        return fight.getStatsByPlayer().values().stream()
                .sorted(Comparator.comparingInt(PlayerStats::getTotalDamage).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Calcule le breakdown global des dégâts par élément.
     */
    @SuppressWarnings("unused")
    public Map<Element, Integer> getDamageByElement(FightModel fight) {
        Map<Element, Integer> result = new EnumMap<>(Element.class);

        if (fight == null) return result;

        fight.getStatsByPlayer().values().forEach(stats -> {
            stats.getSpells().values().forEach(spell -> {
                spell.getDamageByElement().forEach((element, dmg) ->
                        result.merge(element, dmg, Integer::sum)
                );
            });
        });

        return result;
    }

    /**
     * Récupère les dégâts totaux par sort pour un joueur donné.
     */
    @SuppressWarnings("unused")
    public Map<String, Integer> getDamageBySpell(PlayerStats playerStats) {
        if (playerStats == null) return Map.of();

        return playerStats.getSpells().values().stream()
                .collect(Collectors.toMap(
                        SpellStats::getName,
                        SpellStats::getTotal,
                        Integer::sum,
                        LinkedHashMap::new
                ));
    }

    /**
     * Retourne le pourcentage des dégâts d’un joueur par rapport au total global.
     */
    @SuppressWarnings("unused")
    public double getDamagePercent(PlayerStats player, FightModel fight) {
        int total = getTotalDamage(fight);
        return total > 0 ? (double) player.getTotalDamage() / total : 0.0;
    }

    /**
     * Calcule le DPS moyen d’un joueur (simple approximation).
     */
    @SuppressWarnings("unused")
    public double getPlayerDps(PlayerStats player, FightModel fight) {
        if (fight == null || player == null || fight.getRounds().isEmpty()) return 0.0;
        int totalDamage = player.getTotalDamage();
        int totalRounds = fight.getRounds().size();
        return totalRounds > 0 ? (double) totalDamage / totalRounds : totalDamage;
    }

    // === Méthodes pour s'intégrer à la chaîne d'events ===
    public void refreshFromModel(FightModel model) {
        this.lastModel = model; // conserve le modèle si utile
        // calculs paresseux possibles ici, mais on laisse DamageCalculator stateless pour l'instant
        System.out.println("[DamageCalculator] Models refreshed");
    }

    @SuppressWarnings("unused")
    public FightModel getLastModel() {
        return lastModel;
    }

    @SuppressWarnings("unused")
    public void reset() {
        lastModel = null;
    }
}
