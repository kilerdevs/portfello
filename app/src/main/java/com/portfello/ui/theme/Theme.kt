package com.portfello.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme

// Fixed brand palette — deliberately no Material You dynamic color
private val DarkColors = darkColorScheme(
    primary = ElectricTeal,
    onPrimary = Ink,
    primaryContainer = TealDeep,
    onPrimaryContainer = ElectricTeal,
    secondary = Violet,
    onSecondary = TextPrimary,
    secondaryContainer = VioletDeep,
    onSecondaryContainer = VioletPale,
    tertiary = Violet,
    onTertiary = TextPrimary,
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface1,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface2,
    surfaceContainerHigh = Surface2,
    surfaceContainerHighest = SurfaceHigh,
    outline = OutlineDark,
    outlineVariant = OutlineFaint,
    error = LossRed,
    onError = Ink,
)

@Composable
fun PortfelloTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = PortfelloTypography
    ) {
        ProvideVicoTheme(rememberM3VicoTheme()) {
            // root Surface sets LocalContentColor so screens without a Scaffold
            // (onboarding) don't fall back to black text
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                content()
            }
        }
    }
}
