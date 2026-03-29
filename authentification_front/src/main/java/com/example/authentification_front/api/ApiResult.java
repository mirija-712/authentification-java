package com.example.authentification_front.api;

/**
 * Résultat d'un appel API : succès avec valeur ou erreur (message + code HTTP).
 */
public sealed interface ApiResult<T> permits ApiResult.Ok, ApiResult.Err {

	record Ok<T>(T value) implements ApiResult<T> {}

	record Err<T>(String message, int httpStatus) implements ApiResult<T> {}
}
