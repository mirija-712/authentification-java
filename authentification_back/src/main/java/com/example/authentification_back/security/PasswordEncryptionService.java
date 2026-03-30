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
 * TP4 — Chiffrement réversible des mots de passe pour permettre le login TP3 (HMAC avec le mot de passe en clair côté serveur).
 * <ul>
 *   <li><b>Clé</b> : variable d’environnement {@code APP_MASTER_KEY} uniquement (énoncé : jamais en dur).</li>
 *   <li><b>Algo</b> : AES-256-GCM (confidentialité + intégrité / tag d’authentification).</li>
 *   <li><b>Stockage</b> : {@code v1:Base64(iv):Base64(ciphertext)} ; IV différent à chaque chiffrement (pas d’IV fixe).</li>
 * </ul>
 */
@Service
public class PasswordEncryptionService {

	/** Préfixe pour versionner le format stocké en base (évolution future possible v2, …). */
	private static final String FORMAT_V1_PREFIX = "v1:";

	/** Taille IV recommandée pour GCM (12 octets). */
	private static final int GCM_IV_LENGTH = 12;

	/** Tag d’authentification GCM en bits (128 bits = 16 octets dans le flux chiffré). */
	private static final int GCM_TAG_LENGTH = 128;

	/** Réutilisé pour les IV (évite d’instancier un nouveau générateur à chaque chiffrement). */
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final SecretKey aesKey;

	public PasswordEncryptionService(@Value("${APP_MASTER_KEY:}") String appMasterKey) {
		// Spring injecte "" si la variable est absente : même comportement que « non définie ».
		if (appMasterKey == null || appMasterKey.isBlank()) {
			throw new IllegalStateException(
					"Variable d'environnement APP_MASTER_KEY obligatoire : définissez une clé maître (jamais en dur dans le code).");
		}
		// On ne stocke pas la Master Key telle quelle : on en dérive une clé AES-256 (32 octets) via SHA-256.
		byte[] keyBytes = sha256(appMasterKey.getBytes(StandardCharsets.UTF_8));
		this.aesKey = new SecretKeySpec(keyBytes, "AES");
	}

	private static byte[] sha256(byte[] input) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(input);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/**
	 * Chiffre le mot de passe avant persistance JPA.
	 * @return chaîne au format {@code v1:Base64(iv):Base64(ciphertext)} (ciphertext inclut le tag GCM).
	 */
	public String encrypt(String plainPassword) {
		try {
			byte[] iv = new byte[GCM_IV_LENGTH];
			SECURE_RANDOM.nextBytes(iv); // IV aléatoire : interdit d’utiliser un IV fixe (énoncé TP4).
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
			byte[] cipherText = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));
			String ivB64 = Base64.getEncoder().encodeToString(iv);
			String ctB64 = Base64.getEncoder().encodeToString(cipherText);
			return FORMAT_V1_PREFIX + ivB64 + ":" + ctB64;
		} catch (Exception e) {
			throw new IllegalStateException("Chiffrement impossible", e);
		}
	}

	/**
	 * Déchiffre la valeur lue en base pour recalculer le HMAC au login (TP3).
	 * Accepte le format {@code v1:...} ou l’ancien format « un seul Base64 » (migration).
	 */
	public String decrypt(String stored) {
		if (stored == null || stored.isBlank()) {
			throw new IllegalStateException("Valeur chiffrée vide");
		}
		if (stored.startsWith(FORMAT_V1_PREFIX)) {
			return decryptV1(stored);
		}
		return decryptLegacyConcatenated(stored);
	}

	private String decryptV1(String stored) {
		try {
			String payload = stored.substring(FORMAT_V1_PREFIX.length());
			int sep = payload.indexOf(':');
			if (sep < 0) {
				throw new IllegalStateException("Format v1 invalide");
			}
			byte[] iv = Base64.getDecoder().decode(payload.substring(0, sep));
			byte[] cipherText = Base64.getDecoder().decode(payload.substring(sep + 1));
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
			byte[] plain = cipher.doFinal(cipherText); // lève si le ciphertext a été altéré (intégrité GCM)
			return new String(plain, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("Déchiffrement impossible", e);
		}
	}

	/** Compatibilité : anciennes lignes = un seul Base64 (IV de 12 octets + ciphertext GCM concaténés). */
	private String decryptLegacyConcatenated(String stored) {
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
