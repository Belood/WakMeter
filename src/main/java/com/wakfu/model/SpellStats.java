package com.wakfu.model;

import com.wakfu.domain.abilities.Element;

import java.util.EnumMap;
import java.util.Map;

public class SpellStats {
    private final String name;
    private final Map<Element, Integer> damageByElement = new EnumMap<>(Element.class);

    public SpellStats(String name) {
        this.name = name;
    }

    public void addDamage(Element element, int value) {
        damageByElement.merge(element, value, Integer::sum);
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
}
