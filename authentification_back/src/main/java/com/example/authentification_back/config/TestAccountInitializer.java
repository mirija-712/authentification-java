package com.example.authentification_back.config;

import com.example.authentification_back.entity.User;
import com.example.authentification_back.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Initialise le compte de test **obligatoire** du TP1 au démarrage de l'application.
 * <p>
 * Si aucun utilisateur avec l'email {@value #TEST_EMAIL} n'existe, il est créé avec le mot de passe
 * {@value #TEST_PASSWORD}. Cela permet de tester immédiatement avec Postman sans script SQL manuel.
 * <p>
 * Cette implémentation est volontairement dangereuse et ne doit jamais être utilisée en production
 * (mot de passe faible et stocké en clair — voir entité {@link User}).
 */
@Component
public class TestAccountInitializer implements CommandLineRunner {

	/** Email du compte de démonstration imposé par l'énoncé TP1. */
	public static final String TEST_EMAIL = "toto@example.com";

	/** Mot de passe du compte de démonstration (stocké en clair en base — TP1 uniquement). */
	public static final String TEST_PASSWORD = "pwd1234";

	private final UserRepository userRepository;

	public TestAccountInitializer(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public void run(String... args) {
		if (userRepository.existsByEmail(TEST_EMAIL)) {
			return;
		}
		User user = new User();
		user.setEmail(TEST_EMAIL);
		user.setPasswordClear(TEST_PASSWORD);
		userRepository.save(user);
	}
}
