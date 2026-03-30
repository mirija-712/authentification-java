package com.example.authentification_front.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthApiClientTest {

	@Test
	void normalizeBase_returnsDefaultForNullOrBlank() {
		assertEquals("http://localhost:8080", AuthApiClient.normalizeBase(null));
		assertEquals("http://localhost:8080", AuthApiClient.normalizeBase(""));
		assertEquals("http://localhost:8080", AuthApiClient.normalizeBase("   "));
	}

	@Test
	void normalizeBase_trimsAndRemovesTrailingSlash() {
		assertEquals("http://localhost:8080", AuthApiClient.normalizeBase(" http://localhost:8080/ "));
		assertEquals("https://api.example.com", AuthApiClient.normalizeBase("https://api.example.com/"));
	}

	@Test
	void constructor_usesNormalizedBaseUrl() {
		AuthApiClient client = new AuthApiClient(" http://localhost:8080/ ");
		assertNotNull(client);
	}
}
