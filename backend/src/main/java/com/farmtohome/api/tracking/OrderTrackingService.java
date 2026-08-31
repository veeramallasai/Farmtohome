package com.farmtohome.api.tracking;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderTrackingService {

  private final OrderTrackingRepository trackingRepository;

  public OrderTrackingService(OrderTrackingRepository trackingRepository) {
    this.trackingRepository = trackingRepository;
  }

  @Transactional(readOnly = true)
  public List<OrderTrackingEntity> getTrackingForOrder(UUID orderId) {
    List<OrderTrackingEntity> list = trackingRepository.findByOrderIdOrderByTimestampAsc(orderId);
    if (list.isEmpty()) {
      // Seed default delivery tracking timeline
      List<OrderTrackingEntity> steps = new ArrayList<>();
      
      OrderTrackingEntity step1 = new OrderTrackingEntity();
      step1.setOrderId(orderId);
      step1.setStatus("PLACED");
      step1.setTitle("Order Placed");
      step1.setDescription("Your order has been received and confirmed by the farm partner.");
      step1.setLocation("Local Farm Hub");
      step1.setTimestamp(Instant.now().minusSeconds(1800));
      steps.add(step1);

      OrderTrackingEntity step2 = new OrderTrackingEntity();
      step2.setOrderId(orderId);
      step2.setStatus("PACKED");
      step2.setTitle("Freshly Packed");
      step2.setDescription("Produce has been harvested and packed into eco-friendly containers.");
      step2.setLocation("Quality Inspection Hub");
      step2.setTimestamp(Instant.now().minusSeconds(900));
      steps.add(step2);

      OrderTrackingEntity step3 = new OrderTrackingEntity();
      step3.setOrderId(orderId);
      step3.setStatus("OUT_FOR_DELIVERY");
      step3.setTitle("Out for Delivery");
      step3.setDescription("Delivery partner is on the way to your address.");
      step3.setLocation("En route");
      step3.setTimestamp(Instant.now().minusSeconds(300));
      steps.add(step3);

      return trackingRepository.saveAll(steps);
    }
    return list;
  }
}
