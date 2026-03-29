package com.example.authentification_front.policy;

/**
 * Règles alignées sur {@code PasswordPolicyValidator} côté serveur (TP2).
 * <p>
 * Utilisé uniquement pour l'UX (couleurs) : la validation définitive reste côté API.
 */
public final class ClientPasswordPolicy {

	public static final int MIN_LENGTH = 12;

	private ClientPasswordPolicy() {
	}

	public static boolean isCompliant(String password) {
		if (password == null || password.length() < MIN_LENGTH) {
			return false;
		}
		if (!password.matches(".*[A-Z].*")) {
			return false;
		}
		if (!password.matches(".*[a-z].*")) {
			return false;
		}
		if (!password.matches(".*\\d.*")) {
			return false;
		}
		return password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
	}

	/**
	 * Rouge : non conforme. Orange : conforme mais longueur au plus égale au minimum (ou seuil court).
	 * Vert : conforme et longueur strictement supérieure au minimum (meilleur niveau).
	 */
	public static PasswordStrength evaluateStrength(String password) {
		if (password == null || password.isEmpty()) {
			return PasswordStrength.RED;
		}
		if (!isCompliant(password)) {
			return PasswordStrength.RED;
		}
		if (password.length() <= MIN_LENGTH) {
			return PasswordStrength.ORANGE;
		}
		return PasswordStrength.GREEN;
	}
}
