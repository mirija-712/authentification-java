# TP3 — SSO : `email:nonce:timestamp` + HMAC-SHA256

## Objectif

Ne plus envoyer le **mot de passe en clair** sur `POST /api/auth/login`. Le client génère un **nonce** (UUID), un **timestamp** (epoch secondes), calcule un **HMAC-SHA256** et envoie seulement `email`, `nonce`, `timestamp`, `hmac` (hex).

- **Message** : `email:nonce:timestamp` (email normalisé trim + minuscules, comme côté serveur).
- **Clé HMAC** : mot de passe en **UTF-8** (jamais envoyé sur le réseau).
- **Côté serveur** : le mot de passe stocké est **déchiffré** avec la **SMK** (chiffrement réversible), puis le serveur recalcule le HMAC et compare en **temps constant**.
- **Fenêtre temporelle** : ± la valeur `app.auth.timestamp-skew-seconds` (défaut 60 s).
- **Anti-rejeu** : chaque couple `(user_id, nonce)` n’est accepté qu’**une fois** (table `auth_nonce`).

## API

| Méthode | Chemin | Corps | Réponse |
|--------|--------|-------|---------|
| POST | `/api/auth/login` | `{"email","nonce","timestamp","hmac"}` | 200 + `token` |

Il n’y a **pas** d’endpoint séparé « challenge » : un seul aller-retour pour la connexion.

## Configuration

- `app.auth.server-master-key` — clé maître pour chiffrer les mots de passe en base (AES-256-GCM).
- `app.auth.timestamp-skew-seconds` — tolérance horloge (défaut `60`).
- `app.auth.nonce-ttl-seconds` — TTL enregistré sur la ligne nonce (défaut `120`).

## Code

- Backend : `com.example.authentification_back.security.SsoHmac`
- Client JavaFX : `com.example.authentification_front.security.SsoHmac` (même logique)

## Migration MySQL

Voir `authentification_back/src/main/resources/schema-mysql-migration-tp2-to-tp3.sql` si vous migrez depuis une ancienne version (colonnes / tables).
