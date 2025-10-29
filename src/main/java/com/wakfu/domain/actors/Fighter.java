package com.wakfu.domain.actors;

/**
 * Classe de base pour tout acteur d’un combat.
 * Peut être un joueur ou un ennemi.
 */
public abstract class Fighter {
    protected String name;
    protected long id;
    protected boolean isControlledByAI;

    public Fighter(String name, long id, boolean isControlledByAI) {
        this.name = name;
        this.id = id;
        this.isControlledByAI = isControlledByAI;
    }

    public String getName() { return name; }
    public long getId() { return id; }
    public boolean isControlledByAI() { return isControlledByAI; }

    public abstract FighterType getType();

    @Override
    public String toString() {
        return String.format("%s (%s, id=%d)", name, getType(), id);
    }

    public enum FighterType {
        PLAYER, ENEMY
    }
}