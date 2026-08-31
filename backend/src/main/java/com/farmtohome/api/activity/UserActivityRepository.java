package com.farmtohome.api.activity;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserActivityRepository extends JpaRepository<UserActivityEntity, Long> {
  List<UserActivityEntity> findByOwnerUidOrderByCreatedAtDesc(String ownerUid);
}
