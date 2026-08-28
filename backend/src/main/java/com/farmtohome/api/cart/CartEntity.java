package com.farmtohome.api.cart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "carts")
public class CartEntity {
  @Id
  private String ownerUid;
  @Column(nullable = false)
  private String shoppingMode;
  @Column(nullable = false)
  private Instant updatedAt;

  protected CartEntity() {}

  public CartEntity(String ownerUid, String shoppingMode) {
    this.ownerUid = ownerUid;
    this.shoppingMode = shoppingMode;
    this.updatedAt = Instant.now();
  }

  public String getOwnerUid() { return ownerUid; }
  public String getShoppingMode() { return shoppingMode; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setShoppingMode(String shoppingMode) { this.shoppingMode = shoppingMode; }
  public void touch() { this.updatedAt = Instant.now(); }
}
