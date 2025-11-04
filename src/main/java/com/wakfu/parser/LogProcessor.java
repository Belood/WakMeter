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
    // PA regagnés par joueur pour le cast en cours - CUMULE toutes les lignes de PA
    // (ex: "Drazuria: 1PA (Profit)" + "Drazuria: 1PA" = 2 PA total)
    private final Map<String, Integer> paRegainedByCaster = new HashMap<>();
    private String currentPlayerTurn = null;      // joueur actuellement actif
    private int roundNumber = 1;                  // numéro du round en cours
    private final Set<String> playersThisRound = new HashSet<>(); // joueurs ayant déjà joué ce round
    private String firstPlayerThisRound = null;   // identifie le joueur qui ouvre le round

    // Tracking pour détecter un tour de table complet
    private final List<String> turnOrderThisRound = new ArrayList<>(); // ordre des joueurs dans le round
    private long lastTurnStartTime = 0;           // timestamp du dernier début de tour
    private long lastActivityTime = 0;            // timestamp de la dernière activité (cast/dégâts)

    private static final long RECENT_CAST_WINDOW_MS = 10_000;
    private static final long STICKY_CAST_WINDOW_MS = 20_000;
    private static final long ACTIVITY_TIMEOUT_MS = 120_000; // 120s sans activité = timeout
    private static final long MIN_ACTIVITY_WINDOW_MS = 5_000; // Au minimum 5s entre deux rounds
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

        // --- Sort lancé ---
        Matcher mCast = LogPatterns.CAST_SPELL.matcher(line);
        if (mCast.find()) {
            String casterName = mCast.group(1).trim();
            String spellName = mCast.group(2).trim();

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
            paRegainedByCaster.put(casterName, 0); // Réinitialiser les PA regagnés pour ce nouveau cast
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

        // --- PA regagnés ---
        // Ignorer les pertes de PA (avec signe moins)
        if (line.contains("PA") && !line.matches(".*[−–-]\\s*\\d+\\s*PA.*")) {
            Matcher mPA = LogPatterns.PA_REGAIN.matcher(line);
            if (mPA.find()) {
                handlePARegain(mPA, tsNow);
                return;
            }
        }
    }

    // === Gestion des événements ===
    private void handleDirectDamage(Matcher m, long tsNow) {
        String targetName = m.group(1).trim();
        int value = parseIntSafe(m.group(2));
        Element element = Element.fromString(m.group(3).trim());

        Fighter caster = findRecentCaster(tsNow);
        Fighter target = fighters.computeIfAbsent(targetName, n -> new Enemy(n, -1, n));

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

        // Récupérer le baseCost et les PA regagnés
        Integer baseCost = getBaseCostForCaster(caster);
        int paRegained = paRegainedByCaster.getOrDefault(caster.getName(), 0);

        CombatEvent event = new CombatEvent(LocalDateTime.now(), caster, target, ability,
                EventType.DAMAGE, value, element, baseCost, paRegained);
        process(event);
        System.out.printf("[Parser] DIRECT: %s → %s %d (%s) [PA: %s - %d = %s]%n",
                caster.getName(), target.getName(), value, element,
                baseCost, paRegained, baseCost != null ? (baseCost - paRegained) : "?");
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

        // Récupérer le baseCost et les PA regagnés
        Integer baseCost = getBaseCostForCaster(caster);
        int paRegained = paRegainedByCaster.getOrDefault(caster.getName(), 0);

        CombatEvent event = new CombatEvent(LocalDateTime.now(), caster, target, ability,
                EventType.DAMAGE, value, element, baseCost, paRegained);

        process(event);
    }




    // === Gestion du tour ===
    private void detectTurnStart(String playerName) {
        long currentTime = System.currentTimeMillis();
        lastActivityTime = currentTime; // Mettre à jour l'activité (cast ou dégâts)

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
     * Détecte si un timeout d'activité s'est produit (pas de cast ou dégâts depuis longtemps).
     * Utile pour détecter un problème de parsing ou un combat figé.
     * Appelé périodiquement depuis le processeur de logs pour nettoyer les états bloqués.
     */
    public void checkActivityTimeout() {
        long currentTime = System.currentTimeMillis();

        // Si aucune activité depuis ACTIVITY_TIMEOUT_MS et nous sommes en combat
        if (inCombat && lastActivityTime > 0 && currentTime - lastActivityTime > ACTIVITY_TIMEOUT_MS) {
            System.out.println("[Round] ⚠ ACTIVITY TIMEOUT: No activity for " +
                    (ACTIVITY_TIMEOUT_MS / 1000) + " seconds");

            // Si nous avons un round en cours avec au moins 2 joueurs, le clore
            if (playersThisRound.size() >= MIN_PLAYERS_FOR_ROUND && firstPlayerThisRound != null) {
                process(new BattleEvent(LocalDateTime.now(),
                        BattleEvent.BattleState.ROUND_END, roundNumber));
                System.out.println("[Round] <<< ROUND " + roundNumber + " END (activity timeout) <<<");
                System.out.println("[Round] Players who played: " + playersThisRound);

                roundNumber++;
                playersThisRound.clear();
                turnOrderThisRound.clear();
                firstPlayerThisRound = null;
            }

            // Réinitialiser le timestamp pour éviter les timeouts répétitifs
            lastActivityTime = currentTime;
        }
    }

    /**
     * Compte le nombre de joueurs actifs (PLAYER) dans le combat
     */
    private int countActivePlayers() {
        return (int) fighters.values().stream()
                .filter(f -> f.getType() == Fighter.FighterType.PLAYER)
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
     * Gère les PA regagnés détectés dans les logs.
     * Accumule tous les regains de PA pour un même cast (ex: Profit + regain normal).
     */
    private void handlePARegain(Matcher m, long tsNow) {
        String playerName = m.group(1).trim();
        // Le pattern a 3 groupes: (nom):(signe optionnel)(nombre) PA
        // Groupe 1 = nom, Groupe 2 = signe [+]?, Groupe 3 = nombre
        int paAmount = Integer.parseInt(m.group(3).trim());

        // Vérifier si un sort a été récemment lancé par ce joueur
        Long lastCast = lastCastTime.get(playerName);

        // Ignorer les regains de PA qui ne sont pas liés à un sort récent
        if (lastCast == null || (tsNow - lastCast) > RECENT_CAST_WINDOW_MS) {
            System.out.printf("[Parser] PA REGAIN IGNORED (not spell-related): %s +%d PA (lastCast=%s, tsNow=%d, diff=%d)%n",
                    playerName, paAmount, lastCast, tsNow, lastCast != null ? (tsNow - lastCast) : -1);
            return;
        }

        // Ajouter les PA regagnés au compteur du joueur (cumul pour le cast en cours)
        paRegainedByCaster.merge(playerName, paAmount, Integer::sum);

        System.out.printf("[Parser] PA REGAIN: %s +%d PA (cumulative total: %d)%n",
                playerName, paAmount, paRegainedByCaster.get(playerName));
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
        turnOrderThisRound.clear();
        roundNumber = 1;
        lastTurnStartTime = 0;
        lastActivityTime = 0;
    }
}
