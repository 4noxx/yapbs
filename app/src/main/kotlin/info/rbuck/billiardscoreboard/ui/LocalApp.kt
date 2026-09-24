package info.rbuck.billiardscoreboard.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import info.rbuck.billiardscoreboard.BsApplication

@Composable
fun bsApplication(): BsApplication = LocalContext.current.applicationContext as BsApplication
