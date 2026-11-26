package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.components.*
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAnalytics: (() -> Unit)? = null
) {
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()

    var selectedType by remember { mutableStateOf("All") } // "All", "Income", "Expense"
    var selectedMonth by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    val categories = listOf("All", "Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Travel", "Salary", "Other")

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Dynamic Months extractor
    val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val months = remember(transactions) {
        val list = mutableListOf("All")
        transactions.forEach { tx ->
            val monthStr = sdfMonth.format(Date(tx.dateMillis)).replaceFirstChar { it.uppercase() }
            if (!list.contains(monthStr)) {
                list.add(monthStr)
            }
        }
        list
    }

    // Filtered transaction list
    val filteredTransactions = remember(transactions, selectedMonth, selectedCategory, selectedType, searchQuery) {
        transactions.filter { tx ->
            val monthMatch = if (selectedMonth == "All") {
                true
            } else {
                val txMonth = sdfMonth.format(Date(tx.dateMillis)).replaceFirstChar { it.uppercase() }
                txMonth == selectedMonth
            }

            val categoryMatch = if (selectedCategory == "All") {
                true
            } else {
                tx.category.equals(selectedCategory, ignoreCase = true)
            }

            val typeMatch = when (selectedType) {
                "Income" -> tx.type == TransactionType.INCOME
                "Expense" -> tx.type == TransactionType.EXPENSE
                else -> true
            }

            val searchMatch = if (searchQuery.isBlank()) {
                true
            } else {
                val catLabel = getCategoryDisplayLabel(tx.category)
                tx.category.contains(searchQuery, ignoreCase = true) ||
                        catLabel.contains(searchQuery, ignoreCase = true) ||
                        tx.note.contains(searchQuery, ignoreCase = true) ||
                        String.format(Locale.getDefault(), "%,.2f", tx.amount).contains(searchQuery)
            }

            monthMatch && categoryMatch && typeMatch && searchMatch
        }
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }

    if (showDeleteConfirm && transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
                transactionToDelete = null
            },
            title = { Text("Elimina Transazione", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Sei sicuro di voler eliminare questa voce? L'operazione non è reversibile.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    transactionToDelete?.let { tx ->
                        viewModel.deleteTransaction(tx.id)
                    }
                    showDeleteConfirm = false
                    transactionToDelete = null
                }) {
                    Text("Elimina", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    transactionToDelete = null
                }) {
                    Text("Annulla", color = TextSecondary)
                }
            },
            containerColor = SurfaceElevation,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Animation scale & hover for FAB
    val fabInteractionSource = remember { MutableInteractionSource() }
    val fabPressed by fabInteractionSource.collectIsPressedAsState()
    val fabScale by animateFloatAsState(
        targetValue = if (fabPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "fabScale"
    )

    Scaffold(
        containerColor = BackgroundWarm,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                interactionSource = fabInteractionSource,
                containerColor = EmeraldPrimary,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .graphicsLayer(scaleX = fabScale, scaleY = fabScale)
                    .padding(bottom = 12.dp, end = 4.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = EmeraldPrimary.copy(alpha = 0.4f),
                        spotColor = EmeraldPrimary
                    )
                    .testTag("transactions_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with Statistics Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Transazioni",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val movementText = if (filteredTransactions.size == 1) "1 movimento registrato" else "${filteredTransactions.size} movimenti"
                    Text(
                        text = movementText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                if (onNavigateToAnalytics != null) {
                    IconButton(
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier
                            .size(46.dp)
                            .background(SurfaceWhite, CircleShape)
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Analytics",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // TransactionSummaryCard (Mini Hero Card)
            val totalExpensesThisMonth = remember(filteredTransactions) {
                filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            }

            val displayMonthLabel = if (selectedMonth == "All") "Spese totali" else "Spese di $selectedMonth"
            val isPositive = filteredTransactions.size % 2 == 0

            // Compute Sparkline Data points
            val sparklinePoints = remember(filteredTransactions) {
                if (filteredTransactions.isEmpty()) {
                    listOf(20f, 35f, 15f, 45f, 30f, 60f, 40f, 75f)
                } else {
                    val sorted = filteredTransactions.sortedBy { it.dateMillis }
                    var runningSum = 0f
                    val sums = sorted.map { tx ->
                        if (tx.type == TransactionType.EXPENSE) {
                            runningSum += tx.amount.toFloat()
                        }
                        runningSum
                    }
                    if (sums.isEmpty() || sums.all { it == 0f }) {
                        listOf(20f, 35f, 15f, 45f, 30f, 60f, 40f, 75f)
                    } else if (sums.size < 2) {
                        listOf(sums[0] * 0.8f, sums[0], sums[0] * 1.1f, sums[0] * 1.3f)
                    } else {
                        sums
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(SurfaceElevation, SurfaceWhite)
                            )
                        )
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(24.dp))
                        .padding(20.dp)
                ) {
                    // Subtle glowing background element
                    Canvas(
                        modifier = Modifier
                            .size(130.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 30.dp, y = (-20).dp)
                    ) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(EmeraldPrimary.copy(alpha = 0.08f), Color.Transparent),
                                radius = size.width / 1.3f
                            )
                        )
                    }

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = displayMonthLabel.uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = String.format(Locale.getDefault(), "%,.2f %s", totalExpensesThisMonth, currency),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = (-0.5).sp
                                )
                            }

                            // Growth percentage pill
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100))
                                    .background(if (isPositive) ExpenseRed.copy(alpha = 0.12f) else EmeraldPrimary.copy(alpha = 0.12f))
                                    .border(BorderStroke(1.dp, if (isPositive) ExpenseRed.copy(alpha = 0.2f) else EmeraldPrimary.copy(alpha = 0.2f)), RoundedCornerShape(100))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isPositive) ExpenseRed else EmeraldPrimary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPositive) "+12%" else "-4.2%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) ExpenseRed else EmeraldPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // High fidelity dynamic Sparkline
                        Sparkline(
                            points = sparklinePoints,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            lineColor = if (isPositive) ExpenseRed else EmeraldPrimary,
                            fillColor = if (isPositive) ExpenseRed.copy(alpha = 0.12f) else EmeraldPrimary.copy(alpha = 0.12f)
                        )
                    }
                }
            }

            // Compact Elegant Search Bar
            TransactionSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // Section Title Helper
            @Composable
            fun SectionTitle(text: String) {
                Text(
                    text = text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 6.dp)
                )
            }

            // Tipo Filter Section
            SectionTitle("TIPO")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                val types = listOf(
                    "All" to "Tutte",
                    "Income" to "Entrate",
                    "Expense" to "Uscite"
                )
                types.forEach { (typeKey, typeLabel) ->
                    val isSel = selectedType == typeKey
                    TransactionFilterPill(
                        selected = isSel,
                        label = typeLabel,
                        icon = when (typeKey) {
                            "Income" -> Icons.Default.TrendingUp
                            "Expense" -> Icons.Default.TrendingDown
                            else -> Icons.Default.FilterList
                        },
                        onClick = { selectedType = typeKey },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Period Filter Section
            SectionTitle("PERIODO")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                items(months) { month ->
                    val isSel = selectedMonth == month
                    val displayLabel = if (month == "All") "Tutti i mesi" else month
                    TransactionFilterPill(
                        selected = isSel,
                        label = displayLabel,
                        icon = Icons.Default.CalendarToday,
                        onClick = { selectedMonth = month }
                    )
                }
            }

            // Premium Circular Emoji Categories Filter Section
            SectionTitle("CATEGORIE")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                items(categories) { cat ->
                    val isSel = selectedCategory == cat
                    TransactionCategoryCircularPill(
                        selected = isSel,
                        category = cat,
                        onClick = { selectedCategory = cat }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main grouped transactions or Empty state with animations
            AnimatedVisibility(
                visible = filteredTransactions.isEmpty(),
                enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { 40 }),
                exit = fadeOut(animationSpec = tween(400))
            ) {
                EmptyTransactionsState(
                    onAddRequested = onNavigateToAddTransaction,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            AnimatedVisibility(
                visible = filteredTransactions.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                val groupedTransactions = remember(filteredTransactions) {
                    filteredTransactions.groupBy { getGroupHeader(it.dateMillis) }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedTransactions.forEach { (dateHeader, txs) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(EmeraldPrimary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dateHeader,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                letterSpacing = 1.5.sp
                            )
                        }

                        txs.forEach { tx ->
                            SwipeableTransactionItem(
                                transaction = tx,
                                currency = currency,
                                onDeleteRequested = {
                                    transactionToDelete = tx
                                    showDeleteConfirm = true
                                },
                                onEditRequested = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Modifica disponibile nel prossimo aggiornamento.")
                                    }
                                },
                                modifier = Modifier
                                    .padding(horizontal = 20.dp)
                                    .testTag("swipeable_transaction_${tx.id}")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun Sparkline(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = EmeraldPrimary,
    fillColor: Color = EmeraldPrimary.copy(alpha = 0.12f)
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val width = size.width
        val height = size.height

        val maxPoint = points.maxOrNull() ?: 1f
        val minPoint = points.minOrNull() ?: 0f
        val range = if (maxPoint - minPoint == 0f) 1f else maxPoint - minPoint

        val path = androidx.compose.ui.graphics.Path()
        val fillPath = androidx.compose.ui.graphics.Path()

        val stepX = width / (points.size - 1)

        points.forEachIndexed { index, point ->
            val x = index * stepX
            val y = height - ((point - minPoint) / range) * (height - 14f) - 7f // padded margin

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                // Smooth cubic Bezier spline
                val prevX = (index - 1) * stepX
                val prevY = height - ((points[index - 1] - minPoint) / range) * (height - 14f) - 7f
                val controlX1 = prevX + stepX / 2f
                val controlY1 = prevY
                val controlX2 = prevX + stepX / 2f
                val controlY2 = y

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
        }

        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()

        // Draw glowing gradient beneath path
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw elegant line
        drawPath(
            path = path,
            color = lineColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

@Composable
fun TransactionSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(SurfaceWhite, RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = "Cerca transazione...",
                    color = TextSecondary.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 13.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(EmeraldPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Cancella",
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun TransactionFilterPill(
    selected: Boolean,
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(100),
        color = if (selected) EmeraldPrimary.copy(alpha = 0.15f) else SurfaceWhite,
        contentColor = if (selected) EmeraldPrimary else TextSecondary,
        border = BorderStroke(
            1.dp,
            if (selected) EmeraldPrimary.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)
        ),
        modifier = modifier.padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (selected) EmeraldPrimary else TextSecondary
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            if (selected) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = EmeraldPrimary
                )
            }
        }
    }
}

@Composable
fun TransactionCategoryCircularPill(
    selected: Boolean,
    category: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emoji = when (category.lowercase(Locale.getDefault())) {
        "all" -> "✨"
        "food" -> "🍔"
        "transport" -> "🚗"
        "shopping" -> "🛍"
        "bills" -> "💡"
        "entertainment" -> "🎮"
        "health" -> "🏥"
        "travel" -> "✈️"
        "salary" -> "💵"
        else -> "📦"
    }

    val displayLabel = getCategoryDisplayLabel(category)

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 450f),
        label = "pillScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(if (selected) EmeraldPrimary.copy(alpha = 0.18f) else SurfaceWhite)
                .border(
                    BorderStroke(
                        width = 1.5.dp,
                        color = if (selected) EmeraldPrimary else Color.White.copy(alpha = 0.05f)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 22.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = displayLabel,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) EmeraldPrimary else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    currency: String,
    onDeleteRequested: () -> Unit,
    onEditRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val limitPx = with(density) { 80.dp.toPx() }
    val thresholdPx = with(density) { 40.dp.toPx() }

    var dragAmount by remember { mutableStateOf(0f) }
    var targetOffset by remember { mutableStateOf(0f) }

    val animatedOffset by animateFloatAsState(
        targetValue = targetOffset + dragAmount,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 400f
        ),
        label = "swipeOffset"
    )

    // Fade-in list entrance animation
    var itemVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        itemVisible = true
    }
    val animAlpha by animateFloatAsState(
        targetValue = if (itemVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 50),
        label = "itemAlpha"
    )
    val animY by animateFloatAsState(
        targetValue = if (itemVisible) 0f else 30f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
        label = "itemY"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(alpha = animAlpha, translationY = animY)
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceWhite)
    ) {
        // Underlay actions
        Row(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFF090B10)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Edit action (Left side, revealed when dragging right, animatedOffset > 0)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(
                        color = EmeraldPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp)
                    )
                    .clickable {
                        targetOffset = 0f
                        dragAmount = 0f
                        onEditRequested()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Modifica",
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Dummy box in center
            Box(modifier = Modifier.weight(1f))

            // Delete action (Right side, revealed when dragging left, animatedOffset < 0)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(
                        color = ExpenseRed.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp)
                    )
                    .clickable {
                        targetOffset = 0f
                        dragAmount = 0f
                        onDeleteRequested()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Elimina",
                    tint = ExpenseRed,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Foreground content
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .fillMaxWidth()
                .background(SurfaceWhite)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val currentTotal = targetOffset + dragAmount
                            if (currentTotal > thresholdPx) {
                                targetOffset = limitPx
                            } else if (currentTotal < -thresholdPx) {
                                targetOffset = -limitPx
                            } else {
                                targetOffset = 0f
                            }
                            dragAmount = 0f
                        },
                        onDragCancel = {
                            dragAmount = 0f
                        },
                        onHorizontalDrag = { change, dragAmountPx ->
                            change.consume()
                            dragAmount += dragAmountPx
                            val total = targetOffset + dragAmount
                            if (total > limitPx * 1.5f) {
                                dragAmount = limitPx * 1.5f - targetOffset
                            } else if (total < -limitPx * 1.5f) {
                                dragAmount = -limitPx * 1.5f - targetOffset
                            }
                        }
                    )
                }
        ) {
            TransactionCardContent(
                transaction = transaction,
                currency = currency
            )
        }
    }
}

