package com.example.authentification_back.service;

import com.example.authentification_back.dto.LoginRequest;
import com.example.authentification_back.dto.RegisterRequest;
import com.example.authentification_back.dto.UserResponse;
import com.example.authentification_back.entity.User;
import com.example.authentification_back.exception.AuthenticationFailedException;
import com.example.authentification_back.exception.InvalidInputException;
import com.example.authentification_back.exception.ResourceConflictException;
import com.example.authentification_back.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Service principal d'authentification pour le TP1.
 * <p>
 * Cette implémentation est volontairement dangereuse et ne doit jamais être utilisée en production
 * (mots de passe en clair, jeton persistant en base sans expiration — TP1).
 *
 * @see com.example.authentification_back.entity.User
 */
@Service
public class AuthService {

	private static final Logger log = LoggerFactory.getLogger(AuthService.class);

	private final UserRepository userRepository;

	public AuthService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * Crée un compte si l'email est libre et si le mot de passe respecte la règle minimale (4 caractères).
	 *
	 * @throws InvalidInputException       si le mot de passe est trop court (doublon de contrôle avec la validation HTTP)
	 * @throws ResourceConflictException si l'email existe déjà (HTTP 409)
	 */
	@Transactional
	public UserResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		String password = request.password();
		if (password == null || password.length() < 4) {
			log.warn("Inscription échouée: mot de passe trop court pour {}", email);
			throw new InvalidInputException("Le mot de passe doit contenir au moins 4 caractères");
		}
		if (userRepository.existsByEmail(email)) {
			log.warn("Inscription échouée: email déjà utilisé ({})", email);
			throw new ResourceConflictException("Cet email est déjà enregistré");
		}
		User user = new User();
		user.setEmail(email);
		user.setPasswordClear(password);
		userRepository.save(user);
		log.info("Inscription réussie pour l'utilisateur id={} email={}", user.getId(), email);
		return UserResponse.profile(user);
	}

	/**
	 * Authentifie l'utilisateur, remplace le jeton stocké par un nouvel UUID (une seule session « logique » par compte).
	 *
	 * @throws AuthenticationFailedException email inconnu ou mot de passe incorrect (HTTP 401)
	 */
	@Transactional
	public UserResponse login(LoginRequest request) {
		String email = normalizeEmail(request.email());
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> {
					log.warn("Connexion échouée: email inconnu ({})", email);
					return new AuthenticationFailedException("Email inconnu");
				});
		if (!user.getPasswordClear().equals(request.password())) {
			log.warn("Connexion échouée: mot de passe incorrect pour {}", email);
			throw new AuthenticationFailedException("Mot de passe incorrect");
		}
		// Jeton opaque stocké en base ; ne jamais écrire le mot de passe dans les logs.
		String newToken = UUID.randomUUID().toString();
		user.setToken(newToken);
		userRepository.save(user);
		log.info("Connexion réussie pour l'utilisateur id={} email={}", user.getId(), email);
		return UserResponse.login(user, newToken);
	}

	/**
	 * Résout l'utilisateur à partir du jeton présent en base.
	 *
	 * @param rawToken valeur extraite des en-têtes HTTP (jamais journalisée)
	 * @throws AuthenticationFailedException jeton absent, vide ou inconnu
	 */
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

	/** Normalisation pour éviter les doublons de casse / espaces sur l'email. */
	private static String normalizeEmail(String email) {
		if (email == null) {
			return "";
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
