package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val subtitle: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinishOnboarding: () -> Unit
) {
    val pages = remember {
        listOf(
            OnboardingPageData(
                title = "Benvenuto in CashMate AI",
                subtitle = "Il tuo assistente finanziario intelligente."
            ),
            OnboardingPageData(
                title = "Controlla ogni euro",
                subtitle = "Monitora entrate, uscite, budget e conti in un'unica app."
            ),
            OnboardingPageData(
                title = "Lascia lavorare l'AI",
                subtitle = "Ricevi consigli personalizzati per risparmiare e migliorare le tue abitudini."
            ),
            OnboardingPageData(
                title = "Raggiungi i tuoi obiettivi",
                subtitle = "Risparmia con metodo, monitora i tuoi progressi e costruisci il tuo futuro."
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = BackgroundWarm,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        // Premium background radial glow that spreads from the center
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmeraldPrimary.copy(alpha = 0.04f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Top Row)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo + App Name (Branding)
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_cashmate_brand_logo),
                            contentDescription = "CashMate AI Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CashMate AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // "Salta" (Skip Button) in high contrast elegant layout
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .clickable { onFinishOnboarding() }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Salta",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                // Horizontal Pager for Illustrations and text content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { pageIndex ->
                    val page = pages[pageIndex]
                    val isCurrentPage = pagerState.currentPage == pageIndex

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Dynamically animated illustration container
                        OnboardingIllustrationContainer(
                            pageIndex = pageIndex,
                            isCurrentPage = isCurrentPage,
                            modifier = Modifier.padding(bottom = 40.dp)
                        )

                        // Title with bold premium layout
                        Text(
                            text = page.title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Subtitle
                        Text(
                            text = page.subtitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Footer Section (Indicators & Action Button)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Page Indicator (Active is a stretched Green pill)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        repeat(pages.size) { i ->
                            val isSelected = pagerState.currentPage == i
                            val widthAnimation by animateDpAsState(
                                targetValue = if (isSelected) 24.dp else 8.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "indicator_width"
                            )
                            val colorAnimation by animateColorAsState(
                                targetValue = if (isSelected) EmeraldPrimary else Color.White.copy(alpha = 0.15f),
                                label = "indicator_color"
                            )

                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .height(8.dp)
                                    .width(widthAnimation)
                                    .clip(CircleShape)
                                    .background(colorAnimation)
                            )
                        }
                    }

                    // Main Action Button
                    val isLastPage = pagerState.currentPage == pages.size - 1
                    Button(
                        onClick = {
                            if (isLastPage) {
                                onFinishOnboarding()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("onboarding_action_button")
                    ) {
                        Text(
                            text = if (isLastPage) "Inizia ora" else "Continua",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun OnboardingIllustrationContainer(
    pageIndex: Int,
    isCurrentPage: Boolean,
    modifier: Modifier = Modifier
) {
    val animateState = remember { Animatable(0f) }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            animateState.snapTo(0f)
            animateState.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
            )
        } else {
            animateState.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center
    ) {
        when (pageIndex) {
            0 -> WalletIllustration(animateState.value)
            1 -> DashboardIllustration(animateState.value)
            2 -> AiCoachIllustration(animateState.value)
            3 -> ObjectivesIllustration(animateState.value)
        }
    }
}

@Composable
fun WalletIllustration(progress: Float) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .scale(0.85f + 0.15f * progress)
            .alpha(progress),
        contentAlignment = Alignment.Center
    ) {
        // Base subtle neon glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldPrimary.copy(alpha = 0.12f * progress), Color.Transparent),
                    radius = size.minDimension / 1.5f
                ),
                radius = size.minDimension / 2f
            )
        }

        // Modern Card slot / Wallet representation
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(104.dp)
                .border(
                    BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.02f)))),
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF11161F),
                            Color(0xFF090B10)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Smart metallic chip
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Brush.linearGradient(listOf(LimeSecondary, EmeraldPrimary.copy(alpha = 0.4f))))
                    )
                    // Antenna signal representation
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(14.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }

                Column {
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.15f))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                }
            }
        }

        // Beautiful growing line graph rising directly from the card
        Canvas(
            modifier = Modifier
                .width(140.dp)
                .height(55.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-12).dp)
        ) {
            val path = Path().apply {
                moveTo(0f, size.height * 0.85f)
                cubicTo(
                    size.width * 0.25f, size.height * 0.75f,
                    size.width * 0.5f, size.height * 0.25f,
                    size.width * 0.7f, size.height * 0.45f
                )
                cubicTo(
                    size.width * 0.82f, size.height * 0.55f,
                    size.width * 0.92f, size.height * 0.05f,
                    size.width, size.height * 0.12f
                )
            }
            
            val pathMeasure = androidx.compose.ui.graphics.PathMeasure()
            pathMeasure.setPath(path, false)
            val animatedPath = Path()
            pathMeasure.getSegment(0f, pathMeasure.length * progress, animatedPath, true)

            // Draw line
            drawPath(
                path = animatedPath,
                color = EmeraldPrimary,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Dynamic gradient fill area underneath the rising graph
            val fillPath = Path().apply {
                addPath(animatedPath)
                lineTo(size.width * progress, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(EmeraldPrimary.copy(alpha = 0.15f), Color.Transparent),
                    startY = 0f,
                    endY = size.height
                )
            )

            // Growing glowing end point
            if (progress > 0.05f) {
                val position = pathMeasure.getPosition(pathMeasure.length * progress)
                drawCircle(
                    color = EmeraldPrimary,
                    radius = 5.dp.toPx(),
                    center = position
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = position
                )
            }
        }

        // Elegant AI Sparks floating
        AIStarSparkle(
            offsetX = 64.dp,
            offsetY = (-48).dp,
            scale = progress * 1.1f,
            color = LimeSecondary
        )
        AIStarSparkle(
            offsetX = (-68).dp,
            offsetY = 38.dp,
            scale = progress * 0.7f,
            color = EmeraldPrimary
        )
    }
}

