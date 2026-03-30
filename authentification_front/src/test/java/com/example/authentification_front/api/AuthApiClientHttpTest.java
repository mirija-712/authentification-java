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
}
