package com.farmtohome.api.review;

import com.farmtohome.api.product.ProductEntity;
import com.farmtohome.api.product.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

  private final ProductReviewRepository productReviewRepository;
  private final ProductRepository productRepository;

  public ReviewService(
      ProductReviewRepository productReviewRepository,
      ProductRepository productRepository) {
    this.productReviewRepository = productReviewRepository;
    this.productRepository = productRepository;
  }

  @Transactional(readOnly = true)
  public List<ProductReviewEntity> getProductReviews(String productId) {
    return productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId.trim());
  }

  @Transactional
  public Map<String, Object> addReview(String productId, String ownerUid, String userName, Double rating, String comment) {
    String pid = productId.trim();
    ProductReviewEntity review = new ProductReviewEntity();
    review.setProductId(pid);
    review.setOwnerUid(ownerUid);
    review.setUserName((userName != null && !userName.isBlank()) ? userName.trim() : "Customer");
    review.setRating(BigDecimal.valueOf(rating != null ? rating : 5.0).setScale(2, RoundingMode.HALF_UP));
    review.setComment(comment != null ? comment.trim() : "");
    review.setCreatedAt(Instant.now());
    review.setUpdatedAt(Instant.now());

    ProductReviewEntity saved = productReviewRepository.save(review);

    // Recalculate product rating & review_count in real-time
    recalculateProductRating(pid);

    return Map.of(
        "id", saved.getId().toString(),
        "productId", pid,
        "rating", saved.getRating(),
        "comment", saved.getComment(),
        "userName", saved.getUserName()
    );
  }

  @Transactional
  public void deleteReview(UUID reviewId) {
    productReviewRepository.findById(reviewId).ifPresent(review -> {
      String pid = review.getProductId();
      productReviewRepository.delete(review);
      recalculateProductRating(pid);
    });
  }

  private void recalculateProductRating(String productId) {
    List<ProductReviewEntity> reviews = productReviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    productRepository.findById(productId).ifPresent(product -> {
      int count = reviews.size();
      product.setReviewCount(count);
      if (count > 0) {
        double sum = reviews.stream()
            .mapToDouble(r -> r.getRating().doubleValue())
            .sum();
        double avg = sum / count;
        product.setRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
      } else {
        product.setRating(BigDecimal.ZERO);
      }
      productRepository.save(product);
    });
  }
}
