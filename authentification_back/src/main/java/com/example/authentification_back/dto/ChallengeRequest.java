package com.example.authentification_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Corps JSON pour {@code POST /api/auth/challenge} (TP3). */
public record ChallengeRequest(@NotBlank @Email String email) {
}
