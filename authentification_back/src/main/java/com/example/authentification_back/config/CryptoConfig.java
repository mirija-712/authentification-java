package com.example.authentification_back.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Horloge injectable (tests du verrouillage).
 */
@Configuration
@EnableConfigurationProperties(AuthSecurityProperties.class)
public class CryptoConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
