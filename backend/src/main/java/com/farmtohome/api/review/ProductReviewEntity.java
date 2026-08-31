package com.farmtohome.api.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_reviews")
public class ProductReviewEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @Column(name = "product_id", length = 120, nullable = false)
  private String productId = "";

  @Column(name = "owner_uid", length = 160, nullable = false)
  private String ownerUid = "";

  @Column(name = "user_name", length = 160, nullable = false)
  private String userName = "Customer";

  @Column(name = "rating", precision = 3, scale = 2, nullable = false)
  private BigDecimal rating = BigDecimal.valueOf(5.0);

  @Column(name = "comment", columnDefinition = "TEXT", nullable = false)
  private String comment = "";

  @Column(name = "images_json", columnDefinition = "TEXT", nullable = false)
  private String imagesJson = "[]";

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public ProductReviewEntity() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getProductId() { return productId; }
  public void setProductId(String productId) { this.productId = productId; }

  public String getOwnerUid() { return ownerUid; }
  public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

  public String getUserName() { return userName; }
  public void setUserName(String userName) { this.userName = userName; }

  public BigDecimal getRating() { return rating; }
  public void setRating(BigDecimal rating) { this.rating = rating; }

  public String getComment() { return comment; }
  public void setComment(String comment) { this.comment = comment; }

  public String getImagesJson() { return imagesJson; }
  public void setImagesJson(String imagesJson) { this.imagesJson = imagesJson; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
