package com.farmtohome.api.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferenceEntity {

  @Id
  @Column(name = "owner_uid", length = 160, nullable = false)
  private String ownerUid;

  @Column(name = "order_updates", nullable = false)
  private Boolean orderUpdates = true;

  @Column(name = "offers", nullable = false)
  private Boolean offers = true;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public NotificationPreferenceEntity() {}

  public String getOwnerUid() { return ownerUid; }
  public void setOwnerUid(String ownerUid) { this.ownerUid = ownerUid; }

  public Boolean getOrderUpdates() { return orderUpdates; }
  public void setOrderUpdates(Boolean orderUpdates) { this.orderUpdates = orderUpdates; }

  public Boolean getOffers() { return offers; }
  public void setOffers(Boolean offers) { this.offers = offers; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
