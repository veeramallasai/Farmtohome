package com.farmtohome.api.notification;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationService {

  private final UserNotificationRepository notificationRepository;
  private final NotificationPreferenceRepository preferenceRepository;

  public NotificationService(
      UserNotificationRepository notificationRepository,
      NotificationPreferenceRepository preferenceRepository) {
    this.notificationRepository = notificationRepository;
    this.preferenceRepository = preferenceRepository;
  }

  @Transactional(readOnly = true)
  public List<UserNotificationEntity> getNotifications(String ownerUid) {
    List<UserNotificationEntity> list = notificationRepository.findByOwnerUidOrderByCreatedAtDesc(ownerUid);
    if (list.isEmpty()) {
      // Create welcoming notification for new user
      UserNotificationEntity welcome = new UserNotificationEntity();
      welcome.setOwnerUid(ownerUid);
      welcome.setTitle("Welcome to Farm To Home!");
      welcome.setBody("Explore fresh farm produce, vegetables, fruits and organic products delivered directly to your doorstep.");
      welcome.setType("welcome");
      welcome.setIsRead(false);
      welcome.setCreatedAt(Instant.now());
      notificationRepository.save(welcome);
      return List.of(welcome);
    }
    return list;
  }

  @Transactional
  public Map<String, Object> markAsRead(String ownerUid, UUID notificationId) {
    notificationRepository.findById(notificationId).ifPresent(n -> {
      if (n.getOwnerUid().equals(ownerUid)) {
        n.setIsRead(true);
        notificationRepository.save(n);
      }
    });
    return Map.of("success", true);
  }

  @Transactional
  public Map<String, Object> markAllAsRead(String ownerUid) {
    List<UserNotificationEntity> list = notificationRepository.findByOwnerUidOrderByCreatedAtDesc(ownerUid);
    for (UserNotificationEntity n : list) {
      n.setIsRead(true);
    }
    notificationRepository.saveAll(list);
    return Map.of("success", true);
  }

  @Transactional
  public void deleteNotification(String ownerUid, UUID notificationId) {
    notificationRepository.findById(notificationId).ifPresent(n -> {
      if (n.getOwnerUid().equals(ownerUid)) {
        notificationRepository.delete(n);
      }
    });
  }

  @Transactional(readOnly = true)
  public Map<String, Boolean> getPreferences(String ownerUid) {
    return preferenceRepository.findById(ownerUid)
        .map(p -> Map.of("orderUpdates", p.getOrderUpdates(), "offers", p.getOffers()))
        .orElse(Map.of("orderUpdates", true, "offers", true));
  }

  @Transactional
  public Map<String, Boolean> updatePreferences(String ownerUid, Boolean orderUpdates, Boolean offers) {
    NotificationPreferenceEntity pref = preferenceRepository.findById(ownerUid)
        .orElseGet(() -> {
          NotificationPreferenceEntity p = new NotificationPreferenceEntity();
          p.setOwnerUid(ownerUid);
          return p;
        });
    if (orderUpdates != null) pref.setOrderUpdates(orderUpdates);
    if (offers != null) pref.setOffers(offers);
    pref.setUpdatedAt(Instant.now());
    preferenceRepository.save(pref);
    return Map.of("orderUpdates", pref.getOrderUpdates(), "offers", pref.getOffers());
  }
}
