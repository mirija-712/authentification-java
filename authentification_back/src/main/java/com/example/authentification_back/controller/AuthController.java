package com.example.authentification_back.controller;

import com.example.authentification_back.dto.ChangePasswordRequest;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API TP3 : inscription, {@code POST /api/auth/login} avec email / nonce / timestamp / hmac, profil {@code /api/me}.
 */
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

	/**
	 * SSO — corps : {@code email}, {@code nonce}, {@code timestamp} (epoch s), {@code hmac} (hex).
	 */
	@PostMapping("/auth/login")
	public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	/** TP5 — authentification requise (Bearer ou {@code X-Auth-Token}). */
	@PutMapping("/auth/change-password")
	public ResponseEntity<Void> changePassword(
			@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
			@RequestHeader(value = "X-Auth-Token", required = false) String authToken,
			@Valid @RequestBody ChangePasswordRequest request) {
		authService.changePassword(resolveToken(authorization, authToken), request);
		return ResponseEntity.noContent().build();
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
