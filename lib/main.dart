import 'package:anti_theft_tracker_agent/commandHandlers/wipe_command_handler.dart';
import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'services/device_info.dart';
import 'services/location_service.dart';
import 'services/api_service.dart';
import 'services/fcm_service.dart';
import 'package:workmanager/workmanager.dart';
import 'utils/secure_storage.dart';
import 'package:logger/logger.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'commandHandlers/lock_command_handler.dart';

final _logger = Logger();

@pragma('vm:entry-point')
Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
  await Firebase.initializeApp();
  _logger.i("Handling background FCM message: ${message.data}");
  final command = message.data['type'];
  if (command == 'lock') {
    await LockCommandHandler().handle();
  } else if (command == 'wipe') {
    await WipeCommandHandler().handle();

  } else if (command == 'theft_report') {
    final storage = SecureStorageService();
    final deviceId = await storage.readValue('device_id');
    final simSerial = await storage.readValue('sim_serial');

    final location = await LocationService.getLastKnownLocation();

    if (deviceId != null && simSerial != null && location != null) {
      await ApiService.sendTheftReport(
        deviceId: deviceId,
        simSerial: simSerial,
        location: location.toMap(),
      );
    }
  }
}

@pragma('vm:entry-point')
void callbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    final storage = SecureStorageService();
    String? deviceId = await storage.readValue('device_id');
    if (deviceId == null) {
      _logger.w("No deviceId found for task: $task");
      return Future.value(true);
    }

    switch (task) {
      case "fetch-location":
        var location = await LocationService.getLastKnownLocation();
        if (location != null) {
          await ApiService.sendLastKnownLocation(location.toMap(), deviceId);
          _logger.i("Location sent: $location");
        } else {
          _logger.w("Failed to fetch location");
        }
        break;
      default:
        _logger.w("Unknown background task: $task");
    }
    return Future.value(true);
  });
}


Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp();
  FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);

  final storage = SecureStorageService();
  final fcmService = FcmService();
  final prefs = await SharedPreferences.getInstance();
  const deviceId = "test-device-001";
  const simSerial = "test-sim-001";

  try {
    final isRegistered = await storage.readValue('registrationPending') == 'false';
    await Future.wait([
      storage.writeValue('device_id', deviceId),
      storage.writeValue('sim_serial', simSerial),
      prefs.setString('device_id', deviceId),
      prefs.setString('sim_serial', simSerial),
    ]);
    _logger.i("Device ID set: $deviceId");

    String? fcmToken = await fcmService.getFCMToken();
    _logger.d("FCM Token: $fcmToken");

    if (!isRegistered) {
      final deviceInfo = await DeviceInfoService.getDeviceInfo();
      final location = await LocationService.getLastKnownLocation();

      if (location == null) {
        _logger.w('Location is required. Registration postponed.');
        await storage.writeValue('registrationPending', 'true');
        runApp(Container());
        return;
      }

      _logger.d("Device Info: $deviceInfo");
      _logger.d("Last Known Location: $location");

      final response = await ApiService.registerDevice(
        deviceId,
        deviceInfo,
        location.toMap(),
      );
      if (response != null && response.statusCode == 200) {
        _logger.d("Device registration successful: ${response.body}");
        await storage.writeValue('registrationPending', 'false');
      } else {
        _logger.e("Device registration failed: ${response?.body}");
        await storage.writeValue('registrationPending', 'true');
      }
    }

    if (fcmToken != null) {
      await ApiService.updateFcmToken(deviceId, fcmToken); 
      _logger.i("FCM token updated");
    }

    await Workmanager().initialize(callbackDispatcher, isInDebugMode: false);
   await Workmanager().registerPeriodicTask(
      "location-task",
      "fetch-location",
      frequency: const Duration(minutes: 15),
      initialDelay: const Duration(seconds: 30),
      constraints: Constraints(networkType: NetworkType.connected),
    );
    _logger.i("Workmanager initialized and location task registered");
  } catch (e, stack) {
    _logger.e("Error during initialization: $e", stackTrace: stack);
  }

  

  runApp(Container());

  
}