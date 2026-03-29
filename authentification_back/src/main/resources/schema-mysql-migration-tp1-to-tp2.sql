-- Migration manuelle TP1 → TP2 (données existantes avec password_clear)
-- À adapter : sauvegarder les comptes, réinitialiser les mots de passe ou migrer hors ligne.
-- Après migration : les anciennes valeurs en clair ne peuvent pas devenir des hashes BCrypt sans resaisie.

USE authentification;

-- À exécuter uniquement si vous partez encore du schéma TP1 :
-- ALTER TABLE users DROP COLUMN password_clear;
-- ALTER TABLE users ADD COLUMN password_hash VARCHAR(80) NOT NULL;
-- ALTER TABLE users ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0;
-- ALTER TABLE users ADD COLUMN lock_until TIMESTAMP(6) DEFAULT NULL;

-- En pratique souvent : DROP / recréer la table en dev ou exécuter le schema-mysql.sql TP2 sur une base vide.
