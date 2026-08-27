import 'package:flutter/foundation.dart' show kIsWeb, defaultTargetPlatform, TargetPlatform;

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
    defaultValue: 'https://farmtohome-backend-production-3378.up.railway.app',
  );

  static String get baseUrl {
    String url = _defaultProductionUrl;
    if (_overrideBaseUrl.trim().isNotEmpty) {
      url = _overrideBaseUrl.trim();
    } else if (_underscoreApiBaseUrl.trim().isNotEmpty) {
      url = _underscoreApiBaseUrl.trim();
    } else if (_apiUrl.trim().isNotEmpty) {
      url = _apiUrl.trim();
    } else if (_railwayUrl.trim().isNotEmpty) {
      url = _railwayUrl.trim();
    }

    url = _withoutTrailingSlash(url);

    // Auto-correct loopback hosts based on running platform
    if (kIsWeb) {
      // Chrome/Web cannot connect to 10.0.2.2 (times out); map to localhost
      if (url.contains('10.0.2.2')) {
        url = url.replaceAll('10.0.2.2', 'localhost');
      }
    } else if (defaultTargetPlatform == TargetPlatform.android) {
      // Android Emulator cannot connect to localhost directly; map to 10.0.2.2
      if (url.contains('localhost')) {
        url = url.replaceAll('localhost', '10.0.2.2');
      } else if (url.contains('127.0.0.1')) {
        url = url.replaceAll('127.0.0.1', '10.0.2.2');
      }
    }

    return url;
  }




  static const Duration connectTimeout = Duration(seconds: 20);
  static const Duration receiveTimeout = Duration(seconds: 30);
  static const int maximumRetries = 2;
  static const Duration retryDelay = Duration(milliseconds: 600);

  static Uri uri(String path, {Map<String, dynamic>? queryParameters}) {
    final String normalizedPath = path.startsWith('/') ? path : '/$path';
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
