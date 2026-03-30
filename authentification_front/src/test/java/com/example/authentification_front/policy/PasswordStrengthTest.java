package com.example.authentification_front.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PasswordStrengthTest {

	@Test
	void enumValues_areStable() {
		assertEquals(3, PasswordStrength.values().length);
		assertEquals(PasswordStrength.RED, PasswordStrength.valueOf("RED"));
		assertEquals(PasswordStrength.ORANGE, PasswordStrength.valueOf("ORANGE"));
		assertEquals(PasswordStrength.GREEN, PasswordStrength.valueOf("GREEN"));
	}
}
