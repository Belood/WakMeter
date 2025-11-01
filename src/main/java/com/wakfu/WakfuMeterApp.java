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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public class WakfuMeterApp extends Application {

    private LogParser logParser;

    @Override
    public void start(Stage primaryStage) {
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

        // Callback pour démarrer le parser quand un dossier est choisi
        uiManager.setOnLogFolderSelected(path -> {
            try {
                Path logFile = Paths.get(path).resolve("wakfu.log");
                if (logParser != null) logParser.stop();
                logParser = new LogParser(logProcessor);
                logParser.startRealtimeParsing(logFile, eventProcessor::onEvent);
                uiManager.showMessage("Wakfu Meter", "Lecture des logs: " + logFile);
                uiManager.setAppStatus(MessageProvider.logsDetected());
                uiManager.setAppStatus(MessageProvider.waitingCombat());
            } catch (Exception e) {
                uiManager.showError("Erreur", "Impossible de démarrer le parser: " + e.getMessage());
                System.out.printf(e.getMessage());
                uiManager.setAppStatus("Inactif");
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
                logParser = new LogParser(logProcessor);
                logParser.startRealtimeParsing(logFile, eventProcessor::onEvent);
                uiManager.showMessage("Wakfu Meter", "Lecture des logs: " + logFile);
                uiManager.setAppStatus(MessageProvider.logsDetected());
                uiManager.setAppStatus(MessageProvider.waitingCombat());
            } catch (Exception e) {
                uiManager.showError("Erreur", "Impossible de démarrer le parser: " + e.getMessage());
                System.out.printf(e.getMessage());
                uiManager.setAppStatus("Inactif");
            }
            uiManager.setOnLogFolderSelected(path -> {
                try {
                    Path logFile = Paths.get(path).resolve("wakfu.log");
                    if (logParser != null) logParser.stop();
                    logParser = new LogParser(logProcessor);
                    logParser.startRealtimeParsing(logFile, eventProcessor::onEvent);
                    uiManager.showMessage("Wakfu Meter", "Lecture des logs: " + logFile);
                    uiManager.setAppStatus(MessageProvider.logsDetected());
                    uiManager.setAppStatus(MessageProvider.waitingCombat());
                } catch (Exception e) {
                    uiManager.showError("Erreur", "Impossible de démarrer le parser: " + e.getMessage());
                    System.out.printf(e.getMessage());
                    uiManager.setAppStatus("Inactif");
                }
            });
        }

        uiManager.showMessage("Wakfu Meter", "Prêt");

        uiManager.setOnAutoResetChanged(enabled -> {
            if (enabled) {
                // When enabled, only register the hook so that at the START of each battle
                // the damageCalculator and UI will be reset. Do NOT reset immediately when
                // the checkbox is toggled.
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
    }

    @Override
    public void stop() {
        if (logParser != null) logParser.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
