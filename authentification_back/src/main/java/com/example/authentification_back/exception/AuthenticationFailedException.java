package com.example.authentification_back.exception;

/**
 * Échec d'authentification ou accès non autorisé (HTTP 401).
 * <p>
 * Cette implémentation est volontairement dangereuse et ne doit jamais être utilisée en production
 * telle quelle (jeton sans expiration côté serveur ; absence de révocation liste noire, etc.).
 */
public class AuthenticationFailedException extends RuntimeException {

	/**
	 * @param message détail lisible côté client (en TP2+ on uniformisera souvent les messages pour limiter l'énumération)
	 */
	public AuthenticationFailedException(String message) {
		super(message);
	}
}
