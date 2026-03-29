package com.example.authentification_back.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;

/**
 * TP3 — empreinte statique et preuve HMAC par nonce (anti-rejeu du message de connexion).
 * <p>
 * L’empreinte est stockée en base (dérivée du mot de passe à l’inscription) ; le login TP3 n’envoie plus
 * le mot de passe en clair, seulement une preuve {@code HMAC-SHA256(empreinte, nonce)}.
 */
public final class Tp3Proof {

	private static final String HMAC_SHA256 = "HmacSHA256";

	private Tp3Proof() {
	}

	public static String randomAuthSaltHex() {
		byte[] b = new byte[16];
		new SecureRandom().nextBytes(b);
		return HexFormat.of().formatHex(b);
	}

	public static String normalizeEmailForFingerprint(String email) {
		if (email == null) {
			return "";
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * Empreinte déterministe connue du client et du serveur après inscription (aucun secret serveur dans le client).
	 */
	public static String identityFingerprintHex(String email, String password, String authSaltHex) {
		String e = normalizeEmailForFingerprint(email);
		String payload = e + "|" + password + "|" + authSaltHex;
		return sha256Hex(payload.getBytes(StandardCharsets.UTF_8));
	}

	public static String proofHex(String identityFingerprintHex, String nonce) {
		byte[] key = HexFormat.of().parseHex(identityFingerprintHex);
		byte[] tag = hmacSha256(key, nonce.getBytes(StandardCharsets.UTF_8));
		return HexFormat.of().formatHex(tag);
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

	private static String sha256Hex(byte[] input) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
			return HexFormat.of().formatHex(digest);
		} catch (Exception e) {
			throw new IllegalStateException("SHA-256 indisponible", e);
		}
	}

	private static byte[] hmacSha256(byte[] key, byte[] data) {
		try {
			Mac mac = Mac.getInstance(HMAC_SHA256);
			mac.init(new SecretKeySpec(key, HMAC_SHA256));
			return mac.doFinal(data);
		} catch (Exception e) {
			throw new IllegalStateException("HMAC-SHA256 indisponible", e);
		}
	}
}
