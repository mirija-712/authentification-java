package com.example.authentification_front.policy;

/**
 * Indicateur visuel TP2 : rouge / orange / vert (conformité et « force » perçue).
 */
public enum PasswordStrength {
	/** Non conforme à la politique serveur. */
	RED,
	/** Conforme mais encore faible (ex. longueur minimale seulement). */
	ORANGE,
	/** Conforme et niveau jugé suffisant (ex. au-delà du strict minimum). */
	GREEN
}
