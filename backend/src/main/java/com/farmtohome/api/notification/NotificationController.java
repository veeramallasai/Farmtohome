package com.farmtohome.api.notification;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping({"/api/v1/notifications", "/v1/notifications"})
  public List<UserNotificationEntity> getNotifications(
      Principal principal,
      @RequestHeader(value = "X-User-UID", required = false) String headerUid) {
    String uid = resolveUid(principal, headerUid);
    return notificationService.getNotifications(uid);
  }

  @PatchMapping({"/api/v1/notifications/{id}/read", "/v1/notifications/{id}/read"})
  public Map<String, Object> markAsRead(
      @PathVariable String id,
      Principal principal,
      @RequestHeader(value = "X-User-UID", required = false) String headerUid) {
    String uid = resolveUid(principal, headerUid);
    return notificationService.markAsRead(uid, UUID.fromString(id.trim()));
  }

  @PatchMapping({"/api/v1/notifications/read-all", "/v1/notifications/read-all"})
  public Map<String, Object> markAllAsRead(
      Principal principal,
      @RequestHeader(value = "X-User-UID", required = false) String headerUid) {
    String uid = resolveUid(principal, headerUid);
    return notificationService.markAllAsRead(uid);
  }

  @DeleteMapping({"/api/v1/notifications/{id}", "/v1/notifications/{id}"})
  public Map<String, Object> deleteNotification(
      @PathVariable String id,
      Principal principal,
      @RequestHeader(value = "X-User-UID", required = false) String headerUid) {
    String uid = resolveUid(principal, headerUid);
    notificationService.deleteNotification(uid, UUID.fromString(id.trim()));
    return Map.of("success", true);
  }

  @GetMapping({"/api/v1/notification-preferences", "/v1/notification-preferences"})
  public Map<String, Boolean> getPreferences(
      Principal principal,
      @RequestHeader(value = "X-User-UID", required = false) String headerUid) {
    String uid = resolveUid(principal, headerUid);
    return notificationService.getPreferences(uid);
  }

  @PutMapping({"/api/v1/notification-preferences", "/v1/notification-preferences"})
  public Map<String, Boolean> updatePreferences(
      @RequestBody Map<String, Object> body,
      Principal principal,
      @RequestHeader(value = "X-User-UID", required = false) String headerUid) {
    String uid = resolveUid(principal, headerUid);
    Boolean orderUpdates = (Boolean) body.get("orderUpdates");
    Boolean offers = (Boolean) body.get("offers");
    return notificationService.updatePreferences(uid, orderUpdates, offers);
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
