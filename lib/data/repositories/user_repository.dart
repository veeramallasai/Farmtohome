import 'package:farm_to_home_app/core/auth/backend_auth.dart';

import '../../core/network/api_client.dart';
import '../models/user_model.dart';

class UserRepository {
  UserRepository({
    BackendAuth? auth,
    ApiClient? apiClient,
  }) : _auth = auth ?? BackendAuth.instance,
       _apiClient = apiClient ?? ApiClient();

  final BackendAuth _auth;
  final ApiClient _apiClient;

  Stream<UserModel?> watchCurrentUser() async* {
    final User? user = _auth.currentUser;
    if (user != null) {
      yield _fromAuth(user);
    } else {
      yield null;
    }
  }

  Future<UserModel> getCurrentUser() async {
    final User? user = _auth.currentUser;
    if (user == null) throw StateError('Please login to continue.');
    return _fromAuth(user);
  }

  Future<void> saveProfile(UserModel profile) async {
    final User? authUser = _auth.currentUser;
    if (authUser == null) throw StateError('Please login to continue.');

    if (profile.displayName.trim().isNotEmpty) {
      await authUser.updateDisplayName(profile.displayName.trim());
    }
    await syncCurrentUser(
      firstName: profile.firstName,
      lastName: profile.lastName,
      phoneNumber: profile.phoneNumber,
      accountType: profile.isShopOwner ? 'shop_owner' : 'customer',
    );
  }

  Future<void> updateShoppingMode(String mode) async {
    await syncCurrentUser(shoppingMode: mode);
  }

  Future<void> syncCurrentUser({
    String? firstName,
    String? lastName,
    String? phoneNumber,
    String? shoppingMode,
    String? accountType,
  }) async {
    final User? authUser = _auth.currentUser;
    if (authUser == null) return;

    final List<String> names = (authUser.displayName ?? '').trim().split(
      RegExp(r'\s+'),
    );

    final String finalFirstName = firstName ?? (names.isNotEmpty ? names.first : '');
    final String finalLastName = lastName ?? (names.length > 1 ? names.skip(1).join(' ') : '');

    try {
      await _apiClient.put(
        '/api/v1/users/me',
        body: <String, dynamic>{
          'firstName': finalFirstName,
          'lastName': finalLastName,
          'phoneNumber': phoneNumber ?? authUser.phoneNumber ?? '',
          'photoUrl': authUser.photoURL ?? '',
          'shoppingMode': shoppingMode ?? 'home',
          'accountType': accountType ?? 'customer',
        },
      );
    } catch (_) {
      // Allow fallback if offline
    }
  }

  UserModel _fromAuth(User user) {
    final List<String> names = (user.displayName ?? '').trim().split(
      RegExp(r'\s+'),
    );
    return UserModel(
      uid: user.uid,
      firstName: names.isEmpty ? '' : names.first,
      lastName: names.length <= 1 ? '' : names.skip(1).join(' '),
      email: user.email ?? '',
      phoneNumber: user.phoneNumber ?? '',
      photoUrl: user.photoURL ?? '',
      isPhoneVerified: user.phoneNumber?.isNotEmpty == true,
      isProfileComplete: (user.displayName ?? '').trim().isNotEmpty,
    );
  }
}
