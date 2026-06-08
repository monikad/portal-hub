package com.monikabele.portalhub.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
    lightColorScheme(
        primary = PortalBlue,
        onPrimary = OnPortalBlue,
        primaryContainer = PortalBlueLight,
        onPrimaryContainer = OnPortalBlueLight,
        secondary = NeutralGrey,
        onSecondary = OnPortalBlue,
        background = BackgroundLight,
        surface = SurfaceLight,
        onBackground = ContentOnLight,
        onSurface = ContentOnLight,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = PortalBlue,
        onPrimary = OnPortalBlue,
        primaryContainer = PortalBlueDarkCont,
        onPrimaryContainer = OnPortalBlueDarkC,
        secondary = NeutralGreyDark,
        onSecondary = OnPortalBlue,
        background = BackgroundDark,
        surface = SurfaceDark,
        onBackground = ContentOnDark,
        onSurface = ContentOnDark,
    )

@Composable
fun PortalHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
  val colorScheme =
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
      }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
