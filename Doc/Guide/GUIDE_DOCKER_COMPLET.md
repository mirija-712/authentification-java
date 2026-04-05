# Guide Docker — comprendre à quoi ça sert (et comment l’utiliser)

Ce document explique **pourquoi Docker existe**, **ce que ça change pour toi**, et **comment ça s’applique concrètement** à ton projet d’authentification (TP5). L’objectif est que tu saches **à quoi ça sert vraiment**, pas seulement recopier des commandes.

---

## Table des matières

1. [Le problème que Docker résout](#1-le-problème-que-docker-résout)
2. [Image, conteneur, Dockerfile — en une phrase chacun](#2-image-conteneur-dockerfile--en-une-phrase-chacun)
3. [Conteneur vs machine virtuelle](#3-conteneur-vs-machine-virtuelle)
4. [À quoi ça sert vraiment (cas d’usage)](#4-à-quoi-ça-sert-vraiment-cas-dusage)
5. [Ton projet : que fait ton `Dockerfile` ?](#5-ton-projet--que-fait-ton-dockerfile-)
6. [Docker Desktop et le démon Docker](#6-docker-desktop-et-le-démon-docker)
7. [Commandes essentielles (Windows / PowerShell)](#7-commandes-essentielles-windows--powershell)
8. [Variables d’environnement : `APP_MASTER_KEY` et la base de données](#8-variables-denvironnement--app_master_key-et-la-base-de-données)
9. [Docker dans GitHub Actions (ta CI)](#9-docker-dans-github-actions-ta-ci)
10. [Ce que Docker ne remplace pas](#10-ce-que-docker-ne-remplace-pas)
11. [Pièges fréquents et dépannage](#11-pièges-fréquents-et-dépannage)
12. [Glossaire rapide](#12-glossaire-rapide)

---

## 1. Le problème que Docker résout

Quand tu développes une appli (par ex. Spring Boot), elle **dépend d’un environnement** :

- une **version de Java** ;
- des **bibliothèques** ;
- parfois une **base MySQL** avec un port précis ;
- des **variables d’environnement** (`APP_MASTER_KEY`, etc.).

Sur **ta** machine, tout peut marcher. Sur la machine d’un **camarade**, du **prof**, ou un **serveur de production**, ça peut casser pour une raison bête : « chez moi Java 21, chez lui Java 11 », « le port 8080 est déjà pris », « MySQL n’est pas au même endroit », etc.

**Docker** sert à dire : *« Voici une recette qui construit un **paquet exécutable standardisé** : dedans il y a le JRE qu’il faut + ton JAR + la façon de lancer l’app. »*

Quelqu’un d’autre (ou un serveur cloud) peut alors lancer **exactement le même artefact** sans réinstaller Java à la main sur sa machine pour ton projet — à condition d’avoir **Docker** installé.

**En résumé** : Docker vise la **reproductibilité** et le **déploiement** : *« ça tourne pareil partout où Docker tourne »* (avec les mêmes variables / réseau / base, voir plus bas).

---

## 2. Image, conteneur, Dockerfile — en une phrase chacun

| Concept | En une phrase |
|--------|----------------|
| **Image** | Un **modèle figé** (fichiers + métadonnées + commande de démarrage), un peu comme une « classe » ou un « CD d’installation ». |
| **Conteneur** | Une **instance en cours d’exécution** créée à partir d’une image — comme un **processus isolé** avec son propre système de fichiers minimal et ses ports mappés. |
| **Dockerfile** | Le **fichier texte** qui décrit **comment construire** l’image (étapes `FROM`, `COPY`, `RUN`, etc.). |
| **Docker Engine (démon)** | Le **service** qui construit les images et lance les conteneurs (souvent via **Docker Desktop** sur Windows). |
| **Registre (ex. Docker Hub)** | Un **endroit** où on peut **pousser / tirer** des images (comme un GitHub pour des images). *Ton TP5 ne l’oblige pas.* |

Tu **build** une **image** avec `docker build`. Tu **run** un **conteneur** avec `docker run`. Le **Dockerfile** est la recette.

---

## 3. Conteneur vs machine virtuelle

- **VM** : un **système d’exploitation complet** (lourd) qui émule une machine entière.
- **Conteneur** : partage le **noyau** de l’OS hôte (Linux sur les runners CI ; sur Windows, Docker Desktop utilise souvent une VM Linux légère ou WSL2 en coulisse). Ton appli voit un **mini environnement Linux** avec ses propres fichiers et processus, **sans** réinstaller Windows dedans.

**Intérêt** : démarrage plus rapide, images plus légères qu’une VM complète, idéal pour packager **une appli** (un microservice, un JAR, etc.).

---

## 4. À quoi ça sert vraiment (cas d’usage)

### 4.1 Pour ton cours (TP5)

L’énoncé te demande de **conteneuriser** l’app pour montrer que tu sais :

- produire un **artefact déployable** ;
- documenter le déploiement via un **Dockerfile** ;
- intégrer un **`docker build`** dans la **CI** pour prouver que l’image se construit à chaque push (pas seulement « ça marche sur mon PC »).

### 4.2 En entreprise / prod (vision réelle)

- **Même binaire, même runtime** entre préprod et prod.
- **Montée en charge** : on peut lancer **plusieurs conteneurs** derrière un load balancer (souvent avec Kubernetes, Docker Swarm, etc. — hors scope du TP).
- **CI/CD** : la pipeline build l’image, la tague, la pousse vers un registre, un orchestrateur la déploie.

### 4.3 Pour toi aujourd’hui

Même si « ça marche » avec `mvn spring-boot:run`, Docker répond à la question : **« Si je donne ce dépôt à quelqu’un, peut-il lancer l’API de la même façon sans tout reconfigurer ? »** L’image est une **preuve** que tu as une chaîne de build jusqu’au JAR + exécution dans un environnement contrôlé.

---

## 5. Ton projet : que fait ton `Dockerfile` ?

Ton fichier à la **racine** du dépôt fait un **build multi-étapes** (*multi-stage*) :

**Étape 1 — `builder` (image Maven + JDK 21)**  
- Copie le `pom.xml` parent, les modules `authentification_back` et `authentification_front`.  
- Lance Maven pour **compiler et packager** uniquement le backend (`-pl authentification_back -am`).  
- Résultat : un **JAR** dans `authentification_back/target/`.

**Étape 2 — image finale (JRE 17)**  
- Part d’une image **légère** : seulement le **Java Runtime** nécessaire pour **exécuter** le JAR (pas Maven, pas les sources).  
- Copie **uniquement** le JAR dans `/app/app.jar`.  
- `EXPOSE 8080` : **documentation** (ça ne ouvre pas magiquement le port sur ta machine ; le **mapping** se fait au `docker run -p`).  
- `ENTRYPOINT` : commande par défaut au démarrage du conteneur — lancer `java -jar /app/app.jar`.

**Pourquoi deux étapes ?**  
- L’image finale reste **petite** et **sans outils de build**.  
- La construction du JAR est **reproductible** : n’importe qui avec Docker peut rebuild sans installer Maven localement (Maven est *dans* l’étape builder).

**Note** : le **front JavaFX** est copié dans le contexte de build parce que le POM parent / réacteur le référence ; l’**image Docker ne lance que le backend** (le client lourd se lance à part avec `mvn javafx:run`).

---

## 6. Docker Desktop et le démon Docker

Sur **Windows**, tu utilises en général **Docker Desktop** :

- Il démarre le **démon Docker** (le moteur).
- Il fournit la ligne de commande `docker`.
- Sans lui (ou sans le démon démarré), `docker build` / `docker run` échouent avec des erreurs du type *« cannot connect to Docker daemon »*.

**Vérification** :

```powershell
docker version
```

Si « Client » et « Server » répondent, tu es bon.

---

## 7. Commandes essentielles (Windows / PowerShell)

Toutes les commandes ci-dessous supposent que tu es à la **racine du dépôt** (là où se trouvent `Dockerfile` et `pom.xml`).

### 7.1 Construire l’image

```powershell
cd "D:\tp\spring boot\authentification"
docker build -t cdwfs-auth-app .
```

- **`-t cdwfs-auth-app`** : nom (tag) de l’image.  
- **`.`** : **contexte** = fichiers envoyés au démon (souvent filtrés par `.dockerignore`).

La première fois peut être long (téléchargement des images de base Maven et JRE).

### 7.2 Lancer un conteneur

```powershell
docker run --rm -p 8080:8080 -e APP_MASTER_KEY=test_master_key cdwfs-auth-app
```

| Option | Sens |
|--------|------|
| `--rm` | Supprime le conteneur à l’arrêt (pratique en dev). |
| `-p 8080:8080` | Port **hôte** 8080 → port **conteneur** 8080 (là où Spring Boot écoute). |
| `-e APP_MASTER_KEY=...` | Variable d’environnement **obligatoire** pour ton app (TP4) : sans elle, l’appli peut refuser de démarrer. |

Puis ouvre `http://localhost:8080` (les API sont sous `/api/...`).

### 7.3 Arrêter

**Ctrl+C** dans le terminal où tourne `docker run` (si attaché). Avec `--rm`, le conteneur disparaît.

### 7.4 Voir les images et conteneurs

```powershell
docker images
docker ps
```

`docker ps -a` liste aussi les conteneurs arrêtés.

### 7.5 Logs (si le conteneur tourne en arrière-plan)

```powershell
docker logs <container_id>
```

---

## 8. Variables d’environnement : `APP_MASTER_KEY` et la base de données

### 8.1 `APP_MASTER_KEY`

Ton serveur **exige** une clé maître pour le chiffrement des mots de passe. En Docker, tu ne mets **pas** ça dans le Dockerfile en dur (secret dans l’image = mauvaise pratique). Tu passes la valeur au **`docker run`** avec `-e` (ou via un fichier d’env / secrets en prod).

### 8.2 MySQL « sur la machine Windows » alors que l’app est dans le conteneur

Le conteneur a **son propre réseau**. `localhost` **à l’intérieur** du conteneur, ce n’est **pas** ton Windows.

- Si MySQL tourne sur **Windows**, souvent on utilise **`host.docker.internal`** comme hôte JDBC (Docker Desktop sur Windows le fournit).  
- Il faut aussi passer `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` si la config par défaut ne pointe pas vers une base joignable depuis le conteneur.

**Exemple d’esprit** (à adapter à ton port / user / mot de passe) :

```powershell
docker run --rm -p 8080:8080 `
  -e APP_MASTER_KEY=test_master_key `
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3307/authentification?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC `
  -e SPRING_DATASOURCE_USERNAME=root `
  -e SPRING_DATASOURCE_PASSWORD=ton_mot_de_passe `
  cdwfs-auth-app
```

Sans base accessible, Spring Boot peut **échouer au démarrage** même si l’image est correcte.

---

## 9. Docker dans GitHub Actions (ta CI)

Dans `.github/workflows/ci.yml`, après `mvn verify` et SonarCloud, il y a une étape :

```yaml
- name: Build Docker image
  run: docker build -t cdwfs-auth-app:ci .
```

**À quoi ça sert ?**

- Vérifier que le **Dockerfile est valide** et que le build d’image **réussit** sur une machine « neuve » (runner Ubuntu).
- Si un développeur casse le Dockerfile ou le chemin du JAR, la **PR devient rouge** : on attrape l’erreur tôt.

**Ce que la CI ne fait pas forcément** : pousser l’image vers un registre ni la déployer en production — ici c’est surtout une **validation de build** (très courant dans les TP).

---

## 10. Ce que Docker ne remplace pas

- **Docker ne remplace pas Git** : le code vit toujours dans le dépôt ; l’image est un **produit dérivé** du build.  
- **Docker ne magique pas la base de données** : si ton app a besoin de MySQL, il faut **MySQL qui tourne** et **joignable** depuis le conteneur (ou un second conteneur MySQL + réseau Docker — sujet avancé).  
- **Docker ne supprime pas la sécurité applicative** : mauvaise config, secrets en clair, ports exposés — tout ça reste ta responsabilité.  
- **Le client JavaFX** : ce n’est généralement **pas** dans l’image ; l’image TP5 cible le **serveur** Spring Boot.

---

## 11. Pièges fréquents et dépannage

| Symptôme | Piste |
|----------|--------|
| `error during connect` / daemon | Docker Desktop pas lancé. |
| `APP_MASTER_KEY obligatoire` au boot | Oublier `-e APP_MASTER_KEY=...` au `docker run`. |
| App ne joint pas MySQL | Mauvaise URL ; essayer `host.docker.internal` depuis le conteneur vers l’hôte Windows. |
| Port 8080 déjà utilisé | Arrêter l’autre processus ou `docker run -p 8081:8080` et utiliser `http://localhost:8081`. |
| Build très lent la 1ère fois | Normal : téléchargement des couches d’images de base. |
| « Ça marche en local mais pas en Docker » | Souvent **variables d’environnement** ou **URL de base de données** différentes. |

---

## 12. Glossaire rapide

| Terme | Définition courte |
|-------|-------------------|
| **Image** | Modèle immuable pour créer des conteneurs. |
| **Conteneur** | Processus / environnement isolé lancé depuis une image. |
| **Dockerfile** | Recette de construction d’image. |
| **Contexte de build** | Dossier (souvent `.`) envoyé au démon ; `.dockerignore` limite ce qui part. |
| **Layer** | Couche d’image réutilisable (cache Docker). |
| **Multi-stage build** | Plusieurs `FROM` : une phase pour compiler, une autre pour livrer léger. |
| **Tag** | Nom de version d’une image (`cdwfs-auth-app`, `cdwfs-auth-app:ci`). |
| **Port mapping** | `-p hôte:conteneur` pour rendre un port du conteneur accessible sur ta machine. |

---

## En une phrase

**Docker sert à empaqueter ton application (ici le JAR Spring Boot + JRE) dans un format standard, reproductible, prêt à être lancé n’importe où Docker tourne — et ta CI lance `docker build` pour prouver que cette recette reste valide.**

Pour la marche à suivre déjà alignée sur ton TP5 (Postman, erreurs fréquentes, CI), tu peux aussi lire **[GUIDE_TP5.md](./GUIDE_TP5.md)**.
