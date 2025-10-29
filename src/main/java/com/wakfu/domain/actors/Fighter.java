package com.wakfu.domain.actors;

/**
 * Classe de base pour tout acteur d’un combat.
 * Peut être un joueur (Player) ou un ennemi (Enemy).
 */
public abstract class Fighter {
    protected String name;
    protected long id;
    protected FighterType type;

    public Fighter(String name, long id, FighterType type) {
        this.name = name;
        this.id = id;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    public FighterType getType() {
        return type;
    }

    public boolean isControlledByAI() {
        return type == FighterType.ENEMY;
    }

    @Override
    public String toString() {
        return String.format("%s [%s, id=%d]", name, type, id);
    }

    /**
     * Types d’acteurs possibles dans un combat.
     */
    public enum FighterType {
        PLAYER,
        ENEMY
    }
}