package com.wakfu.parser;

import com.wakfu.domain.abilities.IndirectAbilities;
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
import com.wakfu.service.EventProcessor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogProcessor {
    private boolean inCombat = false;
    private final EventProcessor eventProcessor;
    private final Map<String, Fighter> fighters = new HashMap<>();
    private final Map<String, Ability> lastAbilityByCaster = new HashMap<>();
    private final Map<String, Long> lastCastTime = new HashMap<>();
    private String currentPlayerTurn = null;      // joueur actuellement actif
    private int roundNumber = 1;                  // numéro du round en cours
    private final Set<String> playersThisRound = new HashSet<>(); // joueurs ayant déjà joué ce round
    private final Set<String> playersKO = new HashSet<>(); // joueurs KO (ne jouent plus)
    private String firstPlayerThisRound = null;   // identifie le joueur qui ouvre le round

    // Tracking pour détecter un tour de table complet
    private final List<String> turnOrderThisRound = new ArrayList<>(); // ordre des joueurs dans le round
    private long lastTurnStartTime = 0;           // timestamp du dernier début de tour
    private long lastActivityTime = 0;            // timestamp de la dernière activité (cast/dégâts)

    private static final long RECENT_CAST_WINDOW_MS = 5_000;
    private static final long STICKY_CAST_WINDOW_MS = 10_000;
    private static final int MIN_PLAYERS_FOR_ROUND = 2;  // Minimum de joueurs différents pour valider un round

    private static final Pattern TIME_PREFIX =
            Pattern.compile("^[A-Z]+\\s(\\d{2}:\\d{2}:\\d{2}),(\\d{3})");

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public LogProcessor(EventProcessor eventProcessor) {
        this.eventProcessor = eventProcessor;
    }

    // Nouvelle méthode pour exposer le traitement d'un LogEvent
    public void process(LogEvent event) {
        if (event == null) return;
        eventProcessor.onEvent(event);
    }

    // === Analyse de chaque ligne ===
    public void processLine(String line) {
        if (line.isEmpty()) return;
        if (PatternExclusions.shouldIgnore(line)) return;
        line = PatternExclusions.clean(line);

        long tsNow = extractLogMillis(line);

        if (LogPatterns.START_COMBAT.matcher(line).find()) {
            inCombat = true;
            fighters.clear();
            lastAbilityByCaster.clear();
            resetRoundTracking();
            // utilise la chaîne: processLine -> process(LogEvent) -> EventProcessor
            process(new BattleEvent(LocalDateTime.now(), BattleEvent.BattleState.START));
            System.out.println("[Parser] >>> Combat started <<<");
            return;
        }

        if (LogPatterns.END_COMBAT.matcher(line).find()) {
            inCombat = false;
            process(new BattleEvent(LocalDateTime.now(), BattleEvent.BattleState.END));
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

        // --- Joueur KO ---
        Matcher mKO = LogPatterns.PLAYER_KO.matcher(line);
        if (mKO.find()) {
            String playerName = mKO.group(1).trim();
            playersKO.add(playerName);
            System.out.printf("[Parser] %s est KO (ne jouera plus ce combat)%n", playerName);
            // Si c'est le joueur actuel qui meurt, terminer son tour
            if (playerName.equals(currentPlayerTurn)) {
                System.out.printf("[Parser] Tour de %s terminé (KO)%n", currentPlayerTurn);
                currentPlayerTurn = null;
            }
            return;
        }

        // --- Joueur ressuscité ---
        Matcher mRevived = LogPatterns.PLAYER_REVIVED.matcher(line);
        if (mRevived.find()) {
            String playerName = mRevived.group(1).trim();
            if (playersKO.remove(playerName)) {
                System.out.printf("[Parser] %s est ressuscité (peut rejouer)%n", playerName);
            }
            return;
        }

        // --- Fin de tour explicite (secondes reportées) ---
        Matcher mTurnEnd = LogPatterns.TURN_END.matcher(line);
        if (mTurnEnd.find()) {
            String seconds = mTurnEnd.group(1);
            if (currentPlayerTurn != null) {
                System.out.printf("[Parser] Tour de %s terminé (%s secondes reportées)%n", currentPlayerTurn, seconds);
                currentPlayerTurn = null;
            }
            return;
        }

        // --- Sort lancé ---
        Matcher mCast = LogPatterns.CAST_SPELL.matcher(line);
        if (mCast.find()) {
            String casterName = mCast.group(1).trim();
            String spellName = mCast.group(2).trim();

            // Ignorer les sorts lancés par les ennemis
            Fighter caster = fighters.get(casterName);
            if (caster != null && caster.getType() != Fighter.FighterType.PLAYER) {
                //System.out.printf("[Parser] IGNORED (enemy cast): %s lance %s%n", casterName, spellName);
                return;
            }

            // check for special-case mappings
            String mapped = SpecialCase.specialSpell(spellName);
            if (mapped != null) {
                System.out.printf("[Parser] SpecialCase mapping: %s -> %s%n", spellName, mapped);
                spellName = mapped;
            }

            detectTurnStart(casterName);

            Ability ability = new Ability(spellName, "Sort", Element.INCONNU, DamageSourceType.DIRECT);
            lastAbilityByCaster.put(casterName, ability);
            lastCastTime.put(casterName, tsNow);
            System.out.printf("[Parser] %s lance %s%n", casterName, spellName);
            return;
        }

        // --- Dégâts directs ---
        Matcher mDirect = LogPatterns.DAMAGE_DIRECT.matcher(line);
        if (mDirect.find()) {
            handleDirectDamage(mDirect, tsNow);
            return;
        }

        // --- Dégâts indirects ---
        Matcher mIndirect = LogPatterns.DAMAGE_INDIRECT.matcher(line);
        if (mIndirect.find()) {
            handleIndirectDamage(mIndirect, tsNow);
            return;
        }
    }

    // === Gestion des événements ===
    private void handleDirectDamage(Matcher m, long tsNow) {
        String targetName = m.group(1).trim();
        int value = parseIntSafe(m.group(2));
        Element element = Element.fromString(m.group(3).trim());

        Fighter caster = findRecentCaster(tsNow);
        Fighter target = fighters.computeIfAbsent(targetName, n -> new Enemy(n, -1, n));

        // Ignorer les dégâts causés par les ennemis (seulement traiter les dégâts des joueurs)
        if (caster.getType() != Fighter.FighterType.PLAYER) {
            //System.out.printf("[Parser] IGNORED (enemy damage): %s → %s %d (%s)%n", caster.getName(), target.getName(), value, element);
            return;
        }

        // Ignorer les dégâts auto-infligés pour les joueurs
        if (caster instanceof Player && caster.getName().equals(target.getName())) {
            System.out.printf("[Parser] IGNORED (self-damage): %s → %s %d (%s)%n", caster.getName(), target.getName(), value, element);
            return;
        }

        // Ignorer le friendly fire (joueur attaque un autre joueur)
        if (caster instanceof Player && target instanceof Player && !caster.getName().equals(target.getName())) {
            System.out.printf("[Parser] IGNORED (friendly-fire): %s → %s %d (%s)%n", caster.getName(), target.getName(), value, element);
            return;
        }

        Ability ability = lastAbilityByCaster.getOrDefault(
                caster.getName(),
                new Ability("Inconnu", "Sort direct", element, DamageSourceType.DIRECT)
        );
        ability.setElement(element);

        // Récupérer le baseCost
        Integer baseCost = getBaseCostForCaster(caster);

        CombatEvent event = new CombatEvent(LocalDateTime.now(), caster, target, ability,
                EventType.DAMAGE, value, element, baseCost, 0);
        process(event);
        System.out.printf("[Parser] DIRECT: %s → %s %d (%s) [PA: %s]%n",
                caster.getName(), target.getName(), value, element, baseCost);
    }

    private void handleIndirectDamage(Matcher m, long tsNow) {
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

        // Apply special-case mapping for effectName
        String mapped = SpecialCase.specialSpell(effectName);
        if (mapped != null) {
            System.out.printf("[Parser] SpecialCase mapping (indirect): %s -> %s%n", effectName, mapped);
            effectName = mapped;
        }

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

        // Ignorer les dégâts causés par les ennemis (seulement traiter les dégâts des joueurs)
        if (caster.getType() != Fighter.FighterType.PLAYER) {
            //System.out.printf("[Parser] IGNORED (enemy indirect damage): %s → %s %d (%s)%n", caster.getName(), target.getName(), value, element);
            return;
        }

        // Ignorer les dégâts auto-infligés pour les joueurs
        if (caster instanceof Player && caster.getName().equals(target.getName())) {
            System.out.printf("[Parser] IGNORED (self-damage indirect): %s → %s %d (%s)%n", caster.getName(), target.getName(), value, element);
            return;
        }

        // Ignorer le friendly fire (joueur attaque un autre joueur)
        if (caster instanceof Player && target instanceof Player && !caster.getName().equals(target.getName())) {
            System.out.printf("[Parser] IGNORED (friendly-fire indirect): %s → %s %d (%s)%n", caster.getName(), target.getName(), value, element);
            return;
        }

        Ability ability = new Ability(effectName, "Effet indirect", element,
                isTrueIndirect ? DamageSourceType.INDIRECT : DamageSourceType.DIRECT);

        // Récupérer le baseCost
        Integer baseCost = getBaseCostForCaster(caster);

        CombatEvent event = new CombatEvent(LocalDateTime.now(), caster, target, ability,
                EventType.DAMAGE, value, element, baseCost, 0);

        process(event);
    }




    // === Gestion du tour ===
    private void detectTurnStart(String playerName) {
        long currentTime = System.currentTimeMillis();
        lastActivityTime = currentTime; // Mettre à jour l'activité (cast ou dégâts)

        // Ignorer les joueurs KO
        if (playersKO.contains(playerName)) {
            System.out.printf("[Parser] IGNORED cast from KO player: %s%n", playerName);
            return;
        }

        if (currentPlayerTurn == null || !currentPlayerTurn.equals(playerName)) {
            // --- Round Start ---
            if (playersThisRound.isEmpty()) {
                firstPlayerThisRound = playerName;
                turnOrderThisRound.clear();
                process(new BattleEvent(LocalDateTime.now(),
                        BattleEvent.BattleState.ROUND_START, roundNumber));
                System.out.println("[Round] >>> ROUND " + roundNumber + " START <<<");
            }

            // --- Fin du tour précédent ---
            if (currentPlayerTurn != null && !currentPlayerTurn.equals(playerName)) {
                process(new BattleEvent(LocalDateTime.now(),
                        BattleEvent.BattleState.END_TURN, currentPlayerTurn));
                System.out.println("[Turn] <<< " + currentPlayerTurn + " END TURN <<<");
            }

            // --- Nouveau tour ---
            currentPlayerTurn = playerName;
            playersThisRound.add(playerName);
            turnOrderThisRound.add(playerName);
            lastTurnStartTime = currentTime;

            process(new BattleEvent(LocalDateTime.now(),
                    BattleEvent.BattleState.START_TURN, playerName));
            System.out.println("[Turn] >>> " + playerName + " START TURN <<<");

            // --- Détection de fin de round basée sur les patterns ---
            boolean shouldEndRound = false;
            String endReason = "";

            // Cas 1: Le premier joueur rejoue ET au moins 2 joueurs ont joué
            // → Tour de table classique complet
            if (firstPlayerThisRound != null
                    && playerName.equals(firstPlayerThisRound)
                    && playersThisRound.size() >= MIN_PLAYERS_FOR_ROUND) {
                shouldEndRound = true;
                endReason = "first player returned";
            }

            // Cas 2: Le même joueur joue 2 fois de suite après qu'au moins 2 joueurs aient joué
            // (indique que les autres joueurs ont probablement passé/sont morts)
            else if (turnOrderThisRound.size() >= 2) {
                int lastIdx = turnOrderThisRound.size() - 1;
                String lastPlayer = turnOrderThisRound.get(lastIdx);
                String secondLastPlayer = turnOrderThisRound.get(lastIdx - 1);

                if (lastPlayer.equals(secondLastPlayer) && playersThisRound.size() >= MIN_PLAYERS_FOR_ROUND) {
                    shouldEndRound = true;
                    endReason = "player played twice consecutively";
                }
            }

            // Cas 3: Tous les joueurs actifs ont joué (basé sur le nombre de PLAYER dans fighters)
            else if (playersThisRound.size() >= countActivePlayers() && countActivePlayers() >= MIN_PLAYERS_FOR_ROUND) {
                shouldEndRound = true;
                endReason = "all active players have played";
            }

            if (shouldEndRound) {
                process(new BattleEvent(LocalDateTime.now(),
                        BattleEvent.BattleState.ROUND_END, roundNumber));
                System.out.println("[Round] <<< ROUND " + roundNumber + " END (" + endReason + ") <<<");
                System.out.println("[Round] Players who played: " + playersThisRound);
                System.out.println("[Round] Turn order: " + turnOrderThisRound);

                roundNumber++;
                playersThisRound.clear();
                turnOrderThisRound.clear();
                firstPlayerThisRound = playerName;
            }
        } else {
            // Même joueur joue plusieurs actions consécutives
        }
    }

    /**
     * Compte le nombre de joueurs actifs (PLAYER non-KO) dans le combat
     */
    private int countActivePlayers() {
        return (int) fighters.values().stream()
                .filter(f -> f.getType() == Fighter.FighterType.PLAYER)
                .filter(f -> !playersKO.contains(f.getName()))
                .count();
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


    /**
     * Récupère le baseCost du sort actuellement lancé par le caster
     */
    private Integer getBaseCostForCaster(Fighter caster) {
        if (caster == null) return null;

        Ability ability = lastAbilityByCaster.get(caster.getName());
        if (ability == null) return null;

        // Pour l'instant, on ne peut pas déterminer la classe du joueur
        // donc on passe null et SpellCostProvider cherchera dans toutes les classes
        String className = null;

        // Utiliser SpellCostProvider pour obtenir le coût
        return com.wakfu.data.SpellCostProvider.getCostFor(className, ability.getName());
    }

    private void resetRoundTracking() {
        currentPlayerTurn = null;
        firstPlayerThisRound = null;
        playersThisRound.clear();
        playersKO.clear();
        turnOrderThisRound.clear();
        roundNumber = 1;
        lastTurnStartTime = 0;
        lastActivityTime = 0;
    }
}
