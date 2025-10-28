package com.wakfu.parser;

import com.wakfu.model.Ability;
import com.wakfu.model.CombatEvent;
import com.wakfu.model.EventType;
import com.wakfu.model.Fighter;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;

public class LogParser {


    private volatile boolean running = false;
    private Thread watchThread;

    // --- États ---
    private boolean inCombat = false;
    private final Map<String, Fighter> fighters = new HashMap<>();
    private final Map<String, Ability> lastAbilityByCaster = new HashMap<>();
    private final Map<String, Long> lastCastTime = new HashMap<>();

    public void startRealtimeParsing(Path logFilePath, Consumer<CombatEvent> onEvent) {
        if (running) return;
        running = true;
        watchThread = new Thread(() -> watchFile(logFilePath, onEvent), "WakfuLogParserV3");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    public void stop() {
        running = false;
        if (watchThread != null && watchThread.isAlive()) watchThread.interrupt();
    }

    private void watchFile(Path logFilePath, Consumer<CombatEvent> onEvent) {
        try (RandomAccessFile file = new RandomAccessFile(logFilePath.toFile(), "r")) {
            long pointer = file.length();
            while (running) {
                long len = file.length();
                if (len < pointer) pointer = len;
                else if (len > pointer) {
                    file.seek(pointer);
                    String line;
                    while ((line = file.readLine()) != null) {
                        processLine(line.trim(), onEvent);
                    }
                    pointer = file.getFilePointer();
                }
                Thread.sleep(300);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[Parser] Error: " + e.getMessage());
        }
    }

    private void processLine(String line, Consumer<CombatEvent> onEvent) {
        if (line.isEmpty()) return;

        // --- Début combat ---
        if (LogPatterns.START_COMBAT.matcher(line).find()) {
            inCombat = true;
            fighters.clear();
            lastAbilityByCaster.clear();
            System.out.println("[Parser] >>> Combat started <<<");
            return;
        }

        // --- Fin combat ---
        if (LogPatterns.END_COMBAT.matcher(line).find()) {
            inCombat = false;
            System.out.println("[Parser] <<< Combat ended >>>");
            return;
        }

        if (!inCombat) return;

        // --- Détection joueur entrant ---
        Matcher mJoin = LogPatterns.PLAYER_JOIN.matcher(line);
        if (mJoin.find()) {
            String name = mJoin.group(1).trim();
            long id = Long.parseLong(mJoin.group(2));
            boolean ai = Boolean.parseBoolean(mJoin.group(3));
            fighters.put(name, new Fighter(name, id, ai));
            System.out.printf("[Parser] Fighter detected: %s%n", name);
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

        // --- Ligne de dégâts ---
        Matcher mDamage = LogPatterns.DAMAGE.matcher(line);
        if (mDamage.find()) {
            String targetName = mDamage.group(1).trim();
            int dmg = parseIntSafe(mDamage.group(2));
            String element = mDamage.group(3) != null ? mDamage.group(3) : "Inconnu";

            Fighter caster = findRecentCaster();
            Fighter target = fighters.computeIfAbsent(targetName, n -> new Fighter(n, -1, true));
            Ability ability = lastAbilityByCaster.getOrDefault(caster.getName(), new Ability("Inconnu", element));

            // Mets à jour l’élément du sort si détecté
            if ("Inconnu".equals(ability.getElement()) && !element.isBlank()) {
                ability = new Ability(ability.getName(), element);
                lastAbilityByCaster.put(caster.getName(), ability);
            }

            CombatEvent event = new CombatEvent(
                    LocalDateTime.now(),
                    caster,
                    target,
                    ability,
                    EventType.DAMAGE,
                    dmg
            );

            onEvent.accept(event);
            return;
        }
    }

    private Fighter findRecentCaster() {
        long now = System.currentTimeMillis();
        return lastCastTime.entrySet().stream()
                .filter(e -> now - e.getValue() < 1500)
                .map(Map.Entry::getKey)
                .findFirst()
                .map(name -> fighters.getOrDefault(name, new Fighter(name, -1, false)))
                .orElse(new Fighter("Inconnu", -1, true));
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); }
        catch (Exception e) { return 0; }
    }
}
