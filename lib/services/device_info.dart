import 'package:device_info_plus/device_info_plus.dart';
import 'dart:io';
import 'package:logger/logger.dart';
import 'package:flutter/services.dart';
import 'package:anti_theft_tracker_agent/services/api_service.dart';

final _logger = Logger();

class DeviceInfoService {
  static Future<Map<String, dynamic>> getDeviceInfo() async {
    final DeviceInfoPlugin deviceInfo = DeviceInfoPlugin();
    Map<String, dynamic> data = {};

    if (Platform.isAndroid) {
      try{

      
      final androidInfo = await deviceInfo.androidInfo;

      data = {
        'manufacturer': androidInfo.manufacturer,
        'deviceModel': androidInfo.model,
        'androidVersion': androidInfo.version.release,
        'serialNumber': androidInfo.serialNumber,
        'brand': androidInfo.brand,
        'hardware': androidInfo.hardware,
        'sdkInt': androidInfo.version.sdkInt,
        'securityPatch': androidInfo.version.securityPatch,
      };
    }catch(e){
      _logger.e("Failed to fetch device info: $e");
    }
    }
    return data;
  
}


static Future<void> fetchAndSendPhoneNumber() async {
    const platform = MethodChannel('com.antithefttracker.agent/siminfo');

    try {
      final String? phoneNumber = await platform.invokeMethod('getPhoneNumber');
      if (phoneNumber != null && phoneNumber.isNotEmpty) {
        await ApiService.sendPhoneNumber(phoneNumber);
        _logger.i("Phone number sent to backend: $phoneNumber");
      } else {
        _logger.w("No phone number available");
      }
    } on PlatformException catch (e) {
      _logger.e("Failed to fetch phone number, $e");
    }
  }

  static Future<void> fetchAndSendImei() async {
  const platform = MethodChannel('com.antithefttracker.agent/deviceinfo');

  try {
    final String? imei = await platform.invokeMethod('getImei');
    if (imei != null && imei.isNotEmpty) {
      await ApiService.sendImei(imei);
      _logger.i("IMEI sent to backend: $imei");
    } else {
      _logger.w("No IMEI available");
    }
  } on PlatformException catch (e) {
    _logger.e("Failed to fetch IMEI, $e");
  }
}

}