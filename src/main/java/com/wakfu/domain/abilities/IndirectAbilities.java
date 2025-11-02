package com.wakfu.domain.abilities;

import java.util.Set;

/**
 * Contient la liste des sorts identifiés comme "indirects",
 * c’est-à-dire ceux qui infligent des dégâts secondaires
 * après un effet ou une condition.
 */
public final class IndirectAbilities {

    private IndirectAbilities() {} // empêche l’instanciation

    public static final Set<String> NAMES = Set.of(
            "Enflammé","Contre-attaque","Marque itsade"
    );

    /**
     * Vérifie si le nom d’un sort correspond à un effet indirect connu.
     */
    public static boolean isIndirect(String abilityName) {
        if (abilityName == null || abilityName.isBlank()) return false;
        return NAMES.stream().anyMatch(a -> a.equalsIgnoreCase(abilityName.trim()));
    }
}
