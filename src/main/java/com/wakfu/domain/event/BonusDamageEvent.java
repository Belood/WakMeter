package com.wakfu.domain.event;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.actors.Fighter;
import java.time.LocalDateTime;
public class BonusDamageEvent extends LogEvent {
    private final Fighter caster;
    private final String effectName;
    private final Element element;
    private final int value;
    private final String castId;
    public BonusDamageEvent(LocalDateTime timestamp, Fighter caster, String effectName, Element element, int value, String castId) {
        super(timestamp);
        this.caster = caster;
        this.effectName = effectName;
        this.element = element;
        this.value = value;
        this.castId = castId;
    }
    public Fighter getCaster() {
        return caster;
    }
    public String getEffectName() {
        return effectName;
    }
    public Element getElement() {
        return element;
    }
    public int getValue() {
        return value;
    }
    public String getCastId() {
        return castId;
    }
    @Override
    public LogEventType getEventType() {
        return LogEventType.BONUS_DAMAGE;
    }
}