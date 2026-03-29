# TP1 — API d’authentification (volontairement dangereuse)

Back-end Spring Boot : inscription, connexion avec **jeton stocké en base** (colonne `token` sur `users` — variante prévue au TP1 à la place d’une session HTTP), route protégée `GET /api/me`.

> **Important :** ne commitez pas de mots de passe réels sur un dépôt public. Préférez des variables d’environnement ou un `application-local.properties` ignoré par Git.

## Base MySQL (Workbench / port 3307)

1. Créer la base et la table (script fourni : `src/main/resources/schema-mysql.sql`).
2. Vérifier dans `application.properties` :
   - `spring.datasource.url` → port **3307**, base **`authentification`** (voir votre `application.properties`)
   - `spring.datasource.username` / `spring.datasource.password` (utilisateur MySQL, souvent `root` en local)

Avec `spring.jpa.hibernate.ddl-auto=update`, Hibernate peut créer/mettre à jour la table si la base existe déjà.

## Compte de test obligatoire (TP)

Créé au démarrage s’il n’existe pas :

- **Email :** `toto@example.com`
- **Mot de passe :** `pwd1234`

## Lancer l’API

```bash
mvn spring-boot:run
```

API par défaut : `http://localhost:8080`

## Tester avec Postman

1. **POST** `http://localhost:8080/api/auth/register`  
   Corps JSON : `{ "email": "vous@example.com", "password": "1234" }`  
   (mot de passe **≥ 4** caractères, règles volontairement faibles — TP1.)  
   Réponse : `id`, `email`, `createdAt` (pas de `token` tant que vous n’êtes pas connecté).
2. **POST** `http://localhost:8080/api/auth/login`  
   Corps : `{ "email": "toto@example.com", "password": "pwd1234" }`  
   Réponse JSON : `id`, `email`, `createdAt`, **`token`** (UUID enregistré en base).
3. **GET** `http://localhost:8080/api/me`  
   Ajouter l’en-tête **`Authorization`** : `Bearer <token>`  
   (alternative : en-tête **`X-Auth-Token`** avec la valeur du jeton seul).  
   Réponse : `id`, `email`, `createdAt` (le `token` n’est pas renvoyé sur cette route).

Réponses d’erreur JSON : `timestamp`, `status`, `error`, `message`, `path` (HTTP **400** / **401** / **409** selon le cas).

## Journal

Fichier : `logs/authentification.log` (connexion / inscription réussie ou échouée — **jamais** de mot de passe dans les logs).

## Tests

```bash
mvn test
```

Les tests utilisent le profil `test` (H2 en mémoire, voir `application-test.properties`).

## Analyse de sécurité TP1 (aperçu)

1. **Mots de passe en clair** en base : fuite DB = compromission totale des comptes.  
2. **Pas de hachage** : aucune protection si les données sont copiées.  
3. **Jeton en base sans expiration** : fuite ou interception du jeton (réseau, logs, XSS) = accès au compte tant que le jeton n’est pas révoqué.  
4. **Politique de mot de passe minimale (4 caractères)** : bruteforce et devinettes faciles.  
5. **Pas de rate limiting** : tentatives de connexion illimitées (corrigé en TP2).

Cette implémentation est **pédagogique** et **inacceptable en production**.
