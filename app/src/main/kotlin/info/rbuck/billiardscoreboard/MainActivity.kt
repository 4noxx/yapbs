package info.rbuck.billiardscoreboard

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import info.rbuck.billiardscoreboard.i18n.LocalStrings
import info.rbuck.billiardscoreboard.i18n.Strings
import info.rbuck.billiardscoreboard.ui.bsApplication
import info.rbuck.billiardscoreboard.ui.navigation.BsNavHost
import info.rbuck.billiardscoreboard.ui.theme.BilliardScoreboardTheme

class MainActivity : ComponentActivity() {
    // Classic FLAG_FULLSCREEN alongside the insets-controller hide(), instead of relying on
    // enableEdgeToEdge() alone - on Fire OS a plain WindowInsetsController hide() was prone to a
    // brief status-bar flash on any window-focus change (most visibly when a dialog opens/closes,
    // since a dialog's own Window briefly taking focus is exactly such a change).
    @Suppress("DEPRECATION")
    private fun hideStatusBar() {
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
        }
        // Legacy systemUiVisibility alongside the modern InsetsController - this is a synchronous
        // view attribute applied at attach/layout time rather than an async WindowManager-service
        // round trip, so on Fire OS it's a diagnostic test for whether the newer API's inherent
        // one-frame-plus delay is what lets the status bar flash back in on any window-focus change.
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideStatusBar()
        setContent {
            val app = bsApplication()
            val appTheme by app.settingsRepository.appTheme.collectAsStateWithLifecycle()
            val keepScreenOn by app.settingsRepository.keepScreenOn.collectAsStateWithLifecycle()
            val language by app.settingsRepository.language.collectAsStateWithLifecycle()

            LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            BilliardScoreboardTheme(appTheme = appTheme) {
                CompositionLocalProvider(LocalStrings provides Strings.of(language)) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        BsNavHost()
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideStatusBar()
    }
}
