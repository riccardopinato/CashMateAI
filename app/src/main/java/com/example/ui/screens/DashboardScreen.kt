package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.AccountViewModel
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.domain.model.SavingsGoal
import com.example.ui.theme.*
import com.example.ui.components.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAiCoach: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToSplitBill: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSavingsGoals: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {}
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val userProfile by viewModel.currentUser.collectAsStateWithLifecycle()
    val accountViewModel: AccountViewModel = viewModel()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val savingsViewModel: com.example.ui.viewmodel.SavingsViewModel = viewModel()
    val savingsGoals by savingsViewModel.savingsGoals.collectAsStateWithLifecycle()

    // Calculate totals
    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    val balance = if (accounts.isNotEmpty()) accounts.sumOf { it.saldo } else (totalIncome - totalExpense)

    // Calculate remaining budget
    val totalBudgetLimit = budgets.sumOf { it.limitAmount }
    val remainingBudget = if (totalBudgetLimit > 0) {
        (totalBudgetLimit - totalExpense).coerceAtLeast(0.0)
    } else {
        0.0
    }

    // Filter last 4 transactions for recent activity
    val recentTransactions = transactions.take(4)

    // Animation entry state
    var startAnimations by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimations = true
    }

    Scaffold(
        containerColor = BackgroundWarm,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ciao, ${userProfile?.displayName?.ifBlank { "Utente CashMate" } ?: "Utente CashMate"}! 👋",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Cash",
                                fontSize = 24.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Mate AI",
                                fontSize = 24.sp,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                },
                actions = {
                    if (userProfile?.isPremium == true) {
                        Surface(
                            color = LimeSecondary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = "Premium",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF090B10),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    // Bell notification icon with a little green dot
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SurfaceWhite)
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                            .clickable { /* Notify */ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        // Green notification dot at top right
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWarm)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Card Enorme (Total Balance + Custom Curve Graph)
            item {
                AnimatedVisibility(
                    visible = startAnimations,
                    enter = fadeIn(animationSpec = tween(500)) + expandVertically(animationSpec = tween(500))
                ) {
                    HeroBalanceCard(balance = balance, currency = currency, onClick = onNavigateToAccounts)
                }
            }

            // 2. Entrate & Uscite (Two cards side-by-side with glowing mini-sparklines)
            item {
                AnimatedVisibility(
                    visible = startAnimations,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 100)) + slideInVertically(animationSpec = tween(600, delayMillis = 100)) { it / 2 }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            FinanceStatCard(
                                title = "Entrate",
                                value = totalIncome,
                                percentage = "+12,5%",
                                currency = currency,
                                isIncome = true
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            FinanceStatCard(
                                title = "Uscite",
                                value = totalExpense,
                                percentage = "-8,2%",
                                currency = currency,
                                isIncome = false
                            )
                        }
                    }
                }
            }

            // 3. Coach AI (Becomes the main glowing wide insight card with modern vector robot)
            item {
                AnimatedVisibility(
                    visible = startAnimations,
                    enter = fadeIn(animationSpec = tween(700, delayMillis = 200)) + slideInVertically(animationSpec = tween(700, delayMillis = 200)) { it / 2 }
                ) {
                    AiCoachCard(
                        onClick = onNavigateToAiCoach
                    )
                }
            }

            // 3.5. Savings Goals Entry Card
            item {
                AnimatedVisibility(
                    visible = startAnimations,
                    enter = fadeIn(animationSpec = tween(750, delayMillis = 250)) + slideInVertically(animationSpec = tween(750, delayMillis = 250)) { it / 2 }
                ) {
                    SavingsGoalsEntryCard(
                        goals = savingsGoals,
                        onClick = onNavigateToSavingsGoals
                    )
                }
            }

            // 4. Two cards side-by-side: Split Card and Budget Card
            item {
                AnimatedVisibility(
                    visible = startAnimations,
                    enter = fadeIn(animationSpec = tween(800, delayMillis = 300)) + slideInVertically(animationSpec = tween(800, delayMillis = 300)) { it / 2 }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            SplitCard(
                                onClick = onNavigateToSplitBill
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            BudgetCard(
                                remainingBudget = remainingBudget,
                                totalBudgetLimit = totalBudgetLimit,
                                currency = currency,
                                onClick = onNavigateToAnalytics
                            )
                        }
                    }
                }
            }

            // 5. Azioni Rapide (4 modern square cards)
            item {
                AnimatedVisibility(
                    visible = startAnimations,
                    enter = fadeIn(animationSpec = tween(900, delayMillis = 400))
                ) {
                    Column {
                        SectionTitle(text = "Azioni rapide")
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            QuickActionCard(
                                icon = Icons.Default.Add,
                                label = "Aggiungi\nSpesa",
                                circleColor = Color(0xFF00E676),
                                iconColor = Color(0xFF00E676),
                                onClick = onNavigateToAddTransaction
                            )
                            QuickActionCard(
                                icon = Icons.Default.Group,
                                label = "Dividi\nConto",
                                circleColor = Color(0xFF8B5CF6),
                                iconColor = Color(0xFF8B5CF6),
                                onClick = onNavigateToSplitBill
                            )
                            QuickActionCard(
                                icon = Icons.Default.BarChart,
                                label = "Analisi",
                                circleColor = Color(0xFF3B82F6),
                                iconColor = Color(0xFF3B82F6),
                                onClick = onNavigateToAnalytics
                            )
                            QuickActionCard(
                                icon = Icons.Default.SmartToy,
                                label = "AI Coach",
                                circleColor = Color(0xFFFFC857),
                                iconColor = Color(0xFFFFC857),
                                onClick = onNavigateToAiCoach
                            )
                        }
                    }
                }
            }

            // 6. Ultime Transazioni Header
            item {
                AnimatedVisibility(
                    visible = startAnimations,
                    enter = fadeIn(animationSpec = tween(1000, delayMillis = 500))
                ) {
                    SectionTitle(
                        text = "Ultime transazioni",
                        actionText = "Vedi tutte",
                        onActionClick = onNavigateToTransactions
                    )
                }
            }

            // Ultime Transazioni Rows
            if (recentTransactions.isEmpty()) {
                item {
                    BrandCard(borderGlow = false) {
                        BrandEmptyState(
                            title = "Nessun movimento",
                            subtitle = "Tieni traccia delle tue entrate e uscite premendo il tasto sotto.",
                            buttonText = "Aggiungi transazione",
                            onButtonClick = onNavigateToAddTransaction,
                            icon = Icons.Default.ReceiptLong
                        )
                    }
                }
            } else {
                items(recentTransactions) { tx ->
                    AnimatedVisibility(
                        visible = startAnimations,
                        enter = fadeIn(animationSpec = tween(600)) + expandVertically()
                    ) {
                        TransactionRow(transaction = tx, currency = currency)
                    }
                }
            }

            // Extra padding at bottom
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

