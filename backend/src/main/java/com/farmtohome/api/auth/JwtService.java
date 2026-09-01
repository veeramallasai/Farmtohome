package com.farmtohome.api.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private static final ObjectMapper MAPPER = new ObjectMapper();
  private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
  private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

  private final byte[] secret;
  private final String issuer;
  private final long expirationSeconds;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.issuer:farm-to-home-api}") String issuer,
      @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds) {
    if (secret == null || secret.length() < 32) {
      throw new IllegalStateException("app.jwt.secret must be at least 32 characters.");
    }
    this.secret = secret.getBytes(StandardCharsets.UTF_8);
    this.issuer = issuer;
    this.expirationSeconds = expirationSeconds;
  }

  public String generateToken(String subject, String email, String name, String photoUrl) {
    Instant now = Instant.now();
    Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("iss", issuer);
    payload.put("sub", subject);
    payload.put("email", email);
    payload.put("name", name);
    payload.put("picture", photoUrl);
    payload.put("iat", now.getEpochSecond());
    payload.put("exp", now.plusSeconds(expirationSeconds).getEpochSecond());

    String unsignedToken = base64Json(header) + "." + base64Json(payload);
    return unsignedToken + "." + URL_ENCODER.encodeToString(hmac(unsignedToken));
  }

  public VerifiedJwt verify(String token) {
    try {
      String[] parts = token.split("\\.");
      if (parts.length != 3) return null;

      String unsignedToken = parts[0] + "." + parts[1];
      String expectedSignature = URL_ENCODER.encodeToString(hmac(unsignedToken));
      if (!constantTimeEquals(expectedSignature, parts[2])) return null;

      Map<String, Object> payload = MAPPER.readValue(
          URL_DECODER.decode(parts[1]), new TypeReference<Map<String, Object>>() {});
      if (!issuer.equals(payload.get("iss"))) return null;

      long expiresAt = numberClaim(payload.get("exp"));
      if (expiresAt <= Instant.now().getEpochSecond()) return null;

      String subject = stringClaim(payload.get("sub"));
      if (subject.isBlank()) return null;
      return new VerifiedJwt(subject, stringClaim(payload.get("email")));
    } catch (Exception e) {
      return null;
    }
  }

  private static String base64Json(Map<String, Object> value) {
    try {
      return URL_ENCODER.encodeToString(MAPPER.writeValueAsBytes(value));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to create JWT.", e);
    }
  }

  private byte[] hmac(String value) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret, "HmacSHA256"));
      return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    } catch (Exception e) {
      throw new IllegalStateException("Unable to sign JWT.", e);
    }
  }

  private static boolean constantTimeEquals(String expected, String actual) {
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8),
        actual.getBytes(StandardCharsets.UTF_8));
  }

  private static String stringClaim(Object value) {
    return value != null ? value.toString() : "";
  }

  private static long numberClaim(Object value) {
    if (value instanceof Number number) return number.longValue();
    if (value == null) return 0L;
    return Long.parseLong(value.toString());
  }

  public record VerifiedJwt(String subject, String email) {}
}