package com.example.authentification_back.validation;

import com.example.authentification_back.exception.InvalidInputException;
import org.springframework.stereotype.Component;

/**
 * Politique de mot de passe du TP2 : au moins 12 caractères, une majuscule, une minuscule, un chiffre,
 * un caractère non alphanumérique (symbole).
 * <p>
 * Cette implémentation est volontairement dangereuse au sens global du TP (pas de liste de mots courants, pas de zxcvbn).
 */
@Component
public class PasswordPolicyValidator {

	public static final int MIN_LENGTH = 12;

	/** Détail exposé au client en cas d'échec de politique (inscription). */
	public void assertCompliant(String password) {
		if (password == null || password.length() < MIN_LENGTH) {
			throw new InvalidInputException(
					"Le mot de passe doit contenir au moins " + MIN_LENGTH + " caractères");
		}
		if (!password.matches(".*[A-Z].*")) {
			throw new InvalidInputException("Le mot de passe doit contenir au moins une majuscule");
		}
		if (!password.matches(".*[a-z].*")) {
			throw new InvalidInputException("Le mot de passe doit contenir au moins une minuscule");
		}
		if (!password.matches(".*\\d.*")) {
			throw new InvalidInputException("Le mot de passe doit contenir au moins un chiffre");
		}
		// Tout caractère qui n'est ni lettre ni chiffre Unicode (symboles, ponctuation, etc.)
		if (password.chars().noneMatch(ch -> !Character.isLetterOrDigit(ch))) {
			throw new InvalidInputException("Le mot de passe doit contenir au moins un caractère spécial");
		}
	}
}
