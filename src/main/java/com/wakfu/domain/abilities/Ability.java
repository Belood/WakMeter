package com.wakfu.domain.abilities;

/**
 * Représente un sort ou effet dans Wakfu.
 */
public class Ability {

    private final String name;
    private final String category; // ex: "Sort", "Effet indirect"
    private Element element;
    private final DamageSourceType sourceType;

    public Ability(String name, String category, Element element, DamageSourceType sourceType) {
        this.name = name != null ? name.trim() : "Inconnu";
        this.category = category != null ? category.trim() : "Autre";
        this.element = element != null ? element : Element.INCONNU;
        this.sourceType = sourceType != null ? sourceType : DamageSourceType.AUTRE;
    }

    public void setElement(Element element) {
        this.element = element;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public Element getElement() {
        return element;
    }

    public DamageSourceType getSourceType() {
        return sourceType;
    }

    @Override
    public String toString() {
        return String.format("%s [%s - %s]", name, element, sourceType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Ability other)) return false;
        return name.equalsIgnoreCase(other.name)
                && element == other.element
                && sourceType == other.sourceType;
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode() + element.hashCode() * 31 + sourceType.hashCode() * 17;
    }


}
