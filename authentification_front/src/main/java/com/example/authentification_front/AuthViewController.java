package com.example.authentification_front;

import com.example.authentification_front.api.ApiResult;
import com.example.authentification_front.api.AuthApiClient;
import com.example.authentification_front.api.AuthApiClient.MessageDto;
import com.example.authentification_front.api.AuthApiClient.UserDto;
import com.example.authentification_front.policy.ClientPasswordPolicy;
import com.example.authentification_front.policy.PasswordStrength;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Contrôleur FXML : liaison UI ↔ {@link AuthApiClient}.
 */
public class AuthViewController {

	@FXML
	TextField apiBaseUrl;
	@FXML
	CheckBox showPasswords;
	@FXML
	TextField loginEmail;
	@FXML
	PasswordField loginPassword;
	@FXML
	TextField loginPasswordPlain;
	@FXML
	Label loginMessage;
	@FXML
	TextField regEmail;
	@FXML
	PasswordField regPassword;
	@FXML
	TextField regPasswordPlain;
	@FXML
	PasswordField regConfirm;
	@FXML
	TextField regConfirmPlain;
	@FXML
	Label strengthLabel;
	@FXML
	Label registerMessage;
	@FXML
	TextArea profileArea;
	@FXML
	Label profileHint;
	@FXML
	PasswordField changeOldPassword;
	@FXML
	TextField changeOldPlain;
	@FXML
	PasswordField changeNewPassword;
	@FXML
	TextField changeNewPlain;
	@FXML
	PasswordField changeConfirmPassword;
	@FXML
	TextField changeConfirmPlain;
	@FXML
	Label changePasswordMessage;

	private AuthApiClient client;
	private String authToken;

	@FXML
	private void initialize() {
		apiBaseUrl.setText("http://localhost:8080");
		client = new AuthApiClient(apiBaseUrl.getText());
		apiBaseUrl.textProperty().addListener((obs, o, n) -> client = new AuthApiClient(n));
		showPasswords.selectedProperty().addListener((obs, o, show) -> applyPasswordVisibility(show));
		regPassword.textProperty().addListener((obs, o, n) -> updateStrengthLabel());
		regPasswordPlain.textProperty().addListener((obs, o, n) -> updateStrengthLabel());
		regConfirm.textProperty().addListener((obs, o, n) -> updateStrengthLabel());
		regConfirmPlain.textProperty().addListener((obs, o, n) -> updateStrengthLabel());
		updateStrengthLabel();
		profileArea.setText("Non connecté (aucun jeton).");
		changePasswordMessage.setText("Connectez-vous pour activer le changement de mot de passe.");
		changePasswordMessage.setStyle("-fx-text-fill: #666;");
	}

	private void applyPasswordVisibility(boolean showPlain) {
		applyPasswordPair(loginPassword, loginPasswordPlain, showPlain);
		applyPasswordPair(regPassword, regPasswordPlain, showPlain);
		applyPasswordPair(regConfirm, regConfirmPlain, showPlain);
		applyPasswordPair(changeOldPassword, changeOldPlain, showPlain);
		applyPasswordPair(changeNewPassword, changeNewPlain, showPlain);
		applyPasswordPair(changeConfirmPassword, changeConfirmPlain, showPlain);
		updateStrengthLabel();
	}

	private static void applyPasswordPair(PasswordField hidden, TextField plain, boolean showPlain) {
		if (showPlain) {
			plain.setText(hidden.getText());
			hidden.setVisible(false);
			hidden.setManaged(false);
			plain.setVisible(true);
			plain.setManaged(true);
		} else {
			hidden.setText(plain.getText());
			plain.setVisible(false);
			plain.setManaged(false);
			hidden.setVisible(true);
			hidden.setManaged(true);
		}
	}

	private String passwordText(PasswordField hidden, TextField plain) {
		return showPasswords.isSelected() ? plain.getText() : hidden.getText();
	}

