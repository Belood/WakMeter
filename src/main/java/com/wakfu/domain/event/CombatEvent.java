package com.wakfu.domain.event;

import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.actors.Fighter;

import java.time.LocalDateTime;

/**
 * Représente un événement de combat (dégâts, soins, boucliers...).
 */
public class CombatEvent extends LogEvent {

    private final Fighter caster;
    private final Fighter target;
    private final Ability ability;
    private final EventType type;
    private final int value;
    private final Element element;

    public CombatEvent(LocalDateTime timestamp,
                       Fighter caster,
                       Fighter target,
                       Ability ability,
                       EventType type,
                       int value) {
        super(timestamp);
        this.caster = caster;
        this.target = target;
        this.ability = ability;
        this.type = type;
        this.value = value;

        // ✅ Détection automatique de l’élément selon la capacité
        this.element = parseElement(ability.getElement());
    }

    public Fighter getCaster() {
        return caster;
    }

    public Fighter getTarget() {
        return target;
    }

    public Ability getAbility() {
        return ability;
    }

    public EventType getType() {
        return type;
    }

    public int getValue() {
        return value;
    }

    public Element getElement() {
        return element;
    }

    @Override
    public LogEventType getEventType() {
        return LogEventType.COMBAT;
    }


    @Override
    public String toString() {
        return String.format("[%s] %s → %s : %d (%s, %s)",
                type, caster.getName(), target.getName(), value, ability.getName(), element);
    }

    // ===============================================================
    // 🔹 Détection élémentaire (basée sur le texte trouvé dans le log)
    // ===============================================================
    private Element parseElement(String elementName) {
        if (elementName == null) return Element.UNKNOWN;
        String norm = elementName.trim().toLowerCase();
        return switch (norm) {
            case "feu" -> Element.FEU;
            case "terre" -> Element.TERRE;
            case "eau" -> Element.EAU;
            case "air" -> Element.AIR;
            case "lumière", "lumiere" -> Element.LUMIERE;
            case "stasis" -> Element.STASIS;
            default -> Element.UNKNOWN;
        };
    }


}
