package com.farmtohome.api.auth;

import com.farmtohome.api.common.ApiException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class GoogleTokenVerifierService {

  private final GoogleIdTokenVerifier verifier;

  public GoogleTokenVerifierService(
      @Value("${app.google.client-id:}") String rawClientIds) {
    List<String> clientIds = Arrays.stream((rawClientIds != null ? rawClientIds : "").split(","))
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .toList();
    if (clientIds.isEmpty()) {
      throw new IllegalStateException("app.google.client-id must be configured for Google login.");
    }
    this.verifier = new GoogleIdTokenVerifier.Builder(
        new NetHttpTransport(), JacksonFactory.getDefaultInstance())
        .setAudience(clientIds)
        .build();
  }

  public GoogleUser verify(String idToken) {
    try {
      GoogleIdToken verifiedToken = verifier.verify(idToken);
      if (verifiedToken == null) {
        throw invalidToken();
      }

      GoogleIdToken.Payload payload = verifiedToken.getPayload();
      String email = payload.getEmail();
      if (email == null || email.isBlank() || !Boolean.TRUE.equals(payload.getEmailVerified())) {
        throw invalidToken();
      }

      return new GoogleUser(
          payload.getSubject(),
          email.trim().toLowerCase(),
          stringClaim(payload, "name"),
          stringClaim(payload, "picture"));
    } catch (IOException | java.security.GeneralSecurityException e) {
      throw invalidToken();
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