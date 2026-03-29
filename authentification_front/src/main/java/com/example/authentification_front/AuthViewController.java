package com.example.authentification_front;

import com.example.authentification_front.api.ApiResult;
import com.example.authentification_front.api.AuthApiClient;
import com.example.authentification_front.api.AuthApiClient.UserDto;
import com.example.authentification_front.policy.ClientPasswordPolicy;
import com.example.authentification_front.policy.PasswordStrength;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Contrôleur FXML : liaison UI ↔ {@link AuthApiClient}.
 */
public class AuthViewController {

	@FXML
	private TextField apiBaseUrl;
	@FXML
	private TextField loginEmail;
	@FXML
	private PasswordField loginPassword;
	@FXML
	private Label loginMessage;
	@FXML
	private TextField regEmail;
	@FXML
	private PasswordField regPassword;
	@FXML
	private PasswordField regConfirm;
	@FXML
	private Label strengthLabel;
	@FXML
	private Label registerMessage;
	@FXML
	private TextArea profileArea;
	@FXML
	private Label profileHint;

	private AuthApiClient client;
	private String authToken;

	@FXML
	private void initialize() {
		apiBaseUrl.setText("http://localhost:8080");
		client = new AuthApiClient(apiBaseUrl.getText());
		apiBaseUrl.textProperty().addListener((obs, o, n) -> client = new AuthApiClient(n));
		regPassword.textProperty().addListener((obs, o, n) -> updateStrengthLabel());
		regConfirm.textProperty().addListener((obs, o, n) -> updateStrengthLabel());
		updateStrengthLabel();
		profileArea.setText("Non connecté (aucun jeton).");
	}

	private void updateStrengthLabel() {
		String p = regPassword.getText();
		PasswordStrength s = ClientPasswordPolicy.evaluateStrength(p);
		switch (s) {
			case RED -> {
				strengthLabel.setText("Force : non conforme (rouge) — min. " + ClientPasswordPolicy.MIN_LENGTH
						+ " car., maj, min, chiffre, spécial");
				strengthLabel.setStyle("-fx-text-fill: firebrick; -fx-font-weight: bold;");
			}
			case ORANGE -> {
				strengthLabel.setText("Force : conforme mais faible (orange)");
				strengthLabel.setStyle("-fx-text-fill: darkorange; -fx-font-weight: bold;");
			}
			case GREEN -> {
				strengthLabel.setText("Force : conforme — bon niveau (vert)");
				strengthLabel.setStyle("-fx-text-fill: forestgreen; -fx-font-weight: bold;");
			}
		}
	}

	@FXML
	private void onLogin() {
		client = new AuthApiClient(apiBaseUrl.getText());
		loginMessage.setStyle("-fx-text-fill: red;");
		ApiResult<UserDto> r = client.loginWithTp3(loginEmail.getText(), loginPassword.getText());
		if (r instanceof ApiResult.Ok<?> ok) {
			UserDto u = (UserDto) ok.value();
			authToken = u.token;
			loginMessage.setText("Connecté (TP3 : challenge + HMAC). ID=" + u.id + " — jeton pour /api/me.");
			loginMessage.setStyle("-fx-text-fill: green;");
			profileHint.setText("Jeton actif : utilisez « Rafraîchir le profil ».");
			fillProfileArea(u);
		} else if (r instanceof ApiResult.Err<?> err) {
			loginMessage.setText(err.message() + (err.httpStatus() > 0 ? " (HTTP " + err.httpStatus() + ")" : ""));
		}
	}

	@FXML
	private void onRegister() {
		client = new AuthApiClient(apiBaseUrl.getText());
		registerMessage.setStyle("-fx-text-fill: red;");
		ApiResult<UserDto> r = client.register(regEmail.getText(), regPassword.getText(), regConfirm.getText());
		if (r instanceof ApiResult.Ok<?> ok) {
			UserDto u = (UserDto) ok.value();
			registerMessage.setText("Inscription réussie. ID=" + u.id + " — connectez-vous dans l'onglet Connexion.");
			registerMessage.setStyle("-fx-text-fill: green;");
		} else if (r instanceof ApiResult.Err<?> err) {
			registerMessage.setText(err.message() + (err.httpStatus() > 0 ? " (HTTP " + err.httpStatus() + ")" : ""));
		}
	}

	@FXML
	private void onRefreshProfile() {
		client = new AuthApiClient(apiBaseUrl.getText());
		if (authToken == null || authToken.isBlank()) {
			profileArea.setText("Pas de jeton : connectez-vous d'abord.");
			return;
		}
		ApiResult<UserDto> r = client.me(authToken);
		if (r instanceof ApiResult.Ok<?> ok) {
			fillProfileArea((UserDto) ok.value());
			profileHint.setText("Profil à jour.");
		} else if (r instanceof ApiResult.Err<?> err) {
			profileArea.setText("Erreur : " + err.message() + " (HTTP " + err.httpStatus() + ")");
		}
	}

	@FXML
	private void onLogout() {
		authToken = null;
		profileArea.setText("Déconnecté : jeton oublié côté client.");
		profileHint.setText("Reconnectez-vous pour obtenir un nouveau jeton.");
	}

	private void fillProfileArea(UserDto u) {
		StringBuilder sb = new StringBuilder();
		sb.append("id: ").append(u.id).append("\n");
		sb.append("email: ").append(u.email).append("\n");
		sb.append("createdAt: ").append(u.createdAt != null ? u.createdAt : "—").append("\n");
		profileArea.setText(sb.toString());
	}
}
