package com.wakfu.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Charge la ressource `data/SortsPA.json` et expose les coûts (PA) des sorts par classe.
 * Les clés sont normalisées (minuscules, sans accents) pour faciliter la recherche.
 */
public class SpellCostProvider {
    private static final Map<String, Map<String, Integer>> COSTS = new HashMap<>();

    static {
        try (InputStream is = SpellCostProvider.class.getResourceAsStream("/data/SortsPA.json")) {
            if (is != null) {
                ObjectMapper m = new ObjectMapper();
                JsonNode root = m.readTree(is);
                if (root.isArray() && !root.isEmpty()) {
                    JsonNode first = root.get(0);
                    Iterator<String> classNames = first.fieldNames();
                    while (classNames.hasNext()) {
                        String className = classNames.next();
                        JsonNode spellsNode = first.get(className);
                        Map<String, Integer> map = new HashMap<>();
                        if (spellsNode != null && spellsNode.isObject()) {
                            Iterator<String> spellNames = spellsNode.fieldNames();
                            while (spellNames.hasNext()) {
                                String spell = spellNames.next();
                                JsonNode v = spellsNode.get(spell);
                                if (v != null && v.isInt()) map.put(normalize(spell), v.asInt());
                            }
                        }
                        COSTS.put(normalize(className), map);
                    }
                }
            } else {
                System.err.println("[SpellCostProvider] resource SortsPA.json introuvable");
            }
        } catch (Exception e) {
            System.err.println("[SpellCostProvider] erreur lecture SortsPA.json: " + e.getMessage());
        }
    }

    /**
     * Retourne le coût PA pour un sort. Si `className` est fourni, on tente d'abord la recherche
     * dans cette classe; si absent (ou si className==null), on parcourt toutes les classes et
     * retourne le premier coût trouvé pour ce sort.
     */
    public static Integer getCostFor(String className, String spellName) {
        if (spellName == null) return null;
        String normSpell = normalize(spellName);

        // Tentative par classe si fournie
        if (className != null) {
            Map<String, Integer> m = COSTS.get(normalize(className));
            if (m != null && m.containsKey(normSpell)) return m.get(normSpell);
        }

        // Fallback : rechercher le sort parmi toutes les classes
        for (Map<String, Integer> m : COSTS.values()) {
            if (m.containsKey(normSpell)) return m.get(normSpell);
        }
        return null;
    }

    private static String normalize(String s) {
        if (s == null) return null;
        String n = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return n.toLowerCase().trim();
    }
}
