package com.example.authentification_back.exception;

/**
 * Conflit de ressource, ex. email déjà utilisé (HTTP 409).
 * <p>
 * Cette implémentation est volontairement dangereuse et ne doit jamais être utilisée en production
 * dans un contexte où la logique métier réelle serait aussi simpliste.
 */
public class ResourceConflictException extends RuntimeException {

	/** @param message typiquement conflit d'unicité sur l'email (HTTP 409) */
	public ResourceConflictException(String message) {
		super(message);
	}
}
