package com.example.authentification_back.security;

import org.junit.jupiter.api.Test;

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
