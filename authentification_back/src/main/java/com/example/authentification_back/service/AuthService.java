package com.example.authentification_back.service;

import com.example.authentification_back.config.AuthSecurityProperties;
import com.example.authentification_back.dto.LoginRequest;
import com.example.authentification_back.dto.RegisterRequest;
import com.example.authentification_back.dto.UserResponse;
import com.example.authentification_back.entity.AuthNonce;
import com.example.authentification_back.entity.User;
import com.example.authentification_back.exception.AccountLockedException;
import com.example.authentification_back.exception.AuthenticationFailedException;
import com.example.authentification_back.exception.InvalidInputException;
import com.example.authentification_back.exception.ResourceConflictException;
import com.example.authentification_back.repository.AuthNonceRepository;
import com.example.authentification_back.repository.UserRepository;
import com.example.authentification_back.security.PasswordEncryptionService;
import com.example.authentification_back.security.SsoHmac;
import com.example.authentification_back.validation.PasswordPolicyValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Authentification TP3 (SSO 2 étapes logiques, 1 échange réseau) : HMAC + nonce + timestamp, SMK pour le mot de passe.
 */
@Service
public class AuthService {

	public static final String GENERIC_LOGIN_ERROR = "Identifiants invalides";

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;
	private final AuthNonceRepository authNonceRepository;
	private final PasswordEncryptionService passwordEncryptionService;
	private final PasswordPolicyValidator passwordPolicyValidator;
	private final AuthSecurityProperties authProperties;
	private final Clock clock;

	public AuthService(
			UserRepository userRepository,
			AuthNonceRepository authNonceRepository,
			PasswordEncryptionService passwordEncryptionService,
			PasswordPolicyValidator passwordPolicyValidator,
			AuthSecurityProperties authProperties,
			Clock clock) {
		this.userRepository = userRepository;
		this.authNonceRepository = authNonceRepository;
		this.passwordEncryptionService = passwordEncryptionService;
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
		user.setPasswordEncrypted(passwordEncryptionService.encrypt(request.password()));
		userRepository.save(user);
		log.info("Inscription réussie pour l'utilisateur id={} email={}", user.getId(), email);
		return UserResponse.profile(user);
	}

	/**
	 * Ordre des vérifications aligné sur les slides : email → timestamp → nonce → déchiffrement → HMAC (temps constant).
	 */
	@Transactional(noRollbackFor = AuthenticationFailedException.class)
	public UserResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		Instant now = clock.instant();

		Optional<User> optUser = userRepository.findByEmail(email);
		if (optUser.isEmpty()) {
			log.warn("Connexion échouée: email non reconnu");
			throw new AuthenticationFailedException(GENERIC_LOGIN_ERROR);
		}
		User user = optUser.get();

		assertNotLocked(user, now);
		clearExpiredLockIfNeeded(user, now);

		Instant clientTs = Instant.ofEpochSecond(request.timestamp());
		long skewSeconds = Math.abs(Duration.between(now, clientTs).getSeconds());
		if (skewSeconds > authProperties.getTimestampSkewSeconds()) {
			log.warn("Connexion refusée: timestamp hors fenêtre (skew={}s)", skewSeconds);
			throw new AuthenticationFailedException(GENERIC_LOGIN_ERROR);
		}

		if (authNonceRepository.existsByUserIdAndNonce(user.getId(), request.nonce())) {
			log.warn("Connexion refusée: nonce déjà utilisé (rejeu) userId={}", user.getId());
			throw new AuthenticationFailedException(GENERIC_LOGIN_ERROR);
		}

		String plainPassword;
		try {
			plainPassword = passwordEncryptionService.decrypt(user.getPasswordEncrypted());
		} catch (Exception e) {
			log.warn("Déchiffrement impossible pour user id={}", user.getId());
			throw new AuthenticationFailedException(GENERIC_LOGIN_ERROR);
		}

		String message = SsoHmac.messageToSign(email, request.nonce(), request.timestamp());
		String expectedHex = SsoHmac.hmacSha256Hex(plainPassword, message);
		if (!SsoHmac.constantTimeEqualsHex(expectedHex, request.hmac())) {
			log.warn("Connexion échouée: HMAC incorrect (tentative {}/{})",
					user.getFailedLoginAttempts() + 1, authProperties.getMaxFailedAttempts());
			registerFailureAndThrow(user, email, now);
		}

		AuthNonce nonceRow = new AuthNonce();
		nonceRow.setUserId(user.getId());
		nonceRow.setNonce(request.nonce());
		nonceRow.setExpiresAt(now.plusSeconds(authProperties.getNonceTtlSeconds()));
		nonceRow.setConsumed(true);
		authNonceRepository.save(nonceRow);

		return grantSession(user, email);
	}

	private void assertNotLocked(User user, Instant now) {
		if (user.getLockUntil() != null && user.getLockUntil().isAfter(now)) {
			log.warn("Connexion refusée: compte verrouillé id={}", user.getId());
			throw new AccountLockedException("Compte temporairement verrouillé. Réessayez plus tard.");
		}
	}

	private void clearExpiredLockIfNeeded(User user, Instant now) {
		if (user.getLockUntil() != null && !user.getLockUntil().isAfter(now)) {
			user.setLockUntil(null);
			user.setFailedLoginAttempts(0);
			userRepository.save(user);
		}
	}

	private UserResponse grantSession(User user, String email) {
		user.setFailedLoginAttempts(0);
		user.setLockUntil(null);
		String newToken = UUID.randomUUID().toString();
		user.setToken(newToken);
		userRepository.save(user);
		log.info("Connexion réussie pour l'utilisateur id={} email={}", user.getId(), email);
		return UserResponse.login(user, newToken);
	}

	private void registerFailureAndThrow(User user, String email, Instant now) {
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
