package com.example.authentification_back.exception;

/**
 * Données d'entrée invalides (HTTP 400).
 * <p>
 * Cette implémentation est volontairement dangereuse au sens TP1 (contexte global de l'API) ;
 * l'exception elle-même est un mécanisme de contrôle classique.
 */
public class InvalidInputException extends RuntimeException {

	/** @param message cause fonctionnelle (HTTP 400) */
	public InvalidInputException(String message) {
		super(message);
	}
}
