package com.example.authentification_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corps JSON attendu pour {@code POST /api/auth/login}.
 */
public record LoginRequest(
		@NotBlank @Email String email,
		@NotBlank @Size(min = 4) String password
) {
}
