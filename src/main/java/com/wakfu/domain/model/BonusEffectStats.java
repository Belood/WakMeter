package com.wakfu.domain.model;
import com.wakfu.domain.abilities.Element;
import java.util.EnumMap;
import java.util.Map;
public class BonusEffectStats {
    private final String effectName;
    private final Map<Element, Integer> damageByElement = new EnumMap<>(Element.class);
    public BonusEffectStats(String effectName) {
        this.effectName = effectName;
    }
    public void addDamage(Element element, int value) {
        damageByElement.merge(element, value, Integer::sum);
    }
    public String getEffectName() {
        return effectName;
    }
    public Map<Element, Integer> getDamageByElement() {
        return damageByElement;
    }
    public int getTotal() {
        return damageByElement.values().stream().mapToInt(Integer::intValue).sum();
    }
}