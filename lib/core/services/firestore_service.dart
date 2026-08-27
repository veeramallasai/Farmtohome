class FirestoreService {
  FirestoreService();

  Future<void> setDocument(
    String path,
    Map<String, dynamic> data, {
    bool merge = false,
  }) async {}

  Future<void> updateDocument(String path, Map<String, dynamic> data) async {}

  Future<void> deleteDocument(String path) async {}
}
