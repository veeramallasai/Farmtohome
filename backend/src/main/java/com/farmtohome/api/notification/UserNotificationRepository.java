package com.farmtohome.api.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotificationEntity, UUID> {
  List<UserNotificationEntity> findByOwnerUidOrderByCreatedAtDesc(String ownerUid);
}
