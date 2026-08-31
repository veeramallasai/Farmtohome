package com.farmtohome.api.activity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_activities")
public class UserActivityEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "owner_uid", length = 160, nullable = false)
  private String ownerUid = "";

  @Column(name = "activity_type", length = 80, nullable = false)
  private String activityType = "";

  @Column(name = "description", columnDefinition = "TEXT", nullable = false)
  private String description = "";

  @Column(name = "metadata_json", columnDefinition = "TEXT", nullable = false)
  private String metadataJson = "{}";

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  public UserActivityEntity() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getOwnerUid() { return ownerUid; }
  public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

  public String getActivityType() { return activityType; }
  public void setActivityType(String activityType) { this.activityType = activityType; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getMetadataJson() { return metadataJson; }
  public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
