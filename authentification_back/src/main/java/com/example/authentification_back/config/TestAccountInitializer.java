package com.example.authentification_back.config;

import com.example.authentification_back.entity.User;
import com.example.authentification_back.repository.UserRepository;
import com.example.authentification_back.security.PasswordEncryptionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Compte de démonstration (même email / mot de passe que les exemples du cours).
 */
@Component
public class TestAccountInitializer implements CommandLineRunner {

	public static final String TEST_EMAIL = "toto@example.com";

	public static final String TEST_PASSWORD_PLAIN = "Pwd1234!abcd";

	private final UserRepository userRepository;
	private final PasswordEncryptionService passwordEncryptionService;

	public TestAccountInitializer(UserRepository userRepository, PasswordEncryptionService passwordEncryptionService) {
		this.userRepository = userRepository;
		this.passwordEncryptionService = passwordEncryptionService;
	}

	@Override
	public void run(String... args) {
		if (userRepository.existsByEmail(TEST_EMAIL)) {
			return;
		}
		User user = new User();
		user.setEmail(TEST_EMAIL);
		user.setPasswordEncrypted(passwordEncryptionService.encrypt(TEST_PASSWORD_PLAIN));
		userRepository.save(user);
	}
}
