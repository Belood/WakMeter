package com.wakfu.parser;
import com.wakfu.domain.actors.Fighter;
import com.wakfu.domain.abilities.Element;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;
public class SpellCastAggregator {
    private static final long EMISSION_DELAY_MS = 3000;
    private SpellCastEvent currentSpellCast;
    private Timer emissionTimer;
    private final Consumer<SpellCastEvent> eventEmitter;
    public SpellCastAggregator(Consumer<SpellCastEvent> eventEmitter) {
        this.eventEmitter = eventEmitter;
    }
    public void startNewSpellCast(SpellCastEvent spellCastEvent) {
        flushCurrentSpellCast();
        this.currentSpellCast = spellCastEvent;
        scheduleEmission();
    }
    public void addPaRegainToCurrentSpell(int paAmount) {
        if (currentSpellCast != null) {
            currentSpellCast.addPaRegain(paAmount);
            rescheduleEmission();
        }
    }
    public void addDamageToCurrentSpell(SpellCastEvent.DamageInstance damage) {
        if (currentSpellCast != null) {
            currentSpellCast.addDamage(damage.getTarget(), damage.getValue(), damage.getElement());
            rescheduleEmission();
        }
    }
    public void addBonusDamageToCurrentSpell(String effectName, Fighter target, int value, Element element) {
        if (currentSpellCast != null) {
            currentSpellCast.addBonusDamage(effectName, target, value, element);
            rescheduleEmission();
        }
    }
    public void flushCurrentSpellCast() {
        cancelTimer();
        if (currentSpellCast != null) {
            emitSpellCast(currentSpellCast);
            currentSpellCast = null;
        }
    }
    public void reset() {
        cancelTimer();
        currentSpellCast = null;
    }
    private void scheduleEmission() {
        cancelTimer();
        emissionTimer = new Timer(true);
        emissionTimer.schedule(new TimerTask() {
            public void run() {
                synchronized (SpellCastAggregator.this) {
                    if (currentSpellCast != null) {
                        emitSpellCast(currentSpellCast);
                        currentSpellCast = null;
                    }
                }
            }
        }, EMISSION_DELAY_MS);
    }
    private void rescheduleEmission() {
        scheduleEmission();
    }
    private void cancelTimer() {
        if (emissionTimer != null) {
            emissionTimer.cancel();
            emissionTimer = null;
        }
    }
    private void emitSpellCast(SpellCastEvent spellCast) {
        if (eventEmitter != null) {
            eventEmitter.accept(spellCast);
        }
    }
}