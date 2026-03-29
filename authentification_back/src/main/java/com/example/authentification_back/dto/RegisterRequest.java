package com.example.authentification_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps JSON attendu pour {@code POST /api/auth/register}.
 * <p>
 * Les contraintes sont vérifiées avant d'atteindre le service grâce à {@code @Valid} sur le contrôleur.
 */
public record RegisterRequest(
		@NotBlank(message = "L'email est obligatoire")
		@Email(message = "Format d'email invalide")
		String email,
		@NotBlank(message = "Le mot de passe est obligatoire")
		@Size(min = 4, message = "Le mot de passe doit contenir au moins 4 caractères")
		String password
) {
}
