package com.example.authentification_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Point d'entrée Spring Boot du module TP1 (API REST + JPA + MySQL).
 * <p>
 * Le scan des composants couvre ce package et les sous-packages ({@code controller}, {@code service},
 * {@code repository}, etc.).
 */
@SpringBootApplication
public class AuthentificationBackApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthentificationBackApplication.class, args);
	}
}
