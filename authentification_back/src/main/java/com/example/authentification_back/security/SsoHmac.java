package com.example.authentification_back.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Protocole SSO TP3 — client et serveur doivent produire exactement le même tag :
 * {@code message = email + ":" + nonce + ":" + timestamp} (secondes depuis l’epoch),
 * {@code hmac = HMAC_SHA256(key = mot de passe UTF-8, data = message UTF-8)}, envoyé en hexadécimal.
 * <p>
 * Côté serveur (TP4), le mot de passe utilisé comme clé est celui obtenu après {@code decrypt(password_encrypted)} ;
 * il n’est jamais envoyé sur le réseau dans le corps du POST /login.
 */
public final class SsoHmac {

	private static final String HMAC_SHA256 = "HmacSHA256";

	private SsoHmac() {
	}

	/** Email déjà normalisé (trim + lower case), comme côté serveur. */
	public static String messageToSign(String normalizedEmail, String nonce, long timestampEpochSeconds) {
		return normalizedEmail + ":" + nonce + ":" + timestampEpochSeconds;
	}

	public static String hmacSha256Hex(String password, String message) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA256);
			mac.init(new SecretKeySpec(password.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
			byte[] tag = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(tag);
		} catch (Exception e) {
			throw new IllegalStateException("HMAC-SHA256 indisponible", e);
		}
	}

	public static boolean constantTimeEqualsHex(String aHex, String bHex) {
		if (aHex == null || bHex == null || aHex.length() != bHex.length()) {
			return false;
		}
		try {
			byte[] a = HexFormat.of().parseHex(aHex);
			byte[] b = HexFormat.of().parseHex(bHex);
			return MessageDigest.isEqual(a, b);
		} catch (IllegalArgumentException e) {
			return false;
		}
	}
}
