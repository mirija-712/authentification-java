-- TP2 — Schéma MySQL (nouvelle installation)
-- Port : voir application.properties (ex. 3307)

CREATE DATABASE IF NOT EXISTS authentification
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE authentification;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(80) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  token VARCHAR(64) DEFAULT NULL,
  failed_login_attempts INT NOT NULL DEFAULT 0,
  lock_until TIMESTAMP(6) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_token (token)
) ENGINE=InnoDB;
