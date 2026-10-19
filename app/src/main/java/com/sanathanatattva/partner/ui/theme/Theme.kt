package com.sanathanatattva.partner.ui.theme

import android.provider.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

// Light only: the app is used outdoors in sunlight and the designs are cream daylight.
private val colors = lightColorScheme(
    primary = St.Green,
    onPrimary = St.Cream,
    primaryContainer = St.GreenTint,
    onPrimaryContainer = St.GreenDeep,
    secondary = St.Gold,
    onSecondary = St.Ink,
    background = St.Cream,
    onBackground = St.Ink,
    surface = St.Cream,
    onSurface = St.Ink,
    surfaceVariant = St.Beige,
    onSurfaceVariant = St.Ink2,
    surfaceContainerLow = St.Paper,
    surfaceContainer = St.Cream,
    surfaceContainerHigh = St.Cream,
    outline = St.FieldBorder,
    outlineVariant = St.BeigeLine,
    error = St.Brick,
    onError = St.Cream,
)

/** True when the phone's "Remove animations" setting is on: show final frames, skip loops. */
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
fun StTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val kannada = configuration.locales[0].language == "kn"
    val typography = remember(kannada) { stTypography(kannada) }
    val reduceMotion = remember(configuration) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
        MaterialTheme(colorScheme = colors, typography = typography, content = content)
    }
}
