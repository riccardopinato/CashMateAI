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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.SavingsGoal
import com.example.domain.model.Achievement
import com.example.domain.model.TimelineEvent
import com.example.ui.theme.*
import com.example.ui.components.*
import com.example.ui.viewmodel.SavingsViewModel
import com.example.ui.viewmodel.WeeklyReport
import com.example.ui.viewmodel.ProgressStats
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    viewModel: SavingsViewModel,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    onPremiumUpgradeRequired: (com.example.premium.PremiumFeature) -> Unit = {}
) {
    val goals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val timelineEvents by viewModel.timelineEvents.collectAsStateWithLifecycle()
    val weeklyReport by viewModel.weeklyReport.collectAsStateWithLifecycle()
    val stats by viewModel.progressStats.collectAsStateWithLifecycle()
    val badgeLevel by viewModel.userBadgeLevel.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Vaults, 1 = Report, 2 = Trofei, 3 = Timeline

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Goals Engine",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.03f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Indietro",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWarm)
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = {
                        if (com.example.premium.PremiumManager.canUseFeature(com.example.premium.PremiumFeature.GOALS, currentCount = goals.size)) {
                            showCreateDialog = true
                        } else {
                            onPremiumUpgradeRequired(com.example.premium.PremiumFeature.GOALS)
                        }
                    },
                    containerColor = Color(0xFF00E676),
                    contentColor = Color(0xFF090B10),
                    shape = CircleShape,
                    modifier = Modifier
                        .padding(bottom = 16.dp, end = 8.dp)
                        .testTag("create_goal_fab")
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = Color(0xFF00E676).copy(alpha = 0.4f),
                            spotColor = Color(0xFF00E676).copy(alpha = 0.4f)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Nuovo Obiettivo",
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        },
        containerColor = BackgroundWarm
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Elegant Multi-Tab Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val tabLabels = listOf("Vaults", "Report", "Trofei", "Timeline")
                tabLabels.forEachIndexed { index, label ->
                    val isSelected = selectedTab == index
                    val bgSelected = if (isSelected) Color(0xFF00E676).copy(alpha = 0.12f) else Color.Transparent
                    val borderSelected = if (isSelected) BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.25f)) else null
                    val textColor = if (isSelected) Color(0xFF00E676) else TextSecondary

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgSelected)
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Content depending on Tab
            when (selectedTab) {
                0 -> VaultsTabContent(goals, currency, stats, onNavigateToDetail) { showCreateDialog = true }
                1 -> ReportTabContent(weeklyReport, currency)
                2 -> AchievementsTabContent(achievements, badgeLevel)
                3 -> TimelineTabContent(timelineEvents)
            }
        }
    }

    if (showCreateDialog) {
        CreateGoalDialog(
            currency = currency,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, target, initial, emoji, date, desc, color, notes ->
                viewModel.addSavingsGoal(name, target, initial, emoji, date, desc, color, notes)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun HeroSavingsCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                clip = false,
                ambientColor = Color(0xFF00E676).copy(alpha = 0.15f),
                spotColor = Color(0xFF00E676).copy(alpha = 0.15f)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF091F14), Color(0xFF06140E))
                    )
                )
                .padding(22.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(0.7f)) {
                Text(
                    text = "VAULTS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E676),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Metti da parte per i tuoi sogni",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configura obiettivi automatici di risparmio, controlla i tuoi report ed ottieni badge esclusivi.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
            }
            Text(
                text = "💎",
                fontSize = 58.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 10.dp)
            )
        }
    }
}

