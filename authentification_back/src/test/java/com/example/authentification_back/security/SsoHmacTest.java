package com.example.authentification_back.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
	void constantTimeEqualsHex_matches_equal_tags() {
		String m = SsoHmac.messageToSign("x@y.z", "n", 1L);
		String h = SsoHmac.hmacSha256Hex("k", m);
		assertThat(SsoHmac.constantTimeEqualsHex(h, h)).isTrue();
		assertThat(SsoHmac.constantTimeEqualsHex(h, h + "0")).isFalse();
	}
}
