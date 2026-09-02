package com.farmtohome.api.auth;

import com.farmtohome.api.common.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifierService {

  private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierService.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

  private final GoogleIdTokenVerifier verifier;
  private final List<String> clientIds;

  public GoogleTokenVerifierService(
      @Value("${app.google.client-id:}") String rawClientIds) {
    List<String> parsedClientIds = Arrays.stream((rawClientIds != null ? rawClientIds : "").split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .toList();
    if (parsedClientIds.isEmpty()) {
      throw new IllegalStateException("app.google.client-id must be configured for Google login.");
    }
    this.clientIds = parsedClientIds;
    log.info("GoogleTokenVerifierService initialized with app.google.client-id (audience) = {}", clientIds);
    this.verifier = new GoogleIdTokenVerifier.Builder(
        new NetHttpTransport(), JacksonFactory.getDefaultInstance())
        .setAudience(clientIds)
        .build();
  }

  public GoogleUser verify(String idToken) {
    logTokenClaims(idToken);
    try {
      GoogleIdToken verifiedToken = verifier.verify(idToken);
      if (verifiedToken == null) {
        log.warn("verifier.verify() returned null - likely audience mismatch, invalid signature, "
            + "or expired token. Configured audience (app.google.client-id) = {}", clientIds);
        throw invalidToken();
      }

      GoogleIdToken.Payload payload = verifiedToken.getPayload();
      log.info("verifier.verify() succeeded. Token payload extracted. aud={}, iss={}, sub={}",
          payload.getAudience(), payload.getIssuer(), payload.getSubject());

      String email = payload.getEmail();
      if (email == null || email.isBlank()) {
        log.warn("Token verification failed: email claim missing or blank.");
        throw invalidToken();
      }
      if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
        log.warn("Token verification failed: email_verified claim is not true for email={}", email);
        throw invalidToken();
      }

      log.info("Token verified successfully for email={}", email);
      return new GoogleUser(
          payload.getSubject(),
          email.trim().toLowerCase(),
          stringClaim(payload, "name"),
          stringClaim(payload, "picture"));
    } catch (IOException | java.security.GeneralSecurityException e) {
      log.error("Google ID token verification threw an exception. Configured audience "
          + "(app.google.client-id) = {}. Exception: {}", clientIds, e.toString(), e);
      throw invalidToken();
    }
  }

  /**
   * Decodes the JWT payload (without verifying the signature) purely for diagnostic logging
   * purposes, so we can see the token's aud/iss claims even when verification fails.
   */
  private void logTokenClaims(String idToken) {
    try {
      if (idToken == null) {
        log.warn("logTokenClaims: idToken is null.");
        return;
      }
      String[] parts = idToken.split("\\.");
      if (parts.length < 2) {
        log.warn("logTokenClaims: token does not look like a JWT (expected 3 dot-separated parts, got {}).",
            parts.length);
        return;
      }
      byte[] payloadBytes = URL_DECODER.decode(parts[1]);
      Map<String, Object> claims = MAPPER.readValue(
          new String(payloadBytes, StandardCharsets.UTF_8), new TypeReference<Map<String, Object>>() {});
      log.info("Incoming Google ID token claims - aud={}, iss={}, exp={}, email={}. "
          + "Configured backend audience (app.google.client-id) = {}",
          claims.get("aud"), claims.get("iss"), claims.get("exp"), claims.get("email"), clientIds);
    } catch (Exception e) {
      log.warn("logTokenClaims: unable to decode token claims for diagnostic logging: {}", e.toString());
    }
  }

  private static String stringClaim(GoogleIdToken.Payload payload, String name) {
    Object value = payload.get(name);
    return value != null ? value.toString() : null;
  }

  private static ApiException invalidToken() {
    return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid Google ID token.");
  }

  public record GoogleUser(String subject, String email, String name, String picture) {}
}
