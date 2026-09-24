package info.rbuck.billiardscoreboard.ui.components

import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Compose Dialogs (AlertDialog, etc.) open their own Android Window, separate from the
 * Activity's - so MainActivity hiding the status bar doesn't cover it, and it reappears for as
 * long as the dialog is shown. Call this as the first statement in one of the dialog's content
 * slots (e.g. `title = { HideStatusBarInDialog(); Text(...) }`) to hide it there too.
 *
 * DropdownMenu/Popup content is a WindowManager sub-panel, not a DialogWindowProvider, and (unlike
 * a real Window) can't independently control system bars - only the focused top-level window can.
 * Those menus are made non-focusable instead (`properties = PopupProperties(focusable = false)`),
 * so they never take focus away from the dialog/activity window that's already hiding the bar.
 *
 * Same FLAG_FULLSCREEN + hide() combo as MainActivity - a previous version additionally toggled
 * FLAG_NOT_FOCUSABLE off/on around the hide() to dodge the focus-gain reveal, but that SideEffect
 * runs after Compose has already created and shown the dialog's Window, i.e. after the focus-gain
 * that triggers the reveal has already happened, so it couldn't actually prevent it.
 */
@Suppress("DEPRECATION")
@Composable
fun HideStatusBarInDialog() {
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, view).apply {
            hide(WindowInsetsCompat.Type.statusBars())
        }
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }
}
