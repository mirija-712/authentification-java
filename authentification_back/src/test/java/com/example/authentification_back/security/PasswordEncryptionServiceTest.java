package com.example.authentification_back.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TP4 — tests unitaires imposés par l’énoncé : clé obligatoire, cycle chiffrement/déchiffrement,
 * chiffré distinct du clair, intégrité GCM si le ciphertext est modifié.
 */
class PasswordEncryptionServiceTest {

	/** Même valeur que la CI / {@code application-test.properties} (clé factice, pas un secret de prod). */
	private static final String CI_DUMMY_KEY = "test_master_key_for_ci_only";

	@Test
	void constructorRejectsBlankMasterKey() {
		assertThatThrownBy(() -> new PasswordEncryptionService(""))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("APP_MASTER_KEY");
		assertThatThrownBy(() -> new PasswordEncryptionService("   "))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void encryptThenDecrypt_roundTrip() {
		PasswordEncryptionService svc = new PasswordEncryptionService(CI_DUMMY_KEY);
		String plain = "MonMotDePasse!2026";
		String enc = svc.encrypt(plain);
		assertThat(svc.decrypt(enc)).isEqualTo(plain);
	}

	@Test
	void ciphertext_isNotEqualToPlainPassword() {
		PasswordEncryptionService svc = new PasswordEncryptionService(CI_DUMMY_KEY);
		String plain = "Secret123!abc";
		String enc = svc.encrypt(plain);
		assertThat(enc).isNotEqualTo(plain);
		assertThat(enc).startsWith("v1:");
	}

	@Test
	void decryptRejectsNullOrBlankStored() {
		PasswordEncryptionService svc = new PasswordEncryptionService(CI_DUMMY_KEY);
		assertThatThrownBy(() -> svc.decrypt(null)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> svc.decrypt("   ")).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void decryptV1_rejectsInvalidPayload() {
		PasswordEncryptionService svc = new PasswordEncryptionService(CI_DUMMY_KEY);
		assertThatThrownBy(() -> svc.decrypt("v1:pas-de-deuxieme-partie"))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void decryptLegacy_concatenatedIvAndCiphertext_stillWorks() {
		PasswordEncryptionService svc = new PasswordEncryptionService(CI_DUMMY_KEY);
		String plain = "legacy-roundtrip";
		String v1 = svc.encrypt(plain);
		assertThat(v1).startsWith("v1:");
		String payload = v1.substring("v1:".length());
		int sep = payload.indexOf(':');
		byte[] iv = Base64.getDecoder().decode(payload.substring(0, sep));
		byte[] cipherText = Base64.getDecoder().decode(payload.substring(sep + 1));
		byte[] combined = new byte[iv.length + cipherText.length];
		System.arraycopy(iv, 0, combined, 0, iv.length);
		System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
		String legacy = Base64.getEncoder().encodeToString(combined);
		assertThat(svc.decrypt(legacy)).isEqualTo(plain);
	}

	@Test
	void decryptFailsWhenCiphertextTampered() {
		PasswordEncryptionService svc = new PasswordEncryptionService(CI_DUMMY_KEY);
		String enc = svc.encrypt("ok-password");
		assertThat(enc).startsWith("v1:");
		// Corruption minimale du dernier caractère : le tag GCM ne correspond plus → échec au doFinal.
		String tampered = enc.substring(0, enc.length() - 2) + (enc.endsWith("a") ? "b" : "a");
		assertThatThrownBy(() -> svc.decrypt(tampered))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Déchiffrement");
	}
}
