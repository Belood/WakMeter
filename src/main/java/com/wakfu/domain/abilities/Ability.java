package com.wakfu.domain.abilities;

public class Ability {
    private String name;
    private String element;  // Eau, Feu, Terre, Air...
    private int baseDamage;

    public Ability(String name, String element) {
        this.name = name;
        this.element = element;
    }

    public String getName() { return name; }
    public String getElement() { return element; }

    @Override
    public String toString() {
        return String.format("%s (%s)", name, element);
    }
}