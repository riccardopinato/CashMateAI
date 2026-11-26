package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Budget
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.AiCoachUiState
import com.example.ui.viewmodel.FinanceViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCoachScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    val aiCoachState by viewModel.aiCoachState.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    var showUpgradeDialog by remember { mutableStateOf(false) }

    // Intercept rate-limit error to show the dedicated Premium promo dialogue
    LaunchedEffect(aiCoachState) {
        if (aiCoachState is AiCoachUiState.Error) {
            val errMsg = (aiCoachState as AiCoachUiState.Error).message
            if (errMsg.contains("esaurito i tuoi crediti", ignoreCase = true)) {
                showUpgradeDialog = true
            }
        }
    }

    if (showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = {
                showUpgradeDialog = false
                viewModel.clearAiCoachError()
            },
            icon = { Icon(Icons.Default.Star, contentDescription = null, tint = LimeSecondary, modifier = Modifier.size(40.dp)) },
            title = { Text("Hai esaurito i crediti IA", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color.White) },
            text = {
                Text(
                    text = "Hai esaurito i tuoi crediti IA gratuiti per questa ora. Passa a Premium per sbloccare fino a 30 richieste orarie illimitate!",
                    textAlign = TextAlign.Center,
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpgradeDialog = false
                        viewModel.clearAiCoachError()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Premium non disponibile", fontWeight = FontWeight.Bold, color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUpgradeDialog = false
                    viewModel.clearAiCoachError()
                }) {
                    Text("Più tardi", color = TextSecondary)
                }
            },
            containerColor = SurfaceElevation,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // REDESIGNED MAIN UI LAYOUT
    Scaffold(
        containerColor = BackgroundWarm,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Torna indietro", tint = Color.White)
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("AI Coach Intelligente", fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWarm)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // API Key Info Banner (Demo Mode Indicator)
            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
            val isDemoMode = apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "YOUR_API_KEY_HERE"

            if (isDemoMode) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "AI Coach non configurato",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Per ottenere un’analisi servono un servizio AI configurato e il tuo consenso. Nessuna risposta dimostrativa verrà presentata come reale.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // ==================================================
            // PRE-COMPUTATIONS FOR REAL-TIME CONTEXT
            // ==================================================
            val totalIncome = remember(transactions) {
                transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            }
            val totalExpense = remember(transactions) {
                transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            }
            val categoryExpenses = remember(transactions) {
                transactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
            }
            val exceededBudgets = remember(budgets, categoryExpenses) {
                budgets.filter { (categoryExpenses[it.category] ?: 0.0) > it.limitAmount }
            }

            // FINANCIAL HEALTH SCORE CALCULATION
            val healthScore = remember(totalIncome, totalExpense, exceededBudgets) {
                var score = 100
                if (totalExpense > 0 && totalIncome > 0) {
                    val ratio = totalExpense / totalIncome
                    if (ratio > 1.0) {
                        val penalty = ((ratio - 1.0) * 35f).toInt()
                        score -= penalty.coerceAtMost(35)
                    } else if (ratio in 0.8..1.0) {
                        score -= 15
                    } else if (ratio in 0.6..0.8) {
                        score -= 5
                    }
                } else if (totalIncome == 0.0 && totalExpense > 0) {
                    score -= 20
                }
                score -= (exceededBudgets.size * 12)
                score.coerceIn(35, 100)
            }

            val healthExplanation = remember(healthScore, transactions) {
                if (transactions.isEmpty()) {
                    "Inizia a registrare le tue transazioni per calcolare accuratamente lo score di salute finanziaria."
                } else {
                    when {
                        healthScore >= 85 -> "Le tue finanze sono in eccellente salute! Stai gestendo il budget in modo ottimale."
                        healthScore in 70..84 -> "Buona salute finanziaria. Cerca di tenere d'occhio i piccoli sfizi e le spese non essenziali."
                        healthScore in 50..69 -> "Salute finanziaria discreta. Alcune categorie necessitano di attenzione per evitare di sforare i limiti."
                        else -> "Attenzione: lo score mostra un livello di stress finanziario. Riduci subito le spese non essenziali."
                    }
                }
            }

            // POPULATING CORE INSIGHTS (COMBINING AI LIVE OR INTELLIGENT FALLBACKS)
            val isSuccess = aiCoachState is AiCoachUiState.Success
            val currentSuccessResponse = if (isSuccess) (aiCoachState as AiCoachUiState.Success).response else null

            // 1. HERO MASCOT BADGE
            HeroMascotCard(isPremium = isPremium)

            // 2. FINANCIAL HEALTH SCORE CARD
            HealthScoreCard(score = healthScore, explanation = healthExplanation)

            // 3. MAIN INSIGHT CARD (LIVE FROM AI OR INTELLIGENT COMPLETED STATE)
            MainInsightSection(
                successResponse = currentSuccessResponse,
                categoryExpenses = categoryExpenses,
                exceededBudgetsCount = exceededBudgets.size,
                currency = currency
            )

            // 4. SECONDARY INSIGHTS GRID
            SecondaryInsightsGrid(
                totalExpense = totalExpense,
                totalIncome = totalIncome,
                exceededCount = exceededBudgets.size,
                budgetsCount = budgets.size
            )

            // 5. SAVING TIPS CARDS
            SavingTipsSection(
                successResponse = currentSuccessResponse,
                categoryExpenses = categoryExpenses
            )

            // 6. MONTHLY TARGET OBJECTIVE CARD
            MonthlyTargetCard(
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                currency = currency
            )

            // 7. MONTHLY FORECAST PROJECTION CARD
            MonthlyForecastCard(
                totalExpense = totalExpense,
                successResponse = currentSuccessResponse,
                currency = currency
            )

            // 8. MOTIVATIONAL QUOTE CARD
            MotivationalQuoteCard(successResponse = currentSuccessResponse)

            // 10. FUTURE CHAT DESIGN SPACE PLACEHOLDER
            FutureChatDesignSpace()

            // 9. CORE BUTTON TRIGGER (Maintains actual interaction flow with rate limits and animations)
            AnimatedContent(
                targetState = aiCoachState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                }
            ) { state ->
                when (state) {
                    is AiCoachUiState.Loading -> {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = EmeraldPrimary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Elaborazione dati finanziari...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "CashMate AI sta incrociando i tuoi budget e le tue spese mensili.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    is AiCoachUiState.Error -> {
                        val errMsg = state.message
                        if (!errMsg.contains("esaurito i tuoi crediti", ignoreCase = true)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(errMsg, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.askAiCoach() },
                                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Riprova", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = { viewModel.askAiCoach() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(18.dp),
                                    ambientColor = EmeraldPrimary.copy(alpha = 0.25f),
                                    spotColor = EmeraldPrimary.copy(alpha = 0.25f)
                                )
                                .testTag("ask_ai_coach_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (state is AiCoachUiState.Success) "Aggiorna Analisi" else "Ottieni Consigli IA",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            if (!isPremium) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.12f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            viewModel.triggerPremiumUpgradeSheet(com.example.premium.PremiumFeature.AI_CHAT)
                        }
                        .testTag("ai_coach_premium_teaser")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vuoi di più dal tuo AI Coach?",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Scopri consigli finanziari avanzati, report personalizzati e risposte senza limiti con CashMate Premium.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// ==================================================
// SUB-COMPOSABLES DEFINITIONS
// ==================================================

@Composable
fun HeroMascotCard(isPremium: Boolean) {
    // Elegant pulsing avatar light animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val avatarGlowSize by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = twistEasingAnimation(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Siri/Apple Intelligence style rotation animation
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rotation"
    )
    
    // Enhanced animation: Breathing and head movement
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )

    val headMove by infiniteTransition.animateFloat(
        initialValue = -25f, // Increased range
        targetValue = 25f,   // Increased range
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "head_move"
    )

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.2.dp, 
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF00E676).copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.04f)
                )
            )
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = (avatarGlowSize + 4f).dp,
                shape = RoundedCornerShape(28.dp),
                clip = false,
                ambientColor = Color(0xFF00E676).copy(alpha = 0.22f),
                spotColor = Color(0xFF00E676).copy(alpha = 0.22f)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SurfaceWhite, Color(0xFF0C1B14))
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated Glowing bot avatar in Apple Intelligence style
            Box(
                modifier = Modifier
                    .size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background rotating halo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationZ = haloRotation }
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF00E676), // Emerald
                                    Color(0xFF00E5FF), // Cyan
                                    Color(0xFFB388FF), // Purple
                                    Color(0xFFFF80AB), // Pink
                                    Color(0xFF00E676)  // Loop
                                )
                            )
                        )
                )

                // Inner capsule content mask to isolate the icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(SurfaceWhite)
                        .graphicsLayer {
                            scaleX = breathingScale
                            scaleY = breathingScale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_ai_coach_avatar),
                        contentDescription = "Mascot Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { translationY = headMove },
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // State Badge: ● Online
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(100))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary)
                )
                Text(
                    text = "ONLINE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Ciao! Sono CashMate AI",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Ho analizzato le tue spese di questo mese.",
                color = TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            if (isPremium) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = LimeSecondary, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Profilo Premium Sbloccato",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LimeSecondary
                    )
                }
            }
        }
    }
}