@Composable
fun DashboardIllustration(progress: Float) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .scale(0.85f + 0.15f * progress)
            .alpha(progress),
        contentAlignment = Alignment.Center
    ) {
        // Base subtle yellow/lime glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(LimeSecondary.copy(alpha = 0.08f * progress), Color.Transparent),
                    radius = size.minDimension / 1.5f
                ),
                radius = size.minDimension / 2f
            )
        }

        // Modern Premium Dashboard container
        Box(
            modifier = Modifier
                .width(184.dp)
                .height(152.dp)
                .border(
                    BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceWhite)
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SALDO TOTALE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "€ 4.850,00",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                    }
                    
                    // Small floating badge with upward growth indicators
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldPrimary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+12%",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.06f))
                )

                // Elegant growable bar chart
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val targetHeights = listOf(0.4f, 0.65f, 0.5f, 0.85f, 1.0f)
                    targetHeights.forEachIndexed { index, heightFraction ->
                        val individualProgress = remember(progress) {
                            val delayOffset = index * 0.08f
                            ((progress - delayOffset) / (1f - delayOffset)).coerceIn(0f, 1f)
                        }
                        
                        val easedHeightFraction = CubicBezierEasing(0.25f, 1f, 0.5f, 1f).transform(individualProgress)
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(heightFraction * easedHeightFraction)
                                    .width(10.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                EmeraldPrimary,
                                                LimeSecondary.copy(alpha = 0.6f)
                                            )
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = if (individualProgress > 0.5f) 0.4f else 0.1f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiCoachIllustration(progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "hover_coach")
    val hoverOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hover_offset_val"
    )

    Box(
        modifier = Modifier
            .size(200.dp)
            .scale(0.85f + 0.15f * progress)
            .alpha(progress),
        contentAlignment = Alignment.Center
    ) {
        // Glowing radial light green AI coach glow background
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldPrimary.copy(alpha = 0.18f * progress), Color.Transparent),
                    radius = size.minDimension / 1.3f
                ),
                radius = size.minDimension / 1.8f
            )
        }

        // Futuristic Hologram AI Core Core Orb
        Box(
            modifier = Modifier
                .size(94.dp)
                .offset(y = hoverOffset.dp)
                .border(
                    BorderStroke(2.dp, Brush.radialGradient(listOf(Color.White, EmeraldPrimary.copy(alpha = 0.1f)))),
                    shape = CircleShape
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            EmeraldPrimary,
                            Color(0xFF0B2418),
                            Color(0xFF090B10)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                LimeSecondary,
                                EmeraldPrimary
                            )
                        )
                    )
            )
        }

        // Overlapping Insight Card (representing active coach tips)
        Box(
            modifier = Modifier
                .width(164.dp)
                .offset(x = 16.dp, y = 44.dp + hoverOffset.dp * (-1.3f))
                .border(
                    BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f * progress)),
                    shape = RoundedCornerShape(14.dp)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceWhite.copy(alpha = 0.95f))
                .padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(EmeraldLight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🤖",
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = "CONSIGLIO AI",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Risparmiati €42 questo mese! ⚡",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }
        }

        AIStarSparkle(
            offsetX = (-64).dp,
            offsetY = (-32).dp,
            scale = progress * 0.9f,
            color = LimeSecondary
        )
    }
}

