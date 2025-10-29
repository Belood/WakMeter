package com.wakfu.domain.actors;

/**
 * Représente un ennemi (mob, invocation, boss...).
 */
public class Enemy extends Fighter {

    private String breed; // Exemple : "Fantôme Tanukouï-San"

    public Enemy(String name, long id, String breed) {
        super(name, id, FighterType.ENEMY);
        this.breed = breed;
    }

    public String getBreed() {
        return breed;
    }

    @Override
    public FighterType getType() {
        return FighterType.ENEMY;
    }

    @Override
    public String toString() {
        return String.format("%s (%s, id=%d, breed=%s)", name, getType(), id, breed);
    }
}
