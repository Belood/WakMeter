package com.wakfu.domain.model;

import com.wakfu.domain.abilities.Element;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpellStats {
    private final String name;
    private final Map<Element, Integer> damageByElement = new EnumMap<>(Element.class);
    private int castCount = 0;
    private Integer baseCost = null;  // Coût de base du sort (depuis SortsPA.json)
    private int totalPARegained = 0;  // Total des PA regagnés pour ce sort
    private final Set<String> processedCastIds = new HashSet<>();  // Pour éviter de compter plusieurs fois le même cast

    public SpellStats(String name) {
        this.name = name;
    }

    public void addDamage(Element element, int value) {
        addDamage(element, value, null, 0, null);
    }

    public void addDamage(Element element, int value, Integer baseCost, int paRegained) {
        addDamage(element, value, baseCost, paRegained, null);
    }

    public void addDamage(Element element, int value, Integer baseCost, int paRegained, String castId) {
        damageByElement.merge(element, value, Integer::sum);

        // Ne compter le cast qu'une seule fois par castId unique
        if (castId != null && !processedCastIds.contains(castId)) {
            castCount++;
            processedCastIds.add(castId);

            // Enregistrer le baseCost si fourni (normalement constant pour un sort)
            if (baseCost != null && this.baseCost == null) {
                this.baseCost = baseCost;
            }

            // Cumuler les PA regagnés (une seule fois par cast)
            totalPARegained += paRegained;
        } else if (castId == null) {
            // Ancien comportement pour compatibilité (si pas de castId)
            castCount++;

            if (baseCost != null && this.baseCost == null) {
                this.baseCost = baseCost;
            }

            totalPARegained += paRegained;
        }
    }

    public String getName() {
        return name;
    }

    public Map<Element, Integer> getDamageByElement() {
        return damageByElement;
    }

    public int getTotal() {
        return damageByElement.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getCastCount() {
        return castCount;
    }

    public Integer getBaseCost() {
        return baseCost;
    }

    public int getTotalPARegained() {
        return totalPARegained;
    }

    /**
     * Calcule le coût effectif total en PA pour ce sort.
     * = (baseCost * castCount) - totalPARegained
     * Retourne null si le baseCost n'est pas connu.
     */
    public Integer getEffectivePACost() {
        if (baseCost == null) return null;
        int totalBaseCost = baseCost * castCount;
        int effective = totalBaseCost - totalPARegained;
        return Math.max(effective, 0); // Ne peut pas être négatif
    }

    /**
     * Calcule le coût effectif moyen par cast.
     * Retourne null si le baseCost n'est pas connu ou castCount = 0.
     */
    public Double getAverageEffectivePACost() {
        Integer effectiveCost = getEffectivePACost();
        if (effectiveCost == null || castCount == 0) return null;
        return (double) effectiveCost / castCount;
    }
}

