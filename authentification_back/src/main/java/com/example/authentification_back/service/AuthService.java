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

/**
 * Service d'authentification TP2 : BCrypt, politique de mot de passe, verrouillage après échecs répétés.
 * <p>
 * TP2 améliore le stockage mais ne protège pas encore contre le rejeu des requêtes capturées (TP3).
 */
@Service
public class AuthService {

	/** Message unique pour email inconnu ou mot de passe incorrect (recommandation sécurité TP2). */
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

	/**
	 * Les échecs de connexion doivent être persistés (compteur + verrou) même en répondant 401 :
	 * sans {@code noRollbackFor}, la transaction annulerait le {@code save} avant le throw.
	 */
	@Transactional(noRollbackFor = AuthenticationFailedException.class)
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
		// Blocage expiré : on réinitialise compteur et date (persisté pour cohérence avec la base).
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
