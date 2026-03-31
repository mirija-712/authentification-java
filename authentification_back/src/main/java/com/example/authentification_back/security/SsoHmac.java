package com.example.authentification_back.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class SsoHmac {

	private static final String HMAC_SHA256 = "HmacSHA256";

	private SsoHmac() {
	}

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
