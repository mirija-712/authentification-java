package com.example.authentification_back.exception;

import com.example.authentification_back.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Intercepte les exceptions métier et les erreurs de validation Bean Validation pour renvoyer
 * toujours un JSON {@link ApiErrorResponse} cohérent (TP1 / TP2).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(InvalidInputException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalid(InvalidInputException ex, HttpServletRequest req) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
	}

	@ExceptionHandler(AccountLockedException.class)
	public ResponseEntity<ApiErrorResponse> handleLocked(AccountLockedException ex, HttpServletRequest req) {
		return build(HttpStatus.LOCKED, ex.getMessage(), req);
	}

	@ExceptionHandler(AuthenticationFailedException.class)
	public ResponseEntity<ApiErrorResponse> handleAuth(AuthenticationFailedException ex, HttpServletRequest req) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
	}

	@ExceptionHandler(ResourceConflictException.class)
	public ResponseEntity<ApiErrorResponse> handleConflict(ResourceConflictException ex, HttpServletRequest req) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), req);
	}

	/** Erreurs sur les DTO annotés {@code @Valid} (ex. email mal formé, mot de passe trop court). */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return build(HttpStatus.BAD_REQUEST, message.isEmpty() ? "Données invalides" : message, req);
	}

	private static ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, HttpServletRequest req) {
		ApiErrorResponse body = new ApiErrorResponse(
				Instant.now(),
				status.value(),
				status.getReasonPhrase(),
				message,
				req.getRequestURI()
		);
		return ResponseEntity.status(status).body(body);
	}
}
