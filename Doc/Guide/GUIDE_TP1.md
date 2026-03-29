# Guide détaillé — TP1 Serveur d’authentification (API REST Spring Boot)

Ce document reprend le **TP1** du PDF *TP 1-4 Auth Server* et explique l’implémentation du projet `authentification_back` pour que vous puissiez **la comprendre et la recoder** dans le bon ordre.

---

## 1. Objectif du TP1

- Mettre en place une authentification **volontairement faible** (pédagogique), **inacceptable en production**.
- Comprendre pourquoi « ça marche » ne suffit pas côté sécurité.
- Livrer une **API REST** avec **MySQL**, **gestion d’erreurs JSON**, **tests JUnit**, **logging fichier**, **JavaDoc**.

**Variante retenue dans ce projet** (le sujet autorise l’une ou l’autre) :

- **Jeton** généré au login, **stocké en colonne `token`** en base, envoyé au client ; les appels protégés passent le jeton en en-tête (`Authorization: Bearer …` ou `X-Auth-Token`).

*(L’autre option du sujet est une **session HTTP** avec cookie `JSESSIONID` — non utilisée ici.)*

---

## 2. Cahier des charges fonctionnel (rappel)

| Élément | Détail |
|--------|--------|
| Inscription | `POST` avec email + mot de passe |
| Règle mot de passe | **Minimum 4 caractères** (volontairement faible) |
| Serveur | Email **unique** ; mot de passe en **clair** (`password_clear`) |
| Login | `POST` vérifie email + mot de passe |
| Compte test | `toto@example.com` / `pwd1234` |
| Route protégée | `GET /api/me` — uniquement si authentifié (ici : jeton valide) |

---

## 3. Modèle de données MySQL

Table **`users`** (voir `authentification_back/src/main/resources/schema-mysql.sql`) :

| Colonne | Rôle |
|---------|------|
| `id` | Clé primaire auto-incrémentée |
| `email` | Unique, identifiant de connexion |
| `password_clear` | Mot de passe **en clair** (TP1 uniquement) |
| `created_at` | Date de création du compte |
| `token` | Jeton d’accès après login (nullable avant première connexion) |

Contraintes : `email` unique, `token` unique (plusieurs `NULL` autorisés en MySQL pour une colonne unique).

---

## 4. Structure des packages (couches)

```
com.example.authentification_back
├── AuthentificationBackApplication.java   # Point d’entrée Spring Boot
├── controller/     # REST : HTTP ↔ DTO
├── service/        # Règles métier, transactions
├── repository/     # Accès base (Spring Data JPA)
├── entity/         # Modèle JPA ↔ table MySQL
├── dto/            # Objets échangés en JSON (requêtes / réponses)
├── exception/      # Exceptions métier + @ControllerAdvice global
└── config/         # Initialisation (ex. compte de test toto)
```

Ordre logique pour **recoder** :

1. Entité + repository + script SQL  
2. DTO + validation  
3. Exceptions + `GlobalExceptionHandler`  
4. `AuthService` (register, login, currentUser)  
5. `AuthController`  
6. Compte test (`CommandLineRunner`)  
7. `application.properties` + tests  

---

## 5. Configuration (`application.properties`)

Points importants :

- **URL JDBC** : port MySQL (ex. `3307`), nom de la base (ex. `authentification`).
- **`spring.jpa.hibernate.ddl-auto=update`** : Hibernate met à jour le schéma à partir des entités (pratique en dev ; en production on préfère des migrations versionnées).
- **Logging fichier** : `logging.file.name=logs/authentification.log` — **ne jamais y écrire les mots de passe**.

---

## 6. Endpoints HTTP

| Méthode | Chemin | Corps / en-têtes | Réponse typique |
|---------|--------|------------------|-----------------|
| `POST` | `/api/auth/register` | JSON `{ "email", "password" }` | **201** + `id`, `email`, `createdAt` (pas de `token`) |
| `POST` | `/api/auth/login` | JSON `{ "email", "password" }` | **200** + `id`, `email`, `createdAt`, **`token`** |
| `GET` | `/api/me` | `Authorization: Bearer <token>` **ou** `X-Auth-Token: <token>` | **200** + `id`, `email`, `createdAt` |

**Attention** : la route profil est **`/api/me`**, pas `/api/auth/me`.

### Codes HTTP d’erreur (gérés par `GlobalExceptionHandler`)

- **400** : données invalides (`InvalidInputException`, erreurs Bean Validation `@Valid`)
- **401** : login échoué ou jeton absent/invalide (`AuthenticationFailedException`)
- **409** : email déjà enregistré (`ResourceConflictException`)

Le corps d’erreur suit `ApiErrorResponse` : `timestamp`, `status`, `error`, `message`, `path`.

---

## 7. Flux détaillés

### 7.1 Inscription (`register`)

