package com.example.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private val LightColorScheme = lightColorScheme(
    primary = ExamNavyPrimary,
    onPrimary = Color.White,
    primaryContainer = ExamNavyPrimaryContainer,
    onPrimaryContainer = ExamNavyOnPrimaryContainer,
    secondary = ExamRoyalSecondary,
    secondaryContainer = ExamRoyalSecondaryContainer,
    onSecondaryContainer = ExamRoyalOnSecondaryContainer,
    tertiary = ExamSkyTertiary,
    background = ExamLightBackground,
    surface = ExamLightSurface,
    surfaceVariant = ExamLightSurfaceVariant,
    onBackground = ExamLightTextPrimary,
    onSurface = ExamLightTextPrimary,
    onSurfaceVariant = ExamLightTextSecondary,
    outline = ExamLightBorder,
    error = ErrorRed,
    errorContainer = ErrorRedBg,
    onErrorContainer = ErrorRedText
)

@Composable
fun AnshuExamTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // App is permanently set to Light Theme
    val colorScheme = LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            activity?.window?.let { window ->
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
