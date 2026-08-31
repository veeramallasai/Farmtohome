package com.farmtohome.api.user;

import com.farmtohome.api.common.ApiException;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppUserService {
  private final AppUserRepository users;

  public AppUserService(AppUserRepository users) {
    this.users = users;
  }

  @Transactional
  AppUserDtos.Profile sync(String uid, AppUserDtos.SyncRequest request) {
    String cleanUid = text(uid);
    if (cleanUid.isEmpty()) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid login session.");
    }

    Instant now = Instant.now();
    boolean created = !users.existsById(cleanUid);
    AppUserEntity user = users.findById(cleanUid).orElseGet(AppUserEntity::new);
    if (created) {
      user.setFirebaseUid(cleanUid);
      user.setCreatedAt(now);
      user.setActive(true);
      user.setAuthProvider("EMAIL");
    }

    String firstName = preferred(request.firstName(), user.getFirstName());
    String lastName = preferred(request.lastName(), user.getLastName());
    String displayName = (firstName + " " + lastName).trim();
    if (displayName.isEmpty()) displayName = preferred(user.getDisplayName(), cleanUid);

    String email = preferred(request.email(), user.getEmail());
    if (email.isEmpty()) {
      if (cleanUid.contains("@")) {
        email = cleanUid;
      } else if (cleanUid.startsWith("session_anon_") && cleanUid.contains("@")) {
        try {
          String part = cleanUid.substring("session_anon_".length());
          int idx = part.lastIndexOf('_');
          email = (idx > 0) ? part.substring(0, idx) : part;
        } catch (Exception ignored) {
          email = cleanUid + "@farmtohome.internal";
        }
      } else {
        email = cleanUid + "@farmtohome.internal";
      }
    }

    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setDisplayName(displayName);
    user.setEmail(email);
    user.setPhoneNumber(preferred(request.phoneNumber(), user.getPhoneNumber()));
    user.setPhotoUrl(preferred(request.photoUrl(), user.getPhotoUrl()));
    user.setShoppingMode(choice(request.shoppingMode(), user.getShoppingMode(), "home", "shop"));
    user.setAccountType(choice(request.accountType(), user.getAccountType(), "customer", "shop_owner"));
    if (user.getAuthProvider() == null || user.getAuthProvider().isBlank()) {
      user.setAuthProvider("EMAIL");
    }
    user.setActive(true);
    user.setLastLoginAt(now);
    user.setUpdatedAt(now);

    try {
      return AppUserDtos.Profile.from(users.saveAndFlush(user));
    } catch (DataIntegrityViolationException error) {
      throw new ApiException(
          HttpStatus.CONFLICT,
          "This email address or mobile number is already linked to another account.");
    }
  }

  @Transactional(readOnly = true)
  AppUserDtos.Profile get(String uid) {
    return users.findById(uid)
        .map(AppUserDtos.Profile::from)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User profile not found."));
  }

  private static String choice(Object requested, Object current, String first, String second) {
    String value = text(requested).toLowerCase();
    if (value.equals(first) || value.equals(second)) return value;
    value = text(current).toLowerCase();
    return value.equals(second) ? second : first;
  }

  private static String preferred(Object primary, Object fallback) {
    String value = text(primary);
    return value.isEmpty() ? text(fallback) : value;
  }

  private static String text(Object value) {
    return value == null ? "" : value.toString().trim();
  }
}
