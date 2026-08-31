package com.farmtohome.api.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class ProductEntity {
  @Id
  @Column(length = 120)
  private String id;
  @Column(nullable = false)
  private String name;
  @Column(nullable = false)
  private String englishName;
  @Column(nullable = false)
  private String teluguName;
  @Column(nullable = false, length = 800)
  private String description;
  @Column(nullable = false)
  private String category;
  @Column(nullable = false, length = 500)
  private String imageUrl;
  @Column(nullable = false)
  private String unit;
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal price;
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal mrp;
  @Column(nullable = false)
  private String shopUnit;
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal shopPrice;
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal shopMrp;
  @Column(nullable = false)
  private int stockQuantity;
  @Column(nullable = false)
  private boolean active;
  @Column(nullable = false)
  private boolean fresh;
  @Column(nullable = false)
  private boolean available = true;
  @Column(nullable = false)
  private boolean deleted = false;
  @Column(nullable = false, precision = 3, scale = 2)
  private BigDecimal rating;
  @Column(nullable = false)
  private int reviewCount;
  @Column(nullable = false)
  private Instant createdAt;
  @Column(nullable = false)
  private Instant updatedAt;

  public String getId() { return id; }
  public String getName() { return name; }
  public String getEnglishName() { return englishName; }
  public String getTeluguName() { return teluguName; }
  public String getDescription() { return description; }
  public String getCategory() { return category; }
  public String getImageUrl() { return imageUrl; }
  public String getUnit() { return unit; }
  public BigDecimal getPrice() { return price; }
  public BigDecimal getMrp() { return mrp; }
  public String getShopUnit() { return shopUnit; }
  public BigDecimal getShopPrice() { return shopPrice; }
  public BigDecimal getShopMrp() { return shopMrp; }
  public int getStockQuantity() { return stockQuantity; }
  public boolean isActive() { return active; }
  public boolean isFresh() { return fresh; }
  public boolean isAvailable() { return available; }
  public boolean isDeleted() { return deleted; }
  public BigDecimal getRating() { return rating; }
  public int getReviewCount() { return reviewCount; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }

  public void setId(String id) { this.id = id; }
  public void setName(String name) { this.name = name; }
  public void setEnglishName(String englishName) { this.englishName = englishName; }
  public void setTeluguName(String teluguName) { this.teluguName = teluguName; }
  public void setDescription(String description) { this.description = description; }
  public void setCategory(String category) { this.category = category; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
  public void setUnit(String unit) { this.unit = unit; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public void setMrp(BigDecimal mrp) { this.mrp = mrp; }
  public void setShopUnit(String shopUnit) { this.shopUnit = shopUnit; }
  public void setShopPrice(BigDecimal shopPrice) { this.shopPrice = shopPrice; }
  public void setShopMrp(BigDecimal shopMrp) { this.shopMrp = shopMrp; }
  public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
  public void setActive(boolean active) { this.active = active; }
  public void setFresh(boolean fresh) { this.fresh = fresh; }
  public void setAvailable(boolean available) { this.available = available; }
  public void setDeleted(boolean deleted) { this.deleted = deleted; }
  public void setRating(BigDecimal rating) { this.rating = rating; }
  public void setReviewCount(int reviewCount) { this.reviewCount = reviewCount; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
