package com.farmtohome.api.auth;

import com.farmtohome.api.common.ApiException;
import com.farmtohome.api.common.ApiResponse;
import com.farmtohome.api.user.AppUserEntity;
import com.farmtohome.api.user.AppUserRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/auth", "/v1/auth", "/auth"})
public class AuthController {

  private final AppUserRepository userRepository;
  private final EmailOtpService emailOtpService;
  private final GoogleTokenVerifierService googleTokenVerifierService;
  private final JwtService jwtService;

  public AuthController(
      AppUserRepository userRepository,
      EmailOtpService emailOtpService,
      GoogleTokenVerifierService googleTokenVerifierService,
      JwtService jwtService) {
    this.userRepository = userRepository;
    this.emailOtpService = emailOtpService;
    this.googleTokenVerifierService = googleTokenVerifierService;
    this.jwtService = jwtService;
  }

  @PostMapping("/login")
  public ApiResponse<AuthDtos.AuthResponse> login(
      @Valid @RequestBody AuthDtos.LoginRequest request) {
    String email = request.email().trim().toLowerCase();

    Optional<AppUserEntity> entity = userRepository.findByEmail(email);
    if (entity.isEmpty()) {
      // Auto-provision unverified user profile and require OTP verification
      String uid = "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
      String name = email.split("@")[0];
      AppUserEntity newUser = findOrCreateUserEntity(uid, email, name, null);
      newUser.setEmailVerified(false);
      newUser.setActive(false);
      userRepository.save(newUser);

      emailOtpService.sendForEmail(email);
      throw new ApiException(HttpStatus.FORBIDDEN, "Account created. An OTP has been sent to your email for verification.");
    }

    AppUserEntity u = entity.get();
    if (!u.isEmailVerified()) {
      emailOtpService.sendForEmail(email);
      throw new ApiException(HttpStatus.FORBIDDEN, "Your email is not verified yet. A new OTP has been sent to your email.");
    }

    if (!u.isActive()) {
      u.setActive(true);
      userRepository.save(u);
    }

    return processUserLogin(u.getFirebaseUid(), email, u.getDisplayName(), u.getPhotoUrl());
  }

  @PostMapping("/register")
  public ApiResponse<Map<String, Object>> register(
      @Valid @RequestBody AuthDtos.RegisterRequest request) {
    String email = request.email().trim().toLowerCase();

    Optional<AppUserEntity> existingOpt = userRepository.findByEmail(email);
    if (existingOpt.isPresent()) {
      AppUserEntity existing = existingOpt.get();
      if (existing.isEmailVerified()) {
        throw new ApiException(HttpStatus.CONFLICT, "An account already exists with this email. Please login.");
      }
    }

    String uid = existingOpt.map(AppUserEntity::getFirebaseUid)
        .orElseGet(() -> "usr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    String rawName = ((request.firstName() != null ? request.firstName() : "") + " " + (request.lastName() != null ? request.lastName() : "")).trim();
    final String name = rawName.isEmpty() ? email.split("@")[0] : rawName;

    AppUserEntity entity = existingOpt.orElseGet(() -> findOrCreateUserEntity(uid, email, name, null));
    if (request.firstName() != null) entity.setFirstName(request.firstName());
    if (request.lastName() != null) entity.setLastName(request.lastName());
    entity.setEmailVerified(false);
    entity.setActive(false);
    entity.setAuthProvider("EMAIL");
    userRepository.save(entity);

    Map<String, Object> otpResult = emailOtpService.sendForEmail(email);

    Map<String, Object> response = new java.util.LinkedHashMap<>();
    response.put("email", email);
    response.put("maskedEmail", com.farmtohome.api.config.MailConfig.mask(email));
    response.put("requiresEmailVerification", true);
    response.put("message", "Registration successful. Please verify the OTP sent to your email.");
    if (otpResult.containsKey("expiresInSeconds")) response.put("expiresInSeconds", otpResult.get("expiresInSeconds"));

    return ApiResponse.ok(response, "Registration successful. Please verify the OTP sent to your email.");
  }

  @PostMapping({"/social-login", "/google", "/google-login", "/google/login", "/google-signin", "/social", "/social/login"})
  public ApiResponse<AuthDtos.AuthResponse> socialLogin(
      @RequestBody AuthDtos.SocialLoginRequest request) {
    String prov = (request != null && request.provider() != null && !request.provider().isBlank())
        ? request.provider().trim().toLowerCase()
        : "google";

    String rawToken = request != null
        ? (request.idToken() != null ? request.idToken() : (request.token() != null ? request.token() : request.credential()))
        : null;

    if (rawToken == null || rawToken.isBlank()) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Google ID token is required.");
    }

    GoogleTokenVerifierService.GoogleUser googleUser = googleTokenVerifierService.verify(rawToken);

    String email = (request != null && request.email() != null && !request.email().isBlank())
        ? request.email().trim().toLowerCase()
        : googleUser.email();

    if (email == null || email.isBlank() || !email.equalsIgnoreCase(googleUser.email())) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Google account email could not be verified.");
    }

