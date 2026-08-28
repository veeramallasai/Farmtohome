import '../models/cart_model.dart';

class CheckoutRepository {
  CheckoutRepository();

  double calculateDeliveryFee({
    required double subtotal,
    required String mode,
    required String slot,
  }) {
    if (subtotal >= 499) return 0;
    if (mode == 'express') return 49;
    return subtotal < 299 ? 29 : 19;
  }

  double calculateTotal({
    required CartModel cart,
    required double discount,
    required double deliveryFee,
  }) {
    final double raw = cart.subtotal - discount + deliveryFee;
    return raw < 0 ? 0 : raw;
  }
}
