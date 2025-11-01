package com.wakfu.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Sauvegarde l'historique des combats dans 'fight_history.json' (répertoire courant).
 * Format NDJSON : un objet JSON par ligne (append-friendly).
 * Nous sérialisons directement le `FightModel` pour garantir un mapping 1:1.
 */
public class FightHistoryManager {
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final File HISTORY_FILE = new File("fight_history.json");

    public static synchronized void saveFight(Object fightModel) {
        if (fightModel == null) return;

        try (FileWriter fw = new FileWriter(HISTORY_FILE, true)) {
            String json = MAPPER.writeValueAsString(fightModel);
            fw.write(json);
            fw.write(System.lineSeparator());
            fw.flush();
            System.out.println("[FightHistory] Appended fight to " + HISTORY_FILE.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("[FightHistory] Failed to append fight: " + e.getMessage());
        }
    }

    public static synchronized boolean clearHistory() {
        try {
            if (HISTORY_FILE.exists()) {
                return HISTORY_FILE.delete();
            }
            return true;
        } catch (Exception e) {
            System.err.println("[FightHistory] Failed to clear history: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lit toutes les lignes JSON (raw) du fichier NDJSON et retourne une liste de chaînes.
     */
    public static synchronized List<String> readAllRawLines() {
        List<String> out = new ArrayList<>();
        if (!HISTORY_FILE.exists()) return out;

        try (BufferedReader br = new BufferedReader(new FileReader(HISTORY_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                out.add(line);
            }
        } catch (IOException e) {
            System.err.println("[FightHistory] Failed to read history file: " + e.getMessage());
        }
        return out;
    }

    /**
     * Lit et retourne la dernière ligne (raw) du fichier NDJSON, ou null si absent.
     */
    public static synchronized String readLastRawLine() {
        if (!HISTORY_FILE.exists()) return null;
        String lastLine = null;
        try (RandomAccessFile raf = new RandomAccessFile(HISTORY_FILE, "r")) {
            long fileLength = raf.length();
            if (fileLength == 0) return null;
            long pointer = fileLength - 1;
            while (pointer >= 0) {
                raf.seek(pointer);
                int read = raf.read();
                if (read == '\n' && pointer != fileLength - 1) {
                    break;
                }
                pointer--;
            }
            if (pointer < 0) raf.seek(0);
            else raf.seek(pointer + 1);

            lastLine = raf.readLine();
            if (lastLine != null && lastLine.isBlank()) lastLine = null;
        } catch (IOException e) {
            System.err.println("[FightHistory] Failed to read last fight: " + e.getMessage());
        }
        return lastLine;
    }
}
