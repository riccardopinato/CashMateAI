package com.example.ui.screens

import android.text.format.DateFormat
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.Account
import com.example.ui.theme.*
import com.example.ui.components.*
import com.example.ui.viewmodel.AccountViewModel
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    financeViewModel: FinanceViewModel,
    accountViewModel: AccountViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onPremiumUpgradeRequired: (com.example.premium.PremiumFeature) -> Unit = {}
) {
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val currency by accountViewModel.currency.collectAsStateWithLifecycle()
    val transactions by financeViewModel.transactions.collectAsStateWithLifecycle()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    // Calculate total balance across all accounts
    val totalBalance = accounts.sumOf { it.saldo }

    Scaffold(
        containerColor = BackgroundWarm,
        topBar = {
            TopAppBar(
                title = { Text("Wallet & Conti", fontWeight = FontWeight.Black, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("accounts_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTransferDialog = true },
                        modifier = Modifier.testTag("transfer_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Trasferisci denaro",
                            tint = EmeraldPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWarm)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (com.example.premium.PremiumManager.canUseFeature(com.example.premium.PremiumFeature.MULTI_ACCOUNT, currentCount = accounts.size)) {
                        showAddAccountDialog = true
                    } else {
                        onPremiumUpgradeRequired(com.example.premium.PremiumFeature.MULTI_ACCOUNT)
                    }
                },
                containerColor = Color(0xFF00E676),
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier
                    .testTag("add_account_fab")
                    .padding(bottom = 16.dp, end = 8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuovo Conto", modifier = Modifier.size(28.dp))
            }
        }
    ) { innerPadding ->
        var animateItems by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            animateItems = true
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Card: Total balance across all accounts
            item {
                AnimatedVisibility(
                    visible = animateItems,
                    enter = fadeIn(animationSpec = tween(500)) + expandVertically(animationSpec = tween(500))
                ) {
                    Card(
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF1E293B),
                                            Color(0xFF0F172A)
                                        )
                                    )
                                )
                                .padding(24.dp)
                                .fillMaxWidth()
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "SALDO TOTALE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = String.format(Locale.getDefault(), "%,.2f", totalBalance),
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currency,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldPrimary,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { showTransferDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Trasferisci Denaro", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Section Header
            item {
                Text(
                    text = "I tuoi Conti",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // 3. Accounts List
            items(accounts, key = { it.id }) { account ->
                val txCount = transactions.count { it.accountId == account.id }
                val lastUpdatedStr = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(account.aggiornatoIl))

                AnimatedVisibility(
                    visible = animateItems,
                    enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f)) + scaleIn(animationSpec = spring(dampingRatio = 0.8f)),
                    modifier = Modifier.animateItem()
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scaleClickable { /* Detail if needed */ }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(18.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(android.graphics.Color.parseColor(account.colore)).copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = account.icona, fontSize = 22.sp)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = account.nome,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$txCount transazioni • Agg: $lastUpdatedStr",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%,.2f", account.saldo),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currency,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(android.graphics.Color.parseColor(account.colore))
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add Account Dialog
    if (showAddAccountDialog) {
        var nome by remember { mutableStateOf("") }
        var selectedTipo by remember { mutableStateOf("Conto Corrente") }
        var saldoIniziale by remember { mutableStateOf("") }
        var selectedColore by remember { mutableStateOf("#00E676") }
        var selectedIcona by remember { mutableStateOf("🏦") }
        var tipoDropdownExpanded by remember { mutableStateOf(false) }

        val tipiAccount = listOf(
            "Conto Corrente", "Carta di Credito", "Carta di Debito", "Contanti", "PayPal", "Revolut", "Conto Risparmio", "Altro"
        )
        val coloriDisponibili = listOf(
            "#00E676", "#3B82F6", "#FFC857", "#EC4899", "#8B5CF6", "#F97316", "#06B6D4", "#EF4444"
        )
        val iconeDisponibili = listOf(
            "🏦", "💳", "💵", "💼", "🐷", "📈", "🛍️", "🎯", "🏠", "🚗"
        )

        AlertDialog(
            onDismissRequest = { showAddAccountDialog = false },
            title = { Text("Nuovo Conto", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = BackgroundWarm,
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        label = { Text("Nome Conto") },
                        placeholder = { Text("es. Revolut Personale") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_account_name_field")
                    )

                    // Tipo Dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedTipo,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo Conto") },
                            trailingIcon = {
                                IconButton(onClick = { tipoDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tipoDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = tipoDropdownExpanded,
                            onDismissRequest = { tipoDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .background(SurfaceWhite)
                        ) {
                            tipiAccount.forEach { tipo ->
                                DropdownMenuItem(
                                    text = { Text(tipo, color = Color.White) },
                                    onClick = {
                                        selectedTipo = tipo
                                        tipoDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Saldo Iniziale
                    OutlinedTextField(
                        value = saldoIniziale,
                        onValueChange = { saldoIniziale = it },
                        label = { Text("Saldo Iniziale") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_account_balance_field")
                    )

                    // Colore Picker
                    Column {
                        Text("Scegli Colore", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            coloriDisponibili.forEach { colorHex ->
                                val isSelected = selectedColore == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColore = colorHex }
                                )
                            }
                        }
                    }

                    // Icona Picker
                    Column {
                        Text("Scegli Icona", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            iconeDisponibili.take(8).forEach { icon ->
                                val isSelected = selectedIcona == icon
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color.White.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { selectedIcona = icon },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = icon, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val initialSaldo = saldoIniziale.toDoubleOrNull() ?: 0.0
                        if (nome.isNotBlank()) {
                            accountViewModel.addAccount(
                                nome = nome,
                                tipo = selectedTipo,
                                saldoIniziale = initialSaldo,
                                colore = selectedColore,
                                icona = selectedIcona
                            )
                            showAddAccountDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Crea", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAccountDialog = false }) {
                    Text("Annulla", color = TextSecondary)
                }
            }
        )
    }

    // Transfer Dialog
    if (showTransferDialog && accounts.size >= 2) {
        var fromAccount by remember { mutableStateOf(accounts.firstOrNull()) }
        var toAccount by remember { mutableStateOf(accounts.getOrNull(1)) }
        var importoStr by remember { mutableStateOf("") }
        var fromDropdownExpanded by remember { mutableStateOf(false) }
        var toDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("Trasferisci Fondi", fontWeight = FontWeight.Bold, color = Color.White) },
            containerColor = BackgroundWarm,
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Da Account
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = fromAccount?.nome ?: "Scegli Conto di Origine",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Da quale account?") },
                            trailingIcon = {
                                IconButton(onClick = { fromDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { fromDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = fromDropdownExpanded,
                            onDismissRequest = { fromDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .background(SurfaceWhite)
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.nome, color = Color.White) },
                                    onClick = {
                                        fromAccount = acc
                                        fromDropdownExpanded = false
                                        if (toAccount == acc) {
                                            toAccount = accounts.find { it != acc }
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // A Account
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = toAccount?.nome ?: "Scegli Conto di Destinazione",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("A quale account?") },
                            trailingIcon = {
                                IconButton(onClick = { toDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = SurfaceWhite,
                                unfocusedContainerColor = SurfaceWhite
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = toDropdownExpanded,
                            onDismissRequest = { toDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .background(SurfaceWhite)
                        ) {
                            accounts.filter { it != fromAccount }.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.nome, color = Color.White) },
                                    onClick = {
                                        toAccount = acc
                                        toDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Importo
                    OutlinedTextField(
                        value = importoStr,
                        onValueChange = { importoStr = it },
                        label = { Text("Importo") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transfer_amount_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = importoStr.toDoubleOrNull() ?: 0.0
                        val fAcc = fromAccount
                        val tAcc = toAccount
                        if (amount > 0 && fAcc != null && tAcc != null && fAcc.saldo >= amount) {
                            accountViewModel.transfer(fAcc.id, tAcc.id, amount)
                            showTransferDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Trasferisci", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = false }) {
                    Text("Annulla", color = TextSecondary)
                }
            }
        )
    } else if (showTransferDialog) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            title = { Text("Attenzione", color = Color.White) },
            text = { Text("Devi creare almeno due conti diversi per poter effettuare un trasferimento.", color = Color.White) },
            containerColor = BackgroundWarm,
            confirmButton = {
                Button(onClick = { showTransferDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)) {
                    Text("OK", color = Color.Black)
                }
            }
        )
    }
}
