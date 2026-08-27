package com.farmtohome.api.user;

import com.farmtohome.api.common.ApiException;
import com.farmtohome.api.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class AppUserController {
  private final AppUserService users;

  public AppUserController(AppUserService users) {
    this.users = users;
  }

  @PutMapping("/me")
  ApiResponse<AppUserDtos.Profile> sync(
      Authentication authentication,
      @Valid @RequestBody AppUserDtos.SyncRequest request) {
    if (authentication == null || authentication.getName() == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid login session.");
    }
    return ApiResponse.ok(users.sync(authentication.getName(), request), "Profile synchronized.");
  }

  @GetMapping("/me")
  ApiResponse<AppUserDtos.Profile> me(Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid login session.");
    }
    return ApiResponse.ok(users.get(authentication.getName()));
  }
}
