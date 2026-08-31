package com.farmtohome.api.wishlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
    name = "wishlist_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_wishlist_owner_product",
        columnNames = {"owner_uid", "product_id"}
    )
)
public class WishlistEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "owner_uid", length = 160, nullable = false)
  private String ownerUid = "";

  @Column(name = "product_id", length = 120, nullable = false)
  private String productId = "";

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public WishlistEntity() {}

  public WishlistEntity(String ownerUid, String productId) {
    this.ownerUid = ownerUid;
    this.productId = productId;
    this.createdAt = Instant.now();
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getOwnerUid() { return ownerUid; }
  public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

  public String getProductId() { return productId; }
  public void setProductId(String productId) { this.productId = productId; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
