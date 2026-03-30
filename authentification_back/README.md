# API d’authentification — TP3 (Spring Boot)

Back-end : **mot de passe chiffré (SMK)** pour permettre la vérification **HMAC** côté serveur, **politique de mot de passe stricte**, **double confirmation** à l’inscription, **verrouillage** après échecs répétés, **jeton** en base, table **auth_nonce** ; `GET /api/me` protégé.

> Ne commitez pas de secrets sur un dépôt public. Préférez variables d’environnement ou `application-local.properties` ignoré par Git.

## MySQL

1. Script neuf : `src/main/resources/schema-mysql.sql` — table `users` avec **`password_encrypted`**, table **`auth_nonce`**, `failed_login_attempts`, `lock_until`.
2. Depuis TP2 (`password_hash`) : voir `schema-mysql-migration-tp2-to-tp3.sql` (souvent plus simple de recréer la base en dev : impossible de dériver le chiffrement SMK depuis un hash BCrypt sans resaisie du mot de passe).
3. Depuis TP1 : `schema-mysql-migration-tp1-to-tp2.sql` (historique).
4. Variable d’environnement **`APP_MASTER_KEY`** (TP4, obligatoire au démarrage) + `application.properties` : URL MySQL (ex. port **3307**), utilisateur, mot de passe.

## Compte de démo

Créé au démarrage si absent :

| Champ | Valeur |
|-------|--------|
| Email | `toto@example.com` |
| Mot de passe | **`Pwd1234!abcd`** (conforme TP2 : 12+ car., maj, min, chiffre, spécial) |

*(En TP1 l’énoncé utilisait `pwd1234` en clair ; les TP2/3 imposent une politique forte pour ce compte aussi.)*

## Configuration (`application.properties`)

- **`APP_MASTER_KEY`** (env) — clé maître pour chiffrer les mots de passe (TP4, AES-GCM ; pas de valeur par défaut dans le code).
- `app.auth.timestamp-skew-seconds` / `app.auth.nonce-ttl-seconds` — fenêtre horaire et TTL nonce (TP3).
- `app.auth.lock-duration` — durée du blocage après trop d’échecs (défaut `2m`).
- `app.auth.max-failed-attempts` — nombre d’échecs avant blocage (défaut `5`).

## Endpoints

| Méthode | Chemin | Remarques |
|---------|--------|-----------|
| POST | `/api/auth/register` | JSON `email`, `password`, `passwordConfirm` (identiques) |
| POST | `/api/auth/login` | JSON `email`, `nonce`, `timestamp` (epoch s), `hmac` (hex) → réponse avec `token` |
| GET | `/api/me` | `Authorization: Bearer <token>` ou `X-Auth-Token` |

Erreurs JSON : `timestamp`, `status`, `error`, `message`, `path`.

- **400** — validation / politique mot de passe / confirmation différente  
- **401** — identifiants incorrects (**même message** pour email inconnu ou mauvais mot de passe)  
- **423** — compte **verrouillé** après échecs (WebDAV *Locked* ; on pourrait aussi justifier **429**)  
- **409** — email déjà utilisé  

## Postman (exemple register)

```json
{
  "email": "moi@example.com",
  "password": "Aa1!monMotDupasse",
  "passwordConfirm": "Aa1!monMotDupasse"
}
```

Login toto : mot de passe `Pwd1234!abcd`, puis `GET /api/me` avec le `Bearer` reçu.

## Qualité (TP2)

- **Tests** : `mvn test` (profil `test`, H2).  
- **Couverture (JaCoCo)** : `mvn verify` → rapport `target/site/jacoco/index.html` (objectif réaliste ≥ 60 %).  
- **SonarCloud** : connecter le dépôt, corriger bugs / vulnérabilités majeurs, viser le Quality Gate (voir énoncé TP2).

## Journal

`logs/authentification.log` — **jamais** de mot de passe ni de jeton dans les logs.

## Analyse de sécurité (évolution)

TP2 corrige le stockage (**hash** + politique + **lockout**), mais le jeton reste réutilisable s’il est volé, il n’y a pas encore le protocole anti-rejeu du **TP3**. Rédigez les 5+ risques restants pour le compte rendu.

## TP4 — Master Key et CI

- **Guide détaillé (workflow, schémas)** : [`../Doc/Guide/GUIDE_TP4.md`](../Doc/Guide/GUIDE_TP4.md).
- **Démarrage** : définir `APP_MASTER_KEY` dans l’environnement (obligatoire, pas de valeur par défaut dans le code).
- **CI** : workflow [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) — `mvn verify` + SonarCloud ; clé factice `APP_MASTER_KEY=test_master_key_for_ci_only` ; secret **`SONAR_TOKEN`** sur GitHub.
- **SonarCloud** : organisation / `projectKey` dans le `pom.xml` racine ; ne pas commiter de jetons.

Guide détaillé : `../Doc/Guide/GUIDE_TP2.md`.

## Client JavaFX (`../authentification_front`)

Interface de bureau (connexion, inscription avec indicateur de force rouge/orange/vert, profil).  
Démarrer le **backend** avant le client ; URL par défaut `http://localhost:8080`. Voir le `README.md` du module front.
