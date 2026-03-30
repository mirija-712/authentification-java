package com.example.authentification_back;

import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TP4 — test d’intégration « refus de démarrage ».
 * <p>
 * Si {@code APP_MASTER_KEY} est vide, le bean {@link com.example.authentification_back.security.PasswordEncryptionService}
 * lève une {@link IllegalStateException} : le contexte Spring ne peut pas se construire.
 * <p>
 * On force une clé vide via les <b>arguments de ligne de commande</b> Spring Boot (priorité maximale sur le fichier
 * {@code application-test.properties} et sur les variables d’environnement du poste de développement).
 */
class ApplicationMasterKeyStartupTest {

	@Test
	void applicationRefusesToStartWhenAppMasterKeyBlank() {
		SpringApplication app = new SpringApplication(AuthentificationBackApplication.class);
		app.setBannerMode(Banner.Mode.OFF);
		assertThatThrownBy(() -> app.run(
				// Simule une Master Key absente / vide (énoncé TP4).
				"--APP_MASTER_KEY=",
				// H2 en mémoire comme en CI : évite de dépendre de MySQL pour ce test.
				"--spring.profiles.active=test",
				// Pas besoin d’embarquer Tomcat pour valider l’échec du contexte.
				"--spring.main.web-application-type=none"))
				.rootCause()
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("APP_MASTER_KEY");
	}
}
