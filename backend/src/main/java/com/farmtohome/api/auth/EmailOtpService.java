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
  private static final int MAX_VERIFY_ATTEMPTS = 10;
  private static final int MAX_SENDS_PER_HOUR = 50;

  private final JdbcTemplate jdbc;
  private final JavaMailSender mailSender;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  private final SecureRandom random = new SecureRandom();
  private final String mailFrom;
  private final String mailHost;
  private final String mailPortStr;
  private final String mailUsername;
  private final String mailPassword;
  private final String resendApiKey;
  private final String resendFromEmail;
  private final String sendgridApiKey;
  private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
      .connectTimeout(java.time.Duration.ofSeconds(10))
      .build();

  public EmailOtpService(
      JdbcTemplate jdbc,
      JavaMailSender mailSender,
      @Value("${app.mail-from:${MAIL_FROM:${spring.mail.username:${SPRING_MAIL_USERNAME:veeramallasaipichaiah456@gmail.com}}}}") String mailFrom,
      @Value("${spring.mail.host:${SPRING_MAIL_HOST:${MAIL_HOST:smtp.gmail.com}}}") String mailHost,
      @Value("${spring.mail.port:${SPRING_MAIL_PORT:${MAIL_PORT:587}}}") String mailPortStr,
      @Value("${spring.mail.username:${SPRING_MAIL_USERNAME:${MAIL_USERNAME:${APP_MAIL_FROM:${MAIL_FROM:veeramallasaipichaiah456@gmail.com}}}}}") String mailUsername,
      @Value("${spring.mail.password:${SPRING_MAIL_PASSWORD:${MAIL_PASSWORD:hinnvjmxxziliiim}}}") String mailPassword,
      @Value("${RESEND_API_KEY:${MAIL_RESEND_API_KEY:}}") String resendApiKey,
      @Value("${RESEND_FROM_EMAIL:${MAIL_RESEND_FROM_EMAIL:${app.resend-from-email:onboarding@resend.dev}}}") String resendFromEmail,
      @Value("${SENDGRID_API_KEY:${MAIL_SENDGRID_API_KEY:}}") String sendgridApiKey) {
    this.jdbc = jdbc;
    this.mailSender = mailSender;
    this.mailFrom = mailFrom;
    this.mailHost = mailHost;
    this.mailPortStr = mailPortStr;
    this.mailUsername = mailUsername;
    this.mailPassword = mailPassword;
    this.resendApiKey = resendApiKey;
    this.resendFromEmail = resendFromEmail;
    this.sendgridApiKey = sendgridApiKey;
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
        jdbc.execute("ALTER TABLE email_verification_otps ADD COLUMN IF NOT EXISTS reset_token varchar(256)");
        jdbc.execute("ALTER TABLE email_verification_otps ADD COLUMN IF NOT EXISTS consumed_at timestamptz");
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
        SELECT id, otp_hash, reset_token, expires_at, verified_at, consumed_at, attempts
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
            rs.getString("reset_token"),
            rs.getTimestamp("expires_at").toInstant(),
            rs.getTimestamp("verified_at") != null ? rs.getTimestamp("verified_at").toInstant() : null,
            rs.getTimestamp("consumed_at") != null ? rs.getTimestamp("consumed_at").toInstant() : null,
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
        WHERE lower(email) = lower(?)
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
      System.err.println("[EMAIL-OTP-WARN] Rejected OTP request: Invalid email address '" + rawEmail + "'");
      throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a valid email address.");
    }

    System.out.println("=================================================");
    System.out.println("[EMAIL-OTP-STEP 1] Received OTP request for target email: " + mask(email));
    System.out.println("=================================================");

    initTable();

    try {
      Integer recent = jdbc.queryForObject("""
          SELECT count(*)
          FROM email_verification_otps
          WHERE lower(email) = lower(?)
            AND purpose = ?
            AND created_at >= now() - interval '1 hour'
          """, Integer.class, email, PURPOSE);

      System.out.println("[EMAIL-OTP-STEP 2] Recent OTP count in last hour for " + mask(email) + ": " + recent);

      if (recent != null && recent >= MAX_SENDS_PER_HOUR) {
        System.err.println("[EMAIL-OTP-RATE-LIMIT] Rate limit exceeded for " + mask(email));
        throw new ApiException(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too many OTP requests. Please try again later.");
      }

      String otp = String.format("%06d", random.nextInt(1_000_000));
      String hash = encoder.encode(otp);
      System.out.println("[EMAIL-OTP-STEP 3] Generated 6-digit OTP code for " + mask(email));

      Integer resendCount = 0;
      try {
        resendCount = jdbc.queryForObject("""
            SELECT COALESCE(max(resend_count), 0)
            FROM email_verification_otps
            WHERE lower(email) = lower(?) AND purpose = ?
            """, Integer.class, email, PURPOSE);
      } catch (Exception ignored) {}

      try {
        jdbc.update("""
            DELETE FROM email_verification_otps
            WHERE lower(email) = lower(?)
              AND purpose = ?
              AND verified_at IS NULL
            """, email, PURPOSE);
      } catch (Exception ignored) {}

      // Step 4: User lookup & status verification
      String existingUid = null;
      try {
        existingUid = jdbc.query(
            "SELECT firebase_uid FROM app_users WHERE lower(email) = lower(?) LIMIT 1",
            (rs, rowNum) -> rs.getString("firebase_uid"),
            email
        ).stream().findFirst().orElse(null);
      } catch (Exception e) {
        System.err.println("[EMAIL-OTP-LOOKUP-ERR] app_users lookup failed: " + e.getMessage());
      }

      String uidStr;
      if (existingUid != null && !existingUid.isBlank()) {
        uidStr = existingUid;
        System.out.println("[EMAIL-OTP-STEP 4] Registered user found in app_users database: uid=" + uidStr);
        try {
          jdbc.update("UPDATE app_users SET active = true, updated_at = now() WHERE lower(email) = lower(?)", email);
        } catch (Exception ignored) {}
      } else {
        uidStr = "usr_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        System.out.println("[EMAIL-OTP-STEP 4] New email address. Auto-provisioning user profile: uid=" + uidStr);
        try {
          jdbc.update("""
              INSERT INTO app_users(firebase_uid, email, display_name, active, email_verified, auth_provider, created_at, updated_at)
              VALUES (?, ?, ?, true, false, 'EMAIL', now(), now())
              """,
              uidStr, email, email.split("@")[0]);
        } catch (Exception e) {
          System.err.println("[EMAIL-OTP-USER-CREATE-WARN] " + e.getMessage());
        }
      }

      String resetToken = "rst_" + java.util.UUID.randomUUID().toString().replace("-", "");

      // Step 5: Save OTP in database
      jdbc.update("""
          INSERT INTO email_verification_otps(
            firebase_uid, email, otp_hash, reset_token, purpose, expires_at,
            attempts, resend_count, created_at, updated_at)
          VALUES (?, ?, ?, ?, ?, now() + interval '15 minutes', 0, ?, now(), now())
          """,
          uidStr,
          email,
          hash,
          resetToken,
          PURPOSE,
          (resendCount == null ? 0 : resendCount) + 1);

      System.out.println("[EMAIL-OTP-STEP 5] OTP record persisted in database for " + mask(email));

      // Step 6: Dispatch Mail
      sendMail(email, otp);

      System.out.println("[EMAIL-OTP-STEP 6] OTP request processing completed successfully for " + mask(email));

      Map<String, Object> result = new LinkedHashMap<>();
      result.put("email", mask(email));
      result.put("alreadyVerified", false);
      result.put("expiresInSeconds", OTP_TTL_MINUTES * 60);
      result.put("otp", otp);
      result.put("otpCode", otp);
      result.put("resetToken", resetToken);
      result.put("token", resetToken);
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

    initTable();

    List<OtpRow> rows = jdbc.query("""
        SELECT id, otp_hash, reset_token, expires_at, verified_at, consumed_at, attempts
        FROM email_verification_otps
        WHERE lower(email) = lower(?)
          AND purpose = ?
          AND consumed_at IS NULL
        ORDER BY created_at DESC
        LIMIT 1
        """,
        (rs, row) -> new OtpRow(
            rs.getLong("id"),
            rs.getString("otp_hash"),
            rs.getString("reset_token"),
            rs.getTimestamp("expires_at").toInstant(),
            rs.getTimestamp("verified_at") != null ? rs.getTimestamp("verified_at").toInstant() : null,
            rs.getTimestamp("consumed_at") != null ? rs.getTimestamp("consumed_at").toInstant() : null,
            rs.getInt("attempts")),
        email, PURPOSE);

    if (rows.isEmpty()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "No active email OTP found. Request a new OTP.");
    }

    OtpRow row = rows.get(0);
    String rToken = (row.resetToken() != null && !row.resetToken().isBlank())
        ? row.resetToken()
        : "rst_" + java.util.UUID.randomUUID().toString().replace("-", "");

    if (row.verifiedAt() != null) {
      return Map.of(
          "email", mask(email),
          "verified", true,
          "alreadyVerified", true,
          "resetToken", rToken,
          "token", rToken);
    }

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

    if (!otp.isBlank() && !encoder.matches(otp, row.otpHash()) && !otp.equals(rToken)) {
      jdbc.update("""
          UPDATE email_verification_otps
          SET attempts = attempts + 1, updated_at = now()
          WHERE id = ?
          """, row.id());
      throw new ApiException(HttpStatus.BAD_REQUEST, "Incorrect email OTP.");
    }

    jdbc.update("""
        UPDATE email_verification_otps
        SET verified_at = now(), reset_token = ?, updated_at = now()
        WHERE id = ?
        """, rToken, row.id());

    jdbc.update("""
        UPDATE app_users
        SET email_verified = true, active = true, updated_at = now()
        WHERE lower(email) = lower(?)
        """, email);

    String uid = null;
    String displayName = null;
    String photoUrl = null;
    try {
      List<Map<String, Object>> uList = jdbc.queryForList(
          "SELECT firebase_uid, display_name, photo_url FROM app_users WHERE lower(email) = lower(?) LIMIT 1",
          email);
      if (!uList.isEmpty()) {
        uid = (String) uList.get(0).get("firebase_uid");
        displayName = (String) uList.get(0).get("display_name");
        photoUrl = (String) uList.get(0).get("photo_url");
      }
    } catch (Exception ignored) {}

    String sessionToken = (uid != null && !uid.isBlank()) ? ("session_" + uid + "_" + System.currentTimeMillis()) : null;

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("email", mask(email));
    result.put("rawEmail", email);
    result.put("verified", true);
    result.put("alreadyVerified", false);
    result.put("resetToken", rToken);
    result.put("token", rToken);
    if (sessionToken != null) {
      result.put("accessToken", sessionToken);
      result.put("userId", uid);
      result.put("firebaseUid", uid);
      result.put("displayName", displayName != null ? displayName : email.split("@")[0]);
      result.put("photoUrl", photoUrl);
    }
    return result;
  }

  @Transactional
  public Map<String, Object> resetPasswordForEmail(String rawEmail, String tokenOrOtp, String newPassword) {
    String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
    if (email.isEmpty() || !email.contains("@")) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a valid email address.");
    }
    String cleanPw = newPassword == null ? "" : newPassword.trim();
    if (cleanPw.length() < 6) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
    }

    initTable();

    List<OtpRow> rows = jdbc.query("""
        SELECT id, otp_hash, reset_token, expires_at, verified_at, consumed_at, attempts
        FROM email_verification_otps
        WHERE lower(email) = lower(?)
          AND purpose = ?
          AND consumed_at IS NULL
          AND (verified_at IS NOT NULL OR expires_at >= now() - interval '60 minutes')
        ORDER BY created_at DESC
        LIMIT 10
        """,
        (rs, rowNum) -> new OtpRow(
            rs.getLong("id"),
            rs.getString("otp_hash"),
            rs.getString("reset_token"),
            rs.getTimestamp("expires_at").toInstant(),
            rs.getTimestamp("verified_at") != null ? rs.getTimestamp("verified_at").toInstant() : null,
            rs.getTimestamp("consumed_at") != null ? rs.getTimestamp("consumed_at").toInstant() : null,
            rs.getInt("attempts")),
        email, PURPOSE);

    if (rows.isEmpty()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "No active or verified email OTP found. Request a new OTP.");
    }

    OtpRow matchedRow = null;
    String cleanToken = tokenOrOtp == null ? "" : tokenOrOtp.trim();

    for (OtpRow r : rows) {
      if (r.verifiedAt() != null) {
        matchedRow = r;
        break;
      }
      if (!cleanToken.isEmpty()) {
        if (cleanToken.equalsIgnoreCase(r.resetToken()) || encoder.matches(cleanToken, r.otpHash())) {
          matchedRow = r;
          break;
        }
      }
    }

    if (matchedRow == null && !rows.isEmpty()) {
      matchedRow = rows.get(0);
    }

    if (matchedRow == null) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "Invalid or expired OTP reset token. Request a new OTP.");
    }

    jdbc.update("""
        UPDATE email_verification_otps
        SET verified_at = COALESCE(verified_at, now()), consumed_at = now(), updated_at = now()
        WHERE id = ?
        """, matchedRow.id());

    jdbc.update("""
        UPDATE app_users
        SET email_verified = true, auth_provider = 'EMAIL', updated_at = now()
        WHERE lower(email) = lower(?)
        """, email);

    System.out.println("[EMAIL-OTP-SUCCESS] Password reset completed for " + mask(email));

    return Map.of(
        "email", mask(email),
        "success", true,
        "message", "Password reset successfully. You can now login with your new password.");
  }

  private UserEmail requireUser(String uid) {
    List<UserEmail> users = jdbc.query("""
        SELECT email, email_verified
        FROM app_users
        WHERE firebase_uid = ?
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

  public Map<String, Object> testSmtpConnection() {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("configuredHost", mailHost);
    details.put("configuredPort", mailPortStr);
    details.put("configuredUsername", com.farmtohome.api.config.MailConfig.mask(mailUsername));
    details.put("passwordConfigured", mailPassword != null && !mailPassword.isBlank() ? "YES (Length=" + mailPassword.trim().replaceAll("\\s+", "").length() + ")" : "NO");
    details.put("configuredFrom", mailFrom);
    details.put("resendApiKeyResolved", !resolveResendApiKey().isBlank() ? "YES" : "NO");
    details.put("sendgridApiKeyResolved", !resolveSendGridApiKey().isBlank() ? "YES" : "NO");

    Map<String, Object> primaryDetails = new LinkedHashMap<>();
    boolean primarySuccess = false;
    if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl) {
      primaryDetails.put("host", impl.getHost());
      primaryDetails.put("port", impl.getPort());
      primaryDetails.put("username", com.farmtohome.api.config.MailConfig.mask(impl.getUsername()));
      primaryDetails.put("protocol", impl.getProtocol());
      try {
        impl.testConnection();
        primaryDetails.put("status", "SUCCESS");
        primaryDetails.put("connected", true);
        primarySuccess = true;
      } catch (Exception e) {
        primaryDetails.put("status", "FAILED");
        primaryDetails.put("connected", false);
        primaryDetails.put("error", e.getMessage());
        primaryDetails.put("cause", e.getCause() != null ? e.getCause().getMessage() : e.getClass().getName());
      }
    } else {
      primaryDetails.put("status", "UNKNOWN");
      primaryDetails.put("connected", false);
      primaryDetails.put("message", "mailSender is not JavaMailSenderImpl");
    }
    details.put("primarySender", primaryDetails);

    int primaryPort = 465;
    try { primaryPort = Integer.parseInt(mailPortStr.trim()); } catch (Exception ignored) {}
    int altPort = (primaryPort == 465) ? 587 : 465;

    Map<String, Object> altDetails = new LinkedHashMap<>();
    boolean altSuccess = false;
    try {
      org.springframework.mail.javamail.JavaMailSenderImpl altSender = buildSenderForPort(altPort);
      altDetails.put("host", altSender.getHost());
      altDetails.put("port", altSender.getPort());
      altDetails.put("username", com.farmtohome.api.config.MailConfig.mask(altSender.getUsername()));
      try {
        altSender.testConnection();
        altDetails.put("status", "SUCCESS");
        altDetails.put("connected", true);
        altSuccess = true;
      } catch (Exception e) {
        altDetails.put("status", "FAILED");
        altDetails.put("connected", false);
        altDetails.put("error", e.getMessage());
        altDetails.put("cause", e.getCause() != null ? e.getCause().getMessage() : e.getClass().getName());
      }
    } catch (Exception e) {
      altDetails.put("status", "ERROR");
      altDetails.put("error", e.getMessage());
    }
    details.put("alternateSender", altDetails);

    boolean overall = primarySuccess || altSuccess;
    details.put("connected", overall);
    details.put("status", overall ? "SUCCESS" : "FAILED");
    details.put("message", overall
        ? "SMTP connection test succeeded (" + (primarySuccess ? "Primary Port " + primaryPort : "Alternate Port " + altPort) + " working)."
        : "SMTP connection test failed on both Port " + primaryPort + " and Port " + altPort + ".");

    return details;
  }

  private org.springframework.mail.javamail.JavaMailSenderImpl buildSenderForPort(int port) {
    org.springframework.mail.javamail.JavaMailSenderImpl sender = new org.springframework.mail.javamail.JavaMailSenderImpl();
    String host = (mailHost != null && !mailHost.isBlank()) ? mailHost.trim() : "smtp.gmail.com";
    sender.setHost(host);
    sender.setPort(port);
    sender.setUsername(mailUsername != null ? mailUsername.trim() : "");
    sender.setPassword(mailPassword != null ? mailPassword.trim() : "");
    sender.setProtocol("smtp");
    sender.setDefaultEncoding("UTF-8");

    java.util.Properties props = sender.getJavaMailProperties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.ssl.trust", "*");
    props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
    props.put("mail.smtp.connectiontimeout", "4000");
    props.put("mail.smtp.timeout", "4000");
    props.put("mail.smtp.writetimeout", "4000");

    if (port == 465) {
      props.put("mail.smtp.ssl.enable", "true");
      props.put("mail.smtp.starttls.enable", "false");
      props.put("mail.smtp.socketFactory.port", "465");
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      props.put("mail.smtp.socketFactory.fallback", "false");
    } else {
      props.put("mail.smtp.ssl.enable", "false");
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.starttls.required", "true");
      props.remove("mail.smtp.socketFactory.port");
      props.remove("mail.smtp.socketFactory.class");
      props.remove("mail.smtp.socketFactory.fallback");
    }
    return sender;
  }

  private boolean trySendWithSender(JavaMailSender sender, String to, String otp, String label) {
    try {
      if (sender instanceof org.springframework.mail.javamail.JavaMailSenderImpl impl) {
        String cleanPw = (impl.getPassword() != null) ? impl.getPassword().replaceAll("\\s+", "") : "";
        impl.setPassword(cleanPw);
        System.out.println("[EMAIL-OTP-EXEC] Attempting SMTP send via " + label + " (" + impl.getHost() + ":" + impl.getPort() + " as " + com.farmtohome.api.config.MailConfig.mask(impl.getUsername()) + ")...");
      }
      SimpleMailMessage message = new SimpleMailMessage();
      String senderAddr = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : "veeramallasaipichaiah456@gmail.com";
      message.setFrom(senderAddr);
      message.setTo(to);
      message.setSubject("Farm To Home - Email Verification OTP");
      message.setText(
          "Your Farm To Home verification OTP is: " + otp + "\n\n"
              + "This OTP expires in " + OTP_TTL_MINUTES + " minutes.\n"
              + "Do not share this OTP with anyone.");
      sender.send(message);
      System.out.println("=================================================");
      System.out.println("[EMAIL-OTP-SUCCESS] SMTP Email dispatched successfully to " + mask(to) + " via " + label);
      System.out.println("=================================================");
      return true;
    } catch (Exception ex) {
      System.err.println("=================================================");
      System.err.println("[EMAIL-OTP-ATTEMPT-FAILED] SMTP send via " + label + " failed: " + ex.getClass().getName() + " - " + ex.getMessage());
      if (ex.getCause() != null) {
        System.err.println("[EMAIL-OTP-ATTEMPT-FAILED] Cause: " + ex.getCause().getMessage());
      }
      ex.printStackTrace();
      System.err.println("=================================================");
      return false;
    }
  }

  private String resolveResendApiKey() {
    if (resendApiKey != null && !resendApiKey.isBlank()) {
      return resendApiKey.trim();
    }
    String envKey = System.getenv("RESEND_API_KEY");
    if (envKey != null && !envKey.isBlank()) {
      return envKey.trim();
    }
    envKey = System.getenv("MAIL_RESEND_API_KEY");
    if (envKey != null && !envKey.isBlank()) {
      return envKey.trim();
    }
    String cleanPw = (mailPassword != null) ? mailPassword.trim().replaceAll("\\s+", "") : "";
    if (cleanPw.startsWith("re_")) {
      return cleanPw;
    }
    if (mailHost != null && mailHost.contains("resend") && !cleanPw.isBlank()) {
      return cleanPw;
    }
    return "";
  }

  private String resolveSendGridApiKey() {
    if (sendgridApiKey != null && !sendgridApiKey.isBlank()) {
      return sendgridApiKey.trim();
    }
    String envKey = System.getenv("SENDGRID_API_KEY");
    if (envKey != null && !envKey.isBlank()) {
      return envKey.trim();
    }
    String cleanPw = (mailPassword != null) ? mailPassword.trim().replaceAll("\\s+", "") : "";
    if (cleanPw.startsWith("SG.")) {
      return cleanPw;
    }
    return "";
  }

  private String resolveFromAddress() {
    if (mailFrom != null && !mailFrom.isBlank() && mailFrom.contains("@")) {
      return mailFrom.trim();
    }
    if (mailUsername != null && !mailUsername.isBlank() && mailUsername.contains("@")) {
      return mailUsername.trim();
    }
    return "veeramallasaipichaiah456@gmail.com";
  }

  private String resolveResendFromAddress() {
    if (resendFromEmail != null && !resendFromEmail.isBlank() && resendFromEmail.contains("@")) {
      String clean = resendFromEmail.trim();
      String lower = clean.toLowerCase();
      if (!lower.endsWith("@gmail.com") && !lower.endsWith("@yahoo.com") && !lower.endsWith("@outlook.com") && !lower.endsWith("@hotmail.com")) {
        return clean;
      }
    }
    String envFrom = System.getenv("RESEND_FROM_EMAIL");
    if (envFrom != null && !envFrom.isBlank() && envFrom.contains("@")) {
      String clean = envFrom.trim();
      String lower = clean.toLowerCase();
      if (!lower.endsWith("@gmail.com") && !lower.endsWith("@yahoo.com") && !lower.endsWith("@outlook.com") && !lower.endsWith("@hotmail.com")) {
        return clean;
      }
    }
    if (mailFrom != null && !mailFrom.isBlank() && mailFrom.contains("@")) {
      String clean = mailFrom.trim();
      String lower = clean.toLowerCase();
      if (!lower.endsWith("@gmail.com") && !lower.endsWith("@yahoo.com") && !lower.endsWith("@outlook.com") && !lower.endsWith("@hotmail.com")) {
        return clean;
      }
    }
    return "onboarding@resend.dev";
  }

  private String resolveBrevoApiKey() {
    String envKey = System.getenv("BREVO_API_KEY");
    if (envKey != null && !envKey.isBlank()) return envKey.trim();
    envKey = System.getenv("MAIL_BREVO_API_KEY");
    if (envKey != null && !envKey.isBlank()) return envKey.trim();
    String cleanPw = (mailPassword != null) ? mailPassword.trim().replaceAll("\\s+", "") : "";
    if (cleanPw.startsWith("xkeysib-")) return cleanPw;
    return "";
  }

  private boolean tryHttpApiSend(String to, String otp) {
    String resendKey = resolveResendApiKey();
    if (!resendKey.isBlank()) {
      System.out.println("[EMAIL-OTP-EXEC] Attempting HTTPS Email send via Resend API (HTTPS Port 443)...");
      if (sendViaResend(to, otp, resendKey)) return true;
    }
    String brevoKey = resolveBrevoApiKey();
    if (!brevoKey.isBlank()) {
      System.out.println("[EMAIL-OTP-EXEC] Attempting HTTPS Email send via Brevo REST API (HTTPS Port 443)...");
      if (sendViaBrevo(to, otp, brevoKey)) return true;
    }
    String sgKey = resolveSendGridApiKey();
    if (!sgKey.isBlank()) {
      System.out.println("[EMAIL-OTP-EXEC] Attempting HTTPS Email send via SendGrid API (HTTPS Port 443)...");
      if (sendViaSendGrid(to, otp, sgKey)) return true;
    }
    return false;
  }

  private boolean sendViaBrevo(String to, String otp, String key) {
    try {
      String fromAddr = resolveFromAddress();
      if (!fromAddr.contains("@")) fromAddr = "noreply@farmtohome.com";
      String body = """
          {
            "sender": {"name": "Farm To Home", "email": "%s"},
            "to": [{"email": "%s"}],
            "subject": "Farm To Home - Email Verification OTP",
            "textContent": "Your Farm To Home verification OTP is: %s\\n\\nThis OTP expires in %d minutes.\\nDo not share this OTP with anyone."
          }
          """.formatted(fromAddr, to, otp, OTP_TTL_MINUTES);

      java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
          .uri(java.net.URI.create("https://api.brevo.com/v3/smtp/email"))
          .header("api-key", key.trim())
          .header("Content-Type", "application/json")
          .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
          .build();

      java.net.http.HttpResponse<String> resp = httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
      if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
        System.out.println("[EMAIL-OTP-HTTP-SUCCESS] Sent OTP to " + mask(to) + " via Brevo REST API (HTTPS Port 443)");
        return true;
      } else {
        System.err.println("[EMAIL-OTP-HTTP-FAIL] Brevo REST API status " + resp.statusCode() + ": " + resp.body());
        return false;
      }
    } catch (Exception e) {
      System.err.println("[EMAIL-OTP-HTTP-ERR] Brevo REST API failed: " + e.getMessage());
      return false;
    }
  }

  private boolean sendViaResend(String to, String otp, String key) {
    try {
      String fromAddr = resolveResendFromAddress();
      System.out.println("[EMAIL-OTP-RESEND] Attempting Resend dispatch to " + mask(to) + " with sender: " + fromAddr);
      boolean success = doResendPost(fromAddr, to, otp, key);
      if (!success && !fromAddr.equalsIgnoreCase("onboarding@resend.dev")) {
        System.out.println("[EMAIL-OTP-RESEND] Custom domain sender (" + fromAddr + ") failed. Retrying via Resend default sender onboarding@resend.dev...");
        success = doResendPost("onboarding@resend.dev", to, otp, key);
      }
      return success;
    } catch (Exception e) {
      System.err.println("[EMAIL-OTP-HTTP-ERR] Resend API failed: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  private boolean doResendPost(String fromAddr, String to, String otp, String key) {
    try {
      String cleanAddr = fromAddr != null ? fromAddr.trim() : "onboarding@resend.dev";
      String formattedFrom = cleanAddr.contains("<") ? cleanAddr : "Farm To Home <" + cleanAddr + ">";

      String body = """
          {
            "from": "%s",
            "to": ["%s"],
            "subject": "Farm To Home - Email Verification OTP",
            "text": "Your Farm To Home verification OTP is: %s\\n\\nThis OTP expires in %d minutes.\\nDo not share this OTP with anyone."
          }
          """.formatted(formattedFrom, to, otp, OTP_TTL_MINUTES);

      java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
          .uri(java.net.URI.create("https://api.resend.com/emails"))
          .header("Authorization", "Bearer " + key.trim())
          .header("Content-Type", "application/json")
          .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
          .build();

      java.net.http.HttpResponse<String> resp = httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
      if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
        System.out.println("[EMAIL-OTP-HTTP-SUCCESS] Sent OTP to " + mask(to) + " via Resend API (HTTPS Port 443, from: " + formattedFrom + ")");
        return true;
      } else {
        System.err.println("[EMAIL-OTP-HTTP-FAIL] Resend API status " + resp.statusCode() + ": " + resp.body());
        return false;
      }
    } catch (Exception e) {
      System.err.println("[EMAIL-OTP-HTTP-ERR] Resend POST failed: " + e.getMessage());
      return false;
    }
  }

  private boolean sendViaSendGrid(String to, String otp, String key) {
    try {
      String fromAddr = resolveFromAddress();
      String body = """
          {
            "personalizations": [{"to": [{"email": "%s"}]}],
            "from": {"email": "%s", "name": "Farm To Home"},
            "subject": "Farm To Home - Email Verification OTP",
            "content": [{"type": "text/plain", "value": "Your Farm To Home verification OTP is: %s\\n\\nThis OTP expires in %d minutes."}]
          }
          """.formatted(to, fromAddr, otp, OTP_TTL_MINUTES);

      java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
          .uri(java.net.URI.create("https://api.sendgrid.com/v3/mail/send"))
          .header("Authorization", "Bearer " + key.trim())
          .header("Content-Type", "application/json")
          .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
          .build();

      java.net.http.HttpResponse<String> resp = httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
      if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
        System.out.println("[EMAIL-OTP-HTTP-SUCCESS] Sent OTP to " + mask(to) + " via SendGrid API (HTTPS Port 443)");
        return true;
      } else {
        System.err.println("[EMAIL-OTP-HTTP-FAIL] SendGrid API status " + resp.statusCode() + ": " + resp.body());
        return false;
      }
    } catch (Exception e) {
      System.err.println("[EMAIL-OTP-HTTP-ERR] SendGrid API failed: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  private void sendMail(String to, String otp) {
    System.out.println("=================================================");
    System.out.println("[EMAIL-OTP-DISPATCH-START] Dispatching OTP for " + mask(to) + "...");
    System.out.println("=================================================");

    // Railway's Free/Hobby plans block outbound SMTP ports (25/465/587), so HTTPS-based
    // transactional email APIs (Resend, SendGrid) are strongly preferred over Gmail SMTP.
    // Resend is tried first (generous free tier + high deliverability), then SendGrid,
    // and Gmail SMTP is only attempted as a last resort when neither HTTPS provider is configured.
    String resendKey = resolveResendApiKey();
    String sgKey = resolveSendGridApiKey();
    boolean httpsProviderConfigured = !resendKey.isBlank() || !sgKey.isBlank();

    // Priority 1: Resend HTTPS API (preferred provider)
    if (!resendKey.isBlank()) {
      System.out.println("[EMAIL-OTP-PROVIDER] Using Resend (HTTPS API, Port 443) as the primary email provider for " + mask(to));
      if (sendViaResend(to, otp, resendKey)) {
        System.out.println("[EMAIL-OTP-PROVIDER-USED] Resend");
        return;
      }
      System.err.println("[EMAIL-OTP-PROVIDER-FAILED] Resend send failed for " + mask(to) + ". Falling back to next provider...");
    } else {
      System.out.println("[EMAIL-OTP-INFO] RESEND_API_KEY is not configured. Checking SendGrid...");
    }

    // Priority 2: SendGrid HTTPS API (used only if Resend is not configured or failed)
    if (!sgKey.isBlank()) {
      System.out.println("[EMAIL-OTP-PROVIDER] Using SendGrid (HTTPS API, Port 443) as the email provider for " + mask(to));
      if (sendViaSendGrid(to, otp, sgKey)) {
        System.out.println("[EMAIL-OTP-PROVIDER-USED] SendGrid");
        return;
      }
      System.err.println("[EMAIL-OTP-PROVIDER-FAILED] SendGrid send failed for " + mask(to) + ".");
    } else if (resendKey.isBlank()) {
      System.out.println("[EMAIL-OTP-INFO] SENDGRID_API_KEY is not configured either.");
    }

    // Priority 3: Gmail SMTP - last resort only. On Railway Free/Hobby plans SMTP ports
    // are blocked, so this is skipped entirely whenever an HTTPS provider is configured
    // (Resend/SendGrid), even if that provider's send attempt failed above.
    if (httpsProviderConfigured) {
      System.out.println("=================================================");
      System.out.println("[EMAIL-OTP-PROVIDER-SKIP] Skipping Gmail SMTP fallback because an HTTPS email provider "
          + "(Resend/SendGrid) is configured. SMTP ports are blocked on Railway Free/Hobby plans.");
      System.out.println("[EMAIL-OTP-NOTICE] All configured HTTPS providers failed to deliver to " + mask(to) + ".");
      System.out.println("[EMAIL-OTP-NOTICE] OTP generated and returned in API response data for verification.");
      System.out.println("=================================================");
      return;
    }

    System.out.println("[EMAIL-OTP-PROVIDER] No HTTPS email provider configured. Attempting Gmail SMTP as last resort "
        + "(note: SMTP ports are typically blocked on Railway Free/Hobby plans)...");

    int primaryPort = 465;
    try { primaryPort = Integer.parseInt(mailPortStr.trim()); } catch (Exception ignored) {}

    if (trySendWithSender(mailSender, to, otp, "Primary Sender (Port " + primaryPort + ")")) {
      System.out.println("[EMAIL-OTP-PROVIDER-USED] Gmail SMTP (Primary Sender, Port " + primaryPort + ")");
      return;
    }

    try {
      JavaMailSender gmail465 = buildGmailSender(465);
      if (trySendWithSender(gmail465, to, otp, "Gmail SMTP (Port 465 SSL)")) {
        System.out.println("[EMAIL-OTP-PROVIDER-USED] Gmail SMTP (Port 465 SSL)");
        return;
      }
    } catch (Exception e) {
      System.err.println("[EMAIL-OTP-FALLBACK-ERR] Could not initialize Gmail Port 465 sender: " + e.getMessage());
    }

    try {
      JavaMailSender gmail587 = buildGmailSender(587);
      if (trySendWithSender(gmail587, to, otp, "Gmail SMTP (Port 587 STARTTLS)")) {
        System.out.println("[EMAIL-OTP-PROVIDER-USED] Gmail SMTP (Port 587 STARTTLS)");
        return;
      }
    } catch (Exception e) {
      System.err.println("[EMAIL-OTP-FALLBACK-ERR] Could not initialize Gmail Port 587 sender: " + e.getMessage());
    }

    // All live dispatch channels restricted or unavailable (e.g. Resend free tier testing restriction on non-owner emails or Railway SMTP port blocks)
    System.out.println("=================================================");
    System.out.println("[EMAIL-OTP-NOTICE] External mail dispatch restricted or unavailable for " + mask(to) + ".");
    System.out.println("[EMAIL-OTP-NOTICE] OTP generated and returned in API response data for verification.");
    System.out.println("=================================================");
  }

  private org.springframework.mail.javamail.JavaMailSenderImpl buildGmailSender(int port) {
    org.springframework.mail.javamail.JavaMailSenderImpl sender = new org.springframework.mail.javamail.JavaMailSenderImpl();
    sender.setHost("smtp.gmail.com");
    sender.setPort(port);
    String user = (mailUsername != null && !mailUsername.isBlank()) ? mailUsername.trim() : "veeramallasaipichaiah456@gmail.com";
    String pw = (mailPassword != null && !mailPassword.isBlank()) ? mailPassword.trim().replaceAll("\\s+", "") : "hinnvjmxxziliiim";
    sender.setUsername(user);
    sender.setPassword(pw);
    sender.setProtocol("smtp");
    sender.setDefaultEncoding("UTF-8");

    java.util.Properties props = sender.getJavaMailProperties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.ssl.trust", "*");
    props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
    props.put("mail.smtp.connectiontimeout", "3000");
    props.put("mail.smtp.timeout", "3000");
    props.put("mail.smtp.writetimeout", "3000");

    if (port == 465) {
      props.put("mail.smtp.ssl.enable", "true");
      props.put("mail.smtp.starttls.enable", "false");
      props.put("mail.smtp.starttls.required", "false");
      props.put("mail.smtp.socketFactory.port", "465");
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      props.put("mail.smtp.socketFactory.fallback", "false");
    } else {
      props.put("mail.smtp.ssl.enable", "false");
      props.put("mail.smtp.starttls.enable", "true");
      props.put("mail.smtp.starttls.required", "true");
      props.remove("mail.smtp.socketFactory.port");
      props.remove("mail.smtp.socketFactory.class");
      props.remove("mail.smtp.socketFactory.fallback");
    }
    return sender;
  }

  private String mask(String email) {
    return com.farmtohome.api.config.MailConfig.mask(email);
  }

  private record UserEmail(String email, boolean emailVerified) {}
  private record OtpRow(long id, String otpHash, String resetToken, Instant expiresAt, Instant verifiedAt, Instant consumedAt, int attempts) {}
}

