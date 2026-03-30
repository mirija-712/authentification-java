-- TP3/TP4 — schéma MySQL (users.password_encrypted = texte chiffré AES-GCM, format conseillé v1:Base64(iv):Base64(ciphertext), table auth_nonce)

CREATE DATABASE IF NOT EXISTS authentification
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE authentification;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  password_encrypted TEXT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  token VARCHAR(64) DEFAULT NULL,
  failed_login_attempts INT NOT NULL DEFAULT 0,
  lock_until TIMESTAMP(6) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_token (token)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS auth_nonce (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  nonce VARCHAR(128) NOT NULL,
  expires_at TIMESTAMP(6) NOT NULL,
  consumed BIT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_auth_nonce_user_nonce (user_id, nonce),
  KEY idx_auth_nonce_user (user_id)
) ENGINE=InnoDB;
