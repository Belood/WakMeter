package com.wakfu.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Utilitaire minimal pour sauvegarder/charger des paramètres utilisateur
 * dans un fichier JSON `user_settings.json` dans le répertoire courant.
 */
public class UserSettings {
    private static final String FILE_NAME = "user_settings.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Optional<Settings> loadSettings() {
        Path p = Paths.get(FILE_NAME);
        if (!Files.exists(p)) return Optional.empty();
        try {
            Settings s = MAPPER.readValue(p.toFile(), Settings.class);
            return Optional.ofNullable(s);
        } catch (IOException e) {
            System.err.println("[UserSettings] Failed to read settings: " + e.getMessage());
            return Optional.empty();
        }
    }

    public static boolean saveSettings(Settings settings) {
        Path p = Paths.get(FILE_NAME);
        try {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(p.toFile(), settings);
            return true;
        } catch (IOException e) {
            System.err.println("[UserSettings] Failed to write settings: " + e.getMessage());
            return false;
        }
    }

    // Convenience helpers
    public static Optional<String> loadLogFolder() {
        return loadSettings().map(s -> s.logFolder).filter(s -> s != null && !s.isBlank());
    }

    public static boolean saveLogFolder(String folderPath) {
        Settings s = loadSettings().orElseGet(Settings::new);
        s.logFolder = folderPath;
        return saveSettings(s);
    }

    public static Optional<Boolean> loadHistoryEnabled() {
        return loadSettings().map(s -> s.historyEnabled == null ? Boolean.FALSE : s.historyEnabled);
    }

    public static boolean saveHistoryEnabled(boolean enabled) {
        Settings s = loadSettings().orElseGet(Settings::new);
        s.historyEnabled = enabled;
        return saveSettings(s);
    }

    public static class Settings {
        public String logFolder;
        public Boolean historyEnabled = Boolean.FALSE;
    }
}
