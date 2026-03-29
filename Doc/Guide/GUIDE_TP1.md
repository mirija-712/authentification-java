# Guide détaillé — TP1 (fondations et contexte)

Ce document explique **le premier travail pratique** du module *Serveur d’authentification* : objectifs, architecture, données, HTTP et ce que signifie une API « **volontairement dangereuse** ».

**Table des matières :** sections **1 à 11** ci‑dessous (objectifs, architecture, SQL, HTTP, flux, DTO, tests, évolution TP2/TP3, synthèse).

> **Note sur le dépôt :** le code dans `authentification_back` a ensuite évolué vers le **TP2** (BCrypt, politique stricte, verrouillage). Les extraits ci‑dessous marqués **« référence TP1 »** décrivent ce que le sujet demandait au **TP1** ; le détail du **code actuel** (TP1+TP2) est dans [GUIDE_TP2.md](./GUIDE_TP2.md).

---

## 1. Objectifs du TP1 (rappel sujet)

- Mettre en place une **API REST** avec **Spring Boot** et **MySQL**.
- Accepter une authentification **fonctionnelle mais incorrecte pour la production** (mot de passe **en clair** en base, règles de mot de passe **faibles** : minimum **4 caractères**).
- Exposer :
  - **Inscription** `POST /api/auth/register`
  - **Connexion** `POST /api/auth/login`
  - **Profil** `GET /api/me` (réservé aux utilisateurs authentifiés)
- Gérer les erreurs avec des **exceptions métier** et un **`@ControllerAdvice`** qui renvoie toujours le **même format JSON** (`timestamp`, `status`, `error`, `message`, `path`).
- Écrire des **tests JUnit**, du **JavaDoc**, un **README** avec analyse de sécurité (au moins 5 risques).
- Option d’auth : **session HTTP** **ou** **jeton stocké en base** (dans ce projet : **jeton** en colonne `token`).

---

## 2. Architecture logique (couches)

```
Client (Postman, futur client Java)
        │  JSON (HTTP)
        ▼
┌───────────────────┐
│   Controller      │  ← DTO d’entrée (@Valid), codes HTTP (201, 200, …)
└─────────┬─────────┘
          │
┌─────────▼─────────┐
│   Service         │  ← Règles métier, transactions
└─────────┬─────────┘
          │
┌─────────▼─────────┐
│   Repository      │  ← Spring Data JPA (CRUD, requêtes dérivées)
└─────────┬─────────┘
          │
┌─────────▼─────────┐
│   Base MySQL      │  ← Table users
└───────────────────┘
```

- **`@RestController`** : transforme automatiquement les objets Java en **JSON** (via Jackson).
- **`@Service`** : contient la logique ; **`@Transactional`** garantit une transaction base par méthode quand c’est nécessaire.
- **`JpaRepository`** : évite d’écrire du SQL pour les opérations simples (`save`, `findById`, méthodes `findBy…` dérivées du nom).

---

## 3. Modèle de données « référence TP1 »

Le sujet imposait une table minimale du type :

| Colonne          | Rôle |
|------------------|------|
| `id`             | Clé primaire |
| `email`          | Unique |
| `password_clear` | Mot de passe **en clair** (interdit en prod) |
| `created_at`     | Date de création |

**Exemple SQL (TP1) :**

```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_clear VARCHAR(255) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL
);
```

En **variante jeton**, on ajoute une colonne **`token`** (nullable, unique quand non null) pour stocker le jeton émis au login.

---

## 4. Endpoints et codes HTTP (TP1)

| Situation | Code | Exception typique |
|-----------|------|-------------------|
| Données invalides (email, mot de passe trop court, etc.) | **400** | `InvalidInputException` ou erreurs `@Valid` |
| Login refusé (mauvais mot de passe, etc.) | **401** | `AuthenticationFailedException` |
| Email déjà pris | **409** | `ResourceConflictException` |
| Succès inscription | **201** | — |
| Succès login / profil | **200** | — |

Le **`GlobalExceptionHandler`** centralise la traduction **exception → JSON** pour que le client ne reçoive pas des pages d’erreur HTML opaques.

---

## 5. Flux « inscription » (logique TP1)

1. Le client envoie `email` + `password` (et en TP2 ensuite `passwordConfirm`).
2. Le **contrôleur** valide le format (`@Valid`).
3. Le **service** vérifie les règles (unicité email, longueur minimale, etc.).
4. En TP1 on **persistait** `password_clear` tel quel.
5. Réponse : identité du user **sans** exposer le mot de passe.

---

## 6. Flux « connexion » avec jeton (logique)

1. Vérifier email + mot de passe (en TP1 : comparaison **en clair** `password_clear.equals(...)`).
2. Si OK : générer un **UUID** (ou équivalent), le **sauver** dans `users.token`, le **renvoyer** dans le JSON de réponse.
3. Le client envoie ensuite `Authorization: Bearer <token>` (ou `X-Auth-Token`) pour **`GET /api/me`**.
4. Le service cherche l’utilisateur par **`findByToken`** ; si absent → **401**.

---

## 7. Extrait « service d’auth » simplifié (référence TP1, mot de passe en clair)

> *Illustration pédagogique — ne pas reproduire en production.*

```java
// Pseudo-code TP1 : comparaison en clair (à ne plus faire après TP2)
if (!user.getPasswordClear().equals(request.password())) {
    throw new AuthenticationFailedException("Mot de passe incorrect");
}
```

Au **TP2**, cette comparaison est remplacée par **`passwordEncoder.matches(motSaisi, user.getPasswordHash())`**.

---

## 8. DTO, validation et sérialisation

- **`record`** (Java 16+) : classes immuables adaptées aux JSON d’entrée/sortie.
- **`jakarta.validation`** : `@NotBlank`, `@Email`, `@Valid` sur le contrôleur pour rejeter tôt les entrées invalides (**400**).
- **`@JsonInclude(NON_NULL)`** sur la réponse : évite d’envoyer `"token": null` quand on ne veut pas exposer le champ.

---

## 9. Tests (idées TP1)

- Email invalide, mot de passe trop court, inscription OK, doublon email, login OK/KO, `/api/me` sans jeton, `/api/me` avec jeton après login.

En **TP2**, on ajoute des tests sur la **politique de mot de passe**, le **verrouillage**, le **message générique** de login, etc.

---

## 10. Pour aller plus loin : TP2 et TP3

- **TP2** : BCrypt, politique forte, anti brute-force, SonarCloud, couverture. Voir **[GUIDE_TP2.md](./GUIDE_TP2.md)** pour le **code source commenté intégral** du projet actuel.
- **TP3** : protocole HMAC / nonce (pas de mot de passe qui transite comme « preuve » simple).

---

## 11. Synthèse

| TP1 | Limite volontaire |
|-----|-------------------|
| Mot de passe en clair ou faible | Fuite base = comptes compromis |
| Jeton sans expiration | Vol de jeton = session piratée |
| Pas de rate limiting (TP1) | Brute-force sur le login (partiellement corrigé au TP2) |

Ce guide pose le **vocabulaire** et les **flux**.

Pour le **code source complet** du projet actuel (TP2) avec l’**annexe A** (copie intégrale des fichiers principaux + tests), ouvrez **[GUIDE_TP2.md](./GUIDE_TP2.md)** — c’est le document le plus détaillé pour tout recoder depuis zéro. Le même guide décrit le **client JavaFX** `authentification_front` (couplage HTTP avec le backend).
