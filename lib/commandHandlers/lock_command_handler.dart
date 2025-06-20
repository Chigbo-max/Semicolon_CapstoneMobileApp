import 'package:anti_theft_tracker_agent/commandHandlers/command_handler.dart';

import 'package:logger/logger.dart';
import 'package:flutter/services.dart';

final _logger = Logger();

class LockCommandHandler implements CommandHandler{
  @override
  Future<void> handle() async{
    try{
    _logger.i("Executing device lock...");
        const platform = MethodChannel('com.antithefttracker.agent/command');
      await platform.invokeMethod('lockDevice');

    } catch(e){
      _logger.e("Error locking device: $e");
    }
  }
}