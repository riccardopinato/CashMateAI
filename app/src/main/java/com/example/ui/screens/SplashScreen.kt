package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BackgroundWarm
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay

import androidx.compose.ui.draw.scale
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.95f) }

    LaunchedEffect(Unit) {
        // Run animations in parallel
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500)
            )
        }
        
        // Keep visible briefly for premium branding
        delay(1400)
        
        // Snappy fade out
        launch {
            alphaAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 0.95f,
                animationSpec = tween(durationMillis = 300)
            )
        }
        delay(300)
        
        // Finish splash
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWarm),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            // Elegant Centered Logo with custom soft emerald glow
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(32.dp),
                        clip = false,
                        ambientColor = EmeraldPrimary.copy(alpha = 0.4f),
                        spotColor = EmeraldPrimary.copy(alpha = 0.6f)
                    )
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0xFF11161F)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_cashmate_brand_logo),
                    contentDescription = "CashMate AI Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Brand typography
            Text(
                text = "CashMate AI",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "IL TUO ASSISTENTE FINANZIARIO INTELLIGENTE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary,
                letterSpacing = 2.sp
            )
        }
    }
}