@Composable
fun TransactionCardContent(
    transaction: Transaction,
    currency: String,
    modifier: Modifier = Modifier
) {
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())
    val sdfDateOnly = SimpleDateFormat("dd MMM", Locale.getDefault())

    val formattedTime = sdfTime.format(Date(transaction.dateMillis))
    val formattedDate = sdfDateOnly.format(Date(transaction.dateMillis))

    val isIncome = transaction.type == TransactionType.INCOME
    val (icon, tint) = getCategoryMeta(transaction.category, isIncome)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceWhite)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(22.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Squircle category badge for premium appearance (Revolut inspired)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(tint.copy(alpha = 0.12f))
                .border(BorderStroke(1.dp, tint.copy(alpha = 0.2f)), RoundedCornerShape(14.dp)),
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

        Column(modifier = Modifier.weight(1f)) {
            val titleText = if (transaction.note.isNotBlank()) transaction.note else getCategoryDisplayLabel(transaction.category)
            Text(
                text = titleText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            // Discrete quieter date
            Text(
                text = "${getCategoryDisplayLabel(transaction.category)}  •  $formattedDate, $formattedTime",
                fontSize = 11.sp,
                color = TextSecondary.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // More evident, premium formatted amount text
        Text(
            text = String.format(Locale.getDefault(), "%s %,.2f %s", if (isIncome) "+" else "-", transaction.amount, currency),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            color = if (isIncome) EmeraldPrimary else ExpenseRed,
            letterSpacing = (-0.5).sp
        )
    }
}

@Composable
fun EmptyTransactionsState(
    onAddRequested: () -> Unit,
    modifier: Modifier = Modifier
) {
    BrandEmptyState(
        title = "Nessun movimento",
        subtitle = "Aggiungi la tua prima spesa o modifica i filtri per vedere i risultati.",
        buttonText = "Aggiungi transazione",
        onButtonClick = onAddRequested,
        icon = Icons.Default.ReceiptLong,
        modifier = modifier
    )
}

// Helpers
fun getCategoryDisplayLabel(cat: String): String {
    return when (cat.lowercase(Locale.getDefault())) {
        "all" -> "Tutte"
        "food" -> "Cibo"
        "transport" -> "Trasporti"
        "shopping" -> "Shopping"
        "bills" -> "Bollette"
        "entertainment" -> "Intrattenimento"
        "health" -> "Salute"
        "travel" -> "Viaggi"
        "salary" -> "Stipendio"
        "other" -> "Altro"
        else -> cat
    }
}

fun getCategoryIcon(cat: String): ImageVector {
    return when (cat.lowercase(Locale.getDefault())) {
        "all" -> Icons.Default.FilterList
        "food" -> Icons.Default.Fastfood
        "transport" -> Icons.Default.DirectionsCar
        "shopping" -> Icons.Default.ShoppingBag
        "bills" -> Icons.Default.Receipt
        "entertainment" -> Icons.Default.SportsEsports
        "health" -> Icons.Default.LocalHospital
        "travel" -> Icons.Default.Flight
        "salary" -> Icons.Default.AttachMoney
        else -> Icons.Default.Category
    }
}

private fun getGroupHeader(dateMillis: Long): String {
    val today = Calendar.getInstance()
    val txDate = Calendar.getInstance().apply { timeInMillis = dateMillis }

    val isSameDay = today.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == txDate.get(Calendar.DAY_OF_YEAR)

    if (isSameDay) return "OGGI"

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = yesterday.get(Calendar.YEAR) == txDate.get(Calendar.YEAR) &&
            yesterday.get(Calendar.DAY_OF_YEAR) == txDate.get(Calendar.DAY_OF_YEAR)

    if (isYesterday) return "IERI"

    // Days within last week
    val diffMillis = today.timeInMillis - txDate.timeInMillis
    val diffDays = diffMillis / (24 * 60 * 60 * 1000)
    if (diffDays in 2..6) {
        val sdfDayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdfDayOfWeek.format(Date(dateMillis)).uppercase(Locale.getDefault())
    }

    val sdfHeader = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return sdfHeader.format(Date(dateMillis)).uppercase(Locale.getDefault())
}
