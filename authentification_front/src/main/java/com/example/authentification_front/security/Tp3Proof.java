package com.example.authentification_front.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;

/**
 * TP3 — même algorithme que le backend ({@code authentification_back.security.Tp3Proof}) pour la preuve HMAC.
 */
public final class Tp3Proof {

	private static final String HMAC_SHA256 = "HmacSHA256";

	private Tp3Proof() {
	}

	public static String normalizeEmailForFingerprint(String email) {
		if (email == null) {
			return "";
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

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