	private void updateStrengthLabel() {
		String p = passwordText(regPassword, regPasswordPlain);
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
	void onLogin() {
		client = new AuthApiClient(apiBaseUrl.getText());
		loginMessage.setStyle("-fx-text-fill: red;");
		ApiResult<UserDto> r = client.login(loginEmail.getText(), passwordText(loginPassword, loginPasswordPlain));
		if (r instanceof ApiResult.Ok<?> ok) {
			UserDto u = (UserDto) ok.value();
			authToken = u.token;
			loginMessage.setText("Connecté (TP3 : HMAC email:nonce:timestamp). ID=" + u.id + " — jeton pour /api/me.");
			loginMessage.setStyle("-fx-text-fill: green;");
			profileHint.setText("Jeton actif : utilisez « Rafraîchir le profil ».");
			fillProfileArea(u);
		} else if (r instanceof ApiResult.Err<?> err) {
			loginMessage.setText(err.message() + (err.httpStatus() > 0 ? " (HTTP " + err.httpStatus() + ")" : ""));
		}
	}

	@FXML
	void onRegister() {
		client = new AuthApiClient(apiBaseUrl.getText());
		registerMessage.setStyle("-fx-text-fill: red;");
		ApiResult<UserDto> r = client.register(regEmail.getText(), passwordText(regPassword, regPasswordPlain),
				passwordText(regConfirm, regConfirmPlain));
		if (r instanceof ApiResult.Ok<?> ok) {
			UserDto u = (UserDto) ok.value();
			registerMessage.setText("Inscription réussie. ID=" + u.id + " — connectez-vous dans l'onglet Connexion.");
			registerMessage.setStyle("-fx-text-fill: green;");
		} else if (r instanceof ApiResult.Err<?> err) {
			registerMessage.setText(err.message() + (err.httpStatus() > 0 ? " (HTTP " + err.httpStatus() + ")" : ""));
		}
	}

	@FXML
	void onRefreshProfile() {
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
	void onLogout() {
		authToken = null;
		profileArea.setText("Déconnecté : jeton oublié côté client.");
		profileHint.setText("Reconnectez-vous pour obtenir un nouveau jeton.");
		changePasswordMessage.setText("Déconnecté : reconnectez-vous avant de changer le mot de passe.");
		changePasswordMessage.setStyle("-fx-text-fill: #666;");
	}

	@FXML
	void onChangePassword() {
		client = new AuthApiClient(apiBaseUrl.getText());
		changePasswordMessage.setStyle("-fx-text-fill: red;");
		if (authToken == null || authToken.isBlank()) {
			changePasswordMessage.setText("Impossible : aucun jeton actif. Connectez-vous d'abord.");
			return;
		}
		ApiResult<MessageDto> r = client.changePassword(authToken, passwordText(changeOldPassword, changeOldPlain),
				passwordText(changeNewPassword, changeNewPlain),
				passwordText(changeConfirmPassword, changeConfirmPlain));
		if (r instanceof ApiResult.Ok<?> ok) {
			MessageDto body = (MessageDto) ok.value();
			changePasswordMessage.setText(
					body != null && body.message != null && !body.message.isBlank() ? body.message : "Mot de passe changé.");
			changePasswordMessage.setStyle("-fx-text-fill: green;");
			clearPasswordPair(changeOldPassword, changeOldPlain);
			clearPasswordPair(changeNewPassword, changeNewPlain);
			clearPasswordPair(changeConfirmPassword, changeConfirmPlain);
		} else if (r instanceof ApiResult.Err<?> err) {
			changePasswordMessage.setText(err.message() + (err.httpStatus() > 0 ? " (HTTP " + err.httpStatus() + ")" : ""));
		}
	}

	private static void clearPasswordPair(PasswordField hidden, TextField plain) {
		hidden.clear();
		plain.clear();
	}

	private void fillProfileArea(UserDto u) {
		StringBuilder sb = new StringBuilder();
		sb.append("id: ").append(u.id).append("\n");
		sb.append("email: ").append(u.email).append("\n");
		sb.append("createdAt: ").append(u.createdAt != null ? u.createdAt : "—").append("\n");
		profileArea.setText(sb.toString());
	}
}
