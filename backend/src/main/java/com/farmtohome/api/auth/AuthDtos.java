package com.farmtohome.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

  public record LoginRequest(
      @NotBlank @Email String email,
      @NotBlank String password
  ) {}

  public record RegisterRequest(
      @NotBlank @Email String email,
      @NotBlank @Size(min = 6, message = "Password must be at least 6 characters.") String password,
      String firstName,
      String lastName
  ) {}

  public record SocialLoginRequest(
      String provider,
      String email,
      String firstName,
      String lastName,
      String photoUrl,
      String idToken,
      String token,
      String credential
  ) {}

  public record GoogleOAuthRequest(
      String idToken,
      String email,
      String name,
      String photoUrl
  ) {}

  public record ForgotPasswordRequest(
      @NotBlank @Email String email
  ) {}

  public record ResetPasswordRequest(
      @NotBlank @Email String email,
      String resetToken,
      String otpCode,
      String otp,
      String newPassword,
      String password
  ) {}

  public record AuthResponse(
      String accessToken,
      String userId,
      String email,
      String name,
      String photoUrl
  ) {}
}
