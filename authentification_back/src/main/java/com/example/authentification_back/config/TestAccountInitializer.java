package com.example.authentification_back.config;

import com.example.authentification_back.entity.User;
import com.example.authentification_back.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Compte de démonstration pour les tests et Postman. En TP2 le mot de passe doit respecter la politique stricte ;
 * l'email reste celui de l'énoncé ({@value #TEST_EMAIL}).
 */
@Component
public class TestAccountInitializer implements CommandLineRunner {

	public static final String TEST_EMAIL = "toto@example.com";

	/** Mot de passe conforme TP2 (12+ car., maj, min, chiffre, spécial) — à utiliser dans Postman pour ce compte. */
	public static final String TEST_PASSWORD_PLAIN = "Pwd1234!abcd";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public TestAccountInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public void run(String... args) {
		if (userRepository.existsByEmail(TEST_EMAIL)) {
			return;
		}
		User user = new User();
		user.setEmail(TEST_EMAIL);
		user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD_PLAIN));
		userRepository.save(user);
	}
}
