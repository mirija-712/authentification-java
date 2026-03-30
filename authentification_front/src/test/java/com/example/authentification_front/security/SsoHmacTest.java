package com.example.authentification_front.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SsoHmacTest {

	@Test
	void messageToSign_buildsExpectedFormat() {
		String message = SsoHmac.messageToSign("alice@example.com", "abc-123", 1710000000L);
		assertEquals("alice@example.com:abc-123:1710000000", message);
	}

	@Test
	void hmacSha256Hex_isDeterministicForSameInputs() {
		String password = "Pwd1234!abcd";
		String message = "alice@example.com:nonce:1710000000";

		String h1 = SsoHmac.hmacSha256Hex(password, message);
		String h2 = SsoHmac.hmacSha256Hex(password, message);

		assertEquals(h1, h2);
		assertEquals(64, h1.length());
	}

	@Test
	void hmacSha256Hex_changesWhenMessageChanges() {
		String password = "Pwd1234!abcd";
		String h1 = SsoHmac.hmacSha256Hex(password, "alice@example.com:n1:1710000000");
		String h2 = SsoHmac.hmacSha256Hex(password, "alice@example.com:n2:1710000000");

		assertNotEquals(h1, h2);
	}

	@Test
	void constantTimeEqualsHex_returnsTrueForSameHex() {
		String hex = SsoHmac.hmacSha256Hex("Pwd1234!abcd", "alice@example.com:n1:1710000000");
		assertTrue(SsoHmac.constantTimeEqualsHex(hex, hex));
	}

	@Test
	void constantTimeEqualsHex_returnsFalseForDifferentOrInvalidHex() {
		String h1 = SsoHmac.hmacSha256Hex("Pwd1234!abcd", "alice@example.com:n1:1710000000");
		String h2 = SsoHmac.hmacSha256Hex("Pwd1234!abcd", "alice@example.com:n2:1710000000");

		assertFalse(SsoHmac.constantTimeEqualsHex(h1, h2));
		assertFalse(SsoHmac.constantTimeEqualsHex(h1, null));
		assertFalse(SsoHmac.constantTimeEqualsHex("zzzz", "zzzz"));
		assertFalse(SsoHmac.constantTimeEqualsHex("ab", "abc"));
	}
}
