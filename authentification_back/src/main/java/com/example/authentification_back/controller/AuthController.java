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

/**
 * Contrôleur REST du TP1 : exposition des routes sous le préfixe {@code /api}.
 * <p>
 * Cette implémentation est volontairement dangereuse et ne doit jamais être utilisée en production.
 *
 * @see AuthService
 */
@RestController
@RequestMapping("/api")
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	/**
	 * Inscription d'un nouvel utilisateur.
	 *
	 * @return 201 avec le profil créé (sans jeton ; le jeton n'est obtenu qu'après {@link #login}).
	 */
	@PostMapping("/auth/register")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
		UserResponse body = authService.register(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(body);
	}

	/**
	 * Vérifie les identifiants et persiste un nouveau jeton en base pour l'utilisateur.
	 *
	 * @return 200 avec le profil et le champ {@code token} à réutiliser pour {@link #me}.
	 */
	@PostMapping("/auth/login")
	public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
		UserResponse body = authService.login(request);
		return ResponseEntity.ok(body);
	}

	/**
	 * Route protégée : lecture du profil de l'utilisateur identifié par le jeton.
	 * <p>
	 * Chemin exact : {@code GET /api/me} (pas {@code /api/auth/me}).
	 */
	@GetMapping("/me")
	public ResponseEntity<UserResponse> me(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestHeader(value = "X-Auth-Token", required = false) String authToken) {
		String resolved = resolveToken(authorization, authToken);
		return ResponseEntity.ok(authService.currentUser(resolved));
	}

	/**
	 * Priorité au schéma Bearer standard ; sinon repli sur l'en-tête personnalisé (pratique Postman).
	 */
	private static String resolveToken(String authorization, String authToken) {
		String bearer = extractBearer(authorization);
		if (bearer != null && !bearer.isBlank()) {
			return bearer;
		}
		return authToken;
	}

	/** Extrait la valeur après le préfixe {@code Bearer } (insensible à la casse sur le mot Bearer). */
	private static String extractBearer(String authorization) {
		if (authorization == null || authorization.isBlank()) {
			return null;
		}
		String trimmed = authorization.trim();
		if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
			return trimmed.substring(7).trim();
		}
		// Tolère un token brut passé dans Authorization sans préfixe (peu recommandé mais pratique en TP)
		return trimmed;
	}
}
