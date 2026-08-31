package com.farmtohome.api.tracking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_tracking_steps")
public class OrderTrackingEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "status", length = 60, nullable = false)
  private String status = "PLACED";

  @Column(name = "title", length = 200, nullable = false)
  private String title = "";

  @Column(name = "description", columnDefinition = "TEXT", nullable = false)
  private String description = "";

  @Column(name = "location", length = 200, nullable = false)
  private String location = "";

  @Column(name = "timestamp", nullable = false)
  private Instant timestamp = Instant.now();

  public OrderTrackingEntity() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public UUID getOrderId() { return orderId; }
  public void setOrderId(UUID orderId) { this.orderId = orderId; }

  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getLocation() { return location; }
  public void setLocation(String location) { this.location = location; }

  public Instant getTimestamp() { return timestamp; }
  public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
