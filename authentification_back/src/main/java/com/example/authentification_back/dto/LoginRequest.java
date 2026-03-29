package com.example.authentification_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * SSO TP3 — une seule requête : {@code email}, {@code nonce}, {@code timestamp} (epoch secondes), {@code hmac} (hex).
 * Le mot de passe ne transite pas.
 */
public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank String nonce,
		@NotNull Long timestamp,
		@NotBlank String hmac
) {
}
