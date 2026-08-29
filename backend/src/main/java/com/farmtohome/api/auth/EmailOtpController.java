package com.farmtohome.api.auth;

import com.farmtohome.api.common.ApiException;
import com.farmtohome.api.common.ApiResponse;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
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

    if (email == null || email.isBlank()) {
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

    if (email == null || email.isBlank()) {
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

    if (target == null) {
      return ApiResponse.ok(Map.of("email", "", "verified", false));
    }
    return ApiResponse.ok(service.statusForEmail(target));
  }

  @PostMapping({"/request", "/forgot-password", "/send-otp"})
  public ApiResponse<Map<String, Object>> request(
      @Valid @RequestBody EmailOtpDtos.RequestOtpRequest request) {
    return ApiResponse.ok(
        service.sendForEmail(request.email().trim().toLowerCase()),
        "Email OTP sent.");
  }

  @PostMapping({"/verify-reset", "/verify-otp", "/reset-password"})
  public ApiResponse<Map<String, Object>> verifyReset(
      @Valid @RequestBody EmailOtpDtos.VerifyResetRequest request) {
    return ApiResponse.ok(
        service.verifyForEmail(request.email().trim().toLowerCase(), request.otp().trim()),
        "Email verified successfully.");
  }

  @GetMapping({"/smtp-test", "/test-smtp"})
  public ApiResponse<Map<String, Object>> testSmtp() {
    return ApiResponse.ok(service.testSmtpConnection(), "SMTP connection status evaluated.");
  }
}

