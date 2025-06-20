import 'dart:async';
import 'package:geolocator/geolocator.dart';
import 'package:logger/logger.dart';
import '../models/location.dart';

final _logger = Logger();


class LocationService {
  static Future<Location?> getLastKnownLocation() async {
    try {
      bool serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        _logger.w("Location services are disabled.");
        return null;
      }

      LocationPermission permission = await Geolocator.checkPermission();
      if (permission == LocationPermission.denied) {
        permission = await Geolocator.requestPermission();
      }
      if (permission == LocationPermission.deniedForever || permission == LocationPermission.denied) {
        _logger.w("Location permission denied.");
        return null;
      }

      try {
        final settings = const LocationSettings(
          accuracy: LocationAccuracy.high,
        );

        Position position = await Geolocator.getCurrentPosition(locationSettings: settings);
        return Location(latitude: position.latitude, longitude: position.longitude);
      } on TimeoutException catch (_) {
        _logger.w("getCurrentPosition() timed out. Trying last known location...");
      }

      Position? fallback = await Geolocator.getLastKnownPosition();
      if (fallback != null) {
        return Location(latitude: fallback.latitude, longitude: fallback.longitude);
      }

      return null;
    } catch (e, stack) {
      _logger.e("Error getting location: $e, $stack");
      return null;
    }
  }
}
