package com.example.authentification_front.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsoHmacTest {

	@Test
	void messageToSign_joinsEmailNonceTimestamp() {
		assertEquals("user@x.y:abc-uuid:1700000000", SsoHmac.messageToSign("user@x.y", "abc-uuid", 1700000000L));
	}

	@Test
	void hmacSha256Hex_isDeterministicForSameInputs() {
		String h1 = SsoHmac.hmacSha256Hex("secret", "a:b:1");
		String h2 = SsoHmac.hmacSha256Hex("secret", "a:b:1");
		assertEquals(64, h1.length());
		assertEquals(h1, h2);
	}

	@Test
	void hmacSha256Hex_differsWhenMessageChanges() {
		String h1 = SsoHmac.hmacSha256Hex("secret", "a:b:1");
		String h2 = SsoHmac.hmacSha256Hex("secret", "a:b:2");
		assertFalse(h1.equals(h2));
	}

	@Test
	void constantTimeEqualsHex_nullOrLengthMismatch() {
		assertFalse(SsoHmac.constantTimeEqualsHex(null, "00"));
		assertFalse(SsoHmac.constantTimeEqualsHex("00", null));
		assertFalse(SsoHmac.constantTimeEqualsHex("00", "0000"));
	}

	@Test
	void constantTimeEqualsHex_invalidHex() {
		assertFalse(SsoHmac.constantTimeEqualsHex("gg", "gg"));
	}

	@Test
	void constantTimeEqualsHex_equalValues() {
		assertTrue(SsoHmac.constantTimeEqualsHex("0a1b", "0a1b"));
	}

	@Test
	void hmacSha256Hex_rejectsNullPassword() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> SsoHmac.hmacSha256Hex(null, "msg"));
		assertInstanceOf(NullPointerException.class, ex.getCause());
	}
}
