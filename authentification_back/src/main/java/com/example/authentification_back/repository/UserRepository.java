package com.example.authentification_back.repository;

import com.example.authentification_back.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Accès données pour {@link User} : requêtes dérivées Spring Data JPA.
 */
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	/** Utilisé pour authentifier les appels {@code GET /api/me} via le jeton persistant. */
	Optional<User> findByToken(String token);

	boolean existsByEmail(String email);
}
