package com.wakfu.parser;

import com.wakfu.data.IndirectAbilities;
import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.abilities.DamageSourceType;
import com.wakfu.domain.abilities.Element;
import com.wakfu.domain.actors.Enemy;
import com.wakfu.domain.actors.Fighter;
import com.wakfu.domain.actors.Player;
import com.wakfu.domain.event.BattleEvent;
import com.wakfu.domain.event.CombatEvent;
import com.wakfu.domain.event.EventType;
import com.wakfu.domain.event.LogEvent;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogProcessor {
    private boolean inCombat = false;

    private final Map<String, Fighter> fighters = new HashMap<>();
    private final Map<String, Ability> lastAbilityByCaster = new HashMap<>();
    private final Map<String, Long> lastCastTime = new HashMap<>();
    private String currentPlayerTurn = null;      // joueur actuellement actif
    private int roundNumber = 1;                  // numéro du round en cours
    private final Set<String> playersThisRound = new HashSet<>(); // joueurs ayant déjà joué ce round
    private String firstPlayerThisRound = null;   // identifie le joueur qui ouvre le round

    private static final long RECENT_CAST_WINDOW_MS = 10_000;
    private static final long STICKY_CAST_WINDOW_MS = 20_000;

    private static final Pattern TIME_PREFIX =
            Pattern.compile("^[A-Z]+\\s(\\d{2}:\\d{2}:\\d{2}),(\\d{3})");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    // === Analyse de chaque ligne ===
    public void processLine(String line, Consumer<LogEvent> onEvent) {
        if (line.isEmpty()) return;
        line = line
                .replace("(Parade !)", "")
                .replace("(Parade!)", "")
                .replace("(Lumière)", "")
                .replace("(Critiques)","")
                .replaceAll("\\s+", " ")
                .trim();

        long tsNow = extractLogMillis(line);

        if (LogPatterns.START_COMBAT.matcher(line).find()) {
            inCombat = true;
            fighters.clear();
            lastAbilityByCaster.clear();
            onEvent.accept(new BattleEvent(LocalDateTime.now(), BattleEvent.BattleState.START));
            System.out.println("[Parser] >>> Combat started <<<");
            return;
        }

        if (LogPatterns.END_COMBAT.matcher(line).find()) {
            inCombat = false;
            onEvent.accept(new BattleEvent(LocalDateTime.now(), BattleEvent.BattleState.END));
            System.out.println("[Parser] <<< Combat ended >>>");
            return;
        }

        if (!inCombat) return;

        // --- Joueur rejoint le combat ---
        Matcher mJoin = LogPatterns.PLAYER_JOIN.matcher(line);
        if (mJoin.find()) {
            String name = mJoin.group(1).trim();
            long id = Long.parseLong(mJoin.group(2));
            boolean isAI = Boolean.parseBoolean(mJoin.group(3));
            Fighter f = isAI ? new Enemy(name, id, name) : new Player(name, id, Fighter.FighterType.PLAYER);
            fighters.put(name, f);
            System.out.printf("[Parser] Fighter detected: %s [%s, id=%d]%n", name, f.getType(), id);
            return;
        }

        // --- Sort lancé ---
        Matcher mCast = LogPatterns.CAST_SPELL.matcher(line);
        if (mCast.find()) {
            String casterName = mCast.group(1).trim();
            String spellName = mCast.group(2).trim();

            detectTurnStart(casterName, onEvent);

            Ability ability = new Ability(spellName, "Sort", Element.INCONNU, DamageSourceType.DIRECT);
            lastAbilityByCaster.put(casterName, ability);
            lastCastTime.put(casterName, tsNow);
            System.out.printf("[Parser] %s lance %s%n", casterName, spellName);
            return;
        }

        // --- Dégâts directs ---
        Matcher mDirect = LogPatterns.DAMAGE_DIRECT.matcher(line);
        if (mDirect.find()) {
            handleDirectDamage(mDirect, onEvent, tsNow);
            return;
        }

        // --- Dégâts indirects ---
        Matcher mIndirect = LogPatterns.DAMAGE_INDIRECT.matcher(line);
        if (mIndirect.find()) {
            handleIndirectDamage(mIndirect, onEvent, tsNow);
            return;
        }
    }

    // === Gestion des événements ===
    private void handleDirectDamage(Matcher m, Consumer<LogEvent> onEvent, long tsNow) {
        String targetName = m.group(1).trim();
        int value = parseIntSafe(m.group(2));
        Element element = Element.fromString(m.group(3).trim());

        Fighter caster = findRecentCaster(tsNow);
        Fighter target = fighters.computeIfAbsent(targetName, n -> new Enemy(n, -1, n));

        Ability ability = lastAbilityByCaster.getOrDefault(
                caster.getName(),
                new Ability("Inconnu", "Sort direct", element, DamageSourceType.DIRECT)
        );
        ability.setElement(element);
        CombatEvent event = new CombatEvent(LocalDateTime.now(), caster, target, ability, EventType.DAMAGE, value, element);
        onEvent.accept(event);
        System.out.printf("[Parser] DIRECT: %s → %s %d (%s)%n", caster.getName(), target.getName(), value, element);
    }

    private void handleIndirectDamage(Matcher m, Consumer<LogEvent> onEvent, long tsNow) {
        String targetName = m.group(1).trim();
        int value = parseIntSafe(m.group(2));
        String tail = m.group(3).trim();

        Matcher tm = Pattern.compile("\\(([^)]*)\\)").matcher(tail);
        List<String> tokens = new ArrayList<>();
        while (tm.find()) tokens.add(tm.group(1).trim());

        Element element = Element.INCONNU;
        String effectName = null;


        // On extrait l'élément principal et le nom de l'effet (le sort)
        for (String token : tokens) {
            Element e = Element.fromString(token);
            if (e != Element.INCONNU) element = e;
            else effectName = token;
        }

        if (effectName == null) effectName = "Effet indirect";

        // ⚡ Vérification si ce sort est VRAIMENT indirect
        boolean isTrueIndirect = IndirectAbilities.isIndirect(effectName);

        Fighter caster;
        if (isTrueIndirect) {
            // Dégât vraiment indirect → joueur "Indirect"
            caster = fighters.computeIfAbsent("Indirect",
                    n -> new Player("Indirect", -999, Fighter.FighterType.PLAYER));
            System.out.printf("[Parser] INDIRECT: %s %d (%s)%n", effectName, value, element);
        } else {
            // Sinon, on le rattache au dernier lanceur (dégât direct normal)
            caster = findRecentCaster(tsNow);
            System.out.printf("[Parser] REASSIGNED AS DIRECT: %s %d (%s)%n", effectName, value, element);
        }

        Fighter target = fighters.computeIfAbsent(targetName, n -> new Enemy(n, -1, n));

        Ability ability = new Ability(effectName, "Effet indirect", element,
                isTrueIndirect ? DamageSourceType.INDIRECT : DamageSourceType.DIRECT);

        CombatEvent event = new CombatEvent(LocalDateTime.now(), caster, target, ability,
                EventType.DAMAGE, value, element);

        onEvent.accept(event);
    }





    // === Gestion du tour ===
    private void detectTurnStart(String playerName, Consumer<LogEvent> onEvent) {
        if (currentPlayerTurn == null || !currentPlayerTurn.equals(playerName)) {
            // --- Round Start ---
            if (playersThisRound.isEmpty()) {
                firstPlayerThisRound = playerName;
                onEvent.accept(new BattleEvent(LocalDateTime.now(),
                        BattleEvent.BattleState.ROUND_START, roundNumber));
                System.out.println("[Round] >>> ROUND " + roundNumber + " START <<<");
            }

            // --- Fin du tour précédent ---
            if (currentPlayerTurn != null && !currentPlayerTurn.equals(playerName)) {
                onEvent.accept(new BattleEvent(LocalDateTime.now(),
                        BattleEvent.BattleState.END_TURN, currentPlayerTurn));
                System.out.println("[Turn] <<< " + currentPlayerTurn + " END TURN <<<");
            }

            // --- Nouveau tour ---
            currentPlayerTurn = playerName;
            playersThisRound.add(playerName);
            onEvent.accept(new BattleEvent(LocalDateTime.now(),
                    BattleEvent.BattleState.START_TURN, playerName));
            System.out.println("[Turn] >>> " + playerName + " START TURN <<<");

            // --- Fin de round ---
            if (firstPlayerThisRound != null
                    && playerName.equals(firstPlayerThisRound)
                    && playersThisRound.size() > 1) {
                onEvent.accept(new BattleEvent(LocalDateTime.now(),
                        BattleEvent.BattleState.ROUND_END, roundNumber));
                System.out.println("[Round] <<< ROUND " + roundNumber + " END <<<");

                roundNumber++;
                playersThisRound.clear();
                firstPlayerThisRound = playerName;
            }
        }
    }

    // === Utilitaires ===
    private Fighter findRecentCaster(long tsNow) {
        Optional<String> caster = lastCastTime.entrySet().stream()
                .filter(e -> tsNow - e.getValue() <= RECENT_CAST_WINDOW_MS)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        if (caster.isPresent())
            return fighters.getOrDefault(caster.get(), new Player(caster.get(), -1, Fighter.FighterType.PLAYER));

        Optional<String> sticky = lastCastTime.entrySet().stream()
                .filter(e -> tsNow - e.getValue() <= STICKY_CAST_WINDOW_MS)
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);

        if (sticky.isPresent())
            return fighters.getOrDefault(sticky.get(), new Player(sticky.get(), -1, Fighter.FighterType.PLAYER));

        return fighters.computeIfAbsent("Indirect", n -> new Player("Indirect", -999, Fighter.FighterType.PLAYER));
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

    private void resetRoundTracking() {
        currentPlayerTurn = null;
        firstPlayerThisRound = null;
        playersThisRound.clear();
        roundNumber = 1;
    }
}
