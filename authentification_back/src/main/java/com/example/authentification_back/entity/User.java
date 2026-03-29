package com.example.authentification_back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entité JPA mappée sur la table {@code users} (MySQL).
 * <p>
 * Cette implémentation est volontairement dangereuse et ne doit jamais être utilisée en production
 * (mot de passe stocké en clair). Le jeton d'accès est stocké en base après login (variante TP1).
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** Identifiant de connexion ; unique côté base. */
	@Column(nullable = false, unique = true, length = 255)
	private String email;

	/** Mot de passe en clair — uniquement pour le TP1 pédagogique. */
	@Column(name = "password_clear", nullable = false)
	private String passwordClear;

	/** Horodatage de création du compte (renseigné dans {@link #prePersist} si absent). */
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	/**
	 * Jeton généré au login (UUID) ; {@code null} tant que l'utilisateur ne s'est pas connecté.
	 * Unique lorsqu'il est non nul.
	 */
	@Column(unique = true, length = 64)
	private String token;

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordClear() {
		return passwordClear;
	}

	public void setPasswordClear(String passwordClear) {
		this.passwordClear = passwordClear;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
