package com.wakfu.parser;

import com.wakfu.domain.actors.*;
import com.wakfu.domain.event.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

/**
 * Lit le log Wakfu en temps réel et génère des événements structurés.
 */
public class LogParser {

    private volatile boolean running = false;
    private Thread watchThread;
    private final LogProcessor processor;

    public LogParser(LogProcessor processor) {
        this.processor = processor;
    }


    // === Lecture du fichier en temps réel ===
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

    private void watchFile(Path logFilePath, Consumer<LogEvent> onEvent) {
        try (RandomAccessFile raf = new RandomAccessFile(logFilePath.toFile(), "r")) {
            System.out.println("[Parser] Watching log in UTF-8: " + logFilePath);
            long filePointer = raf.length(); // commence à la fin

            while (running) {
                long fileLength = raf.length();
                if (fileLength < filePointer) {
                    filePointer = fileLength; // reset si le log est recréé
                } else if (fileLength > filePointer) {
                    raf.seek(filePointer);
                    String line;
                    while ((line = raf.readLine()) != null) {
                        String utf8Line = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                        // Le LogProcessor gère l'EventProcessor en interne désormais
                        processor.processLine(utf8Line.trim());
                    }
                    filePointer = raf.getFilePointer();
                }
                Thread.sleep(300);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[Parser] Error: " + e.getMessage());
        }
    }


}
