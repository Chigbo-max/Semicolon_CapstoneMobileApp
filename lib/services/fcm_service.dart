import 'package:anti_theft_tracker_agent/commandHandlers/command_handler.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:vibration/vibration.dart'; 
import 'api_service.dart';
import 'package:logger/logger.dart';
import '../commandHandlers/wipe_command_handler.dart';
import '../commandHandlers/theft_report_command_handler.dart';

final _logger = Logger();

class FcmService {
  final FirebaseMessaging _fcm = FirebaseMessaging.instance;

  Future<void> initFcm() async {
    NotificationSettings settings = await _fcm.requestPermission(
      alert: true,
      badge: true,
      sound: true,
      carPlay: false,
      criticalAlert: false,
      provisional: false,
      announcement: false,
    );

    _logger.w('User granted permission: ${settings.authorizationStatus}');

    String? fcmToken = await _fcm.getToken();
    if (fcmToken != null) {
      await ApiService.sendFcmToken(fcmToken);
    }

    FirebaseMessaging.onMessage.listen((RemoteMessage message) {
      if (message.data.containsKey('command')) {
        String cmd = message.data['command'];
        _logger.d("Received command: $cmd");
        _handleCommand(cmd);
      }
    });

    FirebaseMessaging.onBackgroundMessage(_firebaseMessagingBackgroundHandler);
  }

  Future<String?> getFCMToken() async {
    return await _fcm.getToken();
  }

  final Map<String, CommandHandler> _commandHandlers = {
    'wipe': WipeCommandHandler(),
    'theft_report': TheftReportCommandHandler(),
    // Note: 'lock' command is handled directly by native CommandService
  };

  void _handleCommand(String command) {
    _logger.i("Received command to handle: $command");
    
    // Lock command is handled directly by native CommandService
    if (command == 'lock') {
      _logger.i("Lock command received - handled by native CommandService");
      return;
    }
    
    final handler = _commandHandlers[command];
    if (handler != null) {
      _logger.i("Found handler for command: $command");
      handler.handle().then((_) {
        _logger.i("Command $command handled successfully");
      }).catchError((error) {
        _logger.e("Error handling command $command: $error");
      });
    } else {
      _logger.e("Unknown command: $command");
    }
  }

  static Future<void> _firebaseMessagingBackgroundHandler(RemoteMessage message) async {
    if (message.data.containsKey('command')) {
      String cmd = message.data['command'];
      _logger.i("Background command received: $cmd");

      if (cmd == 'theft_report') {
        Vibration.vibrate(duration: 3000);
      }
    }
  }
}