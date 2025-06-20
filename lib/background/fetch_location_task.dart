import 'package:anti_theft_tracker_agent/services/api_service.dart';
import 'package:anti_theft_tracker_agent/services/location_service.dart';
import 'package:anti_theft_tracker_agent/utils/secure_storage.dart';
import 'package:logger/logger.dart';
import 'background_task_handler.dart';



final _logger = Logger();

class FetchLocationTask implements BackgroundTaskHandler {
  @override
  Future<void> execute() async {
      final storage = SecureStorageService();
    String? deviceId = await storage.readValue('deviceId');
    if (deviceId == null) {
      _logger.w("No deviceId found");
      return;
    }

    var location = await LocationService.getLastKnownLocation();
    
    if(location != null){
      await ApiService.sendLastKnownLocation(location.toMap(), deviceId);
    }

  }

}

