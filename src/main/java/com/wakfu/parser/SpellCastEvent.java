package com.wakfu.parser;

import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.actors.Fighter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpellCastEvent {
    private final String castId;
    private final LocalDateTime timestamp;
    private final Fighter caster;
    private final Ability ability;
    private final Integer baseCost;
    private int totalPaRegained = 0;
    private final List<DamageInstance> damageInstances = new ArrayList<>();
    private final List<BonusDamageInstance> bonusDamageInstances = new ArrayList<>();

    public SpellCastEvent(LocalDateTime timestamp, Fighter caster, Ability ability, Integer baseCost) {
        this.castId = UUID.randomUUID().toString();
        this.timestamp = timestamp;
        this.caster = caster;
        this.ability = ability;
        this.baseCost = baseCost;
    }
    
    public void addPaRegain(int paAmount) {
        this.totalPaRegained += paAmount;
    }
    
    public void addDamage(Fighter target, int value, Element element) {
        damageInstances.add(new DamageInstance(target, value, element));
    }
    
    public void addBonusDamage(String effectName, Fighter target, int value, Element element) {
        bonusDamageInstances.add(new BonusDamageInstance(effectName, target, value, element));
    }

    public String getCastId() {
        return castId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public Fighter getCaster() {
        return caster;
    }
    
    public Ability getAbility() {
        return ability;
    }
    
    public Integer getBaseCost() {
        return baseCost;
    }
    
    public int getTotalPaRegained() {
        return totalPaRegained;
    }
    
    public List<DamageInstance> getDamageInstances() {
        return damageInstances;
    }
    
    public int getTotalDamage() {
        return damageInstances.stream().mapToInt(DamageInstance::getValue).sum();
    }
    
    public int getTotalBonusDamage() {
        return bonusDamageInstances.stream().mapToInt(BonusDamageInstance::getValue).sum();
    }

    public List<BonusDamageInstance> getBonusDamageInstances() {
        return bonusDamageInstances;
    }

    public boolean hasDamage() {
        return !damageInstances.isEmpty();
    }
    
    public boolean hasBonusDamage() {
        return !bonusDamageInstances.isEmpty();
    }

    public static class DamageInstance {
        private final Fighter target;
        private final int value;
        private final Element element;
        
        public DamageInstance(Fighter target, int value, Element element) {
            this.target = target;
            this.value = value;
            this.element = element;
        }
        
        public Fighter getTarget() {
            return target;
        }

        public int getValue() {
            return value;
        }

        public Element getElement() {
            return element;
        }
    }

    public static class BonusDamageInstance {
        private final String effectName;
        private final Fighter target;
        private final int value;
        private final Element element;

        public BonusDamageInstance(String effectName, Fighter target, int value, Element element) {
            this.effectName = effectName;
            this.target = target;
            this.value = value;
            this.element = element;
        }

        public String getEffectName() {
            return effectName;
        }

        public Fighter getTarget() {
            return target;
        }
        
        public int getValue() {
            return value;
        }
        
        public Element getElement() {
            return element;
        }
    }
}

