package com.wakfu.parser;

import com.wakfu.domain.abilities.Ability;
import com.wakfu.domain.actors.Enemy;
import com.wakfu.domain.actors.Fighter;
import com.wakfu.domain.actors.Player;
import com.wakfu.domain.event.BattleEvent;
import com.wakfu.domain.event.CombatEvent;
import com.wakfu.domain.event.EventType;
import com.wakfu.domain.event.LogEvent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;

public class LogParser {

    private volatile boolean running = false;
    private Thread watchThread;

    private boolean inCombat = false;
    private final Map<String, Fighter> fighters = new HashMap<>();
    private final Map<String, Ability> lastAbilityByCaster = new HashMap<>();
    private final Map<String, Long> lastCastTime = new HashMap<>();

    public void startRealtimeParsing(Path logFilePath, Consumer<LogEvent> onEvent) {
        if (running) return;
        running = true;
        watchThread = new Thread(() -> watchFile(logFilePath, onEvent), "WakfuLogParser");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    public void stop() {
        running = false;
        if (watchThread != null && watchThread.isAlive()) watchThread.interrupt();
    }

    /**
     * Lecture du fichier en UTF-8, en temps réel (comme tail -f)
     */
    private void watchFile(Path logFilePath, Consumer<LogEvent> onEvent) {
        try (FileInputStream fis = new FileInputStream(logFilePath.toFile());
             InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(isr)) {

            System.out.println("[Parser] Watching log in UTF-8: " + logFilePath);

            // ⚡ Aller directement à la fin du fichier (ignorer tout l’historique)
            long skipped = fis.skip(fis.available());
            System.out.println("[Parser] Ignored existing content (" + skipped + " bytes)");

            String line;
            while (running) {
                while ((line = br.readLine()) != null) {
                    processLine(line.trim(), onEvent);
                }
                Thread.sleep(300);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("[Parser] Error: " + e.getMessage());
        }
    }

    private void processLine(String line, Consumer<LogEvent> onEvent) {
        if (line.isEmpty()) return;
        // --- Début combat ---
        if (LogPatterns.START_COMBAT.matcher(line).find()) {
            inCombat = true;
            fighters.clear();
            lastAbilityByCaster.clear();
            BattleEvent startEvent = new BattleEvent(LocalDateTime.now(), BattleEvent.BattleState.START);
            onEvent.accept(startEvent);
            System.out.println("[Parser] >>> Combat started <<<");
            return;
        }

        // --- Fin combat ---
        if (LogPatterns.END_COMBAT.matcher(line).find()) {
            inCombat = false;
            BattleEvent endEvent = new BattleEvent(LocalDateTime.now(), BattleEvent.BattleState.END);
            onEvent.accept(endEvent);
            System.out.println("[Parser] <<< Combat ended >>>");
            return;
        }

        if (!inCombat) return;

        // --- Détection des entités ---
        Matcher mJoin = LogPatterns.PLAYER_JOIN.matcher(line);
        if (mJoin.find()) {
            String name = mJoin.group(1).trim();
            long id = Long.parseLong(mJoin.group(2));
            boolean isAI = Boolean.parseBoolean(mJoin.group(3));

            Fighter fighter = isAI
                    ? new Enemy(name, id, name)
                    : new Player(name, id, null);

            fighters.put(name, fighter);
            System.out.printf("[Parser] Fighter detected: %s [%s, id=%d]%n", name, fighter.getType(), id);
            return;
        }

        // --- Lancement de sort ---
        Matcher mCast = LogPatterns.CAST_SPELL.matcher(line);
        if (mCast.find()) {
            String casterName = mCast.group(1).trim();
            String spellName = mCast.group(2).trim();
            Ability ability = new Ability(spellName, "Inconnu");
            lastAbilityByCaster.put(casterName, ability);
            lastCastTime.put(casterName, System.currentTimeMillis());
            System.out.printf("[Parser] %s lance %s%n", casterName, spellName);
            return;
        }

        // --- DÉGÂTS ---
        Matcher mDamage = LogPatterns.DAMAGE.matcher(line);
        if (mDamage.find()) {
            System.out.println("[Parser] DAMAGE matched line: " + line);
            handleCombatAction(mDamage, EventType.DAMAGE, onEvent);
            return;
        }

        // --- SOINS ---
        Matcher mHeal = LogPatterns.HEAL.matcher(line);
        if (mHeal.find()) {
            System.out.println("[Parser] HEAL matched line: " + line);
            handleCombatAction(mHeal, EventType.HEAL, onEvent);
            return;
        }

        // --- BOUCLIER ---
        Matcher mShield = LogPatterns.SHIELD.matcher(line);
        if (mShield.find()) {
            System.out.println("[Parser] SHIELD matched line: " + line);
            handleCombatAction(mShield, EventType.SHIELD, onEvent);
        }
    }

    private void handleCombatAction(Matcher matcher, EventType type, Consumer<LogEvent> onEvent) {
        String targetName = matcher.group(1).trim();
        int value = parseIntSafe(matcher.group(2));
        String element = matcher.groupCount() >= 3 ? matcher.group(3) : "Inconnu";

        Fighter caster = findRecentCaster();
        Fighter target = fighters.computeIfAbsent(targetName, n -> new Enemy(n, -1, n));
        Ability ability = lastAbilityByCaster.getOrDefault(
                caster.getName(),
                new Ability("Inconnu", element)
        );

        if ("Inconnu".equals(ability.getElement()) && !element.isBlank()) {
            ability = new Ability(ability.getName(), element);
            lastAbilityByCaster.put(caster.getName(), ability);
        }

        CombatEvent event = new CombatEvent(
                LocalDateTime.now(),
                caster,
                target,
                ability,
                type,
                value
        );

        System.out.printf("[Parser] MATCH %s → %s: %d (%s)%n",
                type, targetName, value, element);

        onEvent.accept(event);
    }

    private Fighter findRecentCaster() {
        long now = System.currentTimeMillis();
        return lastCastTime.entrySet().stream()
                .filter(e -> now - e.getValue() < 3000)
                .map(Map.Entry::getKey)
                .findFirst()
                .map(name -> fighters.getOrDefault(name, new Player(name, -1, null)))
                .orElse(new Enemy("Inconnu", -1, "Inconnu"));
    }

    private int parseIntSafe(String s) {
        try {
            if (s == null) return 0;
            String cleaned = s.replaceAll("[^0-9]", "");
            return cleaned.isEmpty() ? 0 : Integer.parseInt(cleaned);
        } catch (Exception e) {
            System.err.println("[Parser] Invalid number: " + s);
            return 0;
        }
    }
}