1. Le client envoie `RegisterRequest` (validé par `@Valid` : `@Email`, `@Size(min=4)`, etc.).
2. `AuthService` normalise l’email (trim + minuscules).
3. Vérification longueur mot de passe ≥ 4 (doublon côté service avec la validation).
4. Si email déjà présent → `ResourceConflictException` → **409**.
5. Création `User`, sauvegarde ; **pas de token** tant que l’utilisateur ne s’est pas connecté.

### 7.2 Connexion (`login`)

1. Recherche utilisateur par email ; si absent → **401** « Email inconnu ».
2. Comparaison **en clair** du mot de passe (TP1) ; si différent → **401** « Mot de passe incorrect ».
3. Génération d’un **`UUID`** string, affectation à `user.token`, `save`.
4. Réponse JSON avec `UserResponse.login(user, token)` — le champ `token` est sérialisé grâce à `@JsonInclude(NON_NULL)` sur les autres réponses où le token est `null`.

### 7.3 Profil (`/api/me`)

1. `AuthController` lit `Authorization` (préfixe `Bearer `) ou `X-Auth-Token`.
2. `AuthService.currentUser(token)` : si vide → **401** ; sinon `findByToken` ; si inconnu → **401** « Token invalide ».
3. Réponse profil via `UserResponse.profile()` → **pas de champ `token`** dans le JSON.

---

## 8. Fichiers clés du projet (références)

| Fichier | Rôle |
|---------|------|
| `entity/User.java` | Mapping JPA, colonnes dont `password_clear`, `token` |
| `repository/UserRepository.java` | `findByEmail`, `findByToken`, `existsByEmail` |
| `dto/RegisterRequest.java`, `LoginRequest.java` | Entrées JSON + contraintes Bean Validation |
| `dto/UserResponse.java` | Sorties ; fabriques `profile` / `login` |
| `dto/ApiErrorResponse.java` | Format d’erreur uniforme |
| `exception/*.java` | Exceptions métier |
| `exception/GlobalExceptionHandler.java` | Traduction exception → HTTP + JSON |
| `service/AuthService.java` | Cœur métier |
| `controller/AuthController.java` | Mapping REST |
| `config/TestAccountInitializer.java` | Création du compte `toto@example.com` au démarrage s’il manque |
| `resources/schema-mysql.sql` | Script SQL Workbench |
| `src/test/.../AuthApiIntegrationTest.java` | Tests d’intégration MockMvc |

---

## 9. Tests JUnit (idées à couvrir)

Le TP demande au minimum **8 tests** ; le projet en contient davantage, par exemple :

- Email invalide → **400**
- Mot de passe &lt; 4 caractères → **400**
- Inscription OK
- Inscription doublon → **409**
- Login OK + présence du `token`
- Login mauvais mot de passe → **401**
- Email inconnu → **401**
- `GET /api/me` sans jeton → **401**
- `GET /api/me` avec `Bearer` après login → **200**

Profil **`test`** : H2 en mémoire (`application-test.properties`) pour ne pas dépendre de MySQL pendant `mvn test`.

---

## 10. Postman (rappel)

1. `POST /api/auth/login` avec le compte `toto` → copier le **`token`** dans la réponse.
2. `GET http://localhost:8080/api/me` avec en-tête **`Authorization`** : `Bearer <token>` (ou `X-Auth-Token`).

---

## 11. Analyse de sécurité (minimum TP1)

À rédiger dans votre README : au moins **5 risques** (ex. mots de passe en clair, pas de hachage, jeton sans expiration, pas de HTTPS, énumération d’emails sur les messages d’erreur, etc.).

---

## 12. Pour aller plus loin (TP2+)

Le TP2 introduit : politique de mot de passe forte, **BCrypt**, verrouillage anti brute-force, SonarCloud, etc. Le protocole HMAC arrive **au TP3**.

---

## 13. Annexe — où trouver le code commenté

Le détail ligne à ligne est dans le dépôt Maven :

| Fichier | Rôle |
|---------|------|
| `authentification_back/src/main/java/.../AuthentificationBackApplication.java` | `main`, lancement Spring Boot |
| `.../entity/User.java` | Entité JPA |
| `.../repository/UserRepository.java` | Spring Data |
| `.../dto/*.java` | DTO + validation |
| `.../exception/*.java` | Exceptions + `GlobalExceptionHandler` |
| `.../service/AuthService.java` | Règles métier |
| `.../controller/AuthController.java` | REST |
| `.../config/TestAccountInitializer.java` | Compte `toto` |
| `src/main/resources/application.properties` | DataSource, JPA, logs |
| `src/main/resources/schema-mysql.sql` | Script MySQL |
| `src/test/.../AuthApiIntegrationTest.java` | Tests |

Recopier le TP1 à la main : suivre l’ordre de la section 4, en collant ou en retapant chaque fichier depuis l’IDE.

---

*Ce guide décrit l’état du dépôt `authentification_back` au moment de sa rédaction ; les fichiers sources commentés restent la référence exacte pour le comportement.*
