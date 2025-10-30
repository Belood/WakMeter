package com.wakfu.domain.actors;

import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.abilities.DamageSourceType;
import com.wakfu.domain.abilities.SpellDamage;
import com.wakfu.domain.event.EventType;

import java.util.*;

/**
 * Représente un joueur (hérite de Fighter).
 * Gère les dégâts directs et indirects, ainsi que le breakdown par sort et par élément.
 */
public class Player extends Fighter {

    private final List<SpellDamage> spells = new ArrayList<>();
    private final Map<String, Integer> damageByAbility = new HashMap<>();
    private final Map<Element, Integer> damageByElement = new EnumMap<>(Element.class);
    private final Map<DamageSourceType, Integer> damageBySourceType = new EnumMap<>(DamageSourceType.class);
    private final Map<String, Element> elementByAbility = new HashMap<>();
    private PlayerClass playerClass;

    public Player(String name, long id, FighterType type) {
        super(name, id, type);

        for (Element e : Element.values()) {
            damageByElement.put(e, 0);
        }
        for (DamageSourceType s : DamageSourceType.values()) {
            damageBySourceType.put(s, 0);
        }
    }

    // --- Gestion des dégâts / soins / boucliers ---
    public void addSpellDamage(Ability ability, int value) {
        if (ability == null || value <= 0) return;

        spells.add(new SpellDamage(ability, value, EventType.DAMAGE));
        damageByAbility.merge(ability.getName(), value, Integer::sum);
        damageByElement.merge(ability.getElement(), value, Integer::sum);
        damageBySourceType.merge(ability.getSourceType(), value, Integer::sum);
        elementByAbility.putIfAbsent(ability.getName(), ability.getElement());
    }

    public void addSpellHeal(Ability ability, int value) {
        if (ability == null || value <= 0) return;
        spells.add(new SpellDamage(ability, value, EventType.HEAL));
    }

    public void addSpellShield(Ability ability, int value) {
        if (ability == null || value <= 0) return;
        spells.add(new SpellDamage(ability, value, EventType.SHIELD));
    }

    // --- Totaux globaux ---
    public int getTotalDamage() {
        return spells.stream()
                .filter(s -> s.getType() == EventType.DAMAGE)
                .mapToInt(SpellDamage::getDamageDealt)
                .sum();
    }

    public int getTotalHeal() {
        return spells.stream()
                .filter(s -> s.getType() == EventType.HEAL)
                .mapToInt(SpellDamage::getDamageDealt)
                .sum();
    }

    public int getTotalShield() {
        return spells.stream()
                .filter(s -> s.getType() == EventType.SHIELD)
                .mapToInt(SpellDamage::getDamageDealt)
                .sum();
    }

    // --- Breakdowns ---
    public Map<String, Integer> getDamageByAbility() {
        return Collections.unmodifiableMap(damageByAbility);
    }

    public Map<Element, Integer> getDamageByElement() {
        return Collections.unmodifiableMap(damageByElement);
    }

    public Map<DamageSourceType, Integer> getDamageBySourceType() {
        return Collections.unmodifiableMap(damageBySourceType);
    }

    public List<SpellDamage> getSpells() {
        return Collections.unmodifiableList(spells);
    }

    // --- Métadonnées ---
    public void setPlayerClass(PlayerClass playerClass) {
        this.playerClass = playerClass;
    }

    public PlayerClass getPlayerClass() {
        return playerClass;
    }

    @Override
    public FighterType getType() {
        return FighterType.PLAYER;
    }

    @Override
    public String toString() {
        return String.format("%s [Dégâts: %d, Soins: %d, Boucliers: %d]",
                name, getTotalDamage(), getTotalHeal(), getTotalShield());
    }

    public Map<String, Element> getElementByAbility() {
        return elementByAbility;
    }
}
