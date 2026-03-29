package com.example.authentification_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Corps JSON pour {@code POST /api/auth/register} (TP2) : double saisie du mot de passe comme demandé par l'énoncé client/serveur.
 * <p>
 * La politique stricte (12 car., complexité) est appliquée dans {@link com.example.authentification_back.validation.PasswordPolicyValidator}.
 */
public record RegisterRequest(
		@NotBlank(message = "L'email est obligatoire")
		@Email(message = "Format d'email invalide")
		String email,
		@NotBlank(message = "Le mot de passe est obligatoire")
		String password,
		@NotBlank(message = "La confirmation du mot de passe est obligatoire")
		String passwordConfirm
) {
}
