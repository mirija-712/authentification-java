package com.example.authentification_back.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * TP5 — corps JSON pour {@code PUT /api/auth/change-password}.
 */
public record ChangePasswordRequest(
		@NotBlank(message = "L'ancien mot de passe est obligatoire")
		String oldPassword,
		@NotBlank(message = "Le nouveau mot de passe est obligatoire")
		String newPassword,
		@NotBlank(message = "La confirmation du mot de passe est obligatoire")
		String confirmPassword
) {
}