// ==========================================
// REUSABLE PREMIUM UI COMPONENTS
// ==========================================

@Composable
fun HeroBalanceCard(balance: Double, currency: String, onClick: () -> Unit = {}) {
    var selectedInterval by remember { mutableStateOf("7G") }
    var animateChart by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animateChart = true
    }
    val chartProgress by animateFloatAsState(
        targetValue = if (animateChart) 1f else 0f,
        animationSpec = tween(1500, easing = EaseInOutCubic),
        label = "hero_chart"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
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
            .padding(top = 8.dp)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color(0xFF00E676).copy(alpha = 0.25f),
                spotColor = Color(0xFF00E676).copy(alpha = 0.25f)
            )
            .scaleClickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceWhite,
                            Color(0xFF0C1B14) // Rich elegant soft green gradient bottom corner tint
                        )
                    )
                )
                .padding(20.dp)
        ) {
            // Subtle neon gradient radial light on right corner for maximum fintech depth
            Canvas(modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E676).copy(alpha = 0.16f), Color.Transparent),
                        radius = size.width
                    )
                )
            }

            Column {
                Text(
                    text = "SALDO TOTALE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = String.format(Locale.getDefault(), "%,.2f %s", balance, currency),
                            color = TextPrimary,
                            fontSize = 35.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "+18,4%",
                                color = Color(0xFF00E676),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "rispetto al mese scorso",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Neon green line chart on the right side
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(65.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val width = size.width
                            val height = size.height

                            val points = listOf(
                                Offset(0f, height - (height - height * 0.75f) * chartProgress),
                                Offset(width * 0.2f, height - (height - height * 0.65f) * chartProgress),
                                Offset(width * 0.4f, height - (height - height * 0.82f) * chartProgress),
                                Offset(width * 0.6f, height - (height - height * 0.45f) * chartProgress),
                                Offset(width * 0.8f, height - (height - height * 0.35f) * chartProgress),
                                Offset(width, height - (height - height * 0.12f) * chartProgress)
                            )

                            val path = Path().apply {
                                moveTo(points[0].x, points[0].y)
                                for (i in 1 until points.size) {
                                    val prev = points[i - 1]
                                    val curr = points[i]
                                    cubicTo(
                                        (prev.x + curr.x) / 2, prev.y,
                                        (prev.x + curr.x) / 2, curr.y,
                                        curr.x, curr.y
                                    )
                                }
                            }

                            // Glowing curve line
                            drawPath(
                                path = path,
                                color = Color(0xFF00E676),
                                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Gradient shadow under line
                            val fillPath = Path().apply {
                                addPath(path)
                                lineTo(width, height)
                                lineTo(0f, height)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF00E676).copy(alpha = 0.22f), Color.Transparent)
                                )
                            )

                            // Glowing endpoint circle
                            drawCircle(
                                color = Color(0xFF00E676),
                                radius = 4.dp.toPx(),
                                center = points.last()
                            )
                        }

                        // Volume micro bars at the bottom right of the graph card
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 4.dp, bottom = 2.dp)
                        ) {
                            val barHeights = listOf(10.dp, 16.dp, 12.dp, 24.dp, 18.dp)
                            barHeights.forEach { h ->
                                val animatedHeight by animateDpAsState(
                                    targetValue = if (animateChart) h else 0.dp,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(5.dp)
                                        .height(animatedHeight)
                                        .clip(RoundedCornerShape(100))
                                        .background(Color(0xFF00E676).copy(alpha = 0.6f))
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interval Selector pills: 7G, 1M, 3M, 1A
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val intervals = listOf("7G", "1M", "3M", "1A")
                    intervals.forEach { interval ->
                        val isSel = selectedInterval == interval
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
                                .background(if (isSel) Color(0xFF00E676).copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { selectedInterval = interval }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = interval,
                                color = if (isSel) Color(0xFF00E676) else TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FinanceStatCard(
    title: String,
    value: Double,
    percentage: String,
    currency: String,
    isIncome: Boolean
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Colored icon background badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isIncome) Color(0xFF00E676).copy(alpha = 0.12f) else ExpenseRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = if (isIncome) Color(0xFF00E676) else ExpenseRed,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%,.1f %s", value, currency),
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = percentage,
                        color = if (isIncome) Color(0xFF00E676) else ExpenseRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Mini sparkline drawn inside the card
            Box(modifier = Modifier.size(50.dp, 35.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val strokeColor = if (isIncome) Color(0xFF00E676) else ExpenseRed

                    // Draw a mini beautiful wave
                    val path = Path().apply {
                        moveTo(0f, if (isIncome) h * 0.8f else h * 0.2f)
                        cubicTo(
                            w * 0.3f, if (isIncome) h * 0.5f else h * 0.6f,
                            w * 0.6f, if (isIncome) h * 0.7f else h * 0.4f,
                            w, if (isIncome) h * 0.2f else h * 0.8f
                        )
                    }
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun RobotMascot(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Side ear knobs
        drawRoundRect(
            color = Color(0xFF64748B),
            topLeft = Offset(w * 0.05f, h * 0.38f),
            size = Size(w * 0.12f, h * 0.24f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF64748B),
            topLeft = Offset(w * 0.83f, h * 0.38f),
            size = Size(w * 0.12f, h * 0.24f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Antenna stick
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(w * 0.5f, h * 0.3f),
            end = Offset(w * 0.5f, h * 0.12f),
            strokeWidth = 3.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Antenna tip
        drawCircle(
            color = Color(0xFF00E676),
            radius = 6.dp.toPx(),
            center = Offset(w * 0.5f, h * 0.1f)
        )
        drawCircle(
            color = Color(0xFF00E676).copy(alpha = 0.3f),
            radius = 12.dp.toPx(),
            center = Offset(w * 0.5f, h * 0.1f)
        )

        // Main metallic head
        drawRoundRect(
            color = Color(0xFF334155),
            topLeft = Offset(w * 0.15f, h * 0.25f),
            size = Size(w * 0.7f, h * 0.55f),
            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
        )

        // Head inner shadow/highlights
        drawRoundRect(
            color = Color(0xFF475569),
            topLeft = Offset(w * 0.18f, h * 0.28f),
            size = Size(w * 0.64f, h * 0.49f),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
        )

        // Visor glass
        drawRoundRect(
            color = Color(0xFF0F172A),
            topLeft = Offset(w * 0.24f, h * 0.35f),
            size = Size(w * 0.52f, h * 0.35f),
            cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
        )

        // Glowing Robot Eyes
        drawRoundRect(
            color = Color(0xFF00E676),
            topLeft = Offset(w * 0.32f, h * 0.45f),
            size = Size(w * 0.12f, h * 0.08f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF00E676),
            topLeft = Offset(w * 0.56f, h * 0.45f),
            size = Size(w * 0.12f, h * 0.08f),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // Eyes highlights
        drawCircle(
            color = Color.White,
            radius = 1.5.dp.toPx(),
            center = Offset(w * 0.35f, h * 0.47f)
        )
        drawCircle(
            color = Color.White,
            radius = 1.5.dp.toPx(),
            center = Offset(w * 0.59f, h * 0.47f)
        )

        // Neck
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(w * 0.35f, h * 0.8f),
            size = Size(w * 0.3f, h * 0.1f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
    }
}

@Composable
fun AiCoachCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .scaleClickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0C2417), Color(0xFF14532D)),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
                .border(1.dp, Color(0xFF00E676).copy(alpha = 0.25f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            // Background glowing light
            Canvas(
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 40.dp)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF00E676).copy(alpha = 0.18f), Color.Transparent),
                        radius = size.width / 1.5f
                    )
                )
            }

            // Custom cute vector robot mascot
            RobotMascot(
                modifier = Modifier
                    .size(110.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp, y = (-5).dp)
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "COACH AI",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100))
                            .background(Color(0xFF00E676).copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "NUOVO",
                            color = Color(0xFF00E676),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Ottimo lavoro! Questo mese hai speso il 27% in meno rispetto al mese scorso.",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth(0.68f)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA8FF60)),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(100),
                    modifier = Modifier.testTag("bento_ask_advice_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Chiedi Consiglio",
                            color = Color(0xFF090B10),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF090B10),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplitCard(onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .scaleClickable { onClick() }
            .testTag("bento_split_bill_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cena con Amici",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Overlapping initials avatar circles
            Row(
                horizontalArrangement = Arrangement.spacedBy((-10).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val initialLetters = listOf("M", "S", "L")
                val avatarColors = listOf(
                    Color(0xFF3B82F6), // Blue
                    Color(0xFF10B981), // Green
                    Color(0xFF8B5CF6)  // Purple
                )
                avatarColors.forEachIndexed { idx, color ->
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(2.dp, SurfaceWhite, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initialLetters[idx],
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Dividi spesa • 124,00 €",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun BudgetCard(remainingBudget: Double, totalBudgetLimit: Double, currency: String, onClick: () -> Unit) {
    // Elegant entry animation for progress bar
    var targetProgress by remember { mutableStateOf(0f) }
    val progressAnimated by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "budget_progress"
    )

    LaunchedEffect(remainingBudget, totalBudgetLimit) {
        targetProgress = if (totalBudgetLimit > 0) {
            ((totalBudgetLimit - remainingBudget) / totalBudgetLimit).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .scaleClickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Budget",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = String.format(Locale.getDefault(), "%,.0f %s", remainingBudget, currency),
                color = Color(0xFF00E676),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progressAnimated },
                color = if (progressAnimated > 0.85f) ExpenseRed else Color(0xFF00E676),
                trackColor = Color.White.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(100))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "di ${String.format(Locale.getDefault(), "%,.0f", totalBudgetLimit)} $currency",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    label: String,
    circleColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .width(82.dp)
            .clickable { onClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(circleColor.copy(alpha = 0.12f))
                    .border(1.dp, circleColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 13.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun SectionTitle(
    text: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        if (actionText != null && onActionClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.clickable { onActionClick() }
            ) {
                Text(
                    text = actionText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionRow(transaction: Transaction, currency: String) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val formattedDate = sdf.format(Date(transaction.dateMillis))

    val isIncome = transaction.type == TransactionType.INCOME
    val (icon, tint) = getCategoryMeta(transaction.category, isIncome)

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .testTag("transaction_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge inside a rounded container with a colored background
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (transaction.note.isNotBlank()) transaction.note else transaction.category,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${transaction.category} • $formattedDate",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount Text
            Text(
                text = String.format(Locale.getDefault(), "%s%,.2f %s", if (isIncome) "+" else "-", transaction.amount, currency),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = if (isIncome) Color(0xFF00E676) else ExpenseRed
            )
        }
    }
}

// Helpers
fun getCategoryMeta(category: String, isIncome: Boolean): Pair<ImageVector, Color> {
    if (isIncome) return Pair(Icons.Default.ArrowDownward, Color(0xFF00E676))
    return when (category.lowercase(Locale.getDefault())) {
        "food" -> Pair(Icons.Default.Fastfood, Color(0xFFFFC857)) // WarningYellow
        "transport" -> Pair(Icons.Default.DirectionsCar, Color(0xFF3B82F6)) // Blue
        "shopping" -> Pair(Icons.Default.ShoppingBag, Color(0xFFEC4899)) // Pink
        "bills" -> Pair(Icons.Default.Receipt, Color(0xFF8B5CF6)) // Purple
        "entertainment" -> Pair(Icons.Default.SportsEsports, Color(0xFF00E676)) // Emerald
        "health" -> Pair(Icons.Default.LocalHospital, Color(0xFFFF5A5F)) // Red/Danger
        "travel" -> Pair(Icons.Default.Flight, Color(0xFF06B6D4)) // Cyan
        "salary" -> Pair(Icons.Default.AttachMoney, Color(0xFF00E676)) // Emerald
        else -> Pair(Icons.Default.Category, Color(0xFF6B6B6B)) // Gray
    }
}

@Composable
fun SavingsGoalsEntryCard(goals: List<SavingsGoal>, onClick: () -> Unit) {
    val activeGoal = goals.firstOrNull { it.status == "ACTIVE" }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF0C1B14), Color(0xFF11261B)),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    )
                )
                .border(1.dp, Color(0xFF00E676).copy(alpha = 0.18f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            // Background glowing light
            Canvas(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 30.dp)
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFC857).copy(alpha = 0.15f), Color.Transparent),
                        radius = size.width / 1.5f
                    )
                )
            }

            if (activeGoal != null) {
                val progress = (activeGoal.currentAmount / activeGoal.targetAmount).toFloat().coerceIn(0f, 1f)
                val percentage = (progress * 100).toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(activeGoal.emoji, fontSize = 12.sp)
                            }
                            Text("OBIETTIVO ATTIVO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676), letterSpacing = 1.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(activeGoal.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))

                        // Mini progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(100))
                                .background(Color.White.copy(alpha = 0.05f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(100))
                                    .background(Color(0xFF00E676))
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("$percentage%", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E676))
                        Text("completato", fontSize = 9.sp, color = TextSecondary)
                    }
                }
            } else {
                // Beautiful emoji display with shadow
                Text(
                    text = "🎯",
                    fontSize = 58.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-10).dp, y = (-2).dp)
                )

                Column(modifier = Modifier.fillMaxWidth(0.7f)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100))
                            .background(Color(0xFF00E676).copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "NEW FEATURE",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Obiettivi di Risparmio",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Monitora i tuoi risparmi in stile Revolut Vaults.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

