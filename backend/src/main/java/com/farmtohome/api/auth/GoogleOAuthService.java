package com.farmtohome.api.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service to verify Google ID tokens and extract claims securely.
 */
@Service
public class GoogleOAuthService {

  private final String googleClientId;
  private final GoogleIdTokenVerifier verifier;

  public GoogleOAuthService(@Value("${google.oauth2.client-id}") String googleClientId) {
    this.googleClientId = googleClientId;
    HttpTransport transport = new NetHttpTransport();
    JsonFactory jsonFactory = new GsonFactory();
    this.verifier = new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
        .setAudience(Collections.singletonList(googleClientId))
        .setIssuer("https://accounts.google.com")
        .build();
  }

  /**
   * Verify and extract claims from Google ID token.
   * @param idToken JWT from Google OAuth
   * @return Claims map or empty map if invalid
   */
  public Map<String, Object> verifyIdToken(String idToken) {
    if (idToken == null || idToken.isBlank()) {
      return Map.of();
    }

    try {
      GoogleIdToken token = verifier.verify(idToken);
      if (token != null) {
        GoogleIdToken.Payload payload = token.getPayload();
        Map<String, Object> claims = new LinkedHashMap<>();
        putIfNotNull(claims, "sub", payload.getSubject());
        putIfNotNull(claims, "email", payload.getEmail());
        putIfNotNull(claims, "email_verified", payload.getEmailVerified());
        putIfNotNull(claims, "name", payload.get("name"));
        putIfNotNull(claims, "picture", payload.get("picture"));
        putIfNotNull(claims, "given_name", payload.get("given_name"));
        putIfNotNull(claims, "family_name", payload.get("family_name"));
        putIfNotNull(claims, "aud", payload.getAudience());
        putIfNotNull(claims, "iss", payload.getIssuer());
        putIfNotNull(claims, "iat", payload.getIssuedAtTimeSeconds());
        putIfNotNull(claims, "exp", payload.getExpirationTimeSeconds());
        return claims;
      }
      return Map.of();
    } catch (Exception e) {
      return Map.of();
    }
  }

  /**
   * Check if a token's email is verified by Google.
   */
  public boolean isEmailVerifiedByGoogle(String idToken) {
    Map<String, Object> claims = verifyIdToken(idToken);
    Object verified = claims.get("email_verified");
    return verified instanceof Boolean && (Boolean) verified;
  }

  private void putIfNotNull(Map<String, Object> map, String key, Object value) {
    if (value != null) {
      map.put(key, value);
    }
  }
}
