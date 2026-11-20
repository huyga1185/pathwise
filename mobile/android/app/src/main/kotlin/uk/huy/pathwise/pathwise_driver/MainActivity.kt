package uk.huy.pathwise.pathwise_driver

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

/**
 * Keeps the system splash screen until Flutter says the session is known (channel `pathwise/splash`, `dismiss`), so
 * the app does not show a second splash of its own (developer.android.com "Migrate your splash screen": "avoid the
 * duplication of splash screens"). Restoring the session goes over the network, so the splash is let go after
 * [MAX_KEEP_MS] whatever happens ("For network-based loads with indeterminate durations, dismiss the splash screen
 * and show placeholders"); Flutter then shows its loading indicator. A release build on the emulator knows the
 * session in about 2 s (measured 2026-11-20, DRIVER_UI_REDESIGN §8).
 */
class MainActivity : FlutterActivity() {
    @Volatile private var keepSplash = true

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { keepSplash }
        super.onCreate(savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({ keepSplash = false }, MAX_KEEP_MS)
    }

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "pathwise/splash").setMethodCallHandler { call, result ->
            if (call.method == "dismiss") {
                keepSplash = false
                result.success(null)
            } else {
                result.notImplemented()
            }
        }
    }

    private companion object {
        const val MAX_KEEP_MS = 3000L
    }
}
