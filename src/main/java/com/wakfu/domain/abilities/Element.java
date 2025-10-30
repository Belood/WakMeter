package com.wakfu.domain.abilities;

/**
 * Représente les éléments de Wakfu (en français).
 */
public enum Element {
    FEU,
    EAU,
    TERRE,
    AIR,
    LUMIERE,
    STASIS,
    NEUTRE,
    INCONNU;

    /**
     * Convertit un texte en élément.
     * Tolère les accents et majuscules/minuscules.
     */
    public static Element fromString(String raw) {
        if (raw == null) return INCONNU;
        String value = raw.trim()
                .toLowerCase()
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .replace("à", "a")
                .replace("î", "i")
                .replace("ï", "i")
                .replace("ô", "o")
                .replace("û", "u");

        return switch (value) {
            case "feu" -> FEU;
            case "eau" -> EAU;
            case "terre" -> TERRE;
            case "air" -> AIR;
            case "lumiere" -> LUMIERE;
            case "stasis" -> STASIS;
            case "neutre" -> NEUTRE;
            default -> INCONNU;
        };
    }

    @Override
    public String toString() {
        String name = name().charAt(0) + name().substring(1).toLowerCase();
        return name.replace("Inconnu", "Inconnu").replace("Lumiere", "Lumière");
    }
}
