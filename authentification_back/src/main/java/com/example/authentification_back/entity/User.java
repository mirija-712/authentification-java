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
 * Entité utilisateur (TP2) : mot de passe haché avec BCrypt, compteur d'échecs et verrouillage temporaire.
 * <p>
 * TP2 améliore le stockage mais ne protège pas encore contre le rejeu (TP3). Le jeton reste une solution TP1/2 pragmatique.
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	/** Hachage BCrypt du mot de passe (jamais le mot de passe en clair). */
	@Column(name = "password_hash", nullable = false, length = 80)
	private String passwordHash;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(unique = true, length = 64)
	private String token;

	/** Nombre de tentatives de login incorrectes consécutives (remis à 0 après succès). */
	@Column(name = "failed_login_attempts", nullable = false)
	private int failedLoginAttempts = 0;

	/** Fin de période de blocage ; null si le compte n'est pas verrouillé. */
	@Column(name = "lock_until")
	private Instant lockUntil;

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

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
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

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public void setFailedLoginAttempts(int failedLoginAttempts) {
		this.failedLoginAttempts = failedLoginAttempts;
	}

	public Instant getLockUntil() {
		return lockUntil;
	}

	public void setLockUntil(Instant lockUntil) {
		this.lockUntil = lockUntil;
	}
}
