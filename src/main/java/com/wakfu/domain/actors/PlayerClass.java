package com.wakfu.domain.actors;

/**
 * Énumération des classes de personnages Wakfu.
 */
public enum PlayerClass {
    ELIOTROPE("Eliotrope", "logo-eliotrope.png"),
    IOP("Iop", "logo-iop.png"),
    SRAM("Sram", "logo-sram.png"),
    CRA("Cra", "logo-cra.png"),
    SACRIEUR("Sacrieur", "logo-sacrieur.png"),
    ECAFLIP("Écaflip", "logo-ecaflip.png"),
    OUGINAK("Ouginak", "logo-ouginak.png"),
    FECA("Féca", "logo-feca.png"),
    OSAMODAS("Osamodas", "logo-osamodas.png"),
    ENUTROF("Enutrof", "logo-enutrof.png"),
    XELOR("Xélor", "logo-xelor.png"),
    ENIRIPSA("Eniripsa", "logo-eniripsa.png"),
    SADIDA("Sadida", "logo-sadida.png"),
    PANDAWA("Pandawa", "logo-pandawa.png"),
    ROUBLARD("Roublard", "logo-roublard.png"),
    ZOBAL("Zobal", "logo-zobal.png"),
    STEAMER("Steamer", "logo-steamer.png"),
    HUPPERMAGE("Huppermage", "logo-huppermage.png"),
    UNKNOWN("Inconnu", null);

    private final String displayName;
    private final String iconFileName;

    PlayerClass(String displayName, String iconFileName) {
        this.displayName = displayName;
        this.iconFileName = iconFileName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconFileName() {
        return iconFileName;
    }

    /**
     * Retourne la classe correspondant au nom donné (insensible à la casse et aux accents).
     */
    public static PlayerClass fromString(String className) {
        if (className == null) return UNKNOWN;

        String normalized = className.toLowerCase()
                .replace("é", "e")
                .replace("è", "e")
                .replace("ê", "e")
                .trim();

        for (PlayerClass pc : values()) {
            String pcNormalized = pc.displayName.toLowerCase()
                    .replace("é", "e")
                    .replace("è", "e")
                    .replace("ê", "e")
                    .trim();
            if (pcNormalized.equals(normalized)) {
                return pc;
            }
        }
        return UNKNOWN;
    }

    /**
     * Retourne le chemin de l'icône de classe pour l'affichage UI.
     */
    public String getIconPath() {
        if (iconFileName == null) return null;
        return "/assets/classes/" + iconFileName;
    }
}

