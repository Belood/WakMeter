package com.wakfu.domain.actors;

/**
 * Représente un ennemi (mob, invocation, boss...).
 */
public class Enemy extends Fighter {

    private String breed; // ex: "Fantôme Tanukouï-San"

    public Enemy(String name, long id, String breed) {
        super(name, id, true);
        this.breed = breed;
    }

    public String getBreed() { return breed; }

    @Override
    public FighterType getType() {
        return FighterType.ENEMY;
    }
}