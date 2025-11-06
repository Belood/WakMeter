package com.wakfu;

import com.wakfu.data.UserSettings;
import com.wakfu.data.MessageProvider;
import com.wakfu.parser.LogParser;
import com.wakfu.service.EventProcessor;
import com.wakfu.parser.LogProcessor;
import com.wakfu.service.DamageCalculator;
import com.wakfu.ui.UIManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class WakfuMeterApp extends Application {

    private LogParser logParser;

    @Override
    public void start(Stage primaryStage) {
        // === Fix encodage UTF-8 pour la console ===
        try {
            System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8.name()));
            System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            System.err.println("Warning: Could not set UTF-8 encoding for console");
        }

        // === Initialisation des modules ===
        DamageCalculator damageCalculator = new DamageCalculator();
        EventProcessor eventProcessor = new EventProcessor();
        LogProcessor logProcessor = new LogProcessor(eventProcessor);
        UIManager uiManager = new UIManager(primaryStage, damageCalculator);

        // Status listener -> UI
        eventProcessor.addStatusListener(uiManager::setAppStatus);

        // History toggle listener -> EventProcessor + persistance
        uiManager.setOnHistoryChanged(enabled -> {
            eventProcessor.setHistoryEnabled(enabled);
            UserSettings.saveHistoryEnabled(enabled);
        });

        // Restaurer le parametre historyEnabled
        boolean historyEnabled = UserSettings.loadHistoryEnabled().orElse(false);
        uiManager.setHistoryChecked(historyEnabled);
        eventProcessor.setHistoryEnabled(historyEnabled);

        // Enregistrer le callback auto-reset AVANT de restaurer la valeur
        uiManager.setOnAutoResetChanged(enabled -> {
            if (enabled) {
                // When enabled, register the hook so that at the START of each battle
                // the damageCalculator and UI will be reset.
                eventProcessor.setOnBattleStart(() -> {
                    try {
                        damageCalculator.reset();
                        uiManager.resetUI();
                    } catch (Exception ignored) {}
                });
            } else {
                eventProcessor.setOnBattleStart(null);
            }
        });

        // Restaurer le parametre autoReset et déclencher le callback
        boolean autoResetEnabled = UserSettings.loadAutoReset().orElse(true);
        uiManager.setAutoResetChecked(autoResetEnabled);
        // Déclencher manuellement le callback pour enregistrer le hook
        if (autoResetEnabled) {
            eventProcessor.setOnBattleStart(() -> {
                try {
                    damageCalculator.reset();
                    uiManager.resetUI();
                } catch (Exception ignored) {}
            });
        }

        // Callback pour démarrer le parser quand un dossier est choisi
        uiManager.setOnLogFolderSelected(path -> {
            try {
                Path logFile = Paths.get(path).resolve("wakfu.log");
                if (!logFile.toFile().exists()) {
                    uiManager.setAppStatus(MessageProvider.noLogFile());
                    return;
                }
                if (logParser != null) logParser.stop();
                logParser = new LogParser(logProcessor);
                logParser.startRealtimeParsing(logFile, eventProcessor::onEvent);
                uiManager.setAppStatus(MessageProvider.logsDetected());
                uiManager.setAppStatus(MessageProvider.waitingCombat());
            } catch (Exception e) {
                uiManager.showError("Erreur", "Impossible de démarrer le parser: " + e.getMessage());
                System.out.printf(e.getMessage());
            }
        });

        // === Inscription aux notifications du modèle ===
        eventProcessor.addModelListener(model -> {
            // Refresh calculator state then UI
            damageCalculator.refreshFromModel(model);
            uiManager.refresh(model);
        });

        // L'auto-reset est géré via le hook onBattleStart ; ne pas réinitialiser à chaque update du modèle.

        // Si un dossier de logs était sauvegardé, démarrer automatiquement
        Optional<String> saved = UserSettings.loadLogFolder();
        if (saved.isPresent()) {
            uiManager.setOnLogFolderSelected(null); // éviter double-callback pendant démarrage
            // démarrage direct
            try {
                Path logFile = Paths.get(saved.get()).resolve("wakfu.log");
                if (!logFile.toFile().exists()) {
                    uiManager.setAppStatus(MessageProvider.noLogFile());
                } else {
                    logParser = new LogParser(logProcessor);
                    logParser.startRealtimeParsing(logFile, eventProcessor::onEvent);
                    uiManager.setAppStatus(MessageProvider.logsDetected());
                    uiManager.setAppStatus(MessageProvider.waitingCombat());
                }
            } catch (Exception e) {
                uiManager.showError("Erreur", "Impossible de démarrer le parser: " + e.getMessage());
                System.out.printf(e.getMessage());
            }
            uiManager.setOnLogFolderSelected(path -> {
                try {
                    Path logFile = Paths.get(path).resolve("wakfu.log");
                    if (!logFile.toFile().exists()) {
                        uiManager.setAppStatus(MessageProvider.noLogFile());
                        return;
                    }
                    if (logParser != null) logParser.stop();
                    logParser = new LogParser(logProcessor);
                    logParser.startRealtimeParsing(logFile, eventProcessor::onEvent);
                    uiManager.setAppStatus(MessageProvider.logsDetected());
                    uiManager.setAppStatus(MessageProvider.waitingCombat());
                } catch (Exception e) {
                    uiManager.showError("Erreur", "Impossible de démarrer le parser: " + e.getMessage());
                    System.out.printf(e.getMessage());
                }
            });
        } else {
            uiManager.setAppStatus(MessageProvider.addLogPath());
        }
    }

    @Override
    public void stop() {
        if (logParser != null) logParser.stop();
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        // Force System.out en UTF-8 (utiliser FileDescriptor.out pour conserver la sortie console)
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));

        launch(args);
    }
}
