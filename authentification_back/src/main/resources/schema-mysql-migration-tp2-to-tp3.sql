-- TP3 — migration depuis une base TP2 existante (MySQL)

USE authentification;

ALTER TABLE users
  ADD COLUMN auth_salt VARCHAR(64) DEFAULT NULL AFTER lock_until,
  ADD COLUMN identity_fingerprint VARCHAR(64) DEFAULT NULL AFTER auth_salt;

CREATE TABLE IF NOT EXISTS login_nonces (
  nonce VARCHAR(64) NOT NULL,
  email VARCHAR(255) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  consumed BOOLEAN NOT NULL DEFAULT FALSE,
  PRIMARY KEY (nonce),
  KEY idx_login_nonces_email (email)
) ENGINE=InnoDB;
