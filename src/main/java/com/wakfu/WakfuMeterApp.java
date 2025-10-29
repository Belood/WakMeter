package com.wakfu;



//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.


import com.wakfu.domain.event.CombatEvent;
import com.wakfu.parser.LogParser;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Point d'entrée principal de l'application Damage Meter.
 * Gère la séquence complète : parsing -> calcul -> affichage (JavaFX UI).
 */
public class WakfuMeterApp extends Application {

    private LogParser parser;
    private DamageCalculator calculator;
    private UIManager uiManager;

    /**
     * Méthode principale standard pour lancer l'application JavaFX.
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialisation des composants
        parser = new LogParser();
        calculator = new DamageCalculator();
        uiManager = new UIManager(primaryStage);

        // Exemple de fichier log (tu pourras le passer en argument plus tard)
        String logFilePath = "combat.log";

        try {
            run(logFilePath);
        } catch (Exception e) {
            e.printStackTrace();
            uiManager.showError("Erreur", "Impossible de charger le fichier de log : " + e.getMessage());
        }
    }

    /**
     * Exécute le flux principal de traitement :
     * - Lecture du fichier de log
     * - Calcul des statistiques
     * - Affichage du résultat dans l'interface
     *
     * @param logFilePath chemin du fichier de log
     */
    public void run(String logFilePath) {
        // 1. Parse du log
        List<CombatEvent> events = parser.parseLog(logFilePath);

        // 2. Calcul des dégâts par joueur
        List<Player> players = calculator.calculatePlayerStats(events);
        int totalDamage = calculator.calculateTotalDamage(players);

        // 3. Affichage dans l'UI principale
        uiManager.displayPlayerStats(players, totalDamage);
    }
}