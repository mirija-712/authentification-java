# Documentation du parcours authentification

Ce dossier contient des **guides pédagogiques en français** pour recoder et comprendre les projets **backend** (`authentification_back`) et **client JavaFX** (`authentification_front`).

| Fichier | Contenu |
|---------|---------|
| **[GUIDE_TP1.md](./GUIDE_TP1.md)** | Concepts du TP1 (API REST, couches Spring, session vs jeton, modèle **mot de passe en clair**, exceptions HTTP, flux). Extraits « état TP1 » à titre historique. |
| **[GUIDE_TP2.md](./GUIDE_TP2.md)** | Guide technique principal : backend TP2, **couplage avec le client JavaFX**, CORS, code commenté + annexe sources. |
| **[GUIDE_TP3.md](./GUIDE_TP3.md)** | TP3 : **nonce + timestamp**, **HMAC-SHA256** sur `email:nonce:timestamp`, login sans mot de passe sur le POST `/login`. |
| **[GUIDE_TP4.md](./GUIDE_TP4.md)** | TP4 : **APP_MASTER_KEY**, format `v1:...`, workflow inscription/login, **CI GitHub Actions** + SonarCloud (schémas et liens vers le code). |

| Module | README |
|--------|--------|
| Backend | [`../../authentification_back/README.md`](../../authentification_back/README.md) |
| Front JavaFX | [`../../authentification_front/README.md`](../../authentification_front/README.md) |

Commencez par **GUIDE_TP1** pour le contexte, puis **GUIDE_TP2** pour le détail du code et du client. Enchaînez **TP3** (HMAC) puis **TP4** (Master Key + pipeline CI).
