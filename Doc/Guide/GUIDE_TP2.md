# Guide complet — TP2 : code source et explications

Ce document reprend **presque tout le code** du module `authentification_back` tel qu’après le **TP2**, avec une **explication du rôle** de chaque partie. À utiliser comme support pour **recoder** ou **réviser** l’examen.

**Table des matières**

1. [Maven (`pom.xml`)](#1-maven-pomxml)
2. [Configuration Spring](#2-configuration-spring)
3. [Schéma SQL MySQL](#3-schéma-sql-mysql)
4. [Point d’entrée application](#4-point-dentrée-application)
5. [Entité `User`](#5-entité-user)
6. [Repository `UserRepository`](#6-repository-userrepository)
7. [DTO : requêtes et réponses](#7-dto--requêtes-et-réponses)
8. [Exceptions métier](#8-exceptions-métier)
9. [`GlobalExceptionHandler`](#9-globalexceptionhandler)
10. [`PasswordPolicyValidator`](#10-passwordpolicyvalidator)
11. [Configuration crypto et propriétés](#11-configuration-crypto-et-propriétés)
12. [`TestAccountInitializer`](#12-testaccountinitializer)
13. [`AuthService`](#13-authservice)
14. [`AuthController`](#14-authcontroller)
15. [Tests](#15-tests)
16. [Client JavaFX (`authentification_front`)](#16-client-javafx-authentification_front)
17. [Synthèse](#17-synthèse)
18. [Annexe A — code source backend](#annexe-a--code-source-intégral-copie-du-dépôt)

---

## 1. Maven (`pom.xml`)

**Rôle :** déclare les bibliothèques (Spring Boot, JPA, MySQL, H2, **validation**, **spring-security-crypto** pour BCrypt, **JaCoCo** pour la couverture, tests).

**Extraits importants :**

- **`spring-boot-starter-data-jpa`** : Hibernate + Spring Data.
- **`spring-boot-starter-web`** : Tomcat embarqué, Jackson (JSON), `RestController`.
- **`spring-boot-starter-validation`** : `@Valid`, `@NotBlank`, etc.
- **`spring-security-crypto`** : `BCryptPasswordEncoder` **sans** activer tout Spring Security (pas de filtre HTTP imposé).
- **`mysql-connector-j`** (runtime) : driver MySQL en prod/dev.
- **`h2`** (runtime) : base en mémoire pour les tests.
- **`jacoco-maven-plugin`** : rapport de couverture après `mvn verify` (`target/site/jacoco/index.html`).

---

## 2. Configuration Spring

### 2.1 `application.properties` (profil par défaut)

```properties
spring.application.name=authentification_back

# TP2 — anti brute-force (durée après N échecs ; 2 m conformes à l'énoncé)
app.auth.lock-duration=2m
app.auth.max-failed-attempts=5

# MySQL (créer la base authentification avec schema-mysql.sql ou laisser ddl-auto)
spring.datasource.url=jdbc:mysql://localhost:3307/authentification?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=... # à adapter (ne pas commiter en clair sur un dépôt public)

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.open-in-view=false

logging.file.name=logs/authentification.log
logging.level.com.example.authentification_back=INFO

server.port=8080
```

**Explications :**

- **`app.auth.*`** : paramètres injectés dans `AuthSecurityProperties` (durée de blocage, nombre d’échecs max).
- **`ddl-auto=update`** : Hibernate met à jour le schéma selon les entités (pratique en dev ; en prod on préfère Flyway/Liquibase).
- **`open-in-view=false`** : désactive le pattern OSIV (évite les requêtes lazy dans la vue ; ici API REST pure).

### 2.2 `application-test.properties` (profil `test`)

```properties
spring.datasource.url=jdbc:h2:mem:tp1test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect

app.auth.lock-duration=150ms
app.auth.max-failed-attempts=5
```

**Explications :**

- **H2 en mémoire** : les tests CI n’ont pas besoin de MySQL.
- **`MODE=MySQL`** : meilleure compatibilité avec le dialecte / types.
- **`lock-duration=150ms`** : permet de tester le **déverrouillage** sans attendre 2 minutes (le test fait un `Thread.sleep(200)`).

---

## 3. Schéma SQL MySQL

**Fichier :** `src/main/resources/schema-mysql.sql`

```sql
CREATE DATABASE IF NOT EXISTS authentification
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE authentification;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(80) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  token VARCHAR(64) DEFAULT NULL,
  failed_login_attempts INT NOT NULL DEFAULT 0,
  lock_until TIMESTAMP(6) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_token (token)
) ENGINE=InnoDB;
```

**Explications :**

- **`password_hash`** : chaîne BCrypt (en général ~60 caractères ; 80 pour marge).
- **`token`** : UUID au login ; **unique** si non `NULL` (plusieurs lignes peuvent avoir `token` NULL avant premier login).
- **`failed_login_attempts` / `lock_until`** : mécanisme **anti brute-force** du TP2.

---

## 4. Point d’entrée application

**Fichier :** `AuthentificationBackApplication.java`

```java
package com.example.authentification_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthentificationBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthentificationBackApplication.class, args);
	}
}
```

**Explications :**

- **`@SpringBootApplication`** regroupe `@Configuration`, `@EnableAutoConfiguration` et `@ComponentScan` sur ce package et les sous-packages.
- **`SpringApplication.run`** démarre le contexte Spring, Tomcat, JPA, etc.

---

## 5. Entité `User`

**Fichier :** `entity/User.java`

```java
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 80)
	private String passwordHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(unique = true, length = 64)
	private String token;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts = 0;

	@Column(name = "lock_until")
	private Instant lockUntil;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
	// getters / setters ...
}
```

**Explications :**

- **`@Entity`** : cette classe est mappée sur une table JPA (par défaut `users`).
- **`@GeneratedValue(IDENTITY)`** : aligné sur `AUTO_INCREMENT` MySQL.
- **`passwordHash`** : **jamais** le mot de passe en clair ; seulement le **hash BCrypt**.
- **`@PrePersist`** : avant le premier `INSERT`, si `createdAt` est null, on la remplit (évite une erreur NOT NULL).
- **`failedLoginAttempts` / `lockUntil`** : état du verrouillage après échecs de login.

---

## 6. Repository `UserRepository`

**Fichier :** `repository/UserRepository.java`

```java
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	Optional<User> findByToken(String token);

	boolean existsByEmail(String email);
}
```

**Explications :**

- **`JpaRepository<User, Long>`** : CRUD + pagination ; clé primaire de type `Long`.
- **`findByEmail` / `findByToken`** : Spring Data génère le **JPQL** à partir du nom de méthode.
- **`existsByEmail`** : optimisé pour tester l’unicité à l’inscription.

---

## 7. DTO : requêtes et réponses

### 7.1 `RegisterRequest`

```java
public record RegisterRequest(
		@NotBlank(message = "L'email est obligatoire")
		@Email(message = "Format d'email invalide")
		String email,
		@NotBlank(message = "Le mot de passe est obligatoire")
		String password,
		@NotBlank(message = "La confirmation du mot de passe est obligatoire")
		String passwordConfirm
) {}
```

**Explications :**

- **`record`** : immuable, accesseurs `email()`, `password()`, etc.
- **Validation Bean Validation** : exécutée **avant** le service si le contrôleur utilise `@Valid`.
- La **politique forte** (12 caractères, complexité) est appliquée **dans** `PasswordPolicyValidator` (service), pas seulement par `@Size`, pour coller au TP2.

### 7.2 `LoginRequest`

```java
public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank String password
) {}
```

**Explications :**

- **Pas de longueur minimale** sur le mot de passe : un mot de passe court incorrect doit pouvoir retourner le **même message** qu’un email inconnu (recommandation TP2).

### 7.3 `UserResponse`

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(Long id, String email, Instant createdAt, String token) {

	public static UserResponse profile(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt(), null);
	}

	public static UserResponse login(User user, String token) {
		return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt(), token);
	}
}
```

**Explications :**

- **`@JsonInclude(NON_NULL)`** : si `token` est `null`, Jackson **n’écrit pas** la propriété `token` dans le JSON (évite de fuite d’info inutile sur `/api/me` et inscription).
- **`profile`** : réponse sans jeton.
- **`login`** : inclut le jeton à renvoyer au client.

### 7.4 `ApiErrorResponse`

```java
public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path
) {}
```

**Explications :** format unique des erreurs pour Postman / client (aligné sur l’énoncé TP1).

---

## 8. Exceptions métier

| Classe | HTTP | Usage |
|--------|------|--------|
| `InvalidInputException` | 400 | Politique mot de passe, confirmation différente, etc. |
| `AuthenticationFailedException` | 401 | Login refusé, jeton absent/invalide pour `/api/me` |
| `ResourceConflictException` | 409 | Email déjà utilisé |
| `AccountLockedException` | **423** | Compte verrouillé après trop d’échecs |

Toutes étendent **`RuntimeException`** pour ne pas forcer des `throws` partout dans le service.

---

## 9. `GlobalExceptionHandler`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidInputException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalid(InvalidInputException ex, HttpServletRequest req) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
	}

	@ExceptionHandler(AccountLockedException.class)
	public ResponseEntity<ApiErrorResponse> handleLocked(AccountLockedException ex, HttpServletRequest req) {
		return build(HttpStatus.LOCKED, ex.getMessage(), req);
	}

	@ExceptionHandler(AuthenticationFailedException.class)
	public ResponseEntity<ApiErrorResponse> handleAuth(AuthenticationFailedException ex, HttpServletRequest req) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
	}

	@ExceptionHandler(ResourceConflictException.class)
	public ResponseEntity<ApiErrorResponse> handleConflict(ResourceConflictException ex, HttpServletRequest req) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), req);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return build(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Données invalides" : message, req);
	}

	private static ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
		ApiErrorResponse body = new ApiErrorResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				req.getRequestURI()
		);
		return ResponseEntity.status(status).body(body);
	}
}
```

**Explications :**

- **`@RestControllerAdvice`** : intercepte les exceptions **levées par les contrôleurs** (et souvent les services appelés depuis eux).
- **`MethodArgumentNotValidException`** : levée quand `@Valid` échoue sur un DTO (email vide, etc.).
- **`423 LOCKED`** : statut WebDAV « ressource verrouillée » ; l’énoncé autorise aussi **429** (trop de requêtes) avec justification.

---

## 10. `PasswordPolicyValidator`

```java
@Component
public class PasswordPolicyValidator {

	public static final int MIN_LENGTH = 12;

	public void assertCompliant(String password) {
		if (password == null || password.length() < MIN_LENGTH) {
			throw new InvalidInputException(
					"Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères");
		}
		if (!password.matches(".*[A-Z].*")) {
			throw new InvalidInputException("Le mot de passe doit contenir au moins une majuscule");
		}
		if (!password.matches(".*[a-z].*")) {
			throw new InvalidInputException("Le mot de passe doit contenir au moins une minuscule");
		}
		if (!password.matches(".*\\d.*")) {
			throw new InvalidInputException("Le mot de passe doit contenir au moins un chiffre");
		}
		if (password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
			throw new InvalidInputException("Le mot de passe doit contenir au moins un caractère spécial");
		}
	}
}
```

**Explications :**

- **`@Component`** : instancié par Spring ; injecté dans `AuthService`.
- Les **`matches`** testent la présence d’au moins une majuscule, minuscule, chiffre.
- **`!Character.isLetterOrDigit(ch)`** : au moins un caractère qui n’est ni lettre ni chiffre (ponctuation, symboles, etc.).

---

## 11. Configuration crypto et propriétés

### `AuthSecurityProperties`

```java
@ConfigurationProperties(prefix = "app.auth")
public class AuthSecurityProperties {
	private Duration lockDuration = Duration.ofMinutes(2);
	private int maxFailedAttempts = 5;
	// getters / setters pour le binding Spring Boot
}
```

**Explications :** valeurs par défaut = énoncé TP2 ; surchargeables dans `application.properties`.

### `CryptoConfig`

```java
@Configuration
@EnableConfigurationProperties(AuthSecurityProperties.class)
public class CryptoConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
```

**Explications :**

- **`BCryptPasswordEncoder`** : hachage **adaptatif** (coût configurable ; défaut raisonnable).
- **`Clock`** : horloge injectable ; en test on pourrait remplacer par une horloge fixe pour des scénarios déterministes (ici les tests utilisent une durée de lock courte + `sleep`).

---

## 12. `TestAccountInitializer`

```java
@Component
public class TestAccountInitializer implements CommandLineRunner {

	public static final String TEST_EMAIL = "toto@example.com";
	public static final String TEST_PASSWORD_PLAIN = "Pwd1234!abcd";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(String... args) {
		if (userRepository.existsByEmail(TEST_EMAIL)) {
			return;
		}
		User user = new User();
		user.setEmail(TEST_EMAIL);
		user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD_PLAIN));
		userRepository.save(user);
	}
}
```

**Explications :**

- **`CommandLineRunner`** : exécuté **au démarrage** de l’application, après le contexte prêt.
- Le mot de passe **clair** n’est **jamais** stocké ; seul le **hash** est persisté.
- **`Pwd1234!abcd`** respecte la politique (12 caractères, complexité).

---

## 13. `AuthService`

**Fichier :** `service/AuthService.java` (voir le dépôt pour le code complet).

**Idées clés :**

### Inscription `register`

1. **`normalizeEmail`** : trim + minuscules (évite `User@Mail.com` vs `user@mail.com`).
2. **`passwordPolicyValidator.assertCompliant`** : politique TP2.
3. Comparaison **`password` / `passwordConfirm`** → sinon `InvalidInputException`.
4. **`existsByEmail`** → `ResourceConflictException` (409).
5. **`passwordEncoder.encode`** puis **`save`**.

### Connexion `login`

1. Si **email inconnu** → **`AuthenticationFailedException(GENERIC_LOGIN_ERROR)`** avec message **`Identifiants invalides`** (ne pas dire « email inconnu »).
2. Si **`lockUntil` > maintenant** → **`AccountLockedException`** (423).
3. Si le verrou est **expiré** (`lockUntil` dans le passé) : remise à zéro du compteur + `save`.
4. Si **`passwordEncoder.matches`** : reset échecs, nouveau **UUID** dans `token`, `save`, retour **`UserResponse.login`**.
5. Sinon : incrément `failedLoginAttempts` ; si **≥ max** → `lockUntil = now + lockDuration` ; `save` ; **toujours** `GENERIC_LOGIN_ERROR` (401).

### Profil `currentUser`

1. Jeton vide → 401 « Authentification requise » ou « Token invalide ».
2. **`findByToken`** → `UserResponse.profile` (sans `token` dans le JSON grâce à `@JsonInclude`).

**Constante utile :** `GENERIC_LOGIN_ERROR = "Identifiants invalides"` pour les tests et la cohérence métier.

---

## 14. `AuthController`

```java
@RestController
@RequestMapping("/api")
public class AuthController {

	@PostMapping("/auth/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
		UserResponse body = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@PostMapping("/auth/login")
	public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
		UserResponse body = authService.login(request);
		return ResponseEntity.ok(body);
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponse> me(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestHeader(value = "X-Auth-Token", required = false) String authToken) {
		String resolved = resolveToken(authorization, authToken);
		return ResponseEntity.ok(authService.currentUser(resolved));
	}
}
```

**Explications :**

- **`@RequestMapping("/api")`** : préfixe commun ; **chemin réel** `/api/me` (pas `/api/auth/me`).
- **`resolveToken`** : lit `Bearer <token>` ou `X-Auth-Token`.

---

## 15. Tests

### 15.1 `AuthApiIntegrationTest`

- **`@SpringBootTest`** : contexte Spring complet.
- **`@AutoConfigureMockMvc`** : client HTTP simulé.
- **`@ActiveProfiles("test")`** : H2 + lock court.
- **`@Transactional`** : rollback après chaque test pour les données **créées dans le test** (le compte `toto` vient du `CommandLineRunner` et reste disponible).

**Scénarios couverts :** email invalide, mot de passe faible, confirmation différente, inscription OK, conflit email, login OK toto, login avec message générique (mauvais mot de passe / email inconnu), `/api/me` sans jeton, `/api/me` avec Bearer, **verrouillage** après 5 échecs puis **déverrouillage** après délai.

### 15.2 `PasswordPolicyValidatorTest`

Tests **unitaires** sans Spring : instanciation directe de `PasswordPolicyValidator` ; vérifie chaque règle (longueur, majuscule, minuscule, chiffre, spécial, null).

---

## 16. Client JavaFX (`authentification_front`)

Projet **Maven** séparé (module Java **JPMS** `com.example.authentification_front`) qui parle à l’API **HTTP JSON** du backend.

### Démarrage (ordre recommandé)

1. Démarrer **MySQL** (port configuré dans `authentification_back/.../application.properties`, ex. **3307**).
2. Lancer le backend :  
   `cd authentification_back` puis `mvn spring-boot:run` (ou votre IDE).  
   L’API doit répondre sur **`http://localhost:8080`** (voir `server.port`).
3. Lancer le client :  
   `cd authentification_front` puis `mvn javafx:run` (ou `Launcher` depuis l’IDE).  
   Champ **URL de l’API** en haut de fenêtre : par défaut `http://localhost:8080` (modifiable si le port change).

### Rôle des classes principales

| Fichier | Rôle |
|---------|------|
| `Launcher` / `AuthApplication` | Point d’entrée JavaFX ; charge `auth-view.fxml`. |
| `AuthViewController` | Onglets Connexion / Inscription / Profil ; appelle `AuthApiClient`. |
| `api/AuthApiClient` | `HttpClient` Java 11+ : `POST` register/login, `GET` `/api/me` avec `Authorization: Bearer`. |
| `api/ApiResult` | `Ok` / `Err` pour gérer succès et erreurs JSON (`message`, `status`). |
| `policy/ClientPasswordPolicy` | Même règles que le serveur pour l’**indicateur rouge / orange / vert** (TP2). |

### CORS

Le backend expose `WebConfig` : en-têtes CORS sur `/api/**` pour les origines `localhost` (utile pour un futur front **navigateur**). Le client **JavaFX** utilise `HttpClient` : **pas de CORS** côté client lourd.

### Indicateur de force (TP2)

- **Rouge** : mot de passe ne respecte pas la politique (alignée sur `PasswordPolicyValidator`).
- **Orange** : conforme mais longueur au plus égale au minimum (12 caractères).
- **Vert** : conforme et longueur strictement supérieure au minimum.

La **validation définitive** reste toujours côté serveur.

---

## 17. Synthèse

| Sujet | Où c’est dans le code |
|--------|------------------------|
| BCrypt | `CryptoConfig`, `AuthService.register`, `login` |
| Politique mot de passe | `PasswordPolicyValidator` |
| Double confirmation | `RegisterRequest`, `AuthService.register` |
| Lockout | `User.failedLoginAttempts`, `lockUntil`, `AuthService.login`, `AccountLockedException` |
| Jeton `/api/me` | `User.token`, `AuthController`, `AuthService.currentUser` |
| Erreurs JSON | `GlobalExceptionHandler`, `ApiErrorResponse` |

Pour **modifier** le comportement, commencez par **`AuthService`** puis les **tests** correspondants.

---

## Annexe A — Code source intégral (copie du dépôt)

Les blocs ci‑dessous sont une **copie** des fichiers du projet pour lecture hors ligne. Si un fichier change dans Git, comparez avec l’IDE.

### A.1 `AuthentificationBackApplication.java`

```java
package com.example.authentification_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthentificationBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthentificationBackApplication.class, args);
	}
}
```

### A.2 `entity/User.java`

```java
package com.example.authentification_back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 80)
	private String passwordHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(unique = true, length = 64)
	private String token;

	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts = 0;

	@Column(name = "lock_until")
	private Instant lockUntil;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public String getEmail() { return email; }
	public void setEmail(String email) { this.email = email; }
	public String getPasswordHash() { return passwordHash; }
	public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
	public Instant getCreatedAt() { return createdAt; }
	public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
	public String getToken() { return token; }
	public void setToken(String token) { this.token = token; }
	public int getFailedLoginAttempts() { return failedLoginAttempts; }
	public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
	public Instant getLockUntil() { return lockUntil; }
	public void setLockUntil(Instant lockUntil) { this.lockUntil = lockUntil; }
}
```

### A.3 `repository/UserRepository.java`

```java
package com.example.authentification_back.repository;

import com.example.authentification_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	Optional<User> findByToken(String token);

	boolean existsByEmail(String email);
}
```

### A.4 DTO (`RegisterRequest`, `LoginRequest`, `UserResponse`, `ApiErrorResponse`)

```java
package com.example.authentification_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
		@NotBlank(message = "L'email est obligatoire")
		@Email(message = "Format d'email invalide")
		String email,
		@NotBlank(message = "Le mot de passe est obligatoire")
		String password,
		@NotBlank(message = "La confirmation du mot de passe est obligatoire")
		String passwordConfirm
) {}
```

```java
package com.example.authentification_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank String password
) {}
```

```java
package com.example.authentification_back.dto;

import com.example.authentification_back.entity.User;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(Long id, String email, Instant createdAt, String token) {

	public static UserResponse profile(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt(), null);
	}

	public static UserResponse login(User user, String token) {
		return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt(), token);
	}
}
```

```java
package com.example.authentification_back.dto;

import java.time.Instant;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path
) {}
```

### A.5 Exceptions

```java
package com.example.authentification_back.exception;

public class InvalidInputException extends RuntimeException {
	public InvalidInputException(String message) { super(message); }
}
```

```java
package com.example.authentification_back.exception;

public class AuthenticationFailedException extends RuntimeException {
	public AuthenticationFailedException(String message) { super(message); }
}
```

```java
package com.example.authentification_back.exception;

public class ResourceConflictException extends RuntimeException {
	public ResourceConflictException(String message) { super(message); }
}
```

```java
package com.example.authentification_back.exception;

public class AccountLockedException extends RuntimeException {
	public AccountLockedException(String message) { super(message); }
}
```

### A.6 `exception/GlobalExceptionHandler.java`

```java
package com.example.authentification_back.exception;

import com.example.authentification_back.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidInputException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalid(InvalidInputException ex, HttpServletRequest req) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
	}

	@ExceptionHandler(AccountLockedException.class)
	public ResponseEntity<ApiErrorResponse> handleLocked(AccountLockedException ex, HttpServletRequest req) {
		return build(HttpStatus.LOCKED, ex.getMessage(), req);
	}

	@ExceptionHandler(AuthenticationFailedException.class)
	public ResponseEntity<ApiErrorResponse> handleAuth(AuthenticationFailedException ex, HttpServletRequest req) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
	}

	@ExceptionHandler(ResourceConflictException.class)
	public ResponseEntity<ApiErrorResponse> handleConflict(ResourceConflictException ex, HttpServletRequest req) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), req);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return build(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Données invalides" : message, req);
	}

	private static ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
		ApiErrorResponse body = new ApiErrorResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				req.getRequestURI()
		);
		return ResponseEntity.status(status).body(body);
	}
}
```

### A.7 `validation/PasswordPolicyValidator.java`

```java
package com.example.authentification_back.validation;

import com.example.authentification_back.exception.InvalidInputException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyValidator {

	public static final int MIN_LENGTH = 12;

	public void assertCompliant(String password) {
		if (password == null || password.length() < MIN_LENGTH) {
			throw new InvalidInputException(
					"Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères");
		}
		if (!password.matches(".*[A-Z].*")) {
			throw new InvalidInputException("Le mot de passe doit contenir au moins une majuscule");
		}
		if (!password.matches(".*[a-z].*")) {
			throw new InvalidInputException("Le mot de passe doit contenir au moins une minuscule");
		}
		if (!password.matches(".*\\d.*")) {
			throw new InvalidInputException("Le mot de passe doit contenir au moins un chiffre");
		}
		if (password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
			throw new InvalidInputException("Le mot de passe doit contenir au moins un caractère spécial");
		}
	}
}
```

### A.8 `config/AuthSecurityProperties.java` et `config/CryptoConfig.java`

```java
package com.example.authentification_back.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth")
public class AuthSecurityProperties {
	private Duration lockDuration = Duration.ofMinutes(2);
	private int maxFailedAttempts = 5;
	public Duration getLockDuration() { return lockDuration; }
	public void setLockDuration(Duration lockDuration) { this.lockDuration = lockDuration; }
	public int getMaxFailedAttempts() { return maxFailedAttempts; }
	public void setMaxFailedAttempts(int maxFailedAttempts) { this.maxFailedAttempts = maxFailedAttempts; }
}
```

```java
package com.example.authentification_back.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AuthSecurityProperties.class)
public class CryptoConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
```

### A.9 `config/TestAccountInitializer.java`

```java
package com.example.authentification_back.config;

import com.example.authentification_back.entity.User;
import com.example.authentification_back.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TestAccountInitializer implements CommandLineRunner {

	public static final String TEST_EMAIL = "toto@example.com";
	public static final String TEST_PASSWORD_PLAIN = "Pwd1234!abcd";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public TestAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		if (userRepository.existsByEmail(TEST_EMAIL)) {
			return;
		}
		User user = new User();
		user.setEmail(TEST_EMAIL);
		user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD_PLAIN));
		userRepository.save(user);
	}
}
```

### A.10 `service/AuthService.java`

```java
package com.example.authentification_back.service;

import com.example.authentification_back.config.AuthSecurityProperties;
import com.example.authentification_back.dto.LoginRequest;
import com.example.authentification_back.dto.RegisterRequest;
import com.example.authentification_back.dto.UserResponse;
import com.example.authentification_back.entity.User;
import com.example.authentification_back.exception.AccountLockedException;
import com.example.authentification_back.exception.AuthenticationFailedException;
import com.example.authentification_back.exception.InvalidInputException;
import com.example.authentification_back.exception.ResourceConflictException;
import com.example.authentification_back.repository.UserRepository;
import com.example.authentification_back.validation.PasswordPolicyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

	public static final String GENERIC_LOGIN_ERROR = "Identifiants invalides";

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicyValidator passwordPolicyValidator;
	private final AuthSecurityProperties authProperties;
	private final Clock clock;

	public AuthService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			PasswordPolicyValidator passwordPolicyValidator,
			AuthSecurityProperties authProperties,
			Clock clock) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordPolicyValidator = passwordPolicyValidator;
		this.authProperties = authProperties;
		this.clock = clock;
	}

	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		passwordPolicyValidator.assertCompliant(request.password());
		if (!request.password().equals(request.passwordConfirm())) {
			log.warn("Inscription échouée: confirmation différente pour {}", email);
			throw new InvalidInputException("Les mots de passe ne correspondent pas");
		}
		if (userRepository.existsByEmail(email)) {
			log.warn("Inscription échouée: email déjà utilisé ({})", email);
			throw new ResourceConflictException("Cet email est déjà enregistré");
		}
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		userRepository.save(user);
		log.info("Inscription réussie pour l'utilisateur id={} email={}", user.getId(), email);
		return UserResponse.profile(user);
	}

	@Transactional
	public UserResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		String rawPassword = request.password();
		Instant now = clock.instant();

		Optional<User> optUser = userRepository.findByEmail(email);
		if (optUser.isEmpty()) {
			log.warn("Connexion échouée: identifiants invalides (email non reconnu)");
			throw new AuthenticationFailedException(GENERIC_LOGIN_ERROR);
		}
		User user = optUser.get();

		if (user.getLockUntil() != null && user.getLockUntil().isAfter(now)) {
			log.warn("Connexion refusée: compte verrouillé id={}", user.getId());
			throw new AccountLockedException("Compte temporairement verrouillé. Réessayez plus tard.");
		}
		if (user.getLockUntil() != null) {
			user.setLockUntil(null);
			user.setFailedLoginAttempts(0);
			userRepository.save(user);
		}

		if (passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			user.setFailedLoginAttempts(0);
			user.setLockUntil(null);
			String newToken = UUID.randomUUID().toString();
			user.setToken(newToken);
			userRepository.save(user);
			log.info("Connexion réussie pour l'utilisateur id={} email={}", user.getId(), email);
			return UserResponse.login(user, newToken);
		}

		int failures = user.getFailedLoginAttempts() + 1;
		user.setFailedLoginAttempts(failures);
		if (failures >= authProperties.getMaxFailedAttempts()) {
			user.setLockUntil(now.plus(authProperties.getLockDuration()));
			log.warn("Compte verrouillé après {} échecs id={} email={}", failures, user.getId(), email);
		}
		userRepository.save(user);
		log.warn("Connexion échouée: identifiants invalides (tentative {}/{})", failures, authProperties.getMaxFailedAttempts());
		throw new AuthenticationFailedException(GENERIC_LOGIN_ERROR);
	}

	@Transactional(readOnly = true)
	public UserResponse currentUser(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new AuthenticationFailedException("Authentification requise");
		}
		String token = rawToken.trim();
		return userRepository.findByToken(token)
				.map(UserResponse::profile)
				.orElseThrow(() -> new AuthenticationFailedException("Token invalide"));
	}

	private static String normalizeEmail(String email) {
		if (email == null) {
			return "";
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
```

### A.11 `controller/AuthController.java`

```java
package com.example.authentification_back.controller;

import com.example.authentification_back.dto.LoginRequest;
import com.example.authentification_back.dto.RegisterRequest;
import com.example.authentification_back.dto.UserResponse;
import com.example.authentification_back.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/auth/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
		UserResponse body = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	@PostMapping("/auth/login")
	public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
		UserResponse body = authService.login(request);
		return ResponseEntity.ok(body);
	}

	@GetMapping("/me")
	public ResponseEntity<UserResponse> me(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestHeader(value = "X-Auth-Token", required = false) String authToken) {
		String resolved = resolveToken(authorization, authToken);
		return ResponseEntity.ok(authService.currentUser(resolved));
	}

	private static String resolveToken(String authorization, String authToken) {
		String bearer = extractBearer(authorization);
		if (bearer != null && !bearer.isBlank()) {
			return bearer;
		}
		return authToken;
	}

	private static String extractBearer(String authorization) {
		if (authorization == null || authorization.isBlank()) {
			return null;
		}
		String trimmed = authorization.trim();
		if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
			return trimmed.substring(7).trim();
		}
		return trimmed;
	}
}
```

### A.12 `AuthApiIntegrationTest.java` (intégration)

```java
package com.example.authentification_back;

import com.example.authentification_back.config.TestAccountInitializer;
import com.example.authentification_back.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthApiIntegrationTest {

	private static final String STRONG = "Aa1!aaaaaaaa";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	private static String registerJson(String email, String pass, String confirm) {
		return String.format(
				"{\"email\":\"%s\",\"password\":\"%s\",\"passwordConfirm\":\"%s\"}",
				email, pass, confirm);
	}

	@Test
	void register_rejects_invalid_email_format() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson("pas-un-email", STRONG, STRONG)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_rejects_weak_password() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson("user@example.com", "short", "short")))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_rejects_password_confirm() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson("confirm@example.com", STRONG, "Bb2!bbbbbbbb")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Les mots de passe ne correspondent pas"));
	}

	@Test
	void register_ok() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson("newuser@example.com", STRONG, STRONG)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("newuser@example.com"));
	}

	@Test
	void register_conflict_when_email_exists() throws Exception {
		String body = registerJson("dup@example.com", STRONG, STRONG);
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());
	}

	@Test
	void login_ok_with_test_account() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"email\":\"%s\",\"password\":\"%s\"}",
								TestAccountInitializer.TEST_EMAIL,
								TestAccountInitializer.TEST_PASSWORD_PLAIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(TestAccountInitializer.TEST_EMAIL))
				.andExpect(jsonPath("$.token").exists());
	}

	@Test
	void login_fails_with_same_generic_message_for_wrong_password() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"email\":\"%s\",\"password\":\"wrong\"}",
								TestAccountInitializer.TEST_EMAIL)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(AuthService.GENERIC_LOGIN_ERROR));
	}

	@Test
	void login_fails_with_same_generic_message_for_unknown_email() throws Exception {
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"email\":\"%s\",\"password\":\"%s\"}",
								"nobody@example.com",
								TestAccountInitializer.TEST_PASSWORD_PLAIN)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(AuthService.GENERIC_LOGIN_ERROR));
	}

	@Test
	void me_forbidden_without_token() throws Exception {
		mockMvc.perform(get("/api/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void me_ok_after_login_with_bearer_token() throws Exception {
		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format(
								"{\"email\":\"%s\",\"password\":\"%s\"}",
								TestAccountInitializer.TEST_EMAIL,
								TestAccountInitializer.TEST_PASSWORD_PLAIN)))
				.andExpect(status().isOk())
				.andReturn();
		String token = objectMapper.readTree(login.getResponse().getContentAsString()).get("token").asText();
		MvcResult me = mockMvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(TestAccountInitializer.TEST_EMAIL))
				.andReturn();
		assertThat(objectMapper.readTree(me.getResponse().getContentAsString()).has("token")).isFalse();
	}

	@Test
	void account_locks_after_five_failures_then_unlocks_after_delay() throws Exception {
		String email = "lockout@example.com";
		String strong = "Bb2!bbbbbbbb";
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(registerJson(email, strong, strong)))
				.andExpect(status().isCreated());
		for (int i = 0; i < 5; i++) {
			mockMvc.perform(post("/api/auth/login")
							.contentType(MediaType.APPLICATION_JSON)
							.content(String.format("{\"email\":\"%s\",\"password\":\"nope\"}", email)))
					.andExpect(status().isUnauthorized());
		}
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format("{\"email\":\"%s\",\"password\":\"nope\"}", email)))
				.andExpect(status().is(HttpStatus.LOCKED.value()));
		Thread.sleep(200);
		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content(String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, strong)))
				.andExpect(status().isOk());
	}
}
```

### A.13 `PasswordPolicyValidatorTest.java`

```java
package com.example.authentification_back.validation;

import com.example.authentification_back.exception.InvalidInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordPolicyValidatorTest {

	private PasswordPolicyValidator validator;

	@BeforeEach
	void setUp() {
		validator = new PasswordPolicyValidator();
	}

	@Test
	void rejects_too_short() {
		assertThatThrownBy(() -> validator.assertCompliant("Aa1!short"))
				.isInstanceOf(InvalidInputException.class)
				.hasMessageContaining("12");
	}

	@Test
	void rejects_without_uppercase() {
		assertThatThrownBy(() -> validator.assertCompliant("aa1!aaaaaaaa"))
				.isInstanceOf(InvalidInputException.class)
				.hasMessageContaining("majuscule");
	}

	@Test
	void rejects_without_lowercase() {
		assertThatThrownBy(() -> validator.assertCompliant("AA1!AAAAAAAA"))
				.isInstanceOf(InvalidInputException.class)
				.hasMessageContaining("minuscule");
	}

	@Test
	void rejects_without_digit() {
		assertThatThrownBy(() -> validator.assertCompliant("Aa!!aaaaaaaa"))
				.isInstanceOf(InvalidInputException.class)
				.hasMessageContaining("chiffre");
	}

	@Test
	void rejects_without_special_character() {
		assertThatThrownBy(() -> validator.assertCompliant("Aa1aaaaaaaaa"))
				.isInstanceOf(InvalidInputException.class)
				.hasMessageContaining("spécial");
	}

	@Test
	void accepts_compliant_password() {
		assertThatCode(() -> validator.assertCompliant("Aa1!aaaaaaaa")).doesNotThrowAnyException();
	}

	@Test
	void rejects_null() {
		assertThatThrownBy(() -> validator.assertCompliant(null)).isInstanceOf(InvalidInputException.class);
	}
}
```

---

*Document généré à partir du dépôt `authentification_back` — en cas de divergence, le code source dans l’IDE fait foi.*
