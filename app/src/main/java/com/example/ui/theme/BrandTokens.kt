package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.R

/**
 * Brand Tokens for CashMate AI
 * Centralizes visual identities, logo, radius, typography, iconography, gradients and custom glow effects.
 */
object BrandTokens {
    // 1. BRAND COLORS
    val Primary = EmeraldPrimary             // #00E676 - Neon Emerald Green
    val Secondary = LimeSecondary           // #A8FF60 - Modern Lime Green
    val Background = BackgroundWarm         // #090B10 - Midnight Deep Navy/Black
    val Surface = SurfaceWhite              // #11161F - Premium Slate Card Surface
    val SurfaceElevated = SurfaceElevation  // #151C26 - Navigation Rail & Sheet Surface
    
    val TextMain = TextPrimary              // #FFFFFF - White
    val TextMuted = TextSecondary           // #B5BEC8 - Elegant Slate Gray
    
    // 2. BRAND IMAGES & ASSETS
    val AppLogoRes = R.drawable.img_cashmate_brand_logo
    val CoachAvatarRes = R.drawable.img_ai_coach_avatar
    
    // 3. SHAPES & CORNER RADIUS
    val RadiusSmall = RoundedCornerShape(8.dp)
    val RadiusMedium = RoundedCornerShape(12.dp)
    val RadiusLarge = RoundedCornerShape(16.dp)
    val RadiusXLarge = RoundedCornerShape(20.dp)
    val RadiusCard = RoundedCornerShape(24.dp)
    val RadiusSplash = RoundedCornerShape(32.dp)
    
    // 4. GRADIENTS
    val DiagonalPrimaryGradient = Brush.linearGradient(
        colors = listOf(Primary, Secondary)
    )
    
    val RadialBackgroundGlow = Brush.radialGradient(
        colors = listOf(Primary.copy(alpha = 0.05f), Color.Transparent),
        radius = 1200f
    )
    
    val SurfaceGradient = Brush.verticalGradient(
        colors = listOf(Surface, SurfaceElevated)
    )
    
    // 5. SHADOW & GLOW PARAMETERS
    val SoftGlowColor = Primary.copy(alpha = 0.4f)
    val HighGlowColor = Primary.copy(alpha = 0.6f)
    val GlowElevation = 24.dp
}
