package com.example.authentification_back.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Nonce à usage unique pour le login TP3 (anti-rejeu).
 */
@Entity
@Table(name = "login_nonces")
public class LoginNonce {

	@Id
	@Column(length = 64)
	private String nonce;

	@Column(nullable = false, length = 255)
	private String email;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private boolean consumed;

	public String getNonce() {
		return nonce;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Instant expiresAt) {
		this.expiresAt = expiresAt;
	}

	public boolean isConsumed() {
		return consumed;
	}

	public void setConsumed(boolean consumed) {
		this.consumed = consumed;
	}
}
