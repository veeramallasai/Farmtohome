package com.farmtohome.api.category;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "categories")
public class CategoryEntity {

  @Id
  @Column(name = "id", length = 80, nullable = false)
  private String id;

  @Column(name = "name", length = 160, nullable = false)
  private String name;

  @Column(name = "english_name", length = 160, nullable = false)
  private String englishName = "";

  @Column(name = "telugu_name", length = 160, nullable = false)
  private String teluguName = "";

  @Column(name = "image_url", length = 500, nullable = false)
  private String imageUrl = "";

  @Column(name = "icon_name", length = 80, nullable = false)
  private String iconName = "";

  @Column(name = "display_order", nullable = false)
  private Integer displayOrder = 0;

  @Column(name = "active", nullable = false)
  private Boolean active = true;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public CategoryEntity() {}

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getEnglishName() { return englishName; }
  public void setEnglishName(String englishName) { this.englishName = englishName; }

  public String getTeluguName() { return teluguName; }
  public void setTeluguName(String teluguName) { this.teluguName = teluguName; }

  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

  public String getIconName() { return iconName; }
  public void setIconName(String iconName) { this.iconName = iconName; }

  public Integer getDisplayOrder() { return displayOrder; }
  public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

  public Boolean getActive() { return active; }
  public void setActive(Boolean active) { this.active = active; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
