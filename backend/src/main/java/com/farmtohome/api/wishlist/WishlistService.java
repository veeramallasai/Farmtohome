package com.farmtohome.api.wishlist;

import com.farmtohome.api.product.ProductEntity;
import com.farmtohome.api.product.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {

  private final WishlistRepository wishlistRepository;
  private final ProductRepository productRepository;

  public WishlistService(WishlistRepository wishlistRepository, ProductRepository productRepository) {
    this.wishlistRepository = wishlistRepository;
    this.productRepository = productRepository;
  }

  @Transactional(readOnly = true)
  public List<ProductEntity> getFavorites(String ownerUid) {
    List<WishlistEntity> items = wishlistRepository.findByOwnerUidOrderByCreatedAtDesc(ownerUid);
    List<String> productIds = items.stream().map(WishlistEntity::getProductId).toList();
    if (productIds.isEmpty()) {
      return List.of();
    }
    return productRepository.findAllById(productIds);
  }

  @Transactional
  public Map<String, Object> addFavorite(String ownerUid, String productId) {
    if (!wishlistRepository.existsByOwnerUidAndProductId(ownerUid, productId)) {
      wishlistRepository.save(new WishlistEntity(ownerUid, productId));
    }
    return Map.of("success", true, "productId", productId);
  }

  @Transactional
  public Map<String, Object> removeFavorite(String ownerUid, String productId) {
    wishlistRepository.deleteByOwnerUidAndProductId(ownerUid, productId);
    return Map.of("success", true, "productId", productId);
  }

  @Transactional(readOnly = true)
  public boolean isFavorite(String ownerUid, String productId) {
    return wishlistRepository.existsByOwnerUidAndProductId(ownerUid, productId);
  }
}