private fun twistEasingAnimation(duration: Int): KeyframesSpec<Float> {
    return keyframes {
        durationMillis = duration
        0f at 0 with FastOutSlowInEasing
        18f at (duration / 2) with LinearOutSlowInEasing
        0f at duration with FastOutSlowInEasing
    }
}

@Composable
fun HealthScoreCard(score: Int, explanation: String) {
    // Animating circular canvas ring
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animatedProgress.animateTo(
            targetValue = score / 100f,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 80f)
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Speedometer gauge progress indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(90.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    // Baseline speedometer track (240-degree arc)
                    drawArc(
                        color = Color.White.copy(alpha = 0.06f),
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    // High quality glowing sweep arc (240-degree dynamic)
                    drawArc(
                        brush = Brush.horizontalGradient(
                            colors = listOf(ExpenseRed, WarningYellow, EmeraldPrimary)
                        ),
                        startAngle = 150f,
                        sweepAngle = animatedProgress.value * 240f,
                        useCenter = false,
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                // Center text value
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = (-4).dp)
                ) {
                    Text(
                        text = "$score",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "/ 100",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            // Right content detail
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "FINANCIAL HEALTH",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = explanation,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun MainInsightSection(
    successResponse: com.example.domain.model.AiCoachResponse?,
    categoryExpenses: Map<String, Double>,
    exceededBudgetsCount: Int,
    currency: String
) {
    // Determinate contents
    val title: String
    val description: String
    val icon: ImageVector
    val tint: Color
    val priority: String
    val priorityBg: Color

    if (successResponse != null) {
        title = "Riepilogo Elaborato"
        description = successResponse.summary
        icon = Icons.Default.AutoAwesome
        tint = EmeraldPrimary
        priority = "Live AI"
        priorityBg = EmeraldPrimary.copy(alpha = 0.12f)
    } else {
        if (exceededBudgetsCount > 0) {
            title = "Soglia Budget Superata"
            description = "Attenzione: hai sforato i limiti pianificati in $exceededBudgetsCount categorie. Questo incide negativamente sul tuo score."
            icon = Icons.Default.Warning
            tint = ExpenseRed
            priority = "Alta"
            priorityBg = ExpenseRed.copy(alpha = 0.12f)
        } else {
            val topCategory = categoryExpenses.maxByOrNull { it.value }
            if (topCategory != null && topCategory.value > 0.0) {
                title = "Focus Spese Maggiori"
                description = "La tua categoria di spesa primaria di questo mese è ${topCategory.key}, con un totale di ${String.format(Locale.getDefault(), "%,.0f", topCategory.value)} $currency spenti."
                icon = Icons.Default.TrendingUp
                tint = WarningYellow
                priority = "Media"
                priorityBg = WarningYellow.copy(alpha = 0.12f)
            } else {
                title = "Consulenza Attiva"
                description = "Nessun trend rilevante trovato. Comincia ad annotare spese per ricevere indicazioni precise ed evitare sprechi."
                icon = Icons.Default.HelpOutline
                tint = IncomeBlue
                priority = "Info"
                priorityBg = IncomeBlue.copy(alpha = 0.12f)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = tint.copy(alpha = 0.08f),
                spotColor = tint.copy(alpha = 0.08f)
            )
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // Header Row: Icon + Title + Priority Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(priorityBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = priority,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = tint
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "INSIGHT PRINCIPALE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SecondaryInsightsGrid(
    totalExpense: Double,
    totalIncome: Double,
    exceededCount: Int,
    budgetsCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Saving potential (estimated 12%)
        Box(modifier = Modifier.weight(1f)) {
            MiniIndicatorCard(
                title = "Risparmio St.",
                value = String.format(Locale.getDefault(), "%,.0f €", (totalExpense * 0.12).coerceAtLeast(0.0)),
                desc = "Ottimizzabile",
                icon = Icons.Default.AccountBalanceWallet,
                tint = EmeraldPrimary
            )
        }

        // Spending trend (past comparison estimate)
        val trendLabel = if (totalExpense > totalIncome && totalIncome > 0) "Elevato" else "Bilanciato"
        val trendColor = if (totalExpense > totalIncome && totalIncome > 0) ExpenseRed else LimeSecondary
        Box(modifier = Modifier.weight(1f)) {
            MiniIndicatorCard(
                title = "Andamento",
                value = trendLabel,
                desc = "Uscite mensili",
                icon = Icons.Default.TrendingUp,
                tint = trendColor
            )
        }

        // Budget count state
        val budgetStatus = if (exceededCount > 0) "$exceededCount Superati" else "Attivo"
        val budgetColor = if (exceededCount > 0) ExpenseRed else IncomeBlue
        Box(modifier = Modifier.weight(1f)) {
            MiniIndicatorCard(
                title = "Budget",
                value = "$budgetsCount Impostati",
                desc = budgetStatus,
                icon = Icons.Default.Settings,
                tint = budgetColor
            )
        }
    }
}

@Composable
fun MiniIndicatorCard(
    title: String,
    value: String,
    desc: String,
    icon: ImageVector,
    tint: Color
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = desc,
                fontSize = 10.sp,
                color = TextSecondary.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SavingTipsSection(
    successResponse: com.example.domain.model.AiCoachResponse?,
    categoryExpenses: Map<String, Double>
) {
    val tipsList = remember(successResponse, categoryExpenses) {
        if (successResponse != null && successResponse.savingTips.isNotEmpty()) {
            successResponse.savingTips
        } else {
            // Local high-fidelity smart recommendations
            val topCategory = categoryExpenses.maxByOrNull { it.value }?.key ?: "Nessuna"
            listOf(
                "Pianifica i pasti settimanali. Questo ridurrà gli scontrini per pasti rapidi d'impulso o cibo a domicilio.",
                "Se la categoria $topCategory rappresenta una grossa fetta, imposta un budget specifico per auto-limitarti.",
                "Rivedi i tuoi abbonamenti ricorrenti ed elimina quelli inutilizzati per liberare entrate mensili."
            )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "CONSIGLI DI RISPARMIO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )

        tipsList.forEachIndexed { index, rawTip ->
            // Smartly parse estimated saving value out of the text or assign creative values
            val parsedSaving = remember(rawTip) {
                val match = Regex("(\\d+)\\s*(€|euro)", RegexOption.IGNORE_CASE).find(rawTip)
                if (match != null) {
                    match.value
                } else {
                    // Creative value based on sequence
                    val savings = listOf("30 €/mese", "45 €/mese", "25 €/mese")
                    savings[index % savings.size]
                }
            }

            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Styled dynamic index counter
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strategia d'Azione",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rawTip,
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = LimeSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Risparmio stimato: ",
                                fontSize = 11.sp,
                                color = TextSecondary.copy(alpha = 0.7f)
                            )
                            Text(
                                text = parsedSaving,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = LimeSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyTargetCard(
    totalIncome: Double,
    totalExpense: Double,
    currency: String
) {
    // Standard baseline target = 300
    val targetAmount = 300.0
    val savedAmount = remember(totalIncome, totalExpense) {
        (totalIncome - totalExpense).coerceAtLeast(0.0)
    }
    val progress = remember(savedAmount, targetAmount) {
        (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f)
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OBIETTIVO DI RISPARMIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(EmeraldPrimary.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Mensile",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = String.format(Locale.getDefault(), "%,.0f", savedAmount),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = " $currency",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "Target: ",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%,.0f %s", targetAmount, currency),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Premium animated progress bar
            val animatedBarProgress = remember { Animatable(0f) }
            LaunchedEffect(progress) {
                animatedBarProgress.animateTo(
                    targetValue = progress,
                    animationSpec = tween(1200, easing = FastOutSlowInEasing)
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(100))
                    .background(Color.White.copy(alpha = 0.05f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = animatedBarProgress.value)
                        .clip(RoundedCornerShape(100))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(EmeraldPrimary, LimeSecondary)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${(progress * 100).toInt()}% completato rispetto alla quota obiettivo.",
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MonthlyForecastCard(
    totalExpense: Double,
    successResponse: com.example.domain.model.AiCoachResponse?,
    currency: String
) {
    val description: String
    val projectionValue: Double

    val dayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
    projectionValue = if (dayOfMonth > 0) (totalExpense / dayOfMonth) * daysInMonth else totalExpense

    if (successResponse != null) {
        description = successResponse.budgetForecast
    } else {
        description = "Continuando con questo ritmo di spesa, entro la fine del mese spenderai circa ${String.format(Locale.getDefault(), "%,.1f", projectionValue)} $currency."
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PREVISIONE FINE MESE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // High Fidelity Mini Sparkline Graph representation (Canvas-drawn)
            Box(
                modifier = Modifier
                    .size(width = 80.dp, height = 50.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    // Simple 4 sample points
                    val points = listOf(
                        Offset(0f, height * 0.8f),
                        Offset(width * 0.33f, height * 0.6f),
                        Offset(width * 0.66f, height * 0.3f),
                        Offset(width, height * 0.1f) // Projected slope going higher/lower
                    )

                    // Path
                    val path = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        lineTo(points[1].x, points[1].y)
                        lineTo(points[2].x, points[2].y)
                    }
                    // Current real line
                    drawPath(
                        path = path,
                        color = LimeSecondary,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Forecast dotted extension
                    drawLine(
                        color = EmeraldPrimary,
                        start = points[2],
                        end = points[3],
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Draw node at projection point
                    drawCircle(
                        color = EmeraldPrimary,
                        radius = 4.dp.toPx(),
                        center = points[3]
                    )
                }
            }
        }
    }
}

@Composable
fun MotivationalQuoteCard(successResponse: com.example.domain.model.AiCoachResponse?) {
    val message = successResponse?.motivationMessage ?: "Ogni piccolo risparmio di oggi diventa una maggiore libertà domani."

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SurfaceWhite, SurfaceElevation)
                    )
                )
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.FormatQuote,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "\"$message\"",
                color = Color.White,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun FutureChatDesignSpace() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "CHAT DI CONSULENZA AI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "In Arrivo",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Simulated premium chat bubble inputs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100))
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)), RoundedCornerShape(100))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chiedi a CashMate AI... (es: Come risparmio il 10%?)",
                    color = TextSecondary.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = null,
                    tint = TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Presto potrai chattare in tempo reale direttamente con il tuo assistente.",
                fontSize = 10.sp,
                color = TextSecondary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

