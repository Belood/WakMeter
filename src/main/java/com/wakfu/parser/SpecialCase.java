package com.wakfu.parser;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SpecialCase {

    // Map of special spell name replacements. Keys are stored in lower-case for case-insensitive lookup.
    private static final Map<String, String> SPECIAL_SPELLS;

    static {
        Map<String, String> m = new HashMap<>();
        // Example: Exaltation -> Cataclysme
        m.put("Exaltation", "Cataclysme");
        // Add more special cases here if needed
        SPECIAL_SPELLS = Collections.unmodifiableMap(m);
    }

    /**
     * Return an unmodifiable view of the configured special spells map.
     */
    public static Map<String, String> getSpecialSpells() {
        return SPECIAL_SPELLS;
    }

    /**
     * Given a spell name parsed from logs, return the mapped special spell name if present,
     * otherwise return null.
     * Lookup is case-insensitive and trims the input.
     */
    public static String specialSpell(String spellName) {
        if (spellName == null) return null;
        String key = spellName.trim().toLowerCase(Locale.ROOT);
        return SPECIAL_SPELLS.getOrDefault(key, null);
    }
}
