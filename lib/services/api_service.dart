import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:logger/logger.dart';
import 'fcm_service.dart';

final _logger = Logger();

class ApiService {
  static const String _baseUrl = 'https://45329aa2c129.ngrok-free.app';
  static const String _registerUrl = '$_baseUrl/api/v1/device/register';
  static const String _fcmTokenUrl = '$_baseUrl/api/v1/fcm/token';
  static const String _locationUrl ='$_baseUrl/api/v1/device/location/report';
  static const String _theftReportUrl = '$_baseUrl/api/v1/theft/report';

  static Future<http.Response?> registerDevice(
    String deviceId,
    Map<String, dynamic> deviceInfo,
    Map<String, double>? location,
  ) async {
    final token = await FcmService().getFCMToken();

    final url = Uri.parse(_registerUrl);

    final body = {
      'deviceId': deviceId,
      'manufacturer': deviceInfo['manufacturer'],
      'deviceModel': deviceInfo['deviceModel'],
      'serialNumber': deviceInfo['serialNumber'],
      'imei': deviceInfo['serialNumber'],
      'simIccidSlot0': deviceInfo['simIccidSlot0'] ?? '',
      'phoneNumberSlot0': deviceInfo['phoneNumberSlot0'] ?? '',
      'carrierNameSlot0': deviceInfo['carrierNameSlot0'] ?? '',
      'latitude': location?['latitude']?.toDouble(),
      'longitude': location?['longitude']?.toDouble(),
      'fcmToken': token,
    };

    try {
      final response = await http.post(
        url,
        headers: {'Content-Type': 'application/json',
        'deviceId': deviceId},
        body: jsonEncode(body),
      );

      return response;

      // if (response.statusCode != 200) {
      //   _logger.e("Failed to enroll device: ${response.body}");
      // }
    } catch (e) {
      _logger.e("Error sending enrollment data: $e");
      return null;
    }
  }


  static Future<void> sendTheftReport({
  required String deviceId,
  required String simSerial,
  required Map<String, double>? location,
}) async {
  final url = Uri.parse(_theftReportUrl);

  final body = {
    'deviceId': deviceId,
    'simSerial': simSerial,
    'latitude': location?['latitude']?.toString(),
    'longitude': location?['longitude']?.toString(),
  };

  try {
    final response = await http.post(
      url,
      headers: {'Content-Type': 'application/json',
      'deviceId': deviceId
      },
      body: jsonEncode(body),
    );

    if (response.statusCode != 200) {
      _logger.e("Failed to report theft: ${response.body}");
    } else {
      _logger.i("Theft reported successfully");
    }
  } catch (e) {
    _logger.e("Error reporting theft: $e");
  }
}


  static Future<void> sendFcmToken(String? token) async {
    if (token == null) return;

    final url = Uri.parse(_fcmTokenUrl);
    final body = {'token': token};
    _logger.d(body);

    try {
      await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(body),
      );
    } catch (e) {
      _logger.e("Failed to send FCM token: $e");
    }
  }

  static Future<void> syncDeviceMetadata(
    Map<String, dynamic> deviceInfo,
    Map<String, double>? location,
  ) async {
    final url = Uri.parse(_baseUrl);

    final body = {
      'manufacturer': deviceInfo['manufacturer'],
      'deviceModel': deviceInfo['deviceModel'],
      'serialNumber': deviceInfo['serialNumber'],
      'imei': deviceInfo['serialNumber'],
      'simIccidSlot0': '',
      'phoneNumberSlot0': '',
      'latitude': location?['latitude']?.toDouble(),
      'longitude': location?['longitude']?.toDouble(),
    };

    try {
      await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(body),
      );
    } catch (e) {
      _logger.e("Failed to sync metadata, $e");
    }
  }

  static Future<void> sendLastKnownLocation(
  Map<String, double>? location,
  String deviceId,
) async {
  if (location == null) return;

  final url = Uri.parse(_locationUrl);
  final body = {
    'deviceId': deviceId,
    'latitude': location['latitude'],
    'longitude': location['longitude'],
  };

  try {
    final response = await http.post(
      url,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode(body),
    );
    if (response.statusCode != 202) {
      _logger.e("Failed to send location: ${response.body}");
    }
  } catch (e) {
    _logger.e("Failed to send location: $e");
  }
}

  static Future<void> sendPhoneNumber(String phoneNumber) async {
    final url = Uri.parse('$_baseUrl/phone');

    final body = {'phoneNumber': phoneNumber};

    try {
      await http.post(
        url,
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(body),
      );
    } catch (e) {
      _logger.e("Failed to send phone number, $e");
    }
  }


    static Future<void> sendImei(String imei) async {
    final url = Uri.parse('$_baseUrl/imei');

    final body = {'imei': imei};

    try {
      final response = await http.post(url, headers: {'Content-Type': 'application/json'}, body: jsonEncode(body));
      if (response.statusCode != 200) {
        _logger.e("Failed to send IMEI, $response.body");
      }
    } catch (e) {
      _logger.e("Error sending IMEI: $e",);
    }
  }

   static Future<void> updateFcmToken(String deviceId, String fcmToken) async {
  final url = Uri.parse('$_baseUrl/api/v1/fcm/token');
  try {
    final response = await http.post(
      url,
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'deviceId': deviceId, 'fcmToken': fcmToken}),
    ).timeout(Duration(seconds: 10));
    if (response.statusCode == 200) {
      Logger().d('FCM token updated successfully');
    } else {
      Logger().e('Failed to update FCM token: ${response.statusCode} - ${response.body}');
    }
  } catch (e) {
    Logger().e('Error updating FCM token: $e');
  }
}

}
