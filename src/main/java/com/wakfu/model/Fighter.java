package com.wakfu.model;

public class Fighter {
    private String name;
    private long id;
    private boolean isControlledByAI;

    public Fighter(String name, long id, boolean isControlledByAI) {
        this.name = name;
        this.id = id;
        this.isControlledByAI = isControlledByAI;
    }

    public String getName() { return name; }
    public long getId() { return id; }
    public boolean isControlledByAI() { return isControlledByAI; }

    @Override
    public String toString() {
        return String.format("%s [id=%d, AI=%b]", name, id, isControlledByAI);
    }
}