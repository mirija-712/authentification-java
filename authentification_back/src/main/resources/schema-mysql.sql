-- TP1 — À exécuter dans MySQL Workbench (base + table minimale)
-- Port MySQL : 3307 (voir application.properties)

CREATE DATABASE IF NOT EXISTS authentification
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE authentification;

CREATE TABLE IF NOT EXISTS users (
  id BIGINT NOT NULL AUTO_INCREMENT,
  email VARCHAR(255) NOT NULL,
  password_clear VARCHAR(255) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL,
  token VARCHAR(64) DEFAULT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  UNIQUE KEY uk_users_token (token)
) ENGINE=InnoDB;

-- Si la table existait déjà sans colonne token :
-- ALTER TABLE users ADD COLUMN token VARCHAR(64) NULL;
-- CREATE UNIQUE INDEX uk_users_token ON users (token);
