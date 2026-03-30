package com.example.authentification_front.policy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClientPasswordPolicyTest {

	@Test
	void isCompliant_returnsFalseForNullOrTooShort() {
		assertFalse(ClientPasswordPolicy.isCompliant(null));
		assertFalse(ClientPasswordPolicy.isCompliant(""));
		assertFalse(ClientPasswordPolicy.isCompliant("Aa1!short"));
	}

	@Test
	void isCompliant_enforcesUpperLowerDigitSpecial() {
		assertFalse(ClientPasswordPolicy.isCompliant("aa1!aaaaaaaa")); // no upper
		assertFalse(ClientPasswordPolicy.isCompliant("AA1!AAAAAAAA")); // no lower
		assertFalse(ClientPasswordPolicy.isCompliant("Aa!!aaaaaaaa")); // no digit
		assertFalse(ClientPasswordPolicy.isCompliant("Aa1aaaaaaaaa")); // no special
	}

	@Test
	void isCompliant_returnsTrueWhenAllRulesMatch() {
		assertTrue(ClientPasswordPolicy.isCompliant("Aa1!abcdefgh"));
	}

	@Test
	void evaluateStrength_returnsRedForNullEmptyOrNonCompliant() {
		assertEquals(PasswordStrength.RED, ClientPasswordPolicy.evaluateStrength(null));
		assertEquals(PasswordStrength.RED, ClientPasswordPolicy.evaluateStrength(""));
		assertEquals(PasswordStrength.RED, ClientPasswordPolicy.evaluateStrength("Aa1!short"));
	}

	@Test
	void evaluateStrength_returnsOrangeAtMinimumLength() {
		String minLenCompliant = "Aa1!abcdefgh"; // length 12
		assertEquals(12, minLenCompliant.length());
		assertEquals(PasswordStrength.ORANGE, ClientPasswordPolicy.evaluateStrength(minLenCompliant));
	}

	@Test
	void evaluateStrength_returnsGreenAboveMinimumLength() {
		assertEquals(PasswordStrength.GREEN, ClientPasswordPolicy.evaluateStrength("Aa1!abcdefghi"));
	}
}