@Composable
fun ObjectivesIllustration(progress: Float) {
    val circleProgress = remember(progress) { (progress * 1.2f).coerceIn(0f, 1f) }
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .scale(0.85f + 0.15f * progress)
            .alpha(progress),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(EmeraldPrimary.copy(alpha = 0.15f * progress), Color.Transparent),
                    radius = size.minDimension / 1.5f
                ),
                radius = size.minDimension / 2f
            )
        }

        // Circular dynamic Ring Progress Bar
        Canvas(modifier = Modifier.size(132.dp)) {
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                style = Stroke(width = 8.dp.toPx())
            )
            
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(EmeraldPrimary, LimeSecondary, EmeraldPrimary)
                ),
                startAngle = -90f,
                sweepAngle = 360f * circleProgress,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Badge pops up with custom spring scale overshoot transition
        val badgeScale = remember(progress) {
            val threshold = 0.6f
            if (progress < threshold) 0f
            else {
                val scaleProgress = (progress - threshold) / (1f - threshold)
                val overshoot = 1.15f
                if (scaleProgress < 0.7f) {
                    (scaleProgress / 0.7f) * overshoot
                } else {
                    overshoot - ((scaleProgress - 0.7f) / 0.3f) * (overshoot - 1f)
                }
            }
        }

        Box(
            modifier = Modifier
                .size(78.dp)
                .scale(badgeScale)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E293B),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .border(BorderStroke(2.dp, LimeSecondary), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🏆",
                fontSize = 34.sp
            )
        }

        // Floating Confetti Sparkles
        if (progress > 0.7f) {
            val particlesScale = (progress - 0.7f) / 0.3f
            AIStarSparkle(
                offsetX = (-56).dp,
                offsetY = (-52).dp,
                scale = particlesScale * 0.8f,
                color = LimeSecondary
            )
            AIStarSparkle(
                offsetX = 56.dp,
                offsetY = (-52).dp,
                scale = particlesScale * 0.8f,
                color = EmeraldPrimary
            )
            AIStarSparkle(
                offsetX = (-66).dp,
                offsetY = 32.dp,
                scale = particlesScale * 0.6f,
                color = EmeraldPrimary
            )
            AIStarSparkle(
                offsetX = 66.dp,
                offsetY = 32.dp,
                scale = particlesScale * 0.6f,
                color = LimeSecondary
            )
        }
    }
}

@Composable
fun AIStarSparkle(
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    scale: Float,
    color: Color
) {
    Canvas(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(24.dp)
            .scale(scale)
    ) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxSparkSize = size.width / 2f
        
        val path = Path().apply {
            moveTo(centerX, centerY - maxSparkSize)
            quadraticBezierTo(centerX, centerY, centerX + maxSparkSize, centerY)
            quadraticBezierTo(centerX, centerY, centerX, centerY + maxSparkSize)
            quadraticBezierTo(centerX, centerY, centerX - maxSparkSize, centerY)
            quadraticBezierTo(centerX, centerY, centerX, centerY - maxSparkSize)
            close()
        }
        drawPath(path, color)
    }
}
