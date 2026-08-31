package com.farmtohome.api.activity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserActivityService {

  private final UserActivityRepository activityRepository;

  public UserActivityService(UserActivityRepository activityRepository) {
    this.activityRepository = activityRepository;
  }

  @Transactional(readOnly = true)
  public List<UserActivityEntity> getActivities(String ownerUid) {
    return activityRepository.findByOwnerUidOrderByCreatedAtDesc(ownerUid);
  }

  @Transactional
  public UserActivityEntity logActivity(String ownerUid, String type, String description, String metadataJson) {
    UserActivityEntity activity = new UserActivityEntity();
    activity.setOwnerUid(ownerUid);
    activity.setActivityType(type != null ? type.trim() : "general");
    activity.setDescription(description != null ? description.trim() : "");
    activity.setMetadataJson(metadataJson != null ? metadataJson.trim() : "{}");
    activity.setCreatedAt(Instant.now());
    return activityRepository.save(activity);
  }
}
