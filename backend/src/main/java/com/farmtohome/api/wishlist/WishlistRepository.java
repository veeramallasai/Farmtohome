package com.farmtohome.api.wishlist;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends JpaRepository<WishlistEntity, Long> {
  List<WishlistEntity> findByOwnerUidOrderByCreatedAtDesc(String ownerUid);
  Optional<WishlistEntity> findByOwnerUidAndProductId(String ownerUid, String productId);
  boolean existsByOwnerUidAndProductId(String ownerUid, String productId);
  void deleteByOwnerUidAndProductId(String ownerUid, String productId);
}
