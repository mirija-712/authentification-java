package com.example.authentification_front;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Application JavaFX (TP2) : interface vers l'API {@code authentification_back}.
 */
public class AuthApplication extends Application {

	@Override
	public void start(Stage stage) throws IOException {
		FXMLLoader fxmlLoader = new FXMLLoader(AuthApplication.class.getResource("auth-view.fxml"));
		Scene scene = new Scene(fxmlLoader.load(), 720, 520);
		stage.setTitle("Authentification Front");
		stage.setScene(scene);
		stage.setMinWidth(640);
		stage.setMinHeight(480);
		stage.show();
	}
}
