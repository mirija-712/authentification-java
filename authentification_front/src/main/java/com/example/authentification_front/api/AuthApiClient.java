package com.example.authentification_front.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Client HTTP vers l'API REST Spring Boot ({@code authentification_back}).
 */
public final class AuthApiClient {

	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.build();
	private final Gson gson = new Gson();
	private final String baseUrl;

	public AuthApiClient(String baseUrl) {
		this.baseUrl = normalizeBase(baseUrl);
	}

	public static String normalizeBase(String baseUrl) {
		if (baseUrl == null || baseUrl.isBlank()) {
			return "http://localhost:8080";
		}
		String t = baseUrl.trim();
		return t.endsWith("/") ? t.substring(0, t.length() - 1) : t;
	}

	public ApiResult<UserDto> register(String email, String password, String passwordConfirm) {
		String json = String.format("{\"email\":%s,\"password\":%s,\"passwordConfirm\":%s}",
				gson.toJson(email), gson.toJson(password), gson.toJson(passwordConfirm));
		return postJson("/api/auth/register", json, UserDto.class, 201);
	}

	public ApiResult<UserDto> login(String email, String password) {
		String json = String.format("{\"email\":%s,\"password\":%s}",
				gson.toJson(email), gson.toJson(password));
		return postJson("/api/auth/login", json, UserDto.class, 200);
	}

	public ApiResult<UserDto> me(String bearerToken) {
		try {
			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + "/api/me"))
					.timeout(Duration.ofSeconds(30))
					.header("Authorization", "Bearer " + bearerToken)
					.GET()
					.build();
			HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
			return mapResponse(res, UserDto.class, 200);
		} catch (Exception e) {
			return new ApiResult.Err<>("Erreur réseau : " + e.getMessage(), 0);
		}
	}

	private <T> ApiResult<T> postJson(String path, String body, Class<T> okType, int expectedOk) {
		try {
			HttpRequest req = HttpRequest.newBuilder()
					.uri(URI.create(baseUrl + path))
					.timeout(Duration.ofSeconds(30))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
					.build();
			HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
			return mapResponse(res, okType, expectedOk);
		} catch (Exception e) {
			return new ApiResult.Err<>("Erreur réseau : " + e.getMessage(), 0);
		}
	}

	private <T> ApiResult<T> mapResponse(HttpResponse<String> res, Class<T> okType, int expectedOk) {
		int code = res.statusCode();
		String body = res.body() == null ? "" : res.body();
		if (code == expectedOk) {
			try {
				return new ApiResult.Ok<>(gson.fromJson(body, okType));
			} catch (JsonSyntaxException e) {
				return new ApiResult.Err<>("Réponse JSON invalide : " + body, code);
			}
		}
		return new ApiResult.Err<>(parseMessage(body), code);
	}

	private String parseMessage(String json) {
		if (json == null || json.isBlank()) {
			return "Erreur inconnue";
		}
		try {
			ErrorBody e = gson.fromJson(json, ErrorBody.class);
			if (e != null && e.message != null && !e.message.isBlank()) {
				return e.message;
			}
		} catch (JsonSyntaxException ignored) {
			// ignore
		}
		return json.length() > 200 ? json.substring(0, 200) + "…" : json;
	}

	/** DTO aligné sur le JSON du backend (champs publics pour Gson). */
	public static class UserDto {
		public Long id;
		public String email;
		public String createdAt;
		public String token;
	}

	/** Champs publics pour la désérialisation Gson des erreurs API. */
	public static class ErrorBody {
		public String message;
	}
}
