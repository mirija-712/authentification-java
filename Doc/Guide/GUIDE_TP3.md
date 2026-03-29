# TP3 — Challenge / nonce + preuve HMAC

## Objectif

Ne plus envoyer le **mot de passe en clair** sur `POST /api/auth/login` : le client obtient un **nonce** (`POST /api/auth/challenge`), calcule une preuve `HMAC-SHA256(empreinte, nonce)` et envoie seulement `email`, `nonce`, `proof`.

L’**empreinte** (SHA-256 hex de `email|password|authSalt`) est enregistrée à l’inscription avec le sel public `authSalt`.

## API

| Méthode | Chemin | Corps | Réponse |
|--------|--------|-------|---------|
| POST | `/api/auth/challenge` | `{"email":"..."}` | `nonce`, `expiresAt`, `authSalt` |
| POST | `/api/auth/login` | `{"email","nonce","proof"}` **sans** `password` | 200 + `token` |
| POST | `/api/auth/login` | `{"email","password"}` | 200 + `token` (TP2 / comptes sans empreinte) |

## Configuration

- `app.auth.challenge-ttl` — durée de validité du nonce (défaut `5m`).

## Code

- Backend : `com.example.authentification_back.security.Tp3Proof`
- Client : `com.example.authentification_front.security.Tp3Proof` (même logique)

## Migration MySQL

Voir `authentification_back/src/main/resources/schema-mysql-migration-tp2-to-tp3.sql`.

Les comptes créés avant TP3 n’ont pas `authSalt` / `identityFingerprint` : ils restent en **login mot de passe** jusqu’à réinscription ou migration manuelle.
