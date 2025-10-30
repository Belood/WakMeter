package com.wakfu;

import com.wakfu.domain.actors.Player;
import com.wakfu.domain.event.*;
import com.wakfu.parser.LogParser;
import com.wakfu.service.DamageCalculator;
import com.wakfu.ui.UIManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Application principale WakfuMeter :
 * - Détecte automatiquement le dernier fichier de log Wakfu
 * - Parse les événements en temps réel
 * - Met à jour l'UI avec les dégâts des joueurs
 */
public class WakfuMeterApp extends Application {

    private LogParser parser;
    private DamageCalculator calculator;
    private UIManager uiManager;

    // Répertoire des logs Wakfu
    private static final Path LOG_DIR = Paths.get("C:\\Users\\alex_\\AppData\\Roaming\\zaap\\gamesLogs\\wakfu\\logs");

    @Override
    public void start(Stage primaryStage) {
        calculator = new DamageCalculator();
        uiManager = new UIManager(primaryStage, calculator);
        parser = new LogParser();

        try {
            Path logFile = findLatestLogFile(LOG_DIR);
            if (logFile == null) {
                uiManager.showError("Erreur", "Aucun fichier de log trouvé dans : " + LOG_DIR);
                return;
            }

            uiManager.showMessage("Wakfu Meter", "Lecture en temps réel : " + logFile.getFileName());
            parser.startRealtimeParsing(logFile, this::handleLogEvent);

        } catch (IOException e) {
            uiManager.showError("Erreur", "Impossible d'accéder au dossier de logs : " + e.getMessage());
        }
    }

    /**
     * Trouve le fichier de log le plus récemment modifié dans le dossier donné.
     */
    private Path findLatestLogFile(Path directory) throws IOException {
        if (!Files.exists(directory)) return null;

        try (var files = Files.list(directory)) {
            Optional<Path> latest = files
                    .filter(f -> f.getFileName().toString().toLowerCase().contains("log"))
                    .filter(Files::isRegularFile)
                    .max(Comparator.comparingLong(this::getLastModifiedTimeSafe));

            return latest.orElse(null);
        }
    }

    private long getLastModifiedTimeSafe(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private void handleLogEvent(LogEvent event) {
        if (event instanceof BattleEvent battle) {
            handleBattleEvent(battle);
        } else if (event instanceof CombatEvent combat) {
            handleCombatEvent(combat);
        }
    }

    private void handleBattleEvent(BattleEvent battleEvent) {
        switch (battleEvent.getState()) {
            case START -> {
                // Début du combat
                uiManager.showMessage("Combat", "Début du combat détecté !");
                uiManager.handleCombatStart(); // ✅ Auto-reset si activé
            }
            case END -> {
                uiManager.showMessage("Combat", "Combat terminé !");
            }
        }
    }

    private void handleCombatEvent(CombatEvent event) {
        calculator.updateWithEvent(event);

        List<Player> players = calculator.getPlayers();
        int totalDamage = calculator.calculateTotalDamage(players);

        uiManager.displayPlayerStats(players, totalDamage);
//        uiManager.displayElementBreakdown(calculator.getElementalBreakdown());
    }

    @Override
    public void stop() throws Exception {
        if (parser != null) parser.stop();
        super.stop();
    }

    public static void main(String[] args) {
        try {
            System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
        launch(args);
    }
}