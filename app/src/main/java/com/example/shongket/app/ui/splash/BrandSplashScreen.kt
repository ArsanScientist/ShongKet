package com.example.shongket.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shongket.ui.theme.BrandBlue

/**
 * Brand splash shown for a beat after the system splash: the name "সংকেত" / "SHONGKET"
 * beneath it. Compose shapes the Bangla correctly.
 */
@Composable
fun BrandSplashScreen() {
    val shown = remember { mutableStateOf(false) }
    val fade by animateFloatAsState(
        targetValue = if (shown.value) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "splashFade",
    )
    // Trigger the fade-in on first composition.
    androidx.compose.runtime.LaunchedEffect(Unit) { shown.value = true }

    Column(
        Modifier.fillMaxSize().background(BrandBlue),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(24.dp))
        androidx.compose.material3.Text(
            "সংকেত", color = Color.White, fontWeight = FontWeight.ExtraBold,
            fontSize = 56.sp, lineHeight = 60.sp, modifier = Modifier.alpha(fade),
        )
        Spacer(Modifier.height(12.dp))
        androidx.compose.material3.Text(
            "SHONGKET", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold,
            fontSize = 16.sp, letterSpacing = 6.sp, modifier = Modifier.alpha(fade),
        )
    }
}



