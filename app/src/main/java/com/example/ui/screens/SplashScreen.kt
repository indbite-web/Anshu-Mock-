package com.example.ui.screens

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current

    // Color definitions for light theme
    val backgroundColor = Color(0xFFFFFFFF)
    val primaryTextColor = Color(0xFF0F172A)
    val secondaryTextColor = Color(0xFF64748B)

    // Check system animator duration scale for reduced motion accessibility
    val isReducedMotion = remember {
        try {
            val durationScale = Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
            durationScale == 0.0f
        } catch (e: Exception) {
            false
        }
    }

    // Animatable states
    val logoScale = remember { Animatable(if (isReducedMotion) 1f else 0.80f) }
    val logoAlpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }

    val sparkleScale = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val sparkleAlpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val sparkleGlow = remember { Animatable(if (isReducedMotion) 0.5f else 0f) }

    val textAlpha = remember { Animatable(if (isReducedMotion) 1f else 0f) }
    val textOffsetY = remember { Animatable(if (isReducedMotion) 0f else 16f) }

    LaunchedEffect(Unit) {
        if (isReducedMotion) {
            delay(400)
            onSplashFinished()
            return@LaunchedEffect
        }

        // Sequence timeline:
        // 0–350ms: Logo fades in & scales up 0.80 -> 1.04
        launch {
            logoAlpha.animateTo(1.0f, animationSpec = tween(350, easing = LinearOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(
                targetValue = 1.04f,
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            )
            // 350–550ms: Logo settles from 1.04 -> 1.0
            logoScale.animateTo(
                targetValue = 1.00f,
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            )
        }

        // Sparkle animation starts at 480ms
        delay(480)
        launch {
            sparkleAlpha.animateTo(1.0f, animationSpec = tween(200))
        }
        launch {
            sparkleScale.animateTo(
                targetValue = 1.25f,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            )
            sparkleScale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            )
        }
        launch {
            sparkleGlow.animateTo(1.0f, animationSpec = tween(250))
            sparkleGlow.animateTo(0.35f, animationSpec = tween(250))
        }

        // "Powered by AnshuCore" text animation starts at 720ms
        delay(240) // total delay = 720ms
        launch {
            textAlpha.animateTo(1.0f, animationSpec = tween(380, easing = LinearOutSlowInEasing))
        }
        launch {
            textOffsetY.animateTo(0.0f, animationSpec = tween(380, easing = FastOutSlowInEasing))
        }

        // Hold final state until total sequence reaches ~1.5s
        delay(780) // 720 + 780 = 1500ms total
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .systemBarsPadding()
            .testTag("splash_screen_container"),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val screenWidth = maxWidth
            // Responsive logo size scaling between 140.dp and 220.dp depending on screen size
            val logoSize = (screenWidth * 0.42f).coerceIn(140.dp, 220.dp)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                // Main Logo Mark
                AnshuLogoMark(
                    logoScale = logoScale.value,
                    logoAlpha = logoAlpha.value,
                    sparkleScale = sparkleScale.value,
                    sparkleAlpha = sparkleAlpha.value,
                    sparkleGlow = sparkleGlow.value,
                    modifier = Modifier
                        .size(logoSize)
                        .testTag("app_logo_mark")
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Powered by AnshuCore Text
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            alpha = textAlpha.value
                            translationY = textOffsetY.value * density
                        }
                        .testTag("splash_branding_text")
                ) {
                    Text(
                        text = "Powered by",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = secondaryTextColor,
                        letterSpacing = 1.2.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "AnshuCore",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryTextColor,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AnshuLogoMark(
    logoScale: Float,
    logoAlpha: Float,
    sparkleScale: Float,
    sparkleAlpha: Float,
    sparkleGlow: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = logoScale
                scaleY = logoScale
                alpha = logoAlpha
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val scale = w / 800f

            // Left Leg Gradient (Deep Purple -> Blue)
            val leftGrad = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF581C87),
                    Color(0xFF7C3AED),
                    Color(0xFF3B82F6),
                    Color(0xFF2563EB)
                ),
                start = Offset(220f * scale, 540f * scale),
                end = Offset(420f * scale, 200f * scale)
            )

            // Right Leg Gradient (Royal Blue -> Electric Cyan)
            val rightGrad = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1D4ED8),
                    Color(0xFF2563EB),
                    Color(0xFF0080FF),
                    Color(0xFF00A3FF)
                ),
                start = Offset(380f * scale, 200f * scale),
                end = Offset(560f * scale, 540f * scale)
            )

            // Center Glow Gradient
            val centerGrad = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF38BDF8),
                    Color(0xFF93C5FD)
                ),
                start = Offset(340f * scale, 340f * scale),
                end = Offset(460f * scale, 440f * scale)
            )

            val strokeWidth = 116f * scale

            // Draw Left Leg
            drawLine(
                brush = leftGrad,
                start = Offset(270f * scale, 515f * scale),
                end = Offset(400f * scale, 245f * scale),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Draw Right Leg
            drawLine(
                brush = rightGrad,
                start = Offset(400f * scale, 245f * scale),
                end = Offset(530f * scale, 515f * scale),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Draw Center Inner Translucent Blend
            val centerPath = Path().apply {
                moveTo(335f * scale, 435f * scale)
                quadraticTo(400f * scale, 325f * scale, 465f * scale, 435f * scale)
                quadraticTo(400f * scale, 395f * scale, 335f * scale, 435f * scale)
                close()
            }
            drawPath(
                path = centerPath,
                brush = centerGrad,
                alpha = 0.82f
            )

            // Sparkle Star Icon with independent scale and twinkle glow
            if (sparkleAlpha > 0f) {
                val sparkCenterX = 585f * scale
                val sparkCenterY = 250f * scale

                if (sparkleGlow > 0f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF38BDF8).copy(alpha = 0.65f * sparkleGlow * sparkleAlpha),
                                Color(0xFF3B82F6).copy(alpha = 0.25f * sparkleGlow * sparkleAlpha),
                                Color.Transparent
                            ),
                            center = Offset(sparkCenterX, sparkCenterY),
                            radius = 65f * scale * sparkleScale
                        ),
                        radius = 65f * scale * sparkleScale,
                        center = Offset(sparkCenterX, sparkCenterY)
                    )
                }

                val starPath = Path().apply {
                    val cx = sparkCenterX
                    val cy = sparkCenterY
                    val r = 35f * scale * sparkleScale

                    moveTo(cx, cy - r)
                    quadraticTo(cx, cy, cx + r, cy)
                    quadraticTo(cx, cy, cx, cy + r)
                    quadraticTo(cx, cy, cx - r, cy)
                    quadraticTo(cx, cy, cx, cy - r)
                    close()
                }

                drawPath(
                    path = starPath,
                    color = Color(0xFF1D4ED8),
                    alpha = sparkleAlpha
                )
            }
        }
    }
}
