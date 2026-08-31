package com.farmtohome.api.review;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReviewEntity, UUID> {
  List<ProductReviewEntity> findByProductIdOrderByCreatedAtDesc(String productId);
  List<ProductReviewEntity> findByOwnerUidOrderByCreatedAtDesc(String ownerUid);
}
