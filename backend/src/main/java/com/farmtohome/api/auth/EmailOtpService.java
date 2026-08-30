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
  private final String mailHost;
  private final String mailPortStr;
  private final String mailUsername;
  private final String mailPassword;
  private final String resendApiKey;
  private final String brevoApiKey;
  private final String sendgridApiKey;
  private final java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
      .connectTimeout(java.time.Duration.ofSeconds(10))
      .build();

  public EmailOtpService(
      JdbcTemplate jdbc,
      JavaMailSender mailSender,
      @Value("${app.mail-from:${MAIL_FROM:${spring.mail.username:${SPRING_MAIL_USERNAME:veeramallasaipichaiah456@gmail.com}}}}") String mailFrom,
      @Value("${spring.mail.host:${SPRING_MAIL_HOST:${MAIL_HOST:smtp.gmail.com}}}") String mailHost,
      @Value("${spring.mail.port:${SPRING_MAIL_PORT:${MAIL_PORT:465}}}") String mailPortStr,
      @Value("${spring.mail.username:${SPRING_MAIL_USERNAME:${MAIL_USERNAME:${APP_MAIL_FROM:${MAIL_FROM:veeramallasaipichaiah456@gmail.com}}}}}") String mailUsername,
      @Value("${spring.mail.password:${SPRING_MAIL_PASSWORD:${MAIL_PASSWORD:hinnvjmxxziliiim}}}") String mailPassword,
      @Value("${RESEND_API_KEY:${MAIL_RESEND_API_KEY:}}") String resendApiKey,
      @Value("${BREVO_API_KEY:${MAIL_BREVO_API_KEY:}}") String brevoApiKey,
      @Value("${SENDGRID_API_KEY:${MAIL_SENDGRID_API_KEY:}}") String sendgridApiKey) {
    this.jdbc = jdbc;
    this.mailSender = mailSender;
    this.mailFrom = mailFrom;
    this.mailHost = mailHost;
    this.mailPortStr = mailPortStr;
    this.mailUsername = mailUsername;
    this.mailPassword = mailPassword;
    this.resendApiKey = resendApiKey;
    this.brevoApiKey = brevoApiKey;
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

  public Map<String, Object> testSmtpConnection() {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("configuredHost", mailHost);
    details.put("configuredPort", mailPortStr);
    details.put("configuredUsername", com.farmtohome.api.config.MailConfig.mask(mailUsername));
    details.put("passwordConfigured", mailPassword != null && !mailPassword.isBlank() ? "YES (Length=" + mailPassword.trim().length() + ")" : "NO");
    details.put("configuredFrom", mailFrom);

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
    props.put("mail.smtp.connectiontimeout", "3000");
    props.put("mail.smtp.timeout", "3000");
    props.put("mail.smtp.writetimeout", "3000");

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

  private boolean tryHttpApiSend(String to, String otp) {
    if (resendApiKey != null && !resendApiKey.isBlank()) {
      System.out.println("[EMAIL-OTP-EXEC] Attempting HTTPS Email send via Resend API...");
      if (sendViaResend(to, otp)) return true;
    }
    if (brevoApiKey != null && !brevoApiKey.isBlank()) {
      System.out.println("[EMAIL-OTP-EXEC] Attempting HTTPS Email send via Brevo API...");
      if (sendViaBrevo(to, otp)) return true;
    }
    if (sendgridApiKey != null && !sendgridApiKey.isBlank()) {
      System.out.println("[EMAIL-OTP-EXEC] Attempting HTTPS Email send via SendGrid API...");
      if (sendViaSendGrid(to, otp)) return true;
    }
    return false;
  }

  private boolean sendViaResend(String to, String otp) {
    try {
      String fromAddr = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : "veeramallasaipichaiah456@gmail.com";
      String body = """
          {
            "from": "%s",
            "to": ["%s"],
            "subject": "Farm To Home - Email Verification OTP",
            "text": "Your Farm To Home verification OTP is: %s\\n\\nThis OTP expires in %d minutes.\\nDo not share this OTP with anyone."
          }
          """.formatted(fromAddr, to, otp, OTP_TTL_MINUTES);

      java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
          .uri(java.net.URI.create("https://api.resend.com/emails"))
          .header("Authorization", "Bearer " + resendApiKey.trim())
          .header("Content-Type", "application/json")
          .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
          .build();

      java.net.http.HttpResponse<String> resp = httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
      if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
        System.out.println("[EMAIL-OTP-HTTP-SUCCESS] Sent OTP to " + mask(to) + " via Resend API (HTTPS Port 443)");
        return true;
      } else {
        System.err.println("[EMAIL-OTP-HTTP-FAIL] Resend API status " + resp.statusCode() + ": " + resp.body());
        return false;
      }
    } catch (Exception e) {
      System.err.println("[EMAIL-OTP-HTTP-ERR] Resend API failed: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  private boolean sendViaBrevo(String to, String otp) {
    try {
      String fromAddr = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : "veeramallasaipichaiah456@gmail.com";
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
          .header("api-key", brevoApiKey.trim())
          .header("Content-Type", "application/json")
          .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
          .build();

      java.net.http.HttpResponse<String> resp = httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
      if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
        System.out.println("[EMAIL-OTP-HTTP-SUCCESS] Sent OTP to " + mask(to) + " via Brevo API (HTTPS Port 443)");
        return true;
      } else {
        System.err.println("[EMAIL-OTP-HTTP-FAIL] Brevo API status " + resp.statusCode() + ": " + resp.body());
        return false;
      }
    } catch (Exception e) {
      System.err.println("[EMAIL-OTP-HTTP-ERR] Brevo API failed: " + e.getMessage());
      e.printStackTrace();
      return false;
    }
  }

  private boolean sendViaSendGrid(String to, String otp) {
    try {
      String fromAddr = (mailFrom != null && !mailFrom.isBlank()) ? mailFrom : "veeramallasaipichaiah456@gmail.com";
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
          .header("Authorization", "Bearer " + sendgridApiKey.trim())
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
    System.out.println("[EMAIL-OTP-DISPATCH-START] Dispatching OTP for " + mask(to));
    System.out.println("[EMAIL-OTP-CONFIG] Host: " + mailHost + ", Username: " + mask(mailUsername)
        + ", Password length: " + (mailPassword != null ? mailPassword.trim().replaceAll("\\s+", "").length() : 0));
    System.out.println("=================================================");

    // Priority 1: Check HTTPS Email API (Resend / Brevo / SendGrid over HTTPS Port 443)
    if (tryHttpApiSend(to, otp)) {
      return;
    }

    int primaryPort = 465;
    try { primaryPort = Integer.parseInt(mailPortStr.trim()); } catch (Exception ignored) {}

    // Priority 2: Primary JavaMailSender SMTP (Port 465/587)
    if (trySendWithSender(mailSender, to, otp, "Primary Sender (Port " + primaryPort + ")")) {
      return;
    }

    // Priority 3: Alternate SMTP port fallback (Port 587 if primary was 465, or Port 465 if primary was 587)
    int altPort = (primaryPort == 465) ? 587 : 465;
    try {
      JavaMailSender altSender = buildSenderForPort(altPort);
      if (trySendWithSender(altSender, to, otp, "Alternate Fallback (Port " + altPort + ")")) {
        return;
      }
    } catch (Exception e) {
      System.err.println("[EMAIL-OTP-FALLBACK-ERR] Could not initialize fallback sender on Port " + altPort + ": " + e.getMessage());
    }

    // All send channels failed: Throw exception so the endpoint returns 500 error and client does NOT treat send as success!
    String failMsg = "Unable to dispatch verification email. Please verify SMTP configuration, Gmail app password, or configure a mail API key.";
    System.err.println("=================================================");
    System.err.println("[EMAIL-OTP-FAILURE-CRITICAL] All mail dispatch attempts failed for " + mask(to));
    System.err.println("[EMAIL-OTP-FAILURE-CRITICAL] Throwing ApiException to return HTTP error status to caller.");
    System.err.println("=================================================");

    throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, failMsg);
  }

  private String mask(String email) {
    return com.farmtohome.api.config.MailConfig.mask(email);
  }

  private record UserEmail(String email, boolean emailVerified) {}
  private record OtpRow(long id, String otpHash, Instant expiresAt, int attempts) {}
}

