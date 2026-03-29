package com.example.authentification_back.dto;

import java.time.Instant;

/**
 * Enveloppe d'erreur unique pour toutes les exceptions gérées par {@code GlobalExceptionHandler}
 * (format imposé par le TP : timestamp, status, error, message, path).
 */
public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path
) {
}
