# 🧮 Damage Meter Application — UML Design

This document presents the **UML class diagram** and **sequence diagram** for a *Damage Meter* application.  
The app parses a combat log file, calculates per-player and per-ability damage, and displays it in a simple UI with breakdown functionality.

---

## 🧩 Class Diagram

```mermaid
classDiagram
%% ===============================
%% COUCHE PRINCIPALE
%% ===============================

class DamageMeterApp {
    - LogParser parser
    - DamageCalculator calculator
    - UIManager uiManager
    + run(logFilePath: String): void
}

%% ===============================
%% COUCHE PARSING
%% ===============================

class LogParser {
    + parseLog(filePath: String): List<CombatEvent>
}

class CombatEvent {
    + timestamp: Date
    + playerName: String
    + abilityName: String
    + damageAmount: int
}

%% ===============================
%% COUCHE METIER / MODELE
%% ===============================

class DamageCalculator {
    + calculatePlayerStats(events: List<CombatEvent>): List<Player>
    + calculateTotalDamage(players: List<Player>): int
}

class Player {
    + name: String
    + playerClass: PlayerClass
    + spells: List<SpellDamage>
    + getTotalDamage(): int
    + getDamagePercentage(totalDamage: int): float
}

class PlayerClass {
    + name: String
    + abilities: List<Ability>
}

class Ability {
    + name: String
    + baseDamage: int
}

class SpellDamage {
    + ability: Ability
    + damageDealt: int
}

%% ===============================
%% COUCHE PRESENTATION
%% ===============================

class UIManager {
    + displayPlayerStats(players: List<Player>, totalDamage: int): void
    + showSpellBreakdown(player: Player): void
}

class PlayerUI {
    + playerName: String
    + damageBarWidth: float
    + totalDamage: int
    + percentage: float
    + onShowBreakdownClicked(): void
}

class SpellBreakdownUI {
    + playerName: String
    + spells: List<SpellDamage>
    + displayBreakdown(): void
}

%% ===============================
%% RELATIONS ENTRE CLASSES
%% ===============================

DamageMeterApp --> LogParser
DamageMeterApp --> DamageCalculator
DamageMeterApp --> UIManager

LogParser --> CombatEvent
DamageCalculator --> Player
DamageCalculator --> CombatEvent
Player --> PlayerClass
Player --> SpellDamage
PlayerClass --> Ability
SpellDamage --> Ability
UIManager --> PlayerUI
UIManager --> SpellBreakdownUI
UIManager --> Player
PlayerUI --> SpellBreakdownUI : "ouvre"


```

```merdaid
sequenceDiagram
participant User as Utilisateur
participant App as DamageMeterApp
participant Parser as LogParser
participant Calc as DamageCalculator
participant UI as UIManager
participant PlayerUI as PlayerUI
participant Breakdown as SpellBreakdownUI

%% === Phase 1 : Lancement du programme ===
User->>App: run("combat.log")
App->>Parser: parseLog("combat.log")
Parser-->>App: List<CombatEvent>

%% === Phase 2 : Calcul des statistiques ===
App->>Calc: calculatePlayerStats(events)
Calc-->>App: List<Player>
App->>Calc: calculateTotalDamage(players)
Calc-->>App: totalDamage

%% === Phase 3 : Affichage principal ===
App->>UI: displayPlayerStats(players, totalDamage)
UI->>PlayerUI: créer une ligne par joueur
PlayerUI-->>UI: lignes prêtes avec bouton "Breakdown"
UI-->>User: affiche la liste principale des joueurs

%% === Phase 4 : Interaction Breakdown ===
User->>PlayerUI: clic sur "Breakdown"
PlayerUI->>UI: onShowBreakdownClicked()
UI->>UI: showSpellBreakdown(player)
UI->>Breakdown: créer SpellBreakdownUI(player)
Breakdown->>Breakdown: displayBreakdown()
Breakdown-->>User: affiche la répartition des dégâts par sort

%% === Phase 5 : Fermeture Breakdown ===
User->>Breakdown: clic sur "Fermer"
Breakdown->>UI: onClose()
UI->>Breakdown: destroy() / hide()
UI->>UI: displayPlayerStats(players, totalDamage)
UI-->>User: retourne à la vue principale
```