import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:device_info_plus/device_info_plus.dart';
import 'dart:io';
import 'package:uuid/uuid.dart';

class DeviceIdManager {
  static const _deviceIdsKey = 'device_ids';
  static const _generatedDeviceIdKey = 'generated_device_id';
  static const List<String> _staticDeviceIds = [
    'PPR1.180610.011',
    'PPR1.180610.013',
  ];
  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  Future<String> _getCurrentDeviceId() async {
    final deviceInfo = DeviceInfoPlugin();
    if (Platform.isAndroid) {
      final androidInfo = await deviceInfo.androidInfo;
      final serial = androidInfo.serialNumber;
      if (serial.isNotEmpty && serial != 'unknown') {
        return serial;
      } else {
        String? generatedId = await _storage.read(key: _generatedDeviceIdKey);
        if (generatedId == null) {
          generatedId = const Uuid().v4();
          await _storage.write(key: _generatedDeviceIdKey, value: generatedId);
        }
        return generatedId;
      }
    } else if (Platform.isIOS) {
      final iosInfo = await deviceInfo.iosInfo;
      return iosInfo.identifierForVendor ?? '';
    }
    return '';
  }

  Future<void> initializeDeviceIds() async {
    String? storedIds = await _storage.read(key: _deviceIdsKey);
    List<String> ids = storedIds?.split(',') ?? [];
    for (final staticId in _staticDeviceIds) {
      if (!ids.contains(staticId)) ids.add(staticId);
    }
    final currentDeviceId = await _getCurrentDeviceId();
    if (currentDeviceId.isNotEmpty && !ids.contains(currentDeviceId)) {
      ids.add(currentDeviceId);
    }
    await _storage.write(key: _deviceIdsKey, value: ids.join(','));
  }

  Future<String?> getCurrentDeviceId() async {
    final currentDeviceId = await _getCurrentDeviceId();
    String? storedIds = await _storage.read(key: _deviceIdsKey);
    if (storedIds == null) return null;
    List<String> ids = storedIds.split(',');
    return ids.contains(currentDeviceId) ? currentDeviceId : null;
  }

  Future<List<String>> getAllDeviceIds() async {
    String? storedIds = await _storage.read(key: _deviceIdsKey);
    if (storedIds == null) return [];
    return storedIds.split(',');
  }
} 