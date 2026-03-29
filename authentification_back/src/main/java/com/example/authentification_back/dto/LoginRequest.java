package com.example.authentification_back.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Connexion TP2 ({@code password} seul) ou TP3 ({@code nonce} + {@code proof}, sans mot de passe).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginRequest(
		@NotBlank @Email String email,
		String password,
		String nonce,
		String proof
) {
}
