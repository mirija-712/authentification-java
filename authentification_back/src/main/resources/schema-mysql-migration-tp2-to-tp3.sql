-- TP3 — migration depuis une base TP2 (MySQL) vers password_encrypted + auth_nonce
--
-- Le schéma cible est décrit dans schema-mysql.sql (users.password_encrypted, table auth_nonce).
--
-- Limite : un hash BCrypt (TP2) ne permet pas de calculer password_encrypted (chiffré SMK) sans le mot de passe
-- en clair. En développement, recréer la base avec schema-mysql.sql est souvent le plus simple.
-- En production : réinitialisation des mots de passe ou script de migration hors ligne avec resaisie.

USE authentification;

-- 1) Anti-rejeu (user_id, nonce) — même définition que schema-mysql.sql
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

-- 2) Bascule users : password_hash (TP2) → password_encrypted (TP3)
--    À exécuter seulement si la colonne password_hash existe encore (adapter les noms si besoin).
--
-- ALTER TABLE users ADD COLUMN password_encrypted TEXT NULL AFTER email;
-- -- Renseigner password_encrypted pour chaque ligne (impossible depuis seul le hash sans mot de passe).
-- ALTER TABLE users DROP COLUMN password_hash;
-- ALTER TABLE users MODIFY COLUMN password_encrypted TEXT NOT NULL;

-- 3) Nettoyage d’éventuels résidus d’anciens brouillons (à lancer manuellement si ces objets existent)
-- DROP TABLE IF EXISTS login_nonces;
-- ALTER TABLE users DROP COLUMN auth_salt;
-- ALTER TABLE users DROP COLUMN identity_fingerprint;
