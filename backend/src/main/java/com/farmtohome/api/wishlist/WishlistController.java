package com.farmtohome.api.wishlist;

import com.farmtohome.api.product.ProductEntity;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/favorites", "/v1/favorites", "/api/v1/wishlist", "/v1/wishlist"})
public class WishlistController {

  private final WishlistService wishlistService;

  public WishlistController(WishlistService wishlistService) {
    this.wishlistService = wishlistService;
  }

  @GetMapping
  public List<ProductEntity> getFavorites(
      Principal principal,
      @RequestHeader(value = "X-User-UID", required = false) String headerUid) {
    String uid = resolveUid(principal, headerUid);
    return wishlistService.getFavorites(uid);
  }

  @PostMapping("/{productId}")
  public Map<String, Object> addFavorite(
      @PathVariable String productId,
      Principal principal,
      @RequestHeader(value = "X-User-UID", required = false) String headerUid) {
    String uid = resolveUid(principal, headerUid);
    return wishlistService.addFavorite(uid, productId.trim());
  }

  @DeleteMapping("/{productId}")
  public Map<String, Object> removeFavorite(
      @PathVariable String productId,
      Principal principal,
      @RequestHeader(value = "X-User-UID", required = false) String headerUid) {
    String uid = resolveUid(principal, headerUid);
    return wishlistService.removeFavorite(uid, productId.trim());
  }

  private String resolveUid(Principal principal, String headerUid) {
    if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
      return principal.getName().trim();
    }
    if (headerUid != null && !headerUid.isBlank()) {
      return headerUid.trim();
    }
    return "guest_user";
  }
}
