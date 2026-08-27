import '../models/payment_model.dart';

class PaymentRemoteSource {
  PaymentRemoteSource();

  Stream<List<PaymentModel>> watchUserPayments(
    String userId, {
    int limit = 50,
  }) async* {
    yield <PaymentModel>[];
  }

  Future<List<PaymentModel>> getUserPayments(
    String userId, {
    int limit = 50,
  }) async {
    return <PaymentModel>[];
  }

  Future<PaymentModel?> getPayment(String paymentId) async {
    return null;
  }

  Future<void> savePayment(PaymentModel payment) async {}

  Future<void> updatePaymentStatus({
    required String paymentId,
    required String status,
  }) async {}
}
