# 🧮 Damage Meter Application — UML Design

This document presents the **UML class diagram** and **sequence diagram** for a *Damage Meter* application.  
The app parses a combat log file, calculates per-player and per-ability damage, and displays it in a simple UI with breakdown functionality.

---

## 🧩 Class Diagram

```mermaid
classDiagram
%% ===============================
%% COUCHE METIER / MODELE
%% ===============================

class Fighter {
    <<abstract>>
    - name: String
    - id: long
    - isControlledByAI: boolean
    + getName(): String
    + getId(): long
    + isControlledByAI(): boolean
    + getType(): FighterType
}

class FighterType {
    <<enumeration>>
    PLAYER
    ENEMY
}

class Player {
    + playerClass: PlayerClass
    + spells: List<SpellDamage>
    + addSpellDamage(spell: SpellDamage): void
    + getPlayerClass(): PlayerClass
    + getType(): FighterType
}

class Enemy {
    + breed: String
    + getBreed(): String
    + getType(): FighterType
}

Fighter <|-- Player
Fighter <|-- Enemy

class PlayerClass {
    + name: String
    + abilities: List<Ability>
}

class Ability {
    + name: String
    + element: String
    + baseDamage: int
}

class SpellDamage {
    + ability: Ability
    + damageDealt: int
}

PlayerClass --> Ability
Player --> SpellDamage
SpellDamage --> Ability

%% ===============================
%% EVENEMENTS DE COMBAT
%% ===============================

class CombatEvent {
    + timestamp: LocalDateTime
    + caster: Fighter
    + target: Fighter
    + ability: Ability
    + type: EventType
    + value: int
}

class EventType {
    <<enumeration>>
    DAMAGE
    HEAL
    BUFF
    KO
    INSTANT_KO
}

CombatEvent --> Fighter : caster
CombatEvent --> Fighter : target
CombatEvent --> Ability

%% ===============================
%% COUCHE PARSING
%% ===============================

class WakfuLogParserV3 {
    - fighters: Map<String, Fighter>
    - lastAbilityByCaster: Map<String, Ability>
    - lastCastTime: Map<String, Long>
    + startRealtimeParsing(path: Path, onEvent: Consumer<CombatEvent>): void
    + stop(): void
}

class LogPatterns {
    <<static>>
    + START_COMBAT: Pattern
    + END_COMBAT: Pattern
    + PLAYER_JOIN: Pattern
    + CAST_SPELL: Pattern
    + DAMAGE: Pattern
    + KO: Pattern
    + INSTANT_KO: Pattern
    + ELEMENT_REGEX: String
}

WakfuLogParserV3 --> LogPatterns
WakfuLogParserV3 --> CombatEvent
WakfuLogParserV3 --> Fighter


```