package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import com.example.domain.model.SavingsDeposit
import com.example.domain.model.SavingsGoal
import com.example.domain.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.SavingsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalDetailScreen(
    goalId: Int,
    viewModel: SavingsViewModel,
    onNavigateBack: () -> Unit
) {
    val goalFlow = remember(goalId) { viewModel.getGoalById(goalId) }
    val goal by goalFlow.collectAsStateWithLifecycle(initialValue = null)

    val depositsFlow = remember(goalId) { viewModel.getDepositsForGoal(goalId) }
    val deposits by depositsFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()

    var showDepositDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (goal == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundWarm),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF00E676))
        }
        return
    }

    val currentGoal = goal!!
    val progress = (currentGoal.currentAmount / currentGoal.targetAmount).toFloat().coerceIn(0f, 1f)
    val progressPercentage = (progress * 100).toInt()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentGoal.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
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
                actions = {
                    // Archive / Restore action
                    if (currentGoal.status == "ACTIVE" || currentGoal.status == "COMPLETED") {
                        IconButton(
                            onClick = {
                                viewModel.archiveGoal(currentGoal)
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.03f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Archivia",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (currentGoal.status == "ARCHIVED") {
                        IconButton(
                            onClick = {
                                viewModel.restoreGoal(currentGoal)
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(38.dp)
                                .background(Color.White.copy(alpha = 0.03f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Unarchive,
                                contentDescription = "Ripristina",
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.03f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Elimina",
                            tint = ExpenseRed.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWarm)
            )
        },
        containerColor = BackgroundWarm
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Interactive Progress Hero Card
            item {
                GoalProgressHeroCard(currentGoal, progressPercentage, currency)
            }

            // 2. Action: "Aggiungi Risparmio" Button
            if (currentGoal.status == "ACTIVE") {
                item {
                    Button(
                        onClick = { showDepositDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E676),
                            contentColor = Color(0xFF090B10)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("add_saving_button")
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(20.dp),
                                ambientColor = Color(0xFF00E676).copy(alpha = 0.3f),
                                spotColor = Color(0xFF00E676).copy(alpha = 0.3f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aggiungi Risparmio",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            } else if (currentGoal.status == "ARCHIVED") {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Questo obiettivo è archiviato. Ripristinalo per aggiungere nuovi risparmi.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Notes and details display
            if (currentGoal.description.isNotBlank() || currentGoal.notes.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("DETTAGLI OBIETTIVO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFC857), letterSpacing = 1.sp)
                            if (currentGoal.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(currentGoal.description, fontSize = 13.sp, color = Color.White)
                            }
                            if (currentGoal.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Note: ${currentGoal.notes}", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // 3. Badges (Gamification Progress)
            item {
                MilestonesProgressSection(progressPercentage)
            }

            // 4. History Title
            item {
                Text(
                    text = "Storico versamenti",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 5. History list
            if (deposits.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "✍️", fontSize = 28.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Nessun versamento",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Inizia a versare per vedere la cronologia.",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(deposits, key = { it.id }) { deposit ->
                    DepositHistoryItem(deposit, currency)
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showDepositDialog) {
        AddDepositEngineDialog(
            currency = currency,
            transactions = transactions,
            onDismiss = { showDepositDialog = false },
            onConfirm = { amount, note, txId ->
                viewModel.addDeposit(currentGoal.id, amount, note, txId)
                showDepositDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(currentGoal.id)
                        showDeleteConfirm = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    shape = RoundedCornerShape(100)
                ) {
                    Text("Elimina", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annulla", color = TextSecondary)
                }
            },
            title = {
                Text("Elimina Obiettivo", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Sei sicuro di voler eliminare l'obiettivo '${currentGoal.name}'? Tutti i dati di deposito andranno persi.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            containerColor = SurfaceWhite,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
fun GoalProgressHeroCard(
    goal: SavingsGoal,
    progressPercentage: Int,
    currency: String
) {
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(goal.currentAmount) {
        val target = (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
        animatedProgress.animateTo(
            targetValue = target,
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 80f)
        )
    }

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.03f))
                    .border(1.2.dp, Color.White.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = goal.emoji, fontSize = 28.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "%,.2f %s".format(Locale.getDefault(), goal.currentAmount, currency),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = "di %,.2f %s target".format(Locale.getDefault(), goal.targetAmount, currency),
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
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
                                colors = listOf(Color(0xFF00E676).copy(alpha = 0.8f), Color(0xFF00E676))
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(Color(0xFF00E676).copy(alpha = 0.12f))
                        .border(BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.2f)), RoundedCornerShape(100))
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "$progressPercentage% completato",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676)
                    )
                }

                val missing = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
                if (missing > 0.0) {
                    Text(
                        text = "Mancano %,.2f %s".format(Locale.getDefault(), missing, currency),
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Raggiunto! 🎉",
                        fontSize = 12.sp,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MilestonesProgressSection(progressPercentage: Int) {
    val milestones = listOf(
        Triple(25, "🌱 Inizio", Color(0xFF3B82F6)),
        Triple(50, "🥈 Metà", Color(0xFFFFC857)),
        Triple(75, "🥇 Quasi", Color(0xFFA8FF60)),
        Triple(100, "🏆 Fine", Color(0xFF00E676))
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "TAPPE DEL PERCORSO",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                milestones.forEach { (target, label, color) ->
                    val isUnlocked = progressPercentage >= target
                    val alpha = if (isUnlocked) 1f else 0.3f
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isUnlocked) color.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f))
                                .border(
                                    width = 1.dp,
                                    color = if (isUnlocked) color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = if (isUnlocked) color else Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = alpha),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "$target%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) color else TextSecondary.copy(alpha = alpha),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DepositHistoryItem(
    deposit: SavingsDeposit,
    currency: String
) {
    val dateStr = remember(deposit.dateMillis) {
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ITALIAN).format(Date(deposit.dateMillis))
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E676).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = if (deposit.note.isNotBlank()) deposit.note else "Risparmio aggiunto",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = dateStr,
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = "+%,.2f %s".format(Locale.getDefault(), deposit.amount, currency),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF00E676)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDepositEngineDialog(
    currency: String,
    transactions: List<Transaction>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, note: String, transactionId: Int?) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Manuale, 1 = Collega Transazione
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedTxId by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        onConfirm(amount, note, selectedTxId)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E676),
                    contentColor = Color(0xFF090B10)
                ),
                shape = RoundedCornerShape(100),
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Conferma", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla", color = TextSecondary)
            }
        },
        title = {
            Text(
                text = "Aggiungi Risparmio",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Inline tabs for dialog
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(100))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(100))
                            .background(if (selectedTab == 0) Color(0xFF00E676).copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Manuale", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 0) Color(0xFF00E676) else TextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(100))
                            .background(if (selectedTab == 1) Color(0xFF00E676).copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Collega Spesa", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 1) Color(0xFF00E676) else TextSecondary)
                    }
                }

                if (selectedTab == 0) {
                    // Manual Input
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("Importo ($currency)") },
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

                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Nota / Descrizione") },
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
                } else {
                    // Collega Spesa List
                    val recentExpenses = transactions.filter { it.type == com.example.domain.model.TransactionType.EXPENSE }.take(10)
                    if (recentExpenses.isEmpty()) {
                        Text(
                            text = "Nessuna transazione recente trovata da collegare.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column {
                            Text("Seleziona una transazione:", fontSize = 11.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(modifier = Modifier.heightIn(max = 200.dp)) {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(recentExpenses) { tx ->
                                        val isSelected = selectedTxId == tx.id
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) Color(0xFF00E676).copy(alpha = 0.1f) else Color.White.copy(alpha = 0.02f))
                                                .border(
                                                    1.dp,
                                                    if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.05f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    selectedTxId = tx.id
                                                    amountStr = tx.amount.toString()
                                                    note = "Collegato a: ${tx.category} - ${tx.note}"
                                                }
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(tx.category, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                if (tx.note.isNotBlank()) {
                                                    Text(tx.note, fontSize = 10.sp, color = TextSecondary)
                                                }
                                            }
                                            Text("-%,.2f %s".format(Locale.getDefault(), tx.amount, currency), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                                        }
                                    }
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
