package com.farmtohome.api.auth;

import com.farmtohome.api.common.ApiException;
import com.farmtohome.api.common.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/email-otp")
public class EmailOtpController {
  private static final Logger log = LoggerFactory.getLogger(EmailOtpController.class);
  private final EmailOtpService service;

  public EmailOtpController(EmailOtpService service) {
    this.service = service;
  }

  @PostMapping("/send")
  public ApiResponse<Map<String, Object>> send(
      Principal principal,
      @RequestBody(required = false) EmailOtpDtos.RequestOtpRequest request) {
    String email = (request != null && request.email() != null && !request.email().isBlank())
        ? request.email().trim().toLowerCase()
        : (principal != null ? principal.getName() : null);

    log.info("[OTP-CONTROLLER] Received /send request for target email: {}", com.farmtohome.api.config.MailConfig.mask(email));

    if (email == null || email.isBlank()) {
      log.warn("[OTP-CONTROLLER] /send request rejected: Email address is required.");
      throw new ApiException(HttpStatus.BAD_REQUEST, "Email address is required.");
    }
    return ApiResponse.ok(service.sendForEmail(email), "Email OTP sent.");
  }

  @PostMapping("/verify")
  public ApiResponse<Map<String, Object>> verify(
      Principal principal,
      @Valid @RequestBody EmailOtpDtos.VerifyRequest request) {
    String email = (request.email() != null && !request.email().isBlank())
        ? request.email().trim().toLowerCase()
        : (principal != null ? principal.getName() : null);

    log.info("[OTP-CONTROLLER] Received /verify request for email: {}", com.farmtohome.api.config.MailConfig.mask(email));

    if (email == null || email.isBlank()) {
      log.warn("[OTP-CONTROLLER] /verify request rejected: Email address is required.");
      throw new ApiException(HttpStatus.BAD_REQUEST, "Email address is required.");
    }
    return ApiResponse.ok(
        service.verifyForEmail(email, request.otp()),
        "Email verified successfully.");
  }

  @GetMapping("/status")
  public ApiResponse<Map<String, Object>> status(
      Principal principal,
      @RequestParam(required = false) String email) {
    String target = (email != null && !email.isBlank())
        ? email.trim().toLowerCase()
        : (principal != null ? principal.getName() : null);

    log.info("[OTP-CONTROLLER] Received /status request for email: {}", com.farmtohome.api.config.MailConfig.mask(target));

    if (target == null) {
      return ApiResponse.ok(Map.of("email", "", "verified", false));
    }
    return ApiResponse.ok(service.statusForEmail(target));
  }

  @PostMapping({"/request", "/forgot-password", "/send-otp"})
  public ApiResponse<Map<String, Object>> request(
      @Valid @RequestBody EmailOtpDtos.RequestOtpRequest request) {
    String email = request.email().trim().toLowerCase();
    log.info("[OTP-CONTROLLER] Received /send-otp or /request for email: {}", com.farmtohome.api.config.MailConfig.mask(email));
    return ApiResponse.ok(
        service.sendForEmail(email),
        "Email OTP sent.");
  }

  @PostMapping({"/verify-reset", "/verify-otp", "/reset-password"})
  public ApiResponse<Map<String, Object>> verifyReset(
      @Valid @RequestBody EmailOtpDtos.VerifyResetRequest request) {
    String email = request.email().trim().toLowerCase();
    log.info("[OTP-CONTROLLER] Received /verify-otp for email: {}", com.farmtohome.api.config.MailConfig.mask(email));
    return ApiResponse.ok(
        service.verifyForEmail(email, request.otp().trim()),
        "Email verified successfully.");
  }

  @GetMapping({"/smtp-test", "/test-smtp"})
  public ApiResponse<Map<String, Object>> testSmtp() {
    log.info("[OTP-CONTROLLER] Received /smtp-test request");
    return ApiResponse.ok(service.testSmtpConnection(), "SMTP connection status evaluated.");
  }
}


