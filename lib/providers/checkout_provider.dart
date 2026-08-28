import 'package:flutter/foundation.dart';

import '../data/models/cart_model.dart';
import '../data/repositories/checkout_repository.dart';

class CheckoutProvider extends ChangeNotifier {
  CheckoutProvider({CheckoutRepository? repository})
    : _repository = repository ?? CheckoutRepository();

  final CheckoutRepository _repository;
  CartModel? _cart;
  String _deliveryMethod = 'quick';
  String? _errorMessage;

  CartModel? get cart => _cart;
  String get deliveryMethod => _deliveryMethod;
  String? get errorMessage => _errorMessage;

  void initialize(CartModel cart, {String deliveryMethod = 'quick'}) {
    _cart = cart;
    _deliveryMethod = deliveryMethod.trim().toLowerCase();
    notifyListeners();
  }

  void setDeliveryMethod(String value) {
    _deliveryMethod = value.trim().toLowerCase();
    notifyListeners();
  }
}
