package com.farmtohome.api.tracking;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderTrackingRepository extends JpaRepository<OrderTrackingEntity, Long> {
  List<OrderTrackingEntity> findByOrderIdOrderByTimestampAsc(UUID orderId);
}
