import 'package:farm_to_home_app/core/auth/backend_auth.dart';

class FirebaseService {
  FirebaseService._();

  static Future<void> initialize() async {}

  static BackendAuth get auth => BackendAuth.instance;

  static bool get isSignedIn => auth.currentUser != null;
}
