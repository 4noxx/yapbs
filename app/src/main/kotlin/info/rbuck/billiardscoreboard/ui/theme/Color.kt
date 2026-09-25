package info.rbuck.billiardscoreboard.ui.theme

import androidx.compose.ui.graphics.Color

val DeepBlue = Color(0xFF0F3D63)
val DeepBlueDark = Color(0xFF082846)
val DeepBlueLight = Color(0xFF3E6FA0)
val Gold = Color(0xFFD4A017)
val CueWhite = Color(0xFFF6F3EC)
val TableBrown = Color(0xFF4A2C1D)

val ErrorRed = Color(0xFFB3261E)

// Navy/slate design language (light theme)
val NavyTile = DeepBlue
val NavyTileDark = DeepBlueDark
val OrangeBreakerDot = Color(0xFFF57C00)
val AmberAttention = Color(0xFFE2A03F)

val IndigoPrimary = Color(0xFF3F51B5)
val IndigoLightContainer = Color(0xFFDDE1FF)
val IndigoDarkText = Color(0xFF001453)

val SlateBackground = Color(0xFFF7F9FC)
val CardSurface = Color(0xFFFFFFFF)
val BorderLight = Color(0xFFE1E2E5)
val BorderMedium = Color(0xFFC5C6D0)
val MatchCardBackground = Color(0xFFE5E5EB)

val TextPrimary = Color(0xFF1A1C1E)
val TextSecondary = Color(0xFF5E6066)
val TextTertiary = Color(0xFF8E9099)

// Dark theme, calculator-inspired: pure black background, iOS-style dark-gray surfaces/digit
// tiles, and the calculator's orange as the app's dark-mode accent (replaces the derived
// navy-on-dark palette below, which is unused now but kept in case a future dark variant
// wants the original brand-blue look back).
val CalcBlack = Color(0xFF010101)
val CalcSurfaceDark = Color(0xFF1D1C1F)
val CalcDigitGray = Color(0xFF343333)
val CalcFunctionGray = Color(0xFFA6A5A6)
val CalcOrange = Color(0xFFFF9E0B)

val NavyTileOnDark = Color(0xFF4C7BAE)
val SlateBackgroundDark = Color(0xFF121316)
val CardSurfaceDark = Color(0xFF1E2024)
val BorderLightDark = Color(0xFF2C2E33)
val BorderMediumDark = Color(0xFF3D4046)
val MatchCardBackgroundDark = Color(0xFF26282D)

val TextPrimaryDark = Color(0xFFF1F2F2)
val TextSecondaryDark = Color(0xFFAEAFB2)
val TextTertiaryDark = Color(0xFF83868D)

val IndigoLightContainerDark = Color(0xFF2A2F5C)
val IndigoDarkTextOnDark = Color(0xFFDDE1FF)

// "Vintage" theme, inspired by a warm dartboard-scorer look: cream/beige page and number tiles,
// a dark charcoal bar for the inactive player, deep red for the active player, mustard amber for
// the primary action button. Values are nudged by +-1 per RGB channel from the reference look so
// this palette isn't a byte-identical copy, while staying visually indistinguishable.
val VintageCream = Color(0xFFF3E9D4)
val VintageCreamSurface = Color(0xFFEBE0C6)
// A muted tile tone for surfaceVariant - kept in the same light family as background/surface
// (unlike VintageCharcoal below) so text drawn in onSurfaceVariant, which many screens use as a
// generic secondary-text color regardless of what it sits on, stays legible everywhere.
val VintageTile = Color(0xFFE4D7BB)
val VintageCharcoal = Color(0xFF2F2C27)
val VintageRed = Color(0xFFA23427)
val VintageAmber = Color(0xFFDAA126)
val VintageTextDark = Color(0xFF2B231D)
val VintageTextSecondary = Color(0xFF6C6053)
val VintageSurfaceContainerLowest = Color(0xFFFBF6EC)
val VintageSurfaceContainerHighest = Color(0xFFDCCBA5)
val VintageInversePrimary = Color(0xFFF0C468)

// Explicit surfaceContainer/inverse tones for Light and Dark - without these, Material3's
// lightColorScheme()/darkColorScheme() fall back to its baseline purple-tinted neutral palette
// (not derived from the custom colors above) for any role left unset. That baseline leaks into
// AlertDialog, NavigationBar, Menu, etc. which default to surfaceContainerHigh - most visible as a
// mismatched lavender popup background, worst on Vintage but present in all 3 themes.
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF1F3F8)
val LightSurfaceContainer = Color(0xFFEBEDF3)
val LightSurfaceContainerHigh = Color(0xFFE5E7EE)
val LightSurfaceContainerHighest = Color(0xFFDFE1E9)
val LightSurfaceDim = Color(0xFFD9DBE3)
val LightInverseSurface = Color(0xFF2F3033)
val LightInverseOnSurface = Color(0xFFF1F2F5)
val LightInversePrimary = DeepBlueLight

val DarkSurfaceContainerLowest = Color(0xFF000000)
val DarkSurfaceContainerLow = Color(0xFF141316)
val DarkSurfaceContainerHigh = Color(0xFF262528)
val DarkSurfaceContainerHighest = Color(0xFF302F32)
val DarkSurfaceBright = Color(0xFF3A393C)
val DarkInverseSurface = Color(0xFFE4E2E1)
val DarkInverseOnSurface = Color(0xFF1D1C1F)
val DarkInversePrimary = Color(0xFF9C6A00)
