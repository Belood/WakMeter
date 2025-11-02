package com.wakfu.data;

import java.util.Locale;
import java.util.ResourceBundle;

public class MessageProvider {
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("app_messages", Locale.getDefault());

    public static String get(String key) {
        if (BUNDLE.containsKey(key)) return BUNDLE.getString(key);
        return key;
    }

    public static String logsDetected() { return get("logs.detected"); }
    public static String waitingCombat() { return get("waiting.combat"); }
    public static String combatInProgress() { return get("combat.inprogress"); }
    public static String noLogFile() { return get("no.log.file"); }
    public static String appReady() { return get("app.ready"); }
    public static String logFolderSelected() { return get("log.folder.selected"); }
    public static String historyCleared() { return get("history.cleared"); }
    public static String historyClearError() { return get("history.clear.error"); }
    public static String addLogPath() { return get("add.log.path"); }
}

