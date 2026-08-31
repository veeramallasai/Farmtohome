package com.farmtohome.api.tracking;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class OrderTrackingController {

  private final OrderTrackingService trackingService;

  public OrderTrackingController(OrderTrackingService trackingService) {
    this.trackingService = trackingService;
  }

  @GetMapping({"/api/v1/orders/{orderId}/tracking", "/v1/orders/{orderId}/tracking"})
  public List<OrderTrackingEntity> getOrderTracking(@PathVariable String orderId) {
    try {
      return trackingService.getTrackingForOrder(UUID.fromString(orderId.trim()));
    } catch (Exception e) {
      return List.of();
    }
  }
}
