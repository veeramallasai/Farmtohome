import '../models/cart_model.dart';
import '../models/coupon_model.dart';

class CheckoutRepository {
  CheckoutRepository();

  List<CouponModel> get localCoupons => const <CouponModel>[
    CouponModel(
      id: 'fresh10',
      code: 'FRESH10',
      title: '10% fresh savings',
      description: 'Get 10% off up to ₹100',
      discountValue: 10,
      minimumOrder: 299,
      maximumDiscount: 100,
    ),
    CouponModel(
      id: 'farm50',
      code: 'FARM50',
      title: 'Flat ₹50 off',
      description: 'Save ₹50 on orders above ₹499',
      discountType: 'fixed',
      discountValue: 50,
      minimumOrder: 499,
    ),
  ];

  Future<CouponModel?> findCoupon(String code) async {
    final String normalized = code.trim().toUpperCase();
    if (normalized.isEmpty) return null;
    for (final CouponModel coupon in localCoupons) {
      if (coupon.code == normalized) return coupon;
    }
    return null;
  }

  Future<double> validateAndCalculateCoupon({
    required String code,
    required double subtotal,
  }) async {
    final CouponModel? coupon = await findCoupon(code);
    if (coupon == null) throw StateError('Coupon code is not valid.');
    if (!coupon.isCurrentlyValid) {
      throw StateError('Coupon offer is expired.');
    }
    if (subtotal < coupon.minimumOrder) {
      throw StateError('Add items worth ₹${coupon.minimumOrder.toInt()} to use this code.');
    }
    return coupon.calculateDiscount(subtotal);
  }

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
