package com.wakfu.parser;

import com.wakfu.domain.abilities.IndirectAbilities;
import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.abilities.DamageSourceType;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.actors.Fighter;
import com.wakfu.domain.actors.Player;
import com.wakfu.domain.event.BattleEvent;
import com.wakfu.domain.event.CombatEvent;
import com.wakfu.domain.event.EventType;
import com.wakfu.domain.event.LogEvent;
import com.wakfu.service.EventProcessor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogProcessor {
    private boolean inCombat = false;
    private final EventProcessor eventProcessor;
    private final FighterRegistry fighterRegistry;
    private final TurnTracker turnTracker;
    private final SpellCastAggregator spellCastAggregator;

    private final Map<String, Ability> lastAbilityByCaster = new HashMap<>();
    private final Map<String, Long> lastCastTime = new HashMap<>();

    private static final long RECENT_CAST_WINDOW_MS = 5_000;
    private static final long STICKY_CAST_WINDOW_MS = 10_000;

    private static final Pattern TIME_PREFIX =
            Pattern.compile("^[A-Z]+\\s(\\d{2}:\\d{2}:\\d{2}),(\\d{3})");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public LogProcessor(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
        this.fighterRegistry = new FighterRegistry();
        this.turnTracker = new TurnTracker(eventProcessor, fighterRegistry);
        this.spellCastAggregator = new SpellCastAggregator(this::emitCombatEventsFromSpellCast);
    }

    private void emitCombatEventsFromSpellCast(SpellCastEvent spellCast) {
        if (spellCast == null || !spellCast.hasDamage()) {
            return;
        }

        System.out.printf("[Parser] Émission SpellCast: %s par %s - %d dégâts totaux, %d bonus, %d PA regagnés (castId: %s)%n",
                spellCast.getAbility().getName(),
                spellCast.getCaster().getName(),
                spellCast.getTotalDamage(),
                spellCast.getTotalBonusDamage(),
                spellCast.getTotalPaRegained(),
                spellCast.getCastId());

        for (SpellCastEvent.DamageInstance damage : spellCast.getDamageInstances()) {
            CombatEvent event = new CombatEvent(
                spellCast.getTimestamp(),
                spellCast.getCaster(),
                damage.getTarget(),
                spellCast.getAbility(),
                EventType.DAMAGE,
                damage.getValue(),
                damage.getElement(),
                spellCast.getBaseCost(),
                spellCast.getTotalPaRegained(),
                spellCast.getCastId()
            );

            process(event);
        }

        // Émettre les bonus damages
        for (SpellCastEvent.BonusDamageInstance bonus : spellCast.getBonusDamageInstances()) {
            com.wakfu.domain.event.BonusDamageEvent bonusEvent = new com.wakfu.domain.event.BonusDamageEvent(
                spellCast.getTimestamp(),
                spellCast.getCaster(),
                bonus.getEffectName(),
                bonus.getElement(),
                bonus.getValue(),
                spellCast.getCastId()
            );

            process(bonusEvent);
        }
    }

    public void process(LogEvent event) {
        if (event == null) return;
        eventProcessor.onEvent(event);
    }

    public void processLine(String line) {
        if (line.isEmpty()) return;
        if (PatternExclusions.shouldIgnore(line)) return;
        line = PatternExclusions.clean(line);

        long tsNow = extractLogMillis(line);

        if (LogPatterns.START_COMBAT.matcher(line).find()) {
            handleCombatStart();
            return;
        }

        if (LogPatterns.END_COMBAT.matcher(line).find()) {
            handleCombatEnd();
            return;
        }

        if (!inCombat) return;

        Matcher mJoin = LogPatterns.PLAYER_JOIN.matcher(line);
        if (mJoin.find()) {
            handlePlayerJoin(mJoin);
            return;
        }

        Matcher mKO = LogPatterns.PLAYER_KO.matcher(line);
        if (mKO.find()) {
            handlePlayerKO(mKO);
            return;
        }

        Matcher mRevived = LogPatterns.PLAYER_REVIVED.matcher(line);
        if (mRevived.find()) {
            handlePlayerRevived(mRevived);
            return;
        }

        Matcher mTurnEnd = LogPatterns.TURN_END.matcher(line);
        if (mTurnEnd.find()) {
            handleTurnEnd(mTurnEnd);
            return;
        }

        Matcher mCast = LogPatterns.CAST_SPELL.matcher(line);
        if (mCast.find()) {
            handleSpellCast(mCast, tsNow);
            return;
        }

        Matcher mPaGain = LogPatterns.PA_GAIN.matcher(line);
        if (mPaGain.find()) {
            handlePaGain(mPaGain);
            return;
        }

        Matcher mDirect = LogPatterns.DAMAGE_DIRECT.matcher(line);
        if (mDirect.find()) {
            handleDirectDamage(mDirect, tsNow);
            return;
        }

        Matcher mIndirect = LogPatterns.DAMAGE_INDIRECT.matcher(line);
        if (mIndirect.find()) {
            handleIndirectDamage(mIndirect, tsNow);
            return;
        }
    }

    private void handleCombatStart() {
        inCombat = true;
        fighterRegistry.clear();
        lastAbilityByCaster.clear();
        lastCastTime.clear();
        turnTracker.reset();
        spellCastAggregator.reset();
        process(new BattleEvent(LocalDateTime.now(), BattleEvent.BattleState.START));
        System.out.println("[Parser] >>> Combat started <<<");
    }

    private void handleCombatEnd() {
        spellCastAggregator.flushCurrentSpellCast();
        inCombat = false;
        process(new BattleEvent(LocalDateTime.now(), BattleEvent.BattleState.END));
        System.out.println("[Parser] <<< Combat ended >>>");
    }

    private void handlePlayerJoin(Matcher matcher) {
        String name = matcher.group(1).trim();
        long id = Long.parseLong(matcher.group(2));
        boolean isAI = Boolean.parseBoolean(matcher.group(3));
        Fighter fighter = fighterRegistry.getOrCreate(name, isAI, id);
        System.out.printf("[Parser] Fighter detected: %s [%s, id=%d]%n", name, fighter.getType(), id);
    }

    private void handlePlayerKO(Matcher matcher) {
        String playerName = matcher.group(1).trim();
        turnTracker.markPlayerKO(playerName);
    }

    private void handlePlayerRevived(Matcher matcher) {
        String playerName = matcher.group(1).trim();
        turnTracker.revivePlayer(playerName);
    }

    private void handleTurnEnd(Matcher matcher) {
        String seconds = matcher.group(1);
        turnTracker.endCurrentTurn();
        System.out.printf("[Parser] Tour terminé (%s secondes reportées)%n", seconds);
    }

    private void handleSpellCast(Matcher matcher, long tsNow) {
        String casterName = matcher.group(1).trim();
        String spellName = matcher.group(2).trim();

        Fighter caster = fighterRegistry.get(casterName);
        if (caster != null && caster.getType() != Fighter.FighterType.PLAYER) {
            return;
        }

        String mapped = SpecialCase.specialSpell(spellName);
        if (mapped != null) {
            System.out.printf("[Parser] SpecialCase mapping: %s -> %s%n", spellName, mapped);
            spellName = mapped;
        }

        turnTracker.detectTurnStart(casterName);

        if (caster instanceof Player) {
            Player player = (Player) caster;
            detectPlayerClass(player, spellName);
        }

        Ability ability = new Ability(spellName, "Sort", Element.INCONNU, DamageSourceType.DIRECT);
        lastAbilityByCaster.put(casterName, ability);
        lastCastTime.put(casterName, tsNow);

        Integer baseCost = getBaseCostForSpell(spellName);

        SpellCastEvent spellCastEvent = new SpellCastEvent(
            LocalDateTime.now(),
            caster != null ? caster : new Player(casterName, -1, Fighter.FighterType.PLAYER),
            ability,
            baseCost
        );

        spellCastAggregator.startNewSpellCast(spellCastEvent);
        System.out.printf("[Parser] %s lance %s%n", casterName, spellName);
    }

    private void handlePaGain(Matcher matcher) {
        String playerName = matcher.group(1).trim();
        int paGain = parseIntSafe(matcher.group(2));

        Fighter fighter = fighterRegistry.get(playerName);
        if (fighter != null && fighter.getType() == Fighter.FighterType.PLAYER) {
            spellCastAggregator.addPaRegainToCurrentSpell(paGain);
            System.out.printf("[Parser] %s regagne %d PA%n", playerName, paGain);
        }
    }

    private void handleDirectDamage(Matcher matcher, long tsNow) {
        DamageValidator.DamageInfo damageInfo = new DamageValidator.DamageInfo(matcher);

        Fighter caster = findRecentCaster(tsNow);
        Fighter target = fighterRegistry.getOrCreateEnemy(damageInfo.getTargetName());

        if (DamageValidator.shouldIgnoreDamage(caster, target, "direct damage")) {
            return;
        }

        Ability ability = lastAbilityByCaster.getOrDefault(
                caster.getName(),
                new Ability("Inconnu", "Sort direct", damageInfo.getElement(), DamageSourceType.DIRECT)
        );
        ability.setElement(damageInfo.getElement());

        SpellCastEvent.DamageInstance damage = new SpellCastEvent.DamageInstance(
            target,
            damageInfo.getValue(),
            damageInfo.getElement()
        );

        spellCastAggregator.addDamageToCurrentSpell(damage);

        System.out.printf("[Parser] DIRECT: %s → %s %d (%s)%n",
                caster.getName(), target.getName(), damageInfo.getValue(), damageInfo.getElement());
    }

    private void handleIndirectDamage(Matcher matcher, long tsNow) {
        String targetName = matcher.group(1).trim();
        int value = parseIntSafe(matcher.group(2));
        String tail = matcher.group(3).trim();

        Matcher tm = Pattern.compile("\\(([^)]*)\\)").matcher(tail);
        List<String> tokens = new ArrayList<>();
        while (tm.find()) tokens.add(tm.group(1).trim());

        Element element = Element.INCONNU;
        String effectName = null;

        for (String token : tokens) {
            Element e = Element.fromString(token);
            if (e != Element.INCONNU) element = e;
            else effectName = token;
        }

        if (effectName == null) effectName = "Effet indirect";

        boolean isTrueIndirect = IndirectAbilities.isIndirect(effectName);

        String mapped = SpecialCase.specialSpell(effectName);
        if (mapped != null) {
            System.out.printf("[Parser] SpecialCase mapping (indirect): %s -> %s%n", effectName, mapped);
            effectName = mapped;
        }

        Fighter target = fighterRegistry.getOrCreateEnemy(targetName);

        if (isTrueIndirect) {
            // Dégâts vraiment indirects -> créer un SpellCastEvent séparé pour le joueur virtuel "Indirect"
            Fighter indirectCaster = fighterRegistry.getOrCreate("Indirect", false, -999);

            if (DamageValidator.shouldIgnoreDamage(indirectCaster, target, "indirect damage")) {
                return;
            }

            System.out.printf("[Parser] TRUE INDIRECT: %s %d (%s) [caster: Indirect]%n", effectName, value, element);

            // Créer une ability pour l'effet indirect
            Ability indirectAbility = new Ability(effectName, "Effet indirect", element, DamageSourceType.INDIRECT);

            // Créer un nouveau SpellCastEvent pour cet effet indirect
            SpellCastEvent indirectSpellCast = new SpellCastEvent(
                LocalDateTime.now(),
                indirectCaster,
                indirectAbility,
                null // Pas de coût en PA pour les effets indirects
            );

            // Ajouter les dégâts à ce spell cast
            indirectSpellCast.addDamage(target, value, element);

            // Émettre immédiatement cet événement (pas besoin d'attendre un timeout)
            emitCombatEventsFromSpellCast(indirectSpellCast);
        } else {
            // Effet bonus (REASSIGNED AS DIRECT) -> l'ajouter au spell cast du joueur actuel
            Fighter caster = findRecentCaster(tsNow);

            if (DamageValidator.shouldIgnoreDamage(caster, target, "indirect damage")) {
                return;
            }

            System.out.printf("[Parser] REASSIGNED AS DIRECT (BONUS): %s %d (%s)%n", effectName, value, element);
            spellCastAggregator.addBonusDamageToCurrentSpell(effectName, target, value, element);
        }
    }

    private Fighter findRecentCaster(long tsNow) {
        Optional<String> caster = lastCastTime.entrySet().stream()
                .filter(e -> tsNow - e.getValue() <= RECENT_CAST_WINDOW_MS)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        if (caster.isPresent())
            return fighterRegistry.get(caster.get());

        Optional<String> sticky = lastCastTime.entrySet().stream()
                .filter(e -> tsNow - e.getValue() <= STICKY_CAST_WINDOW_MS)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        if (sticky.isPresent())
            return fighterRegistry.get(sticky.get());

        return fighterRegistry.getOrCreate("Indirect", false, -999);
    }

    private void detectPlayerClass(Player player, String spellName) {
        if (player.getPlayerClass() == null) {
            String detectedClass = com.wakfu.data.SpellCostProvider.getClassForSpell(spellName);
            if (detectedClass != null) {
                com.wakfu.domain.actors.PlayerClass playerClass =
                        com.wakfu.domain.actors.PlayerClass.fromString(detectedClass);
                player.setPlayerClass(playerClass);
                System.out.printf("[Parser] Classe détectée pour %s: %s%n", player.getName(), detectedClass);
            }
        }
    }

    private Integer getBaseCostForSpell(String spellName) {
        return com.wakfu.data.SpellCostProvider.getCostFor(null, spellName);
    }

    private int parseIntSafe(String s) {
        try {
            String cleaned = s.replaceAll("[^0-9]", "");
            return cleaned.isEmpty() ? 0 : Integer.parseInt(cleaned);
        } catch (Exception e) {
            System.err.println("[Parser] Invalid number: " + s);
            return 0;
        }
    }

    private long extractLogMillis(String line) {
        Matcher m = TIME_PREFIX.matcher(line);
        if (!m.find()) return System.currentTimeMillis();
        try {
            String formatted = m.group(1) + "." + m.group(2);
            var lt = java.time.LocalTime.parse(formatted, TIME_FORMAT);
            return lt.toSecondOfDay() * 1000L + lt.getNano() / 1_000_000L;
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}

