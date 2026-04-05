module com.example.authentification_front {
	requires javafx.controls;
	requires javafx.fxml;
	/* Platform, Application, Stage — utilisés par l’app et les tests JavaFX (AuthViewControllerTest). */
	requires javafx.graphics;
	requires java.net.http;
	/* Gson est un module nommé (JAR 2.10+) : obligatoire pour compiler AuthApiClient. */
	requires transitive com.google.gson;
	requires jdk.httpserver;

	opens com.example.authentification_front to javafx.fxml;
	/* Gson accède par réflexion aux champs des DTO (UserDto, ErrorBody, etc.). */
	opens com.example.authentification_front.api to com.google.gson;
	exports com.example.authentification_front;
	exports com.example.authentification_front.api;
	exports com.example.authentification_front.policy;
}
