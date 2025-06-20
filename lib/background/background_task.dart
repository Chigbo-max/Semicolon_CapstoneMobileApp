import "package:logger/logger.dart";
import "package:workmanager/workmanager.dart";
import 'background_task_handler.dart';
import 'fetch_location_task.dart';
import 'sync_meta_data_task.dart';

final _logger = Logger();

final Map<String, BackgroundTaskHandler> _taskHandlers = {
  "fetch-location": FetchLocationTask(),
  "sync-metadata": SyncMetaDataTask(),
};


void callbackDispatcher() {
  Workmanager().executeTask((task, inputData) async {
    _logger.d("Background task running: $task");

    final handler = _taskHandlers[task];
    if (handler != null) {
      await handler.execute();
    } else {
      _logger.w("Unknown task: $task");
    }

    return Future.value(true);
  });
}