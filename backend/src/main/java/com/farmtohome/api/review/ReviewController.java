package com.farmtohome.api.review;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class ReviewController {

  private final ReviewService reviewService;

  public ReviewController(ReviewService reviewService) {
    this.reviewService = reviewService;
  }

  @GetMapping({"/api/v1/products/{productId}/reviews", "/v1/products/{productId}/reviews"})
  public List<ProductReviewEntity> getProductReviews(@PathVariable String productId) {
    return reviewService.getProductReviews(productId);
  }

  @PostMapping({"/api/v1/products/{productId}/reviews", "/v1/products/{productId}/reviews"})
  public Map<String, Object> addReview(
      @PathVariable String productId,
      @RequestBody Map<String, Object> body,
      Principal principal,
      @RequestHeader(value = "X-User-UID", required = false) String headerUid) {
    String uid = resolveUid(principal, headerUid);
    String userName = (String) body.getOrDefault("userName", "Customer");
    Number ratingNum = (Number) body.getOrDefault("rating", 5.0);
    String comment = (String) body.getOrDefault("comment", "");
    return reviewService.addReview(productId, uid, userName, ratingNum.doubleValue(), comment);
  }

  @DeleteMapping({"/api/v1/reviews/{reviewId}", "/v1/reviews/{reviewId}"})
  public Map<String, Object> deleteReview(@PathVariable String reviewId) {
    try {
      reviewService.deleteReview(UUID.fromString(reviewId.trim()));
      return Map.of("success", true);
    } catch (Exception e) {
      return Map.of("success", false, "error", e.getMessage());
    }
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
