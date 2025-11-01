# Architecture MVC - WakMeter

## 📋 Vue d'ensemble

L'application WakMeter suit le pattern **Model-View-Controller (MVC)** pour une meilleure séparation des responsabilités et maintenabilité.

---

## 🏛️ Composants Principaux

### 1. **Model (Modèle)**

#### `DamageCalculator`
- **Responsabilité** : Calcule les statistiques de dégâts
- **Méthodes principales** :
  - `getTotalDamage(FightModel)` : Retourne les dégâts totaux
  - `refreshFromModel(FightModel)` : Met à jour l'état interne

#### `FightModel`
- Représente l'état global du combat
- Contient les statistiques par joueur
- Immutable (ne doit pas être modifié directement)

#### `PlayerStats`
- Contient les statistiques d'un joueur
- Inclut la liste des sorts et leurs dégâts
- Accessible via `FightModel.getStatsByPlayer()`

---

### 2. **View (Vues)**

#### `MainUI.java` - Conteneur Principal
```
Responsabilité : Structure visuelle de base (BorderPane)

Composants :
- Top    : Header avec boutons et statut
- Center : VBox pour la liste des joueurs
- Right  : Pane pour l'affichage du breakdown

Méthodes clés :
+ addToHeader(Node) : Ajoute un contrôle au header
+ setAppStatus(String) : Met à jour le statut
+ setBreakdownPanel(Pane) : Affiche le breakdown à droite
+ getCenterContainer() : Retourne le conteneur central
+ getRightPane() : Retourne le pane droit
```

#### `PlayerUI.java` - Affichage d'un Joueur
```
Responsabilité : Render une ligne de joueur

Layout : [Nom] [Barre] [Dégâts] [%] [🔍]

Paramètres du constructeur :
- stats : PlayerStats du joueur
- percentage : Pourcentage (0..1) des dégâts totaux
- barColor : Couleur de la barre
- onBreakdownRequested : Callback quand on clique sur 🔍

Important : N'instancie JAMAIS DamageBreakdownUI
            Appelle uniquement le callback fourni
```

#### `BreakdownPane.java` - Breakdown Réutilisable
```
Responsabilité : Afficher les dégâts par sort

Static buildPanel(Object playerStats) → Pane

Affichage :
- Titre du joueur
- Grille avec colonnes : Sort | Barre | Dégâts | Dégâts/PA | %
- Barres colorées par élément

Utilisation :
Pane panel = BreakdownPane.buildPanel(playerStats);
mainUI.setBreakdownPanel(panel);
```

---

### 3. **Controller (Contrôleur)**

#### `UIManager.java` - Orchestrateur Central
```
Responsabilité : Coordination Model ↔ View

Gère :
1. Événements utilisateur (boutons, selections)
2. Appels au DamageCalculator pour récupérer les données
3. Mises à jour de MainUI avec les données calculées
4. Affichage du breakdown dans le right pane

Flux principal :
1. Utilisateur clique sur un bouton
2. UIManager capture l'événement
3. Interroge DamageCalculator si nécessaire
4. Met à jour MainUI (View)

Flux Breakdown :
1. Utilisateur clique sur 🔍 d'un joueur
2. PlayerUI appelle le callback fourni
3. UIManager.showBreakdownInRightPane() appelé
4. Crée un BreakdownPane avec les données
5. Affiche dans mainUI.setBreakdownPanel()
```

---

## 🔄 Flux de Données

### Initialisation
```
WakfuMeterApp.start()
    ↓
Crée : DamageCalculator, UIManager(primaryStage, calculator)
    ↓
UIManager()
    → Crée MainUI
    → Initialise les contrôles du header
    → Les ajoute à MainUI.addAllToHeader()
    ↓
MainUI.setupLayout()
    → BorderPane avec header, center, right
    → Affiche la fenêtre
```

### Lors d'un combat
```
LogParser détecte les logs
    ↓
EventProcessor.onEvent()
    ↓
Modèle du combat mis à jour
    ↓
eventProcessor.addModelListener() notifie
    ↓
UIManager.refresh(model)
    ↓
DamageCalculator.refreshFromModel(model)
    ↓
UIManager.displayPlayerStats()
    → Crée PlayerUI pour chaque joueur
    → Chaque PlayerUI injecte le callback
    ↓
MainUI.getCenterContainer().add(playerUI.render())
```

### Au clic sur Breakdown
```
Utilisateur clique sur 🔍
    ↓
PlayerUI.onAction() appelle callback
    ↓
UIManager.showBreakdownInRightPane(stats)
    ↓
BreakdownPane.buildPanel(stats) crée Pane
    ↓
mainUI.setBreakdownPanel(pane)
    ↓
Right pane se remplit du breakdown
```

---

## 📐 Avantages de l'Architecture MVC

| Aspect | Avantage |
|--------|----------|
| **Testabilité** | Chaque couche peut être testée indépendamment |
| **Maintenabilité** | Modification d'une vue n'affecte pas le modèle |
| **Extensibilité** | Ajouter une nouvelle vue est facile |
| **Réutilisabilité** | `BreakdownPane` peut s'utiliser partout |
| **Lisibilité** | Code organisé, responsabilités claires |

---

## 🔧 Comment Ajouter une Nouvelle Feature

### Exemple : Ajouter un bouton pour exporter les dégâts

1. **Dans MainUI** : Rien à changer
2. **Dans PlayerUI** : Rien à changer
3. **Dans UIManager** (Controller) :
```java
// Ajouter le bouton
private final Button exportButton = new Button("💾");

// Dans setupHeaderControls()
exportButton.setOnAction(e -> {
    // Appeler le modèle si nécessaire
    String data = damageCalculator.exportData(lastModel);
    // Mettre à jour la vue si nécessaire
    statusLabel.setText("Export réussi");
});

// Ajouter au header
mainUI.addToHeader(exportButton);
```

---

## 📌 Règles de Conception

### 1. **Model (DamageCalculator, FightModel)**
- ✅ Peut être appelé par UIManager
- ✅ Peut être appelé par d'autres models
- ❌ Ne doit JAMAIS faire de UI
- ❌ Ne doit JAMAIS connaître UIManager

### 2. **View (MainUI, PlayerUI, BreakdownPane)**
- ✅ Peut afficher des données
- ✅ Peut appeler des callbacks
- ✅ Peut recevoir des callbacks injectés
- ❌ Ne doit JAMAIS appeler DamageCalculator directement
- ❌ Ne doit JAMAIS modifier le modèle

### 3. **Controller (UIManager)**
- ✅ Peut appeler le model (DamageCalculator)
- ✅ Peut mettre à jour les vues (MainUI, PlayerUI)
- ✅ Peut se faire notifier par le model
- ❌ Ne doit PAS avoir de logique métier complexe
- ❌ Ne doit PAS créer d'UI directement

---

## 🎯 Points d'Extension

### Pour modifier l'interface
→ Éditer `MainUI`, `PlayerUI`, ou `BreakdownPane`

### Pour ajouter une logique de calcul
→ Éditer `DamageCalculator`

### Pour ajouter une interaction utilisateur
→ Éditer `UIManager`

---

## ✅ Checklist de Qualité

- [x] UIManager n'utilise que MainUI pour l'UI
- [x] PlayerUI n'instancie pas DamageBreakdownUI
- [x] Breakdown s'affiche dans le right pane
- [x] Pas de multiples fenêtres pour le breakdown
- [x] Séparation Model/View/Controller respectée
- [x] Compilation sans erreurs
- [x] Architecture documentée

