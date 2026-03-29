package com.example.authentification_back.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Lockout TP2 + fenêtre temporelle et TTL nonce TP3 (énoncé).
 */
@ConfigurationProperties(prefix = "app.auth")
public class AuthSecurityProperties {

	private Duration lockDuration = Duration.ofMinutes(2);

	private int maxFailedAttempts = 5;

	/** Fenêtre acceptée autour de l’heure serveur pour {@code timestamp} (± secondes, défaut 60). */
	private int timestampSkewSeconds = 60;

	/** Durée de conservation du nonce en base (≈ now + 2 min à l’énoncé). */
	private int nonceTtlSeconds = 120;

	public Duration getLockDuration() {
		return lockDuration;
	}

	public void setLockDuration(Duration lockDuration) {
		this.lockDuration = lockDuration;
	}

	public int getMaxFailedAttempts() {
		return maxFailedAttempts;
	}

	public void setMaxFailedAttempts(int maxFailedAttempts) {
		this.maxFailedAttempts = maxFailedAttempts;
	}

	public int getTimestampSkewSeconds() {
		return timestampSkewSeconds;
	}

	public void setTimestampSkewSeconds(int timestampSkewSeconds) {
		this.timestampSkewSeconds = timestampSkewSeconds;
	}

	public int getNonceTtlSeconds() {
		return nonceTtlSeconds;
	}

	public void setNonceTtlSeconds(int nonceTtlSeconds) {
		this.nonceTtlSeconds = nonceTtlSeconds;
	}
}
