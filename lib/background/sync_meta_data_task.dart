import 'package:anti_theft_tracker_agent/background/background_task_handler.dart';
import '../../services/device_info.dart';
import '../../services/api_service.dart';
import '../../services/location_service.dart';

import 'package:logger/logger.dart';

final _logger = Logger();

class SyncMetaDataTask implements BackgroundTaskHandler{
  @override
  Future<void> execute() async {
    try{
       var deviceInfo = await DeviceInfoService.getDeviceInfo();
      var location = await LocationService.getLastKnownLocation();
      await ApiService.syncDeviceMetadata(deviceInfo, location?.toMap());
      await DeviceInfoService.fetchAndSendPhoneNumber();

    }catch(e){
      _logger.e("Error syncing metadata: $e");
    }
  }
}