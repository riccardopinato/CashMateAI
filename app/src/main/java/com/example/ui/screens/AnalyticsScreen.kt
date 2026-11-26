package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.AccountViewModel
import com.example.domain.model.Account
import com.example.domain.model.Budget
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val budgets by viewModel.budgets.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val accountViewModel: AccountViewModel = viewModel()

    var selectedInterval by remember { mutableStateOf("30G") } // 7G, 30G, 6M, 1A
    var selectedAccountFilter by remember { mutableStateOf<Account?>(null) }
    var showAddTxDialog by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }

    // Start-up fade-in animation trigger
    var startAnimations by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimations = true
    }

    // 1. Filter transactions by interval
    val filteredTransactions = remember(transactions, selectedInterval, selectedAccountFilter) {
        val now = System.currentTimeMillis()
        val limitMillis = when (selectedInterval) {
            "7G" -> now - 7L * 24 * 60 * 60 * 1000
            "30G" -> now - 30L * 24 * 60 * 60 * 1000
            "6M" -> now - 180L * 24 * 60 * 60 * 1000
            "1A" -> now - 365L * 24 * 60 * 60 * 1000
            else -> 0L
        }
        transactions.filter { 
            it.dateMillis >= limitMillis && (selectedAccountFilter == null || it.accountId == selectedAccountFilter!!.id)
        }
    }

    val expenseTransactions = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE }
    }

    val totalExpense = remember(expenseTransactions) {
        expenseTransactions.sumOf { it.amount }
    }

    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    val categoryExpenses = remember(expenseTransactions) {
        expenseTransactions
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }

    // 2. Daily Average based on interval selection
    val daysInPeriod = remember(selectedInterval) {
        when (selectedInterval) {
            "7G" -> 7
            "30G" -> 30
            "6M" -> 180
            "1A" -> 365
            else -> 30
        }
    }
    val dailyAverage = remember(totalExpense, daysInPeriod) {
        if (daysInPeriod == 0) 0.0 else totalExpense / daysInPeriod
    }

    // 3. Top category
    val topCategoryName = remember(categoryExpenses) {
        categoryExpenses.maxByOrNull { it.value }?.key ?: "Nessuna"
    }
    val topCategoryAmount = remember(categoryExpenses, topCategoryName) {
        categoryExpenses[topCategoryName] ?: 0.0
    }
    val topCategoryPercentage = remember(totalExpense, topCategoryAmount) {
        if (totalExpense <= 0.0) 0.0 else (topCategoryAmount / totalExpense) * 100.0
    }

    // 4. Comparison percentage with previous equal period
    val comparisonPercentage = remember(transactions, selectedInterval) {
        val now = System.currentTimeMillis()
        val duration = when (selectedInterval) {
            "7G" -> 7L * 24 * 60 * 60 * 1000
            "30G" -> 30L * 24 * 60 * 60 * 1000
            "6M" -> 180L * 24 * 60 * 60 * 1000
            "1A" -> 365L * 24 * 60 * 60 * 1000
            else -> 30L * 24 * 60 * 60 * 1000
        }
        val currentStart = now - duration
        val previousStart = now - (2 * duration)

        val currentExpense = transactions.filter { it.type == TransactionType.EXPENSE && it.dateMillis >= currentStart }.sumOf { it.amount }
        val previousExpense = transactions.filter { it.type == TransactionType.EXPENSE && it.dateMillis in previousStart until currentStart }.sumOf { it.amount }

        if (previousExpense == 0.0) {
            if (currentExpense > 0.0) 100.0 else 0.0
        } else {
            ((currentExpense - previousExpense) / previousExpense) * 100.0
        }
    }

    // 5. Cumulative sparkline data (7 points)
    val sparklinePoints = remember(expenseTransactions, selectedInterval) {
        if (expenseTransactions.isEmpty()) {
            listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        } else {
            val sorted = expenseTransactions.sortedBy { it.dateMillis }
            var sum = 0.0
            val cumulative = sorted.map {
                sum += it.amount
                sum
            }
            if (cumulative.size < 7) {
                val list = cumulative.toMutableList()
                while (list.size < 7) {
                    list.add(0, 0.0)
                }
                list
            } else {
                val step = cumulative.size / 7.0
                List(7) { idx ->
                    val targetIdx = (idx * step).toInt().coerceIn(0, cumulative.lastIndex)
                    cumulative[targetIdx]
                }
            }
        }
    }

    // 6. Non-cumulative absolute Trend Points (7 chunks)
    val trendPoints = remember(expenseTransactions, selectedInterval) {
        val segmentCount = 7
        val duration = when (selectedInterval) {
            "7G" -> 7L * 24 * 60 * 60 * 1000
            "30G" -> 30L * 24 * 60 * 60 * 1000
            "6M" -> 180L * 24 * 60 * 60 * 1000
            "1A" -> 365L * 24 * 60 * 60 * 1000
            else -> 30L * 24 * 60 * 60 * 1000
        }
        val startTime = System.currentTimeMillis() - duration
        val segmentDuration = duration / segmentCount

        val points = DoubleArray(segmentCount) { 0.0 }
        expenseTransactions.forEach { tx ->
            val offset = tx.dateMillis - startTime
            if (offset in 0 until duration) {
                val segmentIdx = (offset / segmentDuration).toInt().coerceIn(0, segmentCount - 1)
                points[segmentIdx] += tx.amount
            }
        }
        points.toList()
    }

    // 7. Dynamic local AI insights
    val aiInsights = remember(expenseTransactions, budgets, comparisonPercentage, topCategoryName, topCategoryPercentage) {
        val list = mutableListOf<Triple<ImageVector, String, String>>()

        if (expenseTransactions.isNotEmpty()) {
            list.add(
                Triple(
                    Icons.Default.TrendingUp,
                    "Impatto Categoria",
                    "L'alimentazione e la categoria $topCategoryName rappresentano il ${String.format(Locale.getDefault(), "%.1f", topCategoryPercentage)}% delle tue spese complessive in questo periodo."
                )
            )
        } else {
            list.add(
                Triple(
                    Icons.Default.Info,
                    "Monitoraggio Attivo",
                    "Nessuna spesa rilevata per questo periodo. Aggiungi transazioni per sbloccare l'analisi automatica."
                )
            )
        }

        val pctAbs = Math.abs(comparisonPercentage)
        if (comparisonPercentage > 0) {
            list.add(
                Triple(
                    Icons.Default.TrendingUp,
                    "Uscite in Aumento",
                    "Le tue uscite totali sono aumentate del ${String.format(Locale.getDefault(), "%.1f", pctAbs)}% rispetto al periodo precedente. Monitora i tuoi limiti!"
                )
            )
        } else if (comparisonPercentage < 0) {
            list.add(
                Triple(
                    Icons.Default.TrendingDown,
                    "Risparmio Rilevato",
                    "Ottimo! Hai ridotto le tue spese del ${String.format(Locale.getDefault(), "%.1f", pctAbs)}% rispetto al periodo scorso. Continua così!"
                )
            )
        } else {
            list.add(
                Triple(
                    Icons.Default.TrendingFlat,
                    "Uscite Stabili",
                    "Il tuo livello di spesa è rimasto costante rispetto all'ultimo intervallo di tempo analizzato."
                )
            )
        }

        val exceededBudgets = budgets.filter { (categoryExpenses[it.category] ?: 0.0) > it.limitAmount }
        if (exceededBudgets.isNotEmpty()) {
            list.add(
                Triple(
                    Icons.Default.PriorityHigh,
                    "Soglia Budget Superata",
                    "Attenzione! Hai superato il limite impostato in ${exceededBudgets.size} categorie, in particolare per ${exceededBudgets.first().category}."
                )
            )
        } else if (budgets.isNotEmpty()) {
            list.add(
                Triple(
                    Icons.Default.CheckCircle,
                    "Budget Sotto Controllo",
                    "Sei completamente in linea con tutti i limiti di budget impostati. Eccellente gestione finanziaria!"
                )
            )
        } else {
            list.add(
                Triple(
                    Icons.Default.AddChart,
                    "Pianifica con i Budget",
                    "Imposta dei limiti di spesa per le tue categorie più frequenti per tenere sotto controllo le uscite mensili."
                )
            )
        }
        list
    }

    // 8. Quick Insights calculations
    val averageDaysBetween = remember(expenseTransactions) {
        if (expenseTransactions.size < 2) "N/D"
        else {
            val dates = expenseTransactions.map { it.dateMillis }.sorted()
            val diff = dates.last() - dates.first()
            val days = diff.toDouble() / (24 * 60 * 60 * 1000)
            val avg = days / (expenseTransactions.size - 1)
            if (avg < 1.0) "Più volte al giorno" else String.format(Locale.getDefault(), "%,.1f gg", avg)
        }
    }

    val mostExpensiveDay = remember(expenseTransactions) {
        if (expenseTransactions.isEmpty()) "Nessuno"
        else {
            val daysOfWeek = listOf("Domenica", "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato")
            val expensesByDay = expenseTransactions.groupBy {
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.dateMillis
                cal.get(Calendar.DAY_OF_WEEK)
            }.mapValues { it.value.sumOf { tx -> tx.amount } }

            val maxDayIndex = expensesByDay.maxByOrNull { it.value }?.key
            if (maxDayIndex != null) daysOfWeek[maxDayIndex - 1] else "N/D"
        }
    }

    val mostFrequentCategory = remember(expenseTransactions) {
        if (expenseTransactions.isEmpty()) "Nessuna"
        else {
            val frequencies = expenseTransactions.groupBy { it.category }.mapValues { it.value.size }
            val maxCat = frequencies.maxByOrNull { it.value }?.key
            if (maxCat != null) {
                val emoji = getCategoryEmoji(maxCat)
                "$emoji $maxCat"
            } else "N/D"
        }
    }

    Scaffold(
        containerColor = BackgroundWarm,
        topBar = {
            TopAppBar(
                title = { Text("Analisi & Statistiche", fontWeight = FontWeight.Black, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWarm)
            )
        }
    ) { innerPadding ->
        // Global empty state if there are zero transactions anywhere
        if (transactions.isEmpty()) {
            EmptyStateLayout(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                onAddTransactionClick = { showAddTxDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 0. ACCOUNT FILTER ROW
                item {
                    val accountsList by accountViewModel.accounts.collectAsStateWithLifecycle()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All Accounts Filter Chip
                        FilterChip(
                            selected = selectedAccountFilter == null,
                            onClick = { selectedAccountFilter = null },
                            label = { Text("Tutti i conti", fontWeight = FontWeight.Bold, color = if (selectedAccountFilter == null) Color.Black else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF00E676),
                                selectedLabelColor = Color.Black,
                                containerColor = SurfaceWhite,
                                labelColor = TextSecondary
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier.testTag("filter_all_accounts")
                        )

                        // Individual Account Filter Chips
                        accountsList.forEach { acc ->
                            val isSel = selectedAccountFilter?.id == acc.id
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedAccountFilter = acc },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(acc.icona, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = acc.nome,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.Black else Color.White
                                        )
                                    }
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(android.graphics.Color.parseColor(acc.colore)),
                                    selectedLabelColor = Color.Black,
                                    containerColor = SurfaceWhite,
                                    labelColor = TextSecondary
                                ),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                                modifier = Modifier.testTag("filter_account_${acc.id}")
                            )
                        }
                    }
                }

                // 1. HERO ANALYTICS CARD
                item {
                    AnimatedVisibility(
                        visible = startAnimations,
                        enter = fadeIn(animationSpec = tween(500)) + expandVertically(animationSpec = tween(500))
                    ) {
                        HeroAnalyticsCard(
                            selectedInterval = selectedInterval,
                            totalExpense = totalExpense,
                            currency = currency,
                            comparisonPercentage = comparisonPercentage,
                            sparklinePoints = sparklinePoints,
                            onIntervalSelected = { selectedInterval = it }
                        )
                    }
                }

                // 2. TWO SIDE-BY-SIDE KPI CARDS
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
                                KpiCard(
                                    title = "Top Categoria",
                                    value = if (topCategoryName == "Nessuna") "Nessuna" else "${getCategoryEmoji(topCategoryName)} $topCategoryName",
                                    desc = "${String.format(Locale.getDefault(), "%.1f", topCategoryPercentage)}% del totale",
                                    icon = Icons.Default.Fastfood,
                                    iconBgColor = EmeraldPrimary.copy(alpha = 0.12f),
                                    iconTintColor = EmeraldPrimary
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                KpiCard(
                                    title = "Media Giornaliera",
                                    value = String.format(Locale.getDefault(), "%,.1f %s", dailyAverage, currency),
                                    desc = "Calcolata su $daysInPeriod giorni",
                                    icon = Icons.Default.CalendarToday,
                                    iconBgColor = IncomeBlue.copy(alpha = 0.12f),
                                    iconTintColor = IncomeBlue
                                )
                            }
                        }
                    }
                }

                // 3. DONUT CHART
                item {
                    AnimatedVisibility(
                        visible = startAnimations,
                        enter = fadeIn(animationSpec = tween(700, delayMillis = 150))
                    ) {
                        DonutChartCard(
                            categoryExpenses = categoryExpenses,
                            totalExpense = totalExpense,
                            totalIncome = totalIncome,
                            currency = currency
                        )
                    }
                }

                // 4. TREND SPESE (LINE CHART)
                item {
                    AnimatedVisibility(
                        visible = startAnimations,
                        enter = fadeIn(animationSpec = tween(800, delayMillis = 200))
                    ) {
                        TrendSpeseCard(
                            trendPoints = trendPoints,
                            selectedInterval = selectedInterval,
                            currency = currency
                        )
                    }
                }

                // 7. TOP CATEGORIE (BAR CHART)
                if (categoryExpenses.isNotEmpty()) {
                    item {
                        AnimatedVisibility(
                            visible = startAnimations,
                            enter = fadeIn(animationSpec = tween(850, delayMillis = 220))
                        ) {
                            TopCategoriesCard(
                                categoryExpenses = categoryExpenses,
                                totalExpense = totalExpense,
                                currency = currency
                            )
                        }
                    }
                }

                // 8. QUICK INSIGHTS
                item {
                    AnimatedVisibility(
                        visible = startAnimations,
                        enter = fadeIn(animationSpec = tween(900, delayMillis = 240))
                    ) {
                        QuickInsightsGrid(
                            averageDaysBetween = averageDaysBetween,
                            mostExpensiveDay = mostExpensiveDay,
                            mostFrequentCategory = mostFrequentCategory
                        )
                    }
                }

                // 6. INSIGHTS AI
                item {
                    AnimatedVisibility(
                        visible = startAnimations,
                        enter = fadeIn(animationSpec = tween(950, delayMillis = 260))
                    ) {
                        InsightsAiCard(
                            insights = aiInsights
                        )
                    }
                }

                // 5. BUDGETS MANAGER
                item {
                    AnimatedVisibility(
                        visible = startAnimations,
                        enter = fadeIn(animationSpec = tween(1000, delayMillis = 280))
                    ) {
                        BudgetsHeader(
                            onAddBudgetClick = { showAddBudgetDialog = true }
                        )
                    }
                }

                if (budgets.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAddBudgetDialog = true }
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Nessun budget impostato.", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                                Text("Tocca qui per configurare un nuovo budget.", fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                } else {
                    items(budgets) { budget ->
                        val spent = categoryExpenses[budget.category] ?: 0.0
                        val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat() else 0f
                        BudgetProgressBarItem(
                            budget = budget,
                            spent = spent,
                            progress = progress,
                            currency = currency,
                            onDelete = { viewModel.deleteBudget(budget.category) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    // Interactive Dialogs for Quick Add
    if (showAddTxDialog) {
        QuickAddTransactionDialog(
            currency = currency,
            onDismiss = { showAddTxDialog = false },
            onConfirm = { amt, category, type, note ->
                viewModel.addTransaction(
                    amount = amt,
                    type = type,
                    category = category,
                    dateMillis = System.currentTimeMillis(),
                    note = note,
                    paymentMethod = "Card"
                )
                showAddTxDialog = false
            }
        )
    }

    if (showAddBudgetDialog) {
        QuickAddBudgetDialog(
            currency = currency,
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { cat, amt ->
                viewModel.addBudget(category = cat, limitAmount = amt)
                showAddBudgetDialog = false
            }
        )
    }
}

// ==========================================
// DETAILED CUSTOM SUB-COMPOSABLES
// ==========================================

@Composable
fun HeroAnalyticsCard(
    selectedInterval: String,
    totalExpense: Double,
    currency: String,
    comparisonPercentage: Double,
    sparklinePoints: List<Double>,
    onIntervalSelected: (String) -> Unit
) {
    val context = LocalContext.current
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
                elevation = 16.dp,
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
                .padding(24.dp)
        ) {
            // Header row
            val titleText = when (selectedInterval) {
                "7G" -> "Ultimi 7 giorni"
                "30G" -> "Ultimi 30 giorni"
                "6M" -> "Ultimi 6 mesi"
                "1A" -> "Ultimo anno"
                else -> "Questo mese"
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = titleText.uppercase(Locale.getDefault()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.3.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(EmeraldPrimary.copy(alpha = 0.12f))
                        .border(BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.2f)), RoundedCornerShape(100))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Uscite",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Total spent
            Text(
                text = String.format(Locale.getDefault(), "%,.2f %s", totalExpense, currency),
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Comparison Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val pctAbs = Math.abs(comparisonPercentage)
                val isIncrease = comparisonPercentage > 0
                val arrowIcon = if (isIncrease) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
                val color = if (isIncrease) ExpenseRed else EmeraldPrimary
                val label = if (isIncrease) "in più" else "in meno"

                Icon(
                    imageVector = arrowIcon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = String.format(Locale.getDefault(), "%.1f%%", pctAbs),
                    color = color,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$label rispetto al periodo precedente",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Premium animated Sparkline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                // Opening draw progress
                val animatedProgress = remember { Animatable(0f) }
                LaunchedEffect(selectedInterval) {
                    animatedProgress.snapTo(0f)
                    animatedProgress.animateTo(
                        1f,
                        animationSpec = tween(1200, easing = FastOutSlowInEasing)
                    )
                }

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    val maxPoint = sparklinePoints.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                    val minPoint = sparklinePoints.minOrNull() ?: 0.0
                    val range = (maxPoint - minPoint).coerceAtLeast(1.0)

                    val points = sparklinePoints.mapIndexed { idx, p ->
                        val x = (idx.toFloat() / (sparklinePoints.size - 1)) * width
                        val ratio = ((p - minPoint) / range).toFloat()
                        val y = height - (ratio * height * 0.75f) - (height * 0.12f)
                        Offset(x, y)
                    }

                    if (points.isNotEmpty()) {
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

                        // Sparkline neon glowing path
                        drawPath(
                            path = path,
                            color = EmeraldPrimary,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                            alpha = animatedProgress.value
                        )

                        // Gradient fill under the curve
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(EmeraldPrimary.copy(alpha = 0.2f), Color.Transparent)
                            ),
                            alpha = animatedProgress.value
                        )

                        // Glowing terminal point
                        drawCircle(
                            color = EmeraldPrimary,
                            radius = 4.5.dp.toPx(),
                            center = points.last(),
                            alpha = animatedProgress.value
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Selector tabs: 7G, 30G, 6M, 1A
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(100))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(100))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val intervals = listOf("7G", "30G", "6M", "1A")
                intervals.forEach { interval ->
                    val isSel = selectedInterval == interval
                    val textLabel = when (interval) {
                        "7G" -> "7G"
                        "30G" -> "30G"
                        "6M" -> "6M"
                        "1A" -> "1A"
                        else -> interval
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(100))
                            .background(if (isSel) EmeraldPrimary else Color.Transparent)
                            .clickable { onIntervalSelected(interval) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = textLabel,
                            color = if (isSel) Color.Black else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    desc: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTintColor: Color
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Icon + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTintColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Value
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle description
            Text(
                text = desc,
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DonutChartCard(
    categoryExpenses: Map<String, Double>,
    totalExpense: Double,
    totalIncome: Double,
    currency: String
) {
    val totalExpenseFloat = totalExpense.toFloat()
    val incomePercent = remember(totalExpense, totalIncome) {
        if (totalIncome <= 0.0) 0f else ((totalExpense / totalIncome) * 100).toFloat()
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DISTRIBUZIONE CATEGORIE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Donut slice construction
            val slices = remember(categoryExpenses, totalExpenseFloat) {
                if (totalExpenseFloat <= 0f) {
                    listOf(DonutSlice("Nessuna spesa", 1.0f, Color.White.copy(alpha = 0.15f)))
                } else {
                    categoryExpenses.map { (cat, amount) ->
                        val pct = (amount.toFloat() / totalExpenseFloat)
                        val (_, color) = getCategoryMeta(cat, false)
                        DonutSlice(cat, pct, color)
                    }.sortedByDescending { it.percentage }
                }
            }

            // Spring Opening Sweep Angle Animation
            val animProgress = remember { Animatable(0f) }
            LaunchedEffect(categoryExpenses, totalExpense) {
                animProgress.snapTo(0f)
                animProgress.animateTo(
                    1f,
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = 120f)
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(190.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dStr = 22.dp.toPx()
                    var startAngle = -90f
                    slices.forEach { slice ->
                        val sweepAngle = slice.percentage * 360f * animProgress.value
                        drawArc(
                            color = slice.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(dStr / 2, dStr / 2),
                            size = Size(size.width - dStr, size.height - dStr),
                            style = Stroke(width = dStr, cap = StrokeCap.Round)
                        )
                        startAngle += sweepAngle
                    }
                }

                // Center text info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "TOTALE SPESO",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%,.0f %s", totalExpense, currency),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    if (totalIncome > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format(Locale.getDefault(), "%.0f%% delle entrate", incomePercent),
                            fontSize = 11.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Premium colorful legend
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (slice in slices) {
                    val amt = categoryExpenses[slice.category] ?: 0.0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.02f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(slice.color)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "${getCategoryEmoji(slice.category)} ${slice.category}",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = String.format(Locale.getDefault(), "%,.2f %s (%.0f%%)", amt, currency, slice.percentage * 100),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrendSpeseCard(
    trendPoints: List<Double>,
    selectedInterval: String,
    currency: String
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "ANDAMENTO SPESE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Progression drawing animation
            val pathAnim = remember { Animatable(0f) }
            LaunchedEffect(trendPoints, selectedInterval) {
                pathAnim.snapTo(0f)
                pathAnim.animateTo(1f, animationSpec = tween(1100, easing = LinearOutSlowInEasing))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    val maxVal = trendPoints.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                    val minVal = 0.0
                    val range = maxVal - minVal

                    // Draw subtle coordinate gridlines
                    val gridLines = 4
                    for (i in 0 until gridLines) {
                        val yCoord = (height / (gridLines - 1)) * i
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(0f, yCoord),
                            end = Offset(width, yCoord),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val points = trendPoints.mapIndexed { idx, p ->
                        val x = (idx.toFloat() / (trendPoints.size - 1)) * width
                        val ratio = ((p - minVal) / range).toFloat()
                        val y = height - (ratio * height * 0.72f) - (height * 0.14f)
                        Offset(x, y)
                    }

                    if (points.isNotEmpty()) {
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

                        // Premium neon gradient path drawing
                        drawPath(
                            path = path,
                            brush = Brush.horizontalGradient(
                                colors = listOf(IncomeBlue, EmeraldPrimary, WarningYellow)
                            ),
                            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                            alpha = pathAnim.value
                        )

                        // Smooth area glow under line
                        val fillPath = Path().apply {
                            addPath(path)
                            lineTo(width, height)
                            lineTo(0f, height)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(EmeraldPrimary.copy(alpha = 0.12f), Color.Transparent)
                            ),
                            alpha = pathAnim.value
                        )

                        // Dot nodes on data points
                        points.forEach { pt ->
                            drawCircle(
                                color = Color.White,
                                radius = 2.dp.toPx(),
                                center = pt,
                                alpha = pathAnim.value
                            )
                            drawCircle(
                                color = EmeraldPrimary,
                                radius = 4.dp.toPx(),
                                center = pt,
                                style = Stroke(width = 1.5.dp.toPx()),
                                alpha = pathAnim.value
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtitle interval explanation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inizio Periodo",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Confronto temporale lineare (7 checkpoint)",
                    fontSize = 11.sp,
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Oggi",
                    fontSize = 11.sp,
                    color = EmeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TopCategoriesCard(
    categoryExpenses: Map<String, Double>,
    totalExpense: Double,
    currency: String
) {
    val topThree = remember(categoryExpenses) {
        categoryExpenses.toList()
            .sortedByDescending { it.second }
            .take(3)
    }

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "TOP CATEGORIE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                topThree.forEach { (cat, amt) ->
                    val percentage = if (totalExpense <= 0.0) 0f else (amt / totalExpense).toFloat()
                    val (_, color) = getCategoryMeta(cat, false)

                    // Progress bar drawing
                    val barWidthAnim = remember { Animatable(0f) }
                    LaunchedEffect(amt) {
                        barWidthAnim.snapTo(0f)
                        barWidthAnim.animateTo(
                            percentage,
                            animationSpec = spring(dampingRatio = 0.8f, stiffness = 110f)
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = getCategoryEmoji(cat),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat,
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%,.1f %s (%.0f%%)", amt, currency, percentage * 100),
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom colored horizontal bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(100))
                                .background(Color.White.copy(alpha = 0.04f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(barWidthAnim.value)
                                    .clip(RoundedCornerShape(100))
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(color.copy(alpha = 0.8f), color)
                                        )
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickInsightsGrid(
    averageDaysBetween: String,
    mostExpensiveDay: String,
    mostFrequentCategory: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "QUICK INSIGHTS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Box 1
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.HourglassEmpty, contentDescription = null, tint = WarningYellow, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Freq. Acquisti", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(averageDaysBetween, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Black)
                }
            }

            // Box 2
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Picco Spesa", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(mostExpensiveDay, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Black)
                }
            }

            // Box 3
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = IncomeBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Canale Freq.", fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(mostFrequentCategory, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun InsightsAiCard(
    insights: List<Triple<ImageVector, String, String>>
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "INSIGHTS AI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                insights.forEach { (icon, title, desc) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = desc,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetsHeader(
    onAddBudgetClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "LIMITI DI BUDGET",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 1.2.sp
        )

        Button(
            onClick = onAddBudgetClick,
            shape = RoundedCornerShape(100),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary.copy(alpha = 0.12f),
                contentColor = EmeraldPrimary
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Imposta", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BudgetProgressBarItem(
    budget: Budget,
    spent: Double,
    progress: Float,
    currency: String,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("budget_manager_row_${budget.category}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, tint) = getCategoryMeta(budget.category, false)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(tint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${getCategoryEmoji(budget.category)} ${budget.category}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        val limitPercent = progress * 100
                        Text(
                            text = "${String.format(Locale.getDefault(), "%.1f", limitPercent)}% del limite",
                            fontSize = 11.sp,
                            color = if (progress > 1f) ExpenseRed else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(Locale.getDefault(), "%,.0f / %,.0f %s", spent, budget.limitAmount, currency),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (progress > 1f) ExpenseRed else Color.White
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Elimina",
                            tint = ExpenseRed.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Premium animated progress bar
            val animatedProgress = remember { Animatable(0f) }
            LaunchedEffect(progress) {
                animatedProgress.snapTo(0f)
                animatedProgress.animateTo(
                    progress.coerceIn(0f, 1.2f),
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 120f)
                )
            }

            val barColor = when {
                progress > 1f -> ExpenseRed
                progress > 0.8f -> WarningYellow
                else -> EmeraldPrimary
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(100))
                    .background(Color.White.copy(alpha = 0.04f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.value.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(100))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(barColor.copy(alpha = 0.8f), barColor)
                            )
                        )
                )
            }
        }
    }
}

@Composable
fun EmptyStateLayout(
    modifier: Modifier = Modifier,
    onAddTransactionClick: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High quality animated illustration on Canvas
        Box(
            modifier = Modifier.size(180.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                // Floating background circle
                drawCircle(
                    color = EmeraldPrimary.copy(alpha = 0.06f),
                    radius = 80.dp.toPx()
                )
                drawCircle(
                    color = EmeraldPrimary.copy(alpha = 0.03f),
                    radius = 90.dp.toPx()
                )
            }
            Icon(
                imageVector = Icons.Default.PieChart,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(76.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "Nessun dato disponibile",
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Aggiungi qualche transazione per sbloccare l'analisi automatica ed i grafici finanziari interattivi.",
            color = TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onAddTransactionClick,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Aggiungi transazione", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
        }
    }
}

// ==========================================
// INTERACTIVE QUICK DIALOGS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTransactionDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, category: String, type: TransactionType, note: String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var category by remember { mutableStateOf("Food") }

    val categories = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Travel", "Salary", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevation),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Aggiungi Transazione",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                // Type Selectors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (type == TransactionType.EXPENSE) ExpenseRed.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { type = TransactionType.EXPENSE }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Uscita", fontWeight = FontWeight.Bold, color = if (type == TransactionType.EXPENSE) ExpenseRed else TextSecondary)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (type == TransactionType.INCOME) EmeraldPrimary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { type = TransactionType.INCOME }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Entrata", fontWeight = FontWeight.Bold, color = if (type == TransactionType.INCOME) EmeraldPrimary else TextSecondary)
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null || input == ".") {
                            amount = input
                        }
                    },
                    label = { Text("Importo ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Descrizione") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Category select spinner / chips list
                Text("Categoria", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
                                .background(if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                .border(BorderStroke(1.dp, if (isSelected) EmeraldPrimary else Color.White.copy(alpha = 0.05f)), RoundedCornerShape(100))
                                .clickable { category = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${getCategoryEmoji(cat)} $cat",
                                color = if (isSelected) EmeraldPrimary else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annulla", color = TextSecondary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull()
                            if (amt != null && amt > 0.0 && note.isNotBlank()) {
                                onConfirm(amt, category, type, note)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp),
                        enabled = amount.toDoubleOrNull() != null && note.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salva", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBudgetDialog(
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (category: String, amount: Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Food") }

    val categories = listOf("Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Travel", "Salary", "Other")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevation),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Imposta Limite Budget",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                // Category
                Text("Seleziona Categoria", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100))
                                .background(if (isSelected) EmeraldPrimary.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                .border(BorderStroke(1.dp, if (isSelected) EmeraldPrimary else Color.White.copy(alpha = 0.05f)), RoundedCornerShape(100))
                                .clickable { category = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${getCategoryEmoji(cat)} $cat",
                                color = if (isSelected) EmeraldPrimary else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Limit Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.toDoubleOrNull() != null || input == ".") {
                            amount = input
                        }
                    },
                    label = { Text("Importo Limite ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                        unfocusedTextColor = Color.White,
                        focusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annulla", color = TextSecondary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull()
                            if (amt != null && amt > 0.0) {
                                onConfirm(category, amt)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp),
                        enabled = amount.toDoubleOrNull() != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salva", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper to get category emojis
fun getCategoryEmoji(category: String): String {
    return when (category.lowercase(Locale.getDefault())) {
        "food" -> "🍔"
        "transport" -> "🚗"
        "shopping" -> "🛍️"
        "bills" -> "🧾"
        "entertainment" -> "🎮"
        "health" -> "🏥"
        "travel" -> "✈️"
        "salary" -> "💰"
        else -> "📦"
    }
}

data class DonutSlice(
    val category: String,
    val percentage: Float,
    val color: Color
)
