package com.wakfu.parser;

import com.wakfu.domain.actors.Fighter;
import com.wakfu.domain.actors.Player;

import java.util.HashMap;
import java.util.Map;

public class FighterRegistry {
    private final Map<String, Fighter> fighters = new HashMap<>();
    
    public Fighter getOrCreate(String name, boolean isAI, long id) {
        return fighters.computeIfAbsent(name, n -> 
            isAI ? new com.wakfu.domain.actors.Enemy(name, id, name) 
                 : new Player(name, id, Fighter.FighterType.PLAYER)
        );
    }
    
    public Fighter getOrCreateEnemy(String name) {
        return fighters.computeIfAbsent(name, n -> 
            new com.wakfu.domain.actors.Enemy(n, -1, n)
        );
    }
    
    public Fighter get(String name) {
        return fighters.get(name);
    }
    
    public void clear() {
        fighters.clear();
    }
    
    public int countActivePlayers(java.util.Set<String> koPlayers) {
        return (int) fighters.values().stream()
                .filter(f -> f.getType() == Fighter.FighterType.PLAYER)
                .filter(f -> !koPlayers.contains(f.getName()))
                .count();
    }
}