    Optional<AppUserEntity> existing = userRepository.findByEmail(email);
    String uid = existing.map(AppUserEntity::getFirebaseUid)
        .orElse("google_" + googleUser.subject());

    String firstName = (request != null && request.firstName() != null) ? request.firstName() : "";
    String lastName = (request != null && request.lastName() != null) ? request.lastName() : "";
    String name = (firstName + " " + lastName).trim();

    if (name.isEmpty() && googleUser.name() != null) {
      name = googleUser.name().trim();
    }
    if (name.isEmpty()) name = email.split("@")[0];

    String photo = (request != null && request.photoUrl() != null) ? request.photoUrl() : null;
    if ((photo == null || photo.isBlank()) && googleUser.picture() != null) {
      photo = googleUser.picture().trim();
    }

    AppUserEntity entity = findOrCreateUserEntity(uid, email, name, photo);
    entity.setAuthProvider(prov);
    entity.setEmailVerified(true);
    entity.setActive(true);
    entity.setDisplayName(name);
    entity.setPhotoUrl(photo);
    userRepository.save(entity);

    return processUserLogin(uid, email, entity.getDisplayName(), entity.getPhotoUrl());
  }

  @PostMapping({"/forgot-password", "/email-otp/forgot-password", "/request-otp", "/email-otp/request-otp"})
  public ApiResponse<Map<String, Object>> forgotPassword(
      @Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
    Map<String, Object> response = emailOtpService.sendForEmail(request.email().trim().toLowerCase());
    return ApiResponse.ok(response, "Password reset OTP sent to email.");
  }

  @PostMapping({"/reset-password", "/email-otp/reset-password", "/email-otp/verify-reset-password"})
  public ApiResponse<Map<String, Object>> resetPassword(
      @Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
    String email = request.email().trim().toLowerCase();
    String newPw = (request.newPassword() != null && !request.newPassword().isBlank())
        ? request.newPassword().trim()
        : (request.password() != null ? request.password().trim() : "");

    if (newPw.length() < 6) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters.");
    }

    String tokenOrOtp = (request.resetToken() != null && !request.resetToken().isBlank())
        ? request.resetToken().trim()
        : (request.otpCode() != null && !request.otpCode().isBlank() ? request.otpCode().trim() : (request.otp() != null ? request.otp().trim() : ""));

    Map<String, Object> response = emailOtpService.resetPasswordForEmail(email, tokenOrOtp, newPw);
    return ApiResponse.ok(response, "Password reset successfully. You can now login with your new password.");
  }

  private ApiResponse<AuthDtos.AuthResponse> processUserLogin(
      String uid, String email, String name, String photoUrl) {
    String token = jwtService.generateToken(uid, email, name, photoUrl);

    userRepository.findById(uid).ifPresent(user -> {
      user.setLastLoginAt(Instant.now());
      userRepository.save(user);
    });

    AuthDtos.AuthResponse response = new AuthDtos.AuthResponse(
        token, uid, email, name != null ? name : email.split("@")[0], photoUrl);
    return ApiResponse.ok(response, "Authentication successful.");
  }

  private AppUserEntity findOrCreateUserEntity(
      String uid, String email, String displayName, String photoUrl) {
    return userRepository.findById(uid).orElseGet(() -> {
      AppUserEntity user = new AppUserEntity();
      user.setFirebaseUid(uid);
      user.setEmail(email != null ? email : "");
      user.setDisplayName(displayName != null ? displayName : (email != null && email.contains("@") ? email.split("@")[0] : uid));
      user.setPhotoUrl(photoUrl != null ? photoUrl : "");
      if (user.getAuthProvider() == null || user.getAuthProvider().isBlank()) {
        user.setAuthProvider("EMAIL");
      }
      user.setActive(true);
      user.setCreatedAt(Instant.now());
      user.setUpdatedAt(Instant.now());
      user.setLastLoginAt(Instant.now());
      return userRepository.save(user);
    });
  }

}
