# Documentation du parcours authentification

Ce dossier contient des **guides pédagogiques en français** pour le projet **backend** (`authentification_back`) et le **client JavaFX** (`authentification_front`).

## Point d’entrée recommandé

| Fichier | Contenu |
|---------|---------|
| **[GUIDE_PROJET_COMPLET.md](./GUIDE_PROJET_COMPLET.md)** | **Référence principale** : structure réelle du dépôt (sans module Maven `common`), workflows (Mermaid), commandes, CI, et **code complet** des classes critiques (`AuthService`, chiffrement, HMAC, contrôleur, client HTTP, etc.). |

## Guides par travail pratique

| Fichier | Contenu |
|---------|---------|
| **[GUIDE_TP1.md](./GUIDE_TP1.md)** | TP1 : contexte API REST, jeton, erreurs JSON, limites volontaires. |
| **[GUIDE_TP2.md](./GUIDE_TP2.md)** | TP2 : politique mot de passe, verrouillage, **beaucoup de code historique BCrypt** ; le login actuel du dépôt est **HMAC (TP3)** — croiser avec le guide projet. |
| **[GUIDE_TP3.md](./GUIDE_TP3.md)** | TP3 : nonce, timestamp, **HMAC-SHA256**, anti-rejeu `auth_nonce`, alignement client/serveur. |
| **[GUIDE_TP4.md](./GUIDE_TP4.md)** | TP4 : **`APP_MASTER_KEY`**, AES-GCM `v1:…`, tests, **GitHub Actions + SonarCloud**. |
| **[GUIDE_TP5_DETAILLE.md](./GUIDE_TP5_DETAILLE.md)** | **TP5 implémenté** : changement de mot de passe, code commenté, **Postman**, **Docker Desktop** pas à pas, CI, dépannage. |

## README des modules

| Module | README |
|--------|--------|
| Backend | [`../../authentification_back/README.md`](../../authentification_back/README.md) |
| Front JavaFX | [`../../authentification_front/README.md`](../../authentification_front/README.md) |

## Ordre de lecture conseillé

1. **GUIDE_PROJET_COMPLET.md** — vue d’ensemble et code actuel.
2. **GUIDE_TP1** → **TP2** → **TP3** → **TP4** — progression pédagogique du sujet.
