class Timestamp {
  const Timestamp(this.seconds, this.nanoseconds);

  final int seconds;
  final int nanoseconds;

  factory Timestamp.now() {
    final DateTime now = DateTime.now();
    return Timestamp.fromDate(now);
  }

  factory Timestamp.fromDate(DateTime date) {
    return Timestamp(
      date.millisecondsSinceEpoch ~/ 1000,
      (date.microsecondsSinceEpoch % 1000000) * 1000,
    );
  }

  DateTime toDate() => DateTime.fromMillisecondsSinceEpoch(seconds * 1000);

  @override
  String toString() => toDate().toIso8601String();

  dynamic toJson() => toDate().toIso8601String();

  static DateTime? parseDateTime(dynamic value) {
    if (value == null) return null;
    if (value is DateTime) return value;
    if (value is Timestamp) return value.toDate();
    if (value is String) return DateTime.tryParse(value);
    if (value is int) return DateTime.fromMillisecondsSinceEpoch(value);
    return null;
  }
}

class FirebaseException implements Exception {
  FirebaseException({required this.plugin, required this.message, this.code = 'unknown'});
  final String plugin;
  final String? message;
  final String code;

  @override
  String toString() => '[$plugin/$code] $message';
}
