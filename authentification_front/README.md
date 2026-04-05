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

### IntelliJ IDEA + `module-info.java` (erreur Gson / JavaFX)

Si au lancement tu vois **`Unresolved compilation problems`** ou **`Gson is not accessible`** alors que **`mvn javafx:run`** fonctionne :

1. **Build → Rebuild Project** après un **`mvn clean compile`** (fenêtre Maven, module `authentification_front`).
2. **File → Settings → Build, Execution, Deployment → Compiler → Java Compiler** : pour le projet / module, utilise le compilateur **Javac** (pas *Eclipse*).
3. **File → Settings → Build, Execution, Deployment → Build Tools → Maven → Runner** : coche **Delegate IDE build/run actions to Maven** (recommandé pour ce module JPMS).
4. Vérifie que le SDK du projet est un **JDK 17+** (ex. Corretto 21) cohérent avec le `pom.xml`.

## Fonctionnalités

- Saisie de l’**URL de l’API** (pour changer de port sans recompiler).
- **Connexion** : récupère le **jeton** et le garde en mémoire pour `/api/me`.
- **Inscription** : `password` + `passwordConfirm` + **indicateur de force** (rouge / orange / vert), comme demandé au TP2 côté client.
- **Profil** : appelle `GET /api/me` avec `Authorization: Bearer <token>`.
- **Mot de passe (TP5)** : appelle `PUT /api/auth/change-password` avec le jeton actif.

## Couplage avec le backend

Aucun lien Maven entre les deux projets : communication **uniquement HTTP/JSON**. Voir `Doc/Guide/GUIDE_TP2.md`, section **Client JavaFX**.
