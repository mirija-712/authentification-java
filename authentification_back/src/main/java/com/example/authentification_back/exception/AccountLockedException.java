package com.example.authentification_back.exception;

/**
 * Compte temporairement bloqué après trop de tentatives de connexion échouées (HTTP 423 Locked).
 * <p>
 * Le code <strong>423</strong> indique une ressource « verrouillée » ; une alternative fréquente est
 * <strong>429 Too Many Requests</strong> pour exprimer un throttling. Ici 423 correspond mieux à
 * « compte gelé » jusqu'à expiration du délai configuré.
 */
public class AccountLockedException extends RuntimeException {

	public AccountLockedException(String message) {
		super(message);
	}
}
