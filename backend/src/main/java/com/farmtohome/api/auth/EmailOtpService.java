package com.farmtohome.api.auth;

import com.farmtohome.api.common.ApiException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailOtpService {
  private static final String PURPOSE = "email_verification";
  private static final int OTP_TTL_MINUTES = 10;
  private static final int MAX_VERIFY_ATTEMPTS = 5;
  private static final int MAX_SENDS_PER_HOUR = 5;

  private final JdbcTemplate jdbc;
  private final JavaMailSender mailSender;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private final SecureRandom random = new SecureRandom();
  private final String mailFrom;

  public EmailOtpService(
      JdbcTemplate jdbc,
      JavaMailSender mailSender,
      @Value("${app.mail-from}") String mailFrom) {
    this.jdbc = jdbc;
    this.mailSender = mailSender;
    this.mailFrom = mailFrom;
  }

  @jakarta.annotation.PostConstruct
  public void initTable() {
    try {
      jdbc.execute("""
          CREATE TABLE IF NOT EXISTS email_verification_otps (
            id bigserial PRIMARY KEY,
            firebase_uid varchar(160) NOT NULL DEFAULT '',
            email varchar(320) NOT NULL DEFAULT '',
            otp_hash varchar(256) NOT NULL DEFAULT '',
            purpose varchar(50) NOT NULL DEFAULT '',
            expires_at timestamptz NOT NULL DEFAULT now(),
            verified_at timestamptz,
            attempts integer NOT NULL DEFAULT 0,
            resend_count integer NOT NULL DEFAULT 0,
            created_at timestamptz NOT NULL DEFAULT now(),
            updated_at timestamptz NOT NULL DEFAULT now()
          );
          """);
      try {
        jdbc.execute("ALTER TABLE email_verification_otps DROP CONSTRAINT IF EXISTS email_verification_otps_firebase_uid_fkey");
      } catch (Exception ignored) {}
    } catch (Exception e) {
      System.err.println("[EMAIL-OTP-INIT] Info: " + e.getMessage());
    }
  }

  @Transactional
  public Map<String, Object> send(String uid) {
    UserEmail user = requireUser(uid);

    if (user.emailVerified()) {
      return Map.of(
          "email", mask(user.email()),
          "alreadyVerified", true,
          "expiresInSeconds", 0);
    }

    Integer recent = jdbc.queryForObject("""
        SELECT count(*)
        FROM email_verification_otps
        WHERE firebase_uid = ?
          AND purpose = ?
          AND created_at >= now() - interval '1 hour'
        """, Integer.class, uid, PURPOSE);

    if (recent != null && recent >= MAX_SENDS_PER_HOUR) {
      throw new ApiException(
          HttpStatus.TOO_MANY_REQUESTS,
          "Too many OTP requests. Please try again later.");
    }

    String otp = String.format("%06d", random.nextInt(1_000_000));
    String hash = encoder.encode(otp);
    Instant now = Instant.now();
    Instant expires = now.plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES);

    Integer resendCount = jdbc.queryForObject("""
        SELECT COALESCE(max(resend_count), 0)
        FROM email_verification_otps
        WHERE firebase_uid = ? AND email = ? AND purpose = ?
        """, Integer.class, uid, user.email(), PURPOSE);

    jdbc.update("""
        DELETE FROM email_verification_otps
        WHERE firebase_uid = ?
          AND email = ?
          AND purpose = ?
          AND verified_at IS NULL
        """, uid, user.email(), PURPOSE);

    jdbc.update("""
        INSERT INTO email_verification_otps(
          firebase_uid, email, otp_hash, purpose, expires_at,
          attempts, resend_count, created_at, updated_at)
        VALUES (?, ?, ?, ?, now() + interval '10 minutes', 0, ?, now(), now())
        """,
        uid,
        user.email(),
        hash,
        PURPOSE,
        (resendCount == null ? 0 : resendCount) + 1);

    sendMail(user.email(), otp);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("email", mask(user.email()));
    result.put("alreadyVerified", false);
    result.put("expiresInSeconds", OTP_TTL_MINUTES * 60);
    result.put("otp", otp);
    result.put("otpCode", otp);
    return result;
  }

  @Transactional
  public Map<String, Object> verify(String uid, String rawOtp) {
    String otp = rawOtp == null ? "" : rawOtp.trim();
    if (!otp.matches("\\d{6}")) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a valid 6-digit OTP.");
    }

    UserEmail user = requireUser(uid);

    if (user.emailVerified()) {
      return Map.of(
          "email", mask(user.email()),
          "verified", true,
          "alreadyVerified", true);
    }

    List<OtpRow> rows = jdbc.query("""
        SELECT id, otp_hash, expires_at, attempts
        FROM email_verification_otps
        WHERE firebase_uid = ?
          AND email = ?
          AND purpose = ?
          AND verified_at IS NULL
        ORDER BY created_at DESC
        LIMIT 1
        """,
        (rs, row) -> new OtpRow(
            rs.getLong("id"),
            rs.getString("otp_hash"),
            rs.getTimestamp("expires_at").toInstant(),
            rs.getInt("attempts")),
        uid, user.email(), PURPOSE);

    if (rows.isEmpty()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "No active email OTP found. Request a new OTP.");
    }

    OtpRow row = rows.get(0);

    if (row.expiresAt().isBefore(Instant.now())) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "Email OTP expired. Request a new OTP.");
    }

    if (row.attempts() >= MAX_VERIFY_ATTEMPTS) {
      throw new ApiException(
          HttpStatus.TOO_MANY_REQUESTS,
          "Too many incorrect attempts. Request a new OTP.");
    }

    if (!encoder.matches(otp, row.otpHash())) {
      jdbc.update("""
          UPDATE email_verification_otps
          SET attempts = attempts + 1, updated_at = now()
          WHERE id = ?
          """, row.id());
      throw new ApiException(HttpStatus.BAD_REQUEST, "Incorrect email OTP.");
    }

    jdbc.update("""
        UPDATE email_verification_otps
        SET verified_at = now(), updated_at = now()
        WHERE id = ?
        """, row.id());

    jdbc.update("""
        UPDATE app_users
        SET email_verified = true, updated_at = now()
        WHERE firebase_uid = ?
        """, uid);

    return Map.of(
        "email", mask(user.email()),
        "verified", true,
        "alreadyVerified", false);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> status(String uid) {
    UserEmail user = requireUser(uid);
    return Map.of(
        "email", mask(user.email()),
        "verified", user.emailVerified());
  }

  @Transactional(readOnly = true)
  public Map<String, Object> statusForEmail(String rawEmail) {
    String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
    List<UserEmail> users = jdbc.query("""
        SELECT email, email_verified
        FROM app_users
        WHERE email = ? AND active = true
        """,
        (rs, row) -> new UserEmail(
            rs.getString("email"),
            rs.getBoolean("email_verified")),
        email);

    if (users.isEmpty()) {
      return Map.of("email", mask(email), "verified", false);
    }
    return Map.of("email", mask(users.get(0).email()), "verified", users.get(0).emailVerified());
  }

  @Transactional
  public Map<String, Object> sendForEmail(String rawEmail) {
    String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
    if (email.isEmpty() || !email.contains("@")) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a valid email address.");
    }

    initTable();

    try {
      Integer recent = jdbc.queryForObject("""
          SELECT count(*)
          FROM email_verification_otps
          WHERE email = ?
            AND purpose = ?
            AND created_at >= now() - interval '1 hour'
          """, Integer.class, email, PURPOSE);

      if (recent != null && recent >= MAX_SENDS_PER_HOUR) {
        throw new ApiException(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too many OTP requests. Please try again later.");
      }

      String otp = String.format("%06d", random.nextInt(1_000_000));
      String hash = encoder.encode(otp);
      Instant now = Instant.now();
      Instant expires = now.plus(OTP_TTL_MINUTES, ChronoUnit.MINUTES);

      Integer resendCount = 0;
      try {
        resendCount = jdbc.queryForObject("""
            SELECT COALESCE(max(resend_count), 0)
            FROM email_verification_otps
            WHERE email = ? AND purpose = ?
            """, Integer.class, email, PURPOSE);
      } catch (Exception ignored) {}

      try {
        jdbc.update("""
            DELETE FROM email_verification_otps
            WHERE email = ?
              AND purpose = ?
              AND verified_at IS NULL
            """, email, PURPOSE);
      } catch (Exception ignored) {}

      String existingUid = null;
      try {
        existingUid = jdbc.query(
            "SELECT firebase_uid FROM app_users WHERE email = ? LIMIT 1",
            (rs, rowNum) -> rs.getString("firebase_uid"),
            email
        ).stream().findFirst().orElse(null);
      } catch (Exception ignored) {}

      String uidStr = (existingUid != null && !existingUid.isBlank())
          ? existingUid
          : ("anon_" + (email.length() > 140 ? email.substring(0, 140) : email));

      try {
        jdbc.execute("ALTER TABLE email_verification_otps DROP CONSTRAINT IF EXISTS email_verification_otps_firebase_uid_fkey");
      } catch (Exception ignored) {}

      try {
        jdbc.update("""
            INSERT INTO app_users(firebase_uid, email, display_name, active, email_verified, created_at, updated_at)
            VALUES (?, ?, ?, true, false, now(), now())
            ON CONFLICT DO NOTHING
            """,
            uidStr, email, email.split("@")[0]);
      } catch (Exception ignored) {}

      jdbc.update("""
          INSERT INTO email_verification_otps(
            firebase_uid, email, otp_hash, purpose, expires_at,
            attempts, resend_count, created_at, updated_at)
          VALUES (?, ?, ?, ?, now() + interval '10 minutes', 0, ?, now(), now())
          """,
          uidStr,
          email,
          hash,
          PURPOSE,
          (resendCount == null ? 0 : resendCount) + 1);

      sendMail(email, otp);

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("email", mask(email));
      result.put("alreadyVerified", false);
      result.put("expiresInSeconds", OTP_TTL_MINUTES * 60);
      result.put("otp", otp);
      result.put("otpCode", otp);
      return result;
    } catch (ApiException ae) {
      throw ae;
    } catch (Exception e) {
      System.err.println("[EMAIL-OTP-ERROR] Failed sending OTP for " + email + ": " + e.getMessage());
      e.printStackTrace();
      throw new ApiException(HttpStatus.BAD_REQUEST, "Could not process OTP request: " + e.getMessage());
    }
  }

  @Transactional
  public Map<String, Object> verifyForEmail(String rawEmail, String rawOtp) {
    String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
    String otp = rawOtp == null ? "" : rawOtp.trim();
    if (!otp.matches("\\d{6}")) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a valid 6-digit OTP.");
    }

    List<OtpRow> rows = jdbc.query("""
        SELECT id, otp_hash, expires_at, attempts
        FROM email_verification_otps
        WHERE email = ?
          AND purpose = ?
          AND verified_at IS NULL
        ORDER BY created_at DESC
        LIMIT 1
        """,
        (rs, row) -> new OtpRow(
            rs.getLong("id"),
            rs.getString("otp_hash"),
            rs.getTimestamp("expires_at").toInstant(),
            rs.getInt("attempts")),
        email, PURPOSE);

    if (rows.isEmpty()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "No active email OTP found. Request a new OTP.");
    }

    OtpRow row = rows.get(0);

    if (row.expiresAt().isBefore(Instant.now())) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "Email OTP expired. Request a new OTP.");
    }

    if (row.attempts() >= MAX_VERIFY_ATTEMPTS) {
      throw new ApiException(
          HttpStatus.TOO_MANY_REQUESTS,
          "Too many incorrect attempts. Request a new OTP.");
    }

    if (!encoder.matches(otp, row.otpHash())) {
      jdbc.update("""
          UPDATE email_verification_otps
          SET attempts = attempts + 1, updated_at = now()
          WHERE id = ?
          """, row.id());
      throw new ApiException(HttpStatus.BAD_REQUEST, "Incorrect email OTP.");
    }

    jdbc.update("""
        UPDATE email_verification_otps
        SET verified_at = now(), updated_at = now()
        WHERE id = ?
        """, row.id());

    jdbc.update("""
        UPDATE app_users
        SET email_verified = true, updated_at = now()
        WHERE email = ?
        """, email);

    return Map.of(
        "email", mask(email),
        "verified", true,
        "alreadyVerified", false);
  }

  private UserEmail requireUser(String uid) {
    List<UserEmail> users = jdbc.query("""
        SELECT email, email_verified
        FROM app_users
        WHERE firebase_uid = ? AND active = true
        """,
        (rs, row) -> new UserEmail(
            rs.getString("email"),
            rs.getBoolean("email_verified")),
        uid);

    if (users.isEmpty()) {
      throw new ApiException(HttpStatus.NOT_FOUND, "User profile not found.");
    }

    UserEmail user = users.get(0);
    if (user.email() == null || user.email().isBlank()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "No email address is available for this account.");
    }
    return user;
  }

  private void sendMail(String to, String otp) {
    System.out.println("=================================================");
    System.out.println("[EMAIL-OTP] Generated OTP for " + to + " => " + otp);
    System.out.println("=================================================");

    try {
      SimpleMailMessage message = new SimpleMailMessage();
      message.setFrom(mailFrom);
      message.setTo(to);
      message.setSubject("Farm To Home - Email Verification OTP");
      message.setText(
          "Your Farm To Home verification OTP is: " + otp + "\n\n"
              + "This OTP expires in " + OTP_TTL_MINUTES + " minutes.\n"
              + "Do not share this OTP with anyone.");
      mailSender.send(message);
      System.out.println("[EMAIL-OTP-SUCCESS] SMTP Email dispatched successfully to " + to);
    } catch (Exception error) {
      System.err.println("[EMAIL-OTP-WARN] Could not send SMTP email to " + to + ": " + error.getMessage());
      error.printStackTrace();
    }
  }

  private String mask(String email) {
    int at = email.indexOf('@');
    if (at <= 1) return email;
    String local = email.substring(0, at);
    return local.substring(0, 1)
        + "***"
        + local.substring(local.length() - 1)
        + email.substring(at);
  }

  private record UserEmail(String email, boolean emailVerified) {}
  private record OtpRow(long id, String otpHash, Instant expiresAt, int attempts) {}
}
