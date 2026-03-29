package com.example.authentification_back.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Paramètres anti brute-force (TP2) : durée de blocage et nombre d'échecs avant verrouillage.
 * <p>
 * TP2 (lockout) + TP3 (durée de validité du nonce challenge).
 */
@ConfigurationProperties(prefix = "app.auth")
public class AuthSecurityProperties {

	/** Durée pendant laquelle le compte reste verrouillé après trop d'échecs (défaut : 2 minutes, énoncé TP2). */
	private Duration lockDuration = Duration.ofMinutes(2);

	/** Nombre maximum de mots de passe incorrects avant blocage (défaut : 5, énoncé TP2). */
	private int maxFailedAttempts = 5;

	/** Durée de validité d’un nonce TP3 après {@code POST /api/auth/challenge}. */
	private Duration challengeTtl = Duration.ofMinutes(5);

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

	public Duration getChallengeTtl() {
		return challengeTtl;
	}

	public void setChallengeTtl(Duration challengeTtl) {
		this.challengeTtl = challengeTtl;
	}
}
