# Client JavaFX — `authentification_front`

Interface **JavaFX** (FXML) pour l’API REST du module **`authentification_back`** (TP2).

## Prérequis

- **JDK 21** (aligné sur le `pom.xml`).
- Backend démarré et joignable (par défaut **`http://localhost:8080`**).

## Lancer l’application

À la racine de ce module :

```bash
mvn javafx:run
```

Ou exécuter la classe **`Launcher`** depuis l’IDE.

## Fonctionnalités

- Saisie de l’**URL de l’API** (pour changer de port sans recompiler).
- **Connexion** : récupère le **jeton** et le garde en mémoire pour `/api/me`.
- **Inscription** : `password` + `passwordConfirm` + **indicateur de force** (rouge / orange / vert), comme demandé au TP2 côté client.
- **Profil** : appelle `GET /api/me` avec `Authorization: Bearer <token>`.
- **Mot de passe (TP5)** : appelle `PUT /api/auth/change-password` avec le jeton actif.

## Couplage avec le backend

Aucun lien Maven entre les deux projets : communication **uniquement HTTP/JSON**. Voir `Doc/Guide/GUIDE_TP2.md`, section **Client JavaFX**.
