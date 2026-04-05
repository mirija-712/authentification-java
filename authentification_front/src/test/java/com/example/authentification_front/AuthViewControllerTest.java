package com.example.authentification_front;

import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests du contrôleur FXML : nécessite l’initialisation du toolkit JavaFX.
 */
class AuthViewControllerTest {

	@BeforeAll
	static void initJavaFx() {
		try {
			Platform.startup(() -> {
			});
		} catch (IllegalStateException ignored) {
			// toolkit déjà démarré (ex. autre classe de test)
		}
	}

	private static void runOnFx(ThrowingRunnable action) throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Throwable> error = new AtomicReference<>();
		Platform.runLater(() -> {
			try {
				action.run();
			} catch (Throwable t) {
				error.set(t);
			} finally {
				latch.countDown();
			}
		});
		assertTrue(latch.await(30, TimeUnit.SECONDS), "JavaFX runLater timeout");
		if (error.get() != null) {
			Throwable t = error.get();
			if (t instanceof Exception e) {
				throw e;
			}
			throw new RuntimeException(t);
		}
	}

	@FunctionalInterface
	private interface ThrowingRunnable {
		void run() throws Exception;
	}

	private static AuthViewController loadController() throws Exception {
		AtomicReference<AuthViewController> ref = new AtomicReference<>();
		runOnFx(() -> {
			FXMLLoader loader = new FXMLLoader(AuthApplication.class.getResource("auth-view.fxml"));
			Parent root = loader.load();
			assertTrue(root != null);
			ref.set(loader.getController());
		});
		return ref.get();
	}

	@Test
	void loadFxml_initializesDefaults() throws Exception {
		AuthViewController c = loadController();
		runOnFx(() -> {
			assertEquals("http://localhost:8080", c.apiBaseUrl.getText());
			assertFalse(c.showPasswords.isSelected());
			assertTrue(c.loginPassword.isVisible());
			assertTrue(c.profileArea.getText().contains("Non connecté"));
		});
	}

	@Test
	void showPasswords_togglesLoginFields() throws Exception {
		AuthViewController c = loadController();
		runOnFx(() -> {
			c.loginPassword.setText("secret-pwd");
			c.showPasswords.setSelected(true);
			assertEquals("secret-pwd", c.loginPasswordPlain.getText());
			assertFalse(c.loginPassword.isVisible());
			assertTrue(c.loginPasswordPlain.isVisible());
			c.showPasswords.setSelected(false);
			assertEquals("secret-pwd", c.loginPassword.getText());
			assertTrue(c.loginPassword.isVisible());
		});
	}

	@Test
	void strengthLabel_redThenGreen() throws Exception {
		AuthViewController c = loadController();
		runOnFx(() -> {
			c.regPassword.setText("weak");
			assertTrue(c.strengthLabel.getText().contains("rouge"));
			c.regPassword.setText("Aa1!bbbbbbbbbbbb");
			assertTrue(c.strengthLabel.getText().contains("vert"));
		});
	}

	@Test
	void onRefreshProfile_withoutToken_setsMessage() throws Exception {
		AuthViewController c = loadController();
		runOnFx(() -> {
			c.onRefreshProfile();
			assertTrue(c.profileArea.getText().contains("Pas de jeton"));
		});
	}

	@Test
	void onChangePassword_withoutToken_setsMessage() throws Exception {
		AuthViewController c = loadController();
		runOnFx(() -> {
			c.onChangePassword();
			assertTrue(c.changePasswordMessage.getText().contains("jeton"));
		});
	}

	private static void writePostLogin(HttpServer server, int status, String jsonBody) {
		server.createContext("/api/auth/login", exchange -> {
			try {
				exchange.getRequestBody().readAllBytes();
				if (!"POST".equals(exchange.getRequestMethod())) {
					exchange.sendResponseHeaders(405, -1);
					return;
				}
				byte[] body = jsonBody.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
				exchange.sendResponseHeaders(status, body.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(body);
				}
			} finally {
				exchange.close();
			}
		});
	}

	private static void writePutChangePassword(HttpServer server, int status, String jsonBody) {
		server.createContext("/api/auth/change-password", exchange -> {
			try {
				exchange.getRequestBody().readAllBytes();
				if (!"PUT".equals(exchange.getRequestMethod())) {
					exchange.sendResponseHeaders(405, -1);
					return;
				}
				byte[] body = jsonBody.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
				exchange.sendResponseHeaders(status, body.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(body);
				}
			} finally {
				exchange.close();
			}
		});
	}

	@Test
	void onLogin_success_setsTokenAndProfile() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writePostLogin(server, 200,
				"{\"id\":9,\"email\":\"u@u.u\",\"createdAt\":null,\"token\":\"tok-ui\"}");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthViewController c = loadController();
			runOnFx(() -> {
				c.apiBaseUrl.setText("http://127.0.0.1:" + port);
				c.loginEmail.setText("u@u.u");
				c.loginPassword.setText("AnyPwd1!xxxx");
				c.onLogin();
				assertTrue(c.loginMessage.getText().contains("Connecté"));
				assertTrue(c.profileArea.getText().contains("u@u.u"));
			});
		} finally {
			server.stop(0);
		}
	}

	@Test
	void onLogin_error_showsMessage() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writePostLogin(server, 401, "{\"message\":\"Non autorisé\"}");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthViewController c = loadController();
			runOnFx(() -> {
				c.apiBaseUrl.setText("http://127.0.0.1:" + port);
				c.loginEmail.setText("x@y.z");
				c.loginPassword.setText("pwd");
				c.onLogin();
				assertTrue(c.loginMessage.getText().contains("Non autorisé"));
			});
		} finally {
			server.stop(0);
		}
	}

	@Test
	void onChangePassword_success_clearsFields() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writePostLogin(server, 200,
				"{\"id\":1,\"email\":\"a@a.a\",\"createdAt\":null,\"token\":\"t1\"}");
		writePutChangePassword(server, 200, "{\"message\":\"Mot de passe changé avec succès\"}");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthViewController c = loadController();
			runOnFx(() -> {
				c.apiBaseUrl.setText("http://127.0.0.1:" + port);
				c.loginEmail.setText("a@a.a");
				c.loginPassword.setText("Pwd1!xxxxxxxxx");
				c.onLogin();
				c.changeOldPassword.setText("old");
				c.changeNewPassword.setText("Nn1!nnnnnnnnnn");
				c.changeConfirmPassword.setText("Nn1!nnnnnnnnnn");
				c.onChangePassword();
				assertTrue(c.changePasswordMessage.getText().contains("succès"));
				assertEquals("", c.changeOldPassword.getText());
				assertEquals("", c.changeNewPassword.getText());
			});
		} finally {
			server.stop(0);
		}
	}

	@Test
	void onRefreshProfile_afterLogin_showsProfile() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writePostLogin(server, 200,
				"{\"id\":7,\"email\":\"p@p.p\",\"createdAt\":\"2026-01-01\",\"token\":\"t7\"}");
		server.createContext("/api/me", exchange -> {
			try {
				exchange.getRequestBody().readAllBytes();
				if (!"GET".equals(exchange.getRequestMethod())) {
					exchange.sendResponseHeaders(405, -1);
					return;
				}
				String json = "{\"id\":7,\"email\":\"p@p.p\",\"createdAt\":\"2026-01-01\"}";
				byte[] body = json.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
				exchange.sendResponseHeaders(200, body.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(body);
				}
			} finally {
				exchange.close();
			}
		});
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthViewController c = loadController();
			runOnFx(() -> {
				c.apiBaseUrl.setText("http://127.0.0.1:" + port);
				c.loginEmail.setText("p@p.p");
				c.loginPassword.setText("Pwd1!xxxxxxxxx");
				c.onLogin();
				c.onRefreshProfile();
				assertTrue(c.profileArea.getText().contains("p@p.p"));
				assertTrue(c.profileHint.getText().contains("Profil à jour"));
			});
		} finally {
			server.stop(0);
		}
	}

	@Test
	void onChangePassword_success_usesFallbackMessageWhenBodyEmpty() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writePostLogin(server, 200,
				"{\"id\":1,\"email\":\"a@a.a\",\"createdAt\":null,\"token\":\"t1\"}");
		writePutChangePassword(server, 200, "{}");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthViewController c = loadController();
			runOnFx(() -> {
				c.apiBaseUrl.setText("http://127.0.0.1:" + port);
				c.loginEmail.setText("a@a.a");
				c.loginPassword.setText("Pwd1!xxxxxxxxx");
				c.onLogin();
				c.changeOldPassword.setText("o");
				c.changeNewPassword.setText("Nn1!nnnnnnnnnn");
				c.changeConfirmPassword.setText("Nn1!nnnnnnnnnn");
				c.onChangePassword();
				assertEquals("Mot de passe changé.", c.changePasswordMessage.getText());
			});
		} finally {
			server.stop(0);
		}
	}

	@Test
	void onLogout_clearsTokenAndUpdatesMessages() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writePostLogin(server, 200,
				"{\"id\":1,\"email\":\"a@a.a\",\"createdAt\":null,\"token\":\"tok\"}");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthViewController c = loadController();
			runOnFx(() -> {
				c.apiBaseUrl.setText("http://127.0.0.1:" + port);
				c.loginEmail.setText("a@a.a");
				c.loginPassword.setText("Pwd1!xxxxxxxxx");
				c.onLogin();
				c.onLogout();
				assertTrue(c.changePasswordMessage.getText().contains("Reconnectez-vous"));
				c.onChangePassword();
				assertTrue(c.changePasswordMessage.getText().contains("jeton actif"));
			});
		} finally {
			server.stop(0);
		}
	}

	@Test
	void onRegister_success_setsMessage() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/api/auth/register", exchange -> {
			try {
				exchange.getRequestBody().readAllBytes();
				if (!"POST".equals(exchange.getRequestMethod())) {
					exchange.sendResponseHeaders(405, -1);
					return;
				}
				String json = "{\"id\":5,\"email\":\"n@n.n\",\"createdAt\":null,\"token\":null}";
				byte[] body = json.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
				exchange.sendResponseHeaders(201, body.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(body);
				}
			} finally {
				exchange.close();
			}
		});
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthViewController c = loadController();
			runOnFx(() -> {
				c.apiBaseUrl.setText("http://127.0.0.1:" + port);
				c.regEmail.setText("n@n.n");
				c.regPassword.setText("Aa1!bbbbbbbb");
				c.regConfirm.setText("Aa1!bbbbbbbb");
				c.onRegister();
				assertTrue(c.registerMessage.getText().contains("Inscription réussie"));
			});
		} finally {
			server.stop(0);
		}
	}
}
