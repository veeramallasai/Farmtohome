import '../models/user_model.dart';

class UserRemoteSource {
  UserRemoteSource();

  Stream<UserModel?> watchUser(String userId) async* {
    yield null;
  }

  Future<UserModel?> getUser(String userId) async {
    return null;
  }

  Future<void> saveUser(UserModel user) async {}

  Future<void> updateFields(String userId, Map<String, dynamic> fields) async {}
}
