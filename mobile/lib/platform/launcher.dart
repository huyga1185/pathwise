import 'package:url_launcher/url_launcher.dart';

/// Port for leaving the app (ARCHITECTURE §2.1): the phone dialer (`tel:`) and the maps app (D-07).
abstract interface class Launcher {
  /// Opens [uri] in the app that handles it; false when none could.
  Future<bool> open(Uri uri);
}

class UrlLauncher implements Launcher {
  const UrlLauncher();

  @override
  Future<bool> open(Uri uri) async {
    try {
      return await launchUrl(uri, mode: LaunchMode.externalApplication);
    } on Exception {
      return false;
    }
  }
}