@Composable
fun VaultsTabContent(
    goals: List<SavingsGoal>,
    currency: String,
    stats: ProgressStats,
    onNavigateToDetail: (Int) -> Unit,
    onCreateClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Hero Card
        item {
            HeroSavingsCard()
        }

        // 2. Statistics Panel
        item {
            StatsGridPanel(stats, currency)
        }

        // 3. Goals Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "I tuoi obiettivi",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                if (goals.isNotEmpty()) {
                    Text(
                        text = "${goals.size} attivi",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 4. Goals Empty State or Cards list
        if (goals.isEmpty()) {
            item {
                BrandCard(borderGlow = false) {
                    BrandEmptyState(
                        title = "Nessun obiettivo",
                        subtitle = "Crea il tuo primo obiettivo di risparmio.",
                        buttonText = "Crea Goal",
                        onButtonClick = onCreateClick,
                        icon = Icons.Default.Stars
                    )
                }
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                var itemVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    itemVisible = true
                }
                AnimatedVisibility(
                    visible = itemVisible,
                    enter = fadeIn(animationSpec = spring(stiffness = 300f)) +
                            scaleIn(initialScale = 0.9f) +
                            expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SavingsGoalEngineCard(
                        goal = goal,
                        currency = currency,
                        onClick = { onNavigateToDetail(goal.id) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun StatsGridPanel(stats: ProgressStats, currency: String) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceElevation),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "PANORAMICA PROGRESSI",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E676),
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Risparmio Totale", fontSize = 11.sp, color = TextSecondary)
                    Text("%,.2f %s".format(Locale.getDefault(), stats.totalSavingsAmount, currency), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Completamento", fontSize = 11.sp, color = TextSecondary)
                    Text("${stats.overallCompletionPercentage}%", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF00E676))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Attivi / Completati", fontSize = 11.sp, color = TextSecondary)
                    Text("${stats.activeGoalsCount} / ${stats.completedGoalsCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Media Mensile (Est.)", fontSize = 11.sp, color = TextSecondary)
                    Text("%,.1f %s".format(Locale.getDefault(), stats.monthlyAverageSavings, currency), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SavingsGoalEngineCard(
    goal: SavingsGoal,
    currency: String,
    onClick: () -> Unit
) {
    val progress = (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    val progressPercentage = (progress * 100).toInt()
    val missing = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)

    val now = System.currentTimeMillis()
    val daysRemaining = if (goal.targetDateMillis != null) {
        val diff = goal.targetDateMillis - now
        (diff / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
    } else null

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 100f)
        )
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier
            .fillMaxWidth()
            .scaleClickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.04f))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = goal.emoji, fontSize = 20.sp)
                    }

                    Column {
                        Text(
                            text = goal.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (daysRemaining != null) {
                            Text(
                                text = "$daysRemaining giorni rimanenti",
                                fontSize = 11.sp,
                                color = if (daysRemaining < 7) ExpenseRed else TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "Nessuna scadenza",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(Color(0xFF00E676).copy(alpha = 0.12f))
                        .border(BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.25f)), RoundedCornerShape(100))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$progressPercentage%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E676)
                    )
                }
            }

            if (goal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = goal.description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Animated Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(100))
                    .background(Color.White.copy(alpha = 0.03f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress.value)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(100))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF00E676).copy(alpha = 0.7f), Color(0xFF00E676))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Accumulato", fontSize = 10.sp, color = TextSecondary)
                    Text("%,.2f %s".format(Locale.getDefault(), goal.currentAmount, currency), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Target", fontSize = 10.sp, color = TextSecondary)
                    Text("%,.2f %s".format(Locale.getDefault(), goal.targetAmount, currency), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                }
            }

            if (missing > 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Mancano: %,.2f %s".format(Locale.getDefault(), missing, currency),
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Stima completamento: ${if (goal.currentAmount > 0) "Sulla buona strada" else "Da iniziare"}",
                        fontSize = 11.sp,
                        color = Color(0xFFFFC857),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ReportTabContent(report: WeeklyReport, currency: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevation),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("📊", fontSize = 24.sp)
                        Column {
                            Text("Weekly Report", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("Riepilogo delle performance degli ultimi 7 giorni", fontSize = 12.sp, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Saldo Finanziario
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Flusso di Cassa Netto", fontSize = 12.sp, color = TextSecondary)
                            Text(
                                text = "${if (report.balanceThisWeek >= 0) "+" else ""}%,.2f %s".format(Locale.getDefault(), report.balanceThisWeek, currency),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = if (report.balanceThisWeek >= 0) Color(0xFF00E676) else ExpenseRed
                            )
                        }

                        // Trend comparison tag
                        val trendColor = if (report.trendPercentage >= 0) Color(0xFF00E676) else ExpenseRed
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
                                .background(trendColor.copy(alpha = 0.1f))
                                .border(1.dp, trendColor.copy(alpha = 0.25f), RoundedCornerShape(100))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "${if (report.trendPercentage >= 0) "▲ +" else "▼ "}%,.1f%% vs scorsa sett.".format(Locale.getDefault(), report.trendPercentage),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = trendColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(18.dp))

                    // Metrics Flow Row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF00E676), CircleShape))
                                Text("Entrate", fontSize = 11.sp, color = TextSecondary)
                            }
                            Text("%,.2f %s".format(Locale.getDefault(), report.incomeThisWeek, currency), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(ExpenseRed, CircleShape))
                                Text("Uscite", fontSize = 11.sp, color = TextSecondary)
                            }
                            Text("%,.2f %s".format(Locale.getDefault(), report.expenseThisWeek, currency), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // Categorie Insights
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("INSIGHT CATEGORIE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFC857), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryReportItem(
                            modifier = Modifier.weight(1f),
                            title = "Principale spesa",
                            category = report.topCategory,
                            icon = "🔴"
                        )
                        CategoryReportItem(
                            modifier = Modifier.weight(1f),
                            title = "Peggior categoria",
                            category = report.worstCategory,
                            icon = "⚠️"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryReportItem(
                            modifier = Modifier.weight(1f),
                            title = "Miglior risorsa",
                            category = report.bestCategory,
                            icon = "🟢"
                        )
                        CategoryReportItem(
                            modifier = Modifier.weight(1f),
                            title = "Obiettivi avanzati",
                            category = "${report.advancedGoalsCount} Goal",
                            icon = "🎯"
                        )
                    }
                }
            }
        }

        // Unlocked Achievements this week
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("TROFEI SBLOCCATI QUESTA SETTIMANA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676), letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(14.dp))

                    if (report.unlockedAchievementsCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFC857).copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🏆", fontSize = 20.sp)
                            }
                            Column {
                                Text(
                                    text = "Grande Costanza!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Hai sbloccato ${report.unlockedAchievementsCount} nuovi trofei negli ultimi 7 giorni.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Nessun trofeo sbloccato questa settimana. Continua a risparmiare per ottenerne di nuovi!",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun CategoryReportItem(modifier: Modifier = Modifier, title: String, category: String, icon: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 11.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(icon, fontSize = 14.sp)
                Text(
                    text = category,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AchievementsTabContent(achievements: List<Achievement>, badgeLevel: String) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Current User Badge Card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFC857).copy(alpha = 0.35f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = false,
                        ambientColor = Color(0xFFFFC857).copy(alpha = 0.2f),
                        spotColor = Color(0xFFFFC857).copy(alpha = 0.2f)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(SurfaceWhite, Color(0xFF1B160C))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Giant Badge Display
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFC857).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFFFC857).copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    badgeLevel.contains("Diamond") -> "💎"
                                    badgeLevel.contains("Platinum") -> "❄️"
                                    badgeLevel.contains("Gold") -> "🥇"
                                    badgeLevel.contains("Silver") -> "🥈"
                                    else -> "🥉"
                                },
                                fontSize = 34.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("LIVELLO TROFEI", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFC857), letterSpacing = 1.sp)
                            Text(
                                text = "Grado $badgeLevel",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Continua a completare obiettivi e a tracciare le tue finanze per salire di grado.",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Tutti i Trofei",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        items(achievements) { ach ->
            AchievementItemRow(ach)
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun AchievementItemRow(achievement: Achievement) {
    val alpha = if (achievement.isUnlocked) 1f else 0.45f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) SurfaceWhite else SurfaceWhite.copy(alpha = 0.02f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (achievement.isUnlocked) Color(0xFF00E676).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f)
        ),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) Color(0xFF00E676).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.02f)
                    )
                    .border(
                        width = 1.dp,
                        color = if (achievement.isUnlocked) Color(0xFF00E676).copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = achievement.icon,
                    fontSize = 20.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = achievement.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (achievement.isUnlocked) Color.White else Color.White.copy(alpha = 0.7f)
                    )
                    if (achievement.isUnlocked) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Sbloccato",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Text(
                    text = achievement.description,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 15.sp
                )
                if (achievement.isUnlocked && achievement.unlockDateMillis != null) {
                    val dateStr = remember(achievement.unlockDateMillis) {
                        SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).format(Date(achievement.unlockDateMillis))
                    }
                    Text(
                        text = "Sbloccato il $dateStr",
                        fontSize = 10.sp,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineTabContent(events: List<TimelineEvent>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        item {
            Text(
                text = "Cronologia Finanziaria",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp, top = 4.dp)
            )
        }

        if (events.isEmpty()) {
            item {
                Text(
                    text = "Nessun evento registrato nella cronologia. Crea obiettivi o registra transazioni per iniziare a popolare il tuo diario finanziario.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp, start = 12.dp, end = 12.dp),
                    lineHeight = 18.sp
                )
            }
        } else {
            items(events) { event ->
                TimelineItemRow(event)
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun TimelineItemRow(event: TimelineEvent) {
    val dateStr = remember(event.dateMillis) {
        SimpleDateFormat("dd MMM yyyy", Locale.ITALIAN).format(Date(event.dateMillis))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline vertical bar
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E676))
                    .border(3.dp, Color(0xFF090B10), CircleShape)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.5.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )
        }

        // Timeline item card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp, start = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.03f))
                        .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(event.icon, fontSize = 16.sp)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = event.description,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                    Text(
                        text = dateStr,
                        fontSize = 9.sp,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, targetAmount: Double, initialAmount: Double, emoji: String, targetDate: Long?, desc: String, color: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("") }
    var initialStr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#00E676") }
    var selectedEmoji by remember { mutableStateOf("✈️") }
    var dateMillis by remember { mutableStateOf<Long?>(null) }

    val emojis = listOf("✈️", "🏠", "🚗", "💻", "🎓", "🎁", "💰", "🌴", "🚲", "🧸")
    val colors = listOf("#00E676", "#3B82F6", "#8B5CF6", "#EF4444", "#F59E0B")

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val target = targetStr.toDoubleOrNull() ?: 0.0
                    val initial = initialStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && target > 0) {
                        onConfirm(name, target, initial, selectedEmoji, dateMillis, description, selectedColor, notes)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color(0xFF090B10)
                ),
                shape = RoundedCornerShape(100),
                enabled = name.isNotBlank() && (targetStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Crea", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = TextSecondary)
            }
        },
        title = {
            Text(
                text = "Nuovo Obiettivo",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Emoji row selector
                item {
                    Column {
                        Text(
                            text = "Icona / Emoji",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            emojis.forEach { emoji ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (selectedEmoji == emoji) Color(0xFF00E676).copy(alpha = 0.15f) else Color.Transparent)
                                        .border(
                                            width = 1.dp,
                                            color = if (selectedEmoji == emoji) Color(0xFF00E676) else Color.White.copy(alpha = 0.1f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedEmoji = emoji },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = emoji, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }

                // Name field
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome obiettivo") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676),
                            focusedLabelColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Description field
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrizione breve") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676),
                            focusedLabelColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Target Amount field
                item {
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it },
                        label = { Text("Importo target ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676),
                            focusedLabelColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Initial Amount field
                item {
                    OutlinedTextField(
                        value = initialStr,
                        onValueChange = { initialStr = it },
                        label = { Text("Versamento iniziale ($currency) - opzionale") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676),
                            focusedLabelColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Notes field
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Note personali") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676),
                            focusedLabelColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Color Selection Row
                item {
                    Column {
                        Text(
                            text = "Colore obiettivo",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            colors.forEach { hex ->
                                val rgb = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedColor == hex
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(rgb)
                                        .border(
                                            width = 2.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = hex }
                                )
                            }
                        }
                    }
                }

                // Quick target date selector
                item {
                    Column {
                        Text(
                            text = "Scadenza desiderata",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val intervals = listOf(
                                Pair("3 Mesi", 3),
                                Pair("6 Mesi", 6),
                                Pair("1 Anno", 12)
                            )
                            intervals.forEach { (label, months) ->
                                val targetTime = remember {
                                    val cal = Calendar.getInstance()
                                    cal.add(Calendar.MONTH, months)
                                    cal.timeInMillis
                                }
                                val isSelected = dateMillis == targetTime
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(100))
                                        .background(if (isSelected) Color(0xFF00E676).copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(100)
                                        )
                                        .clickable {
                                            dateMillis = if (isSelected) null else targetTime
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF00E676) else Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = SurfaceWhite,
        shape = RoundedCornerShape(24.dp)
    )
}
