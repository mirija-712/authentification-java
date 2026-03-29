package com.example.authentification_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Corps JSON pour {@code POST /api/auth/login} (TP2).
 * <p>
 * Aucune longueur minimale sur le mot de passe à la validation HTTP : un mot de passe incorrect court doit
 * produire le même message générique que pour un email inconnu (recommandation TP2).
 */
public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank String password
) {
}
