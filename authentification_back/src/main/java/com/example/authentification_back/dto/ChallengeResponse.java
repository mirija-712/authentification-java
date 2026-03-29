package com.example.authentification_back.dto;

/**
 * Réponse au challenge TP3 : nonce à usage unique, date d’expiration, sel public pour calculer l’empreinte côté client.
 */
public record ChallengeResponse(String nonce, String expiresAt, String authSalt) {
}
