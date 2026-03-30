package com.example.authentification.common.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests unitaires du module partagé (évite duplication front/back pour Sonar).
 */
class SsoHmacTest {

	@Test
	void messageToSign_joins_with_colons() {
		assertThat(SsoHmac.messageToSign("a@b.c", "nonce-1", 1700000000L))
				.isEqualTo("a@b.c:nonce-1:1700000000");
	}

	@Test
	void hmacSha256Hex_is_deterministic() {
		String m = SsoHmac.messageToSign("u@e.com", "n", 42L);
		String a = SsoHmac.hmacSha256Hex("secret", m);
		String b = SsoHmac.hmacSha256Hex("secret", m);
		assertThat(a).isEqualTo(b).hasSize(64);
	}

	@Test
	void hmacSha256Hex_changesWhenMessageChanges() {
		String password = "Pwd1234!abcd";
		String h1 = SsoHmac.hmacSha256Hex(password, "alice@example.com:n1:1710000000");
		String h2 = SsoHmac.hmacSha256Hex(password, "alice@example.com:n2:1710000000");
		assertNotEquals(h1, h2);
	}

	@Test
	void constantTimeEqualsHex_matches_equal_tags() {
		String m = SsoHmac.messageToSign("x@y.z", "n", 1L);
		String h = SsoHmac.hmacSha256Hex("k", m);
		assertThat(SsoHmac.constantTimeEqualsHex(h, h)).isTrue();
		assertThat(SsoHmac.constantTimeEqualsHex(h, h + "0")).isFalse();
	}

	@Test
	void constantTimeEqualsHex_edgeCases() {
		String hex = SsoHmac.hmacSha256Hex("Pwd1234!abcd", "alice@example.com:n1:1710000000");
		assertTrue(SsoHmac.constantTimeEqualsHex(hex, hex));
		String h2 = SsoHmac.hmacSha256Hex("Pwd1234!abcd", "alice@example.com:n2:1710000000");
		assertFalse(SsoHmac.constantTimeEqualsHex(hex, h2));
		assertFalse(SsoHmac.constantTimeEqualsHex(hex, null));
		assertFalse(SsoHmac.constantTimeEqualsHex("zzzz", "zzzz"));
		assertFalse(SsoHmac.constantTimeEqualsHex("ab", "abc"));
	}
}
