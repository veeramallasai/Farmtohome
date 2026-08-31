package com.farmtohome.api.activity;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/activities", "/v1/activities"})
public class UserActivityController {

  private final UserActivityService activityService;

  public UserActivityController(UserActivityService activityService) {
    this.activityService = activityService;
  }

  @GetMapping
  public List<UserActivityEntity> getActivities(
      Principal principal,
      @RequestHeader(value = "X-User-UID", required = false) String headerUid) {
    String uid = resolveUid(principal, headerUid);
    return activityService.getActivities(uid);
  }

  @PostMapping
  public UserActivityEntity logActivity(
      @RequestBody Map<String, Object> body,
      Principal principal,
      @RequestHeader(value = "X-User-UID", required = false) String headerUid) {
    String uid = resolveUid(principal, headerUid);
    String type = (String) body.getOrDefault("type", "USER_ACTION");
    String description = (String) body.getOrDefault("description", "");
    String metadata = (String) body.getOrDefault("metadata", "{}");
    return activityService.logActivity(uid, type, description, metadata);
  }

  private String resolveUid(Principal principal, String headerUid) {
    if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
      return principal.getName().trim();
    }
    if (headerUid != null && !headerUid.isBlank()) {
      return headerUid.trim();
    }
    return "customer_user";
  }
}
