package com.example.authentification_back.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Chiffrement réversible du mot de passe avec la SMK (énoncé TP3) — AES-256-GCM.
 * <p>
 * Usage pédagogique uniquement ; en production on préfère des preuves sans stockage réversible.
 */
@Service
public class PasswordEncryptionService {

	private static final int GCM_IV_LENGTH = 12;
	private static final int GCM_TAG_LENGTH = 128;

	private final SecretKey aesKey;

	public PasswordEncryptionService(@Value("${app.auth.server-master-key}") String serverMasterKey) {
		if (serverMasterKey == null || serverMasterKey.isBlank()) {
			throw new IllegalStateException("app.auth.server-master-key doit être défini");
		}
		byte[] keyBytes = sha256(serverMasterKey.getBytes(StandardCharsets.UTF_8));
		this.aesKey = new SecretKeySpec(keyBytes, "AES");
	}

	private static byte[] sha256(byte[] input) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(input);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public String encrypt(String plainPassword) {
		try {
			byte[] iv = new byte[GCM_IV_LENGTH];
			new SecureRandom().nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
			byte[] cipherText = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));
			byte[] combined = new byte[iv.length + cipherText.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
			return Base64.getEncoder().encodeToString(combined);
		} catch (Exception e) {
			throw new IllegalStateException("Chiffrement impossible", e);
		}
	}

	public String decrypt(String stored) {
		try {
			byte[] combined = Base64.getDecoder().decode(stored);
			byte[] iv = new byte[GCM_IV_LENGTH];
			System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
			byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
			System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
			byte[] plain = cipher.doFinal(cipherText);
			return new String(plain, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("Déchiffrement impossible", e);
		}
	}
}
