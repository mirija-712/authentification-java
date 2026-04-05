package com.example.authentification_front.api;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests HTTP réels via {@link HttpServer} (JDK) pour couvrir {@link AuthApiClient} sans backend Spring.
 */
class AuthApiClientHttpTest {

	private static void writeJson(HttpServer server, String path, int status, String jsonBody) {
		server.createContext(path, exchange -> {
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

	private static void writeGetJson(HttpServer server, String path, int status, String jsonBody) {
		server.createContext(path, exchange -> {
			try {
				exchange.getRequestBody().readAllBytes();
				if (!"GET".equals(exchange.getRequestMethod())) {
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

	private static void writePutJson(HttpServer server, String path, int status, String jsonBody) {
		server.createContext(path, exchange -> {
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
	void register_returnsOkOn201() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writeJson(server, "/api/auth/register", 201,
				"{\"id\":1,\"email\":\"moi@example.com\",\"createdAt\":\"2026-01-01\",\"token\":null}");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
			ApiResult<AuthApiClient.UserDto> r = client.register("moi@example.com", "Aa1!bbbbbbbb", "Aa1!bbbbbbbb");
			assertInstanceOf(ApiResult.Ok.class, r);
			AuthApiClient.UserDto u = ((ApiResult.Ok<AuthApiClient.UserDto>) r).value();
			assertEquals(1L, u.id);
			assertEquals("moi@example.com", u.email);
		} finally {
			server.stop(0);
		}
	}

	@Test
	void register_returnsErrWithApiMessageOnConflict() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writeJson(server, "/api/auth/register", 409, "{\"message\":\"Email déjà utilisé\"}");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
			ApiResult<AuthApiClient.UserDto> r = client.register("x@y.z", "Aa1!bbbbbbbb", "Aa1!bbbbbbbb");
			assertInstanceOf(ApiResult.Err.class, r);
			ApiResult.Err<AuthApiClient.UserDto> e = (ApiResult.Err<AuthApiClient.UserDto>) r;
			assertEquals(409, e.httpStatus());
			assertTrue(e.message().contains("Email"));
		} finally {
			server.stop(0);
		}
	}

	@Test
	void register_returnsErrWhenJsonInvalidOnSuccessStatus() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writeJson(server, "/api/auth/register", 201, "{not-json");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
			ApiResult<AuthApiClient.UserDto> r = client.register("a@b.c", "Aa1!bbbbbbbb", "Aa1!bbbbbbbb");
			assertInstanceOf(ApiResult.Err.class, r);
			assertTrue(((ApiResult.Err<?>) r).message().contains("JSON"));
		} finally {
			server.stop(0);
		}
	}

	@Test
	void login_returnsOkOn200() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writeJson(server, "/api/auth/login", 200,
				"{\"id\":2,\"email\":\"a@b.c\",\"createdAt\":null,\"token\":\"tok-123\"}");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
			ApiResult<AuthApiClient.UserDto> r = client.login("a@b.c", "Pwd1234!abcd");
			assertInstanceOf(ApiResult.Ok.class, r);
			assertEquals("tok-123", ((ApiResult.Ok<AuthApiClient.UserDto>) r).value().token);
		} finally {
			server.stop(0);
		}
	}

	@Test
	void me_returnsProfileOn200() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writeGetJson(server, "/api/me", 200, "{\"id\":3,\"email\":\"z@z.z\",\"createdAt\":\"x\"}");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
			ApiResult<AuthApiClient.UserDto> r = client.me("secret-token");
			assertInstanceOf(ApiResult.Ok.class, r);
			assertEquals("z@z.z", ((ApiResult.Ok<AuthApiClient.UserDto>) r).value().email);
		} finally {
			server.stop(0);
		}
	}

	@Test
	void me_returnsErrOn401WithJsonMessage() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writeGetJson(server, "/api/me", 401, "{\"message\":\"Jeton invalide\"}");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
			ApiResult<AuthApiClient.UserDto> r = client.me("bad");
			assertInstanceOf(ApiResult.Err.class, r);
			ApiResult.Err<AuthApiClient.UserDto> e = (ApiResult.Err<AuthApiClient.UserDto>) r;
			assertEquals(401, e.httpStatus());
			assertTrue(e.message().contains("Jeton"));
		} finally {
			server.stop(0);
		}
	}

	@Test
	void me_returnsNetworkErrWhenPortClosed() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		int port = server.getAddress().getPort();
		server.stop(0);
		AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
		ApiResult<AuthApiClient.UserDto> r = client.me("t");
		assertInstanceOf(ApiResult.Err.class, r);
		assertTrue(((ApiResult.Err<?>) r).message().contains("Erreur réseau"));
	}

	@Test
	void changePassword_returnsOkOn200() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writePutJson(server, "/api/auth/change-password", 200, "{\"message\":\"Mot de passe changé avec succès\"}");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
			ApiResult<AuthApiClient.MessageDto> r = client.changePassword("tok", "a", "b", "b");
			assertInstanceOf(ApiResult.Ok.class, r);
			assertEquals("Mot de passe changé avec succès", ((ApiResult.Ok<AuthApiClient.MessageDto>) r).value().message);
		} finally {
			server.stop(0);
		}
	}

	@Test
	void changePassword_returnsErrOn400() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writePutJson(server, "/api/auth/change-password", 400, "{\"message\":\"Ancien mot de passe incorrect\"}");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
			ApiResult<AuthApiClient.MessageDto> r = client.changePassword("tok", "x", "y", "y");
			assertInstanceOf(ApiResult.Err.class, r);
			assertEquals(400, ((ApiResult.Err<?>) r).httpStatus());
			assertTrue(((ApiResult.Err<?>) r).message().contains("Ancien"));
		} finally {
			server.stop(0);
		}
	}

	@Test
	void changePassword_returnsErrWhenJsonInvalidOn200() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		writePutJson(server, "/api/auth/change-password", 200, "{bad");
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
			ApiResult<AuthApiClient.MessageDto> r = client.changePassword("tok", "a", "b", "b");
			assertInstanceOf(ApiResult.Err.class, r);
			assertTrue(((ApiResult.Err<?>) r).message().contains("JSON"));
		} finally {
			server.stop(0);
		}
	}

	@Test
	void changePassword_returnsNetworkErrWhenPortClosed() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		int port = server.getAddress().getPort();
		server.stop(0);
		AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
		ApiResult<AuthApiClient.MessageDto> r = client.changePassword("t", "a", "b", "b");
		assertInstanceOf(ApiResult.Err.class, r);
		assertTrue(((ApiResult.Err<?>) r).message().contains("Erreur réseau"));
	}

	@Test
	void login_returnsErrWhenBodyTooLongWithoutJsonMessage() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		String longBody = "x".repeat(250);
		server.createContext("/api/auth/login", exchange -> {
			try {
				exchange.getRequestBody().readAllBytes();
				if (!"POST".equals(exchange.getRequestMethod())) {
					exchange.sendResponseHeaders(405, -1);
					return;
				}
				byte[] body = longBody.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
				exchange.sendResponseHeaders(500, body.length);
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
			AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
			ApiResult<AuthApiClient.UserDto> r = client.login("a@b.c", "pwd");
			assertInstanceOf(ApiResult.Err.class, r);
			String msg = ((ApiResult.Err<?>) r).message();
			assertTrue(msg.length() <= 210);
			assertTrue(msg.endsWith("…") || msg.length() < 250);
		} finally {
			server.stop(0);
		}
	}

	@Test
	void me_returnsErrWhenEmptyBodyOnError() throws Exception {
		HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
		server.createContext("/api/me", exchange -> {
			try {
				exchange.getRequestBody().readAllBytes();
				if (!"GET".equals(exchange.getRequestMethod())) {
					exchange.sendResponseHeaders(405, -1);
					return;
				}
				exchange.sendResponseHeaders(403, -1);
			} finally {
				exchange.close();
			}
		});
		server.start();
		try {
			int port = server.getAddress().getPort();
			AuthApiClient client = new AuthApiClient("http://127.0.0.1:" + port);
			ApiResult<AuthApiClient.UserDto> r = client.me("t");
			assertInstanceOf(ApiResult.Err.class, r);
			assertEquals("Erreur inconnue", ((ApiResult.Err<?>) r).message());
		} finally {
			server.stop(0);
		}
	}
}
