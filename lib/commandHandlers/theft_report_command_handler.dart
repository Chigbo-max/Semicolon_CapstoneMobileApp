import 'package:anti_theft_tracker_agent/commandHandlers/command_handler.dart';
import 'package:vibration/vibration.dart'; 


class TheftReportCommandHandler implements CommandHandler{
  @override
  Future<void> handle() async{
        Vibration.vibrate(duration: 2000);

  }
}