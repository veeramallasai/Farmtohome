import 'package:flutter/foundation.dart' show kDebugMode, kIsWeb, defaultTargetPlatform, TargetPlatform;

class BackendConfig {
  BackendConfig._();

  static const String _overrideBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: '',
  );

  static const String _underscoreApiBaseUrl = String.fromEnvironment(
    '_API_BASE_URL',
    defaultValue: '',
  );

  static const String _apiUrl = String.fromEnvironment(
    'API_URL',
    defaultValue: '',
  );

  static const String _railwayUrl = String.fromEnvironment(
    'RAILWAY_URL',
    defaultValue: '',
  );

  static const String _defaultProductionUrl = String.fromEnvironment(
    'PROD_BACKEND_URL',
    defaultValue: 'https://farmtohome-backend-production-3378.up.railway.app/api/v1',
  );

  static String get baseUrl {
    String url;
    if (_overrideBaseUrl.trim().isNotEmpty) {
      url = _overrideBaseUrl.trim();
    } else if (_underscoreApiBaseUrl.trim().isNotEmpty) {
      url = _underscoreApiBaseUrl.trim();
    } else if (_apiUrl.trim().isNotEmpty) {
      url = _apiUrl.trim();
    } else if (_railwayUrl.trim().isNotEmpty) {
      url = _railwayUrl.trim();
    } else if (kDebugMode) {
      if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
        url = 'http://10.0.2.2:8085/api/v1';
      } else {
        url = 'http://localhost:8085/api/v1';
      }
    } else {
      url = _defaultProductionUrl;
    }

    String cleaned = _withoutTrailingSlash(url);
    if (!cleaned.startsWith('http://') && !cleaned.startsWith('https://')) {
      cleaned = 'https://$cleaned';
    }
    if (!cleaned.endsWith('/api/v1') && !cleaned.contains('/api/')) {
      cleaned = '$cleaned/api/v1';
    }
    return cleaned;
  }

  static const Duration connectTimeout = Duration(seconds: 20);
  static const Duration receiveTimeout = Duration(seconds: 30);
  static const int maximumRetries = 2;
  static const Duration retryDelay = Duration(milliseconds: 600);

  static Uri uri(String path, {Map<String, dynamic>? queryParameters}) {
    String p = path.trim();
    if (p.startsWith('/api/v1/')) {
      p = p.substring('/api/v1'.length);
    } else if (p.startsWith('api/v1/')) {
      p = p.substring('api/v1'.length);
    }
    final String normalizedPath = p.startsWith('/') ? p : '/$p';
    final Map<String, String> query = <String, String>{};
    queryParameters?.forEach((String key, dynamic value) {
      if (value != null) query[key] = value.toString();
    });
    return Uri.parse(
      '$baseUrl$normalizedPath',
    ).replace(queryParameters: query.isEmpty ? null : query);
  }

  static String _withoutTrailingSlash(String value) =>
      value.endsWith('/') ? value.substring(0, value.length - 1) : value;
}
