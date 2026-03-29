package com.example.authentification_back.service;

import com.example.authentification_back.config.AuthSecurityProperties;
import com.example.authentification_back.security.Tp3Proof;
import com.example.authentification_back.dto.ChallengeRequest;
import com.example.authentification_back.dto.ChallengeResponse;
import com.example.authentification_back.dto.LoginRequest;
import com.example.authentification_back.dto.RegisterRequest;
import com.example.authentification_back.dto.UserResponse;
import com.example.authentification_back.entity.LoginNonce;
import com.example.authentification_back.entity.User;
import com.example.authentification_back.exception.AccountLockedException;
import com.example.authentification_back.exception.AuthenticationFailedException;
import com.example.authentification_back.exception.InvalidInputException;
import com.example.authentification_back.exception.ResourceConflictException;
import com.example.authentification_back.repository.LoginNonceRepository;
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

/**
 * Service d'authentification TP2 (BCrypt, lockout) + TP3 (challenge/nonce + preuve HMAC).
 */
@Service
public class AuthService {

	/** Message unique pour email inconnu ou mot de passe incorrect (recommandation sécurité TP2). */
	public static final String GENERIC_LOGIN_ERROR = "Identifiants invalides";

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;
	private final LoginNonceRepository loginNonceRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordPolicyValidator passwordPolicyValidator;
	private final AuthSecurityProperties authProperties;
	private final Clock clock;

	public AuthService(
			UserRepository userRepository,
			LoginNonceRepository loginNonceRepository,
			PasswordEncoder passwordEncoder,
			PasswordPolicyValidator passwordPolicyValidator,
			AuthSecurityProperties authProperties,
			Clock clock) {
		this.userRepository = userRepository;
		this.loginNonceRepository = loginNonceRepository;
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
		String authSalt = Tp3Proof.randomAuthSaltHex();
		String fingerprint = Tp3Proof.identityFingerprintHex(email, request.password(), authSalt);
		User user = new User();
		user.setEmail(email);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setAuthSalt(authSalt);
		user.setIdentityFingerprint(fingerprint);
		userRepository.save(user);
		log.info("Inscription réussie pour l'utilisateur id={} email={}", user.getId(), email);
		return UserResponse.profile(user);
	}

	/**
	 * Émet un nonce à usage unique pour l’email (TP3). Réponse générique si l’email est inconnu (énumération).
	 */
	@Transactional
	public ChallengeResponse createChallenge(ChallengeRequest request) {
		String email = normalizeEmail(request.email());
		Optional<User> opt = userRepository.findByEmail(email);
		if (opt.isEmpty()) {
			log.warn("Challenge refusé: email inconnu");
			throw new AuthenticationFailedException(GENERIC_LOGIN_ERROR);
		}
		User user = opt.get();
		if (user.getIdentityFingerprint() == null || user.getIdentityFingerprint().isBlank()
				|| user.getAuthSalt() == null || user.getAuthSalt().isBlank()) {
			throw new InvalidInputException(
					"Compte non éligible au login TP3 : utilisez la connexion par mot de passe.");
		}
		String nonce = UUID.randomUUID().toString();
		Instant expires = clock.instant().plus(authProperties.getChallengeTtl());
		LoginNonce row = new LoginNonce();
		row.setNonce(nonce);
		row.setEmail(email);
		row.setExpiresAt(expires);
		row.setConsumed(false);
		loginNonceRepository.save(row);
		return new ChallengeResponse(nonce, expires.toString(), user.getAuthSalt());
	}

	/**
	 * Les échecs de connexion doivent être persistés (compteur + verrou) même en répondant 401 :
	 * sans {@code noRollbackFor}, la transaction annulerait le {@code save} avant le throw.
	 */
	@Transactional(noRollbackFor = AuthenticationFailedException.class)
	public UserResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		boolean hasPassword = request.password() != null && !request.password().isBlank();
		boolean hasProof = request.nonce() != null && !request.nonce().isBlank()
				&& request.proof() != null && !request.proof().isBlank();
		if (hasPassword == hasProof) {
			throw new InvalidInputException(
					"Envoyez soit le mot de passe (TP2), soit nonce+proof sans mot de passe (TP3).");
		}
		if (hasPassword) {
			return loginWithPassword(email, request.password());
		}
		return loginWithProof(email, request.nonce(), request.proof());
	}

	private UserResponse loginWithPassword(String email, String rawPassword) {
		Instant now = clock.instant();
		Optional<User> optUser = userRepository.findByEmail(email);
		if (optUser.isEmpty()) {
			log.warn("Connexion échouée: identifiants invalides (email non reconnu)");
			throw new AuthenticationFailedException(GENERIC_LOGIN_ERROR);
		}
		User user = optUser.get();
		assertNotLocked(user, now);
		clearExpiredLockIfNeeded(user, now);

		if (passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
			return grantSession(user, email);
		}
		registerFailureAndThrow(user, email, now);
		throw new IllegalStateException("registerFailureAndThrow doit lever une exception");
	}

	private UserResponse loginWithProof(String email, String nonce, String proofHex) {
		Instant now = clock.instant();
		Optional<User> optUser = userRepository.findByEmail(email);
		if (optUser.isEmpty()) {
			log.warn("Connexion TP3 échouée: email inconnu");
			throw new AuthenticationFailedException(GENERIC_LOGIN_ERROR);
		}
		User user = optUser.get();
		assertNotLocked(user, now);
		clearExpiredLockIfNeeded(user, now);

		if (user.getIdentityFingerprint() == null || user.getIdentityFingerprint().isBlank()) {
			throw new InvalidInputException("Connexion TP3 indisponible pour ce compte.");
		}

		Optional<LoginNonce> optNonce = loginNonceRepository.findByNonce(nonce);
		if (optNonce.isEmpty()) {
			log.warn("Connexion TP3 échouée: nonce inconnu");
			registerFailureAndThrow(user, email, now);
		}
		LoginNonce row = optNonce.get();
		if (!email.equals(row.getEmail())) {
			log.warn("Connexion TP3 échouée: nonce/email incohérents");
			registerFailureAndThrow(user, email, now);
		}
		if (row.isConsumed() || row.getExpiresAt().isBefore(now)) {
			log.warn("Connexion TP3 échouée: nonce expiré ou déjà utilisé");
			registerFailureAndThrow(user, email, now);
		}

		String expected = Tp3Proof.proofHex(user.getIdentityFingerprint(), nonce);
		row.setConsumed(true);
		loginNonceRepository.save(row);

		if (!Tp3Proof.constantTimeEqualsHex(expected, proofHex)) {
			log.warn("Connexion TP3 échouée: preuve HMAC incorrecte");
			registerFailureAndThrow(user, email, now);
		}

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
