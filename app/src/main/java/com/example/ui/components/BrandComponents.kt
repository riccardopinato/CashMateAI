package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Brand Components for CashMate AI
 * Centralizes UI elements for an incredibly consistent, premium experience.
 */

/**
 * 1. Shimmer Modifier for Skeleton Loading screens.
 * Animates a glowing linear gradient across any component.
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        Color(0xFF161E2E),
        Color(0xFF232D3F),
        Color(0xFF161E2E)
    )

    this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 200f, translateAnim - 200f),
            end = Offset(translateAnim + 200f, translateAnim + 200f)
        )
    )
}

/**
 * Reusable Shimmer Rectangle to quickly build Skeleton Screen layouts.
 */
@Composable
fun ShimmerBlock(
    width: Dp = Dp.Unspecified,
    height: Dp,
    cornerRadius: Dp = 12.dp,
    modifier: Modifier = Modifier
) {
    val sizeModifier = if (width == Dp.Unspecified) {
        Modifier.fillMaxWidth().height(height)
    } else {
        Modifier.size(width, height)
    }

    Box(
        modifier = modifier
            .then(sizeModifier)
            .clip(RoundedCornerShape(cornerRadius))
            .shimmerEffect()
    )
}

/**
 * Skeleton Loader Card for Accounts/Transactions/Analytics
 */
@Composable
fun ShimmerCardLoader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceWhite)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ShimmerBlock(width = 36.dp, height = 36.dp, cornerRadius = 18.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        ShimmerBlock(width = 100.dp, height = 12.dp)
                        ShimmerBlock(width = 60.dp, height = 8.dp)
                    }
                }
                ShimmerBlock(width = 70.dp, height = 14.dp)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.04f))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBlock(width = 120.dp, height = 10.dp)
                ShimmerBlock(width = 40.dp, height = 10.dp)
            }
        }
    }
}

/**
 * 2. Standardized Scale Touch Action Modifier.
 * Adds tactile bouncy scale-down feedback on click to give UI high-quality physicality.
 * Triggers lightweight haptic vibration on down tap.
 */
fun Modifier.scaleClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "click_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = androidx.compose.foundation.LocalIndication.current,
            enabled = enabled,
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        )
}

/**
 * 3. Standard Brand Card container
 */
@Composable
fun BrandCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderGlow: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier.scaleClickable { onClick() }
    } else {
        modifier
    }

    val border = if (borderGlow) {
        BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = listOf(
                    EmeraldPrimary.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        )
    } else {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.06f))
    }

    val shadowModifier = if (borderGlow) {
        Modifier.shadow(
            elevation = 14.dp,
            shape = RoundedCornerShape(20.dp),
            clip = false,
            ambientColor = EmeraldPrimary.copy(alpha = 0.15f),
            spotColor = EmeraldPrimary.copy(alpha = 0.15f)
        )
    } else {
        Modifier
    }

    Box(
        modifier = shadowModifier
            .then(cardModifier)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceWhite,
                        if (borderGlow) Color(0xFF0C1B14) else SurfaceWhite
                    )
                )
            )
            .border(border, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column {
            content()
        }
    }
}

/**
 * 4. Beautiful, consistent Empty State component
 * Displays high-quality custom geometric illustration, title, subtitle, and primary CTA.
 */
@Composable
fun BrandEmptyState(
    title: String,
    subtitle: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
    icon: ImageVector = Icons.Default.Info,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Futuristic geometric Canvas graphic representing digital placeholder
        Box(
            modifier = Modifier
                .size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 0.93f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2000, easing = EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse_scale"
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
            ) {
                // Subtle radial backdrop glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(EmeraldPrimary.copy(alpha = 0.08f), Color.Transparent),
                        radius = size.width / 1.5f
                    )
                )
                
                // Fine futuristic outline circle
                drawCircle(
                    color = EmeraldPrimary.copy(alpha = 0.15f),
                    radius = size.width / 3f,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Beautiful overlapping mini geometric card element
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceWhite)
                    .border(BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f)), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (buttonText != null && onButtonClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onButtonClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(44.dp)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * 5. Elegant notification / Feedback alert banner
 */
@Composable
fun BrandSnackbarContent(
    message: String,
    isSuccess: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSuccess) Color(0xFF0B2418) else Color(0xFF2C1013))
            .border(
                BorderStroke(
                    1.dp,
                    if (isSuccess) EmeraldPrimary.copy(alpha = 0.3f) else ExpenseRed.copy(alpha = 0.3f)
                ),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isSuccess) EmeraldPrimary else ExpenseRed),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSuccess) "✓" else "✕",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
            Text(
                text = message,
                fontSize = 13.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * 6. Standardized Section Header to ensure uniform design.
 */
@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = (-0.3).sp
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                color = EmeraldPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onActionClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}
