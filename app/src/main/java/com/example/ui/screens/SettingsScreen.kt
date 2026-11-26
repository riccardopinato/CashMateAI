package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.premium.PremiumManager
import com.example.premium.PremiumPlan
import com.example.premium.PremiumFeature
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FinanceViewModel,
    onLogoutSuccess: () -> Unit,
    onNavigateToAutomaticRules: () -> Unit = {},
    onNavigateToRecurringTransactions: () -> Unit = {},
    onNavigateToPaywall: () -> Unit = {}
) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val aiUsageLogs by viewModel.aiUsageLogs.collectAsStateWithLifecycle()
    val userProfile by viewModel.currentUser.collectAsStateWithLifecycle()
    val isAutoSyncEnabled by viewModel.isAutoSyncEnabled.collectAsStateWithLifecycle()
    val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsStateWithLifecycle()
    val connectivityStatus by viewModel.connectivityStatus.collectAsStateWithLifecycle()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetCategory by remember { mutableStateOf("Food") }
    var budgetAmountStr by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Mock states for premium settings switches / notifications
    var isDarkTheme by remember { mutableStateOf(true) }
    var isNotificationsEnabled by remember { mutableStateOf(true) }
    var isBiometricsEnabled by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }

    // Calculate total estimated tokens consumed
    val totalEstimatedTokens = remember(aiUsageLogs) {
        aiUsageLogs.sumOf { it.tokensEstimated }
    }

    // Elegant Dialog to configure budget limits
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                    }
                    Text("Limite Budget", fontWeight = FontWeight.Black, color = Color.White, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Imposta un limite mensile per categoria. CashMate AI monitorerà l'andamento delle spese per te.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                    
                    Text("Seleziona Categoria", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val cats = listOf("Food", "Shopping", "Entertainment", "Bills")
                        cats.forEach { cat ->
                            val isSel = budgetCategory == cat
                            val emoji = when(cat) {
                                "Food" -> "🍟"
                                "Shopping" -> "🛍️"
                                "Entertainment" -> "🎬"
                                else -> "🔌"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) EmeraldPrimary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
                                    .border(1.dp, if (isSel) EmeraldPrimary else Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                    .clickable { budgetCategory = cat }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(emoji, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(cat, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (isSel) EmeraldPrimary else TextSecondary)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = budgetAmountStr,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.toDoubleOrNull() != null) {
                                budgetAmountStr = input
                            }
                        },
                        label = { Text("Limite Massimo ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            focusedLabelColor = EmeraldPrimary,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = SurfaceElevation,
                            unfocusedContainerColor = SurfaceElevation
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val amt = budgetAmountStr.toDoubleOrNull()
                val isSaveEnabled = amt != null && amt > 0
                Button(
                    onClick = {
                        if (isSaveEnabled) {
                            viewModel.addBudget(budgetCategory, amt!!)
                            budgetAmountStr = ""
                            showBudgetDialog = false
                        }
                    },
                    enabled = isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        disabledContainerColor = Color.White.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Salva", color = if (isSaveEnabled) Color.Black else TextSecondary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Annulla", color = TextSecondary)
                }
            },
            containerColor = SurfaceElevation,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Currency Selection Modal Dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Seleziona Valuta", fontWeight = FontWeight.Black, color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val currencies = listOf("€" to "Euro (EUR)", "$" to "Dollaro (USD)", "£" to "Sterlina (GBP)", "¥" to "Yen (JPY)")
                    currencies.forEach { (symbol, name) ->
                        val isSel = currency == symbol
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) EmeraldPrimary.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable {
                                    viewModel.updateCurrency(symbol)
                                    showCurrencyDialog = false
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$symbol  $name", color = if (isSel) EmeraldPrimary else Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Chiudi", color = EmeraldPrimary)
                }
            },
            containerColor = SurfaceElevation,
            shape = RoundedCornerShape(24.dp)
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(36.dp)) },
            title = { Text("Conferma Disconnessione", fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center) },
            text = {
                Text(
                    "Sei sicuro di voler uscire da CashMate AI? Tutti i tuoi dati sono salvati in sicurezza, ma dovrai rieffettuare l'accesso.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout(onLogoutSuccess)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Disconnetti", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Annulla", color = TextSecondary)
                }
            },
            containerColor = SurfaceElevation,
            shape = RoundedCornerShape(24.dp)
        )
    }

    Scaffold(
        containerColor = BackgroundWarm,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Impostazioni", fontWeight = FontWeight.Black, color = Color.White, fontSize = 20.sp)
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // HERO HEADER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(EmeraldPrimary, LimeSecondary)
                            )
                        )
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(SurfaceWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userProfile?.displayName?.take(2)?.uppercase(Locale.getDefault()) ?: "AI",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "CashMate AI",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Personalizza la tua esperienza CashMate AI",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // SECTION: CASHMATE PREMIUM
            val context = LocalContext.current
            val currentPlan by PremiumManager.currentPlan.collectAsState()
            val expirationDate by PremiumManager.expirationDate.collectAsState()

            Text(
                text = "CASHMATE PREMIUM",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().testTag("premium_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Piano Attuale: ${currentPlan.name}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isPremium) "Scadenza: $expirationDate" else "Sblocca analisi ed AI senza limiti",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onNavigateToPaywall,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("settings_manage_sub_button")
                        ) {
                            Text(if (isPremium) "Gestisci" else "Passa a Pro", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "Ripristino acquisti non configurato.", Toast.LENGTH_SHORT).show()
                            },
                            enabled = false,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("settings_restore_purchases_button")
                        ) {
                            Text("Ripristina", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // SECTION 1: ACCOUNT
            Text(
                text = "IL TUO ACCOUNT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Styled Avatar with Initials fallback
                        val initials = (userProfile?.displayName?.ifBlank { "Utente CashMate" } ?: "Utente CashMate")
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
                            .map { it.first().uppercaseChar() }
                            .joinToString("")

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f))
                                .border(BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = EmeraldPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userProfile?.displayName?.ifBlank { "Utente CashMate" } ?: "Utente CashMate", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(userProfile?.email?.ifBlank { "user@cashmate.ai" } ?: "user@cashmate.ai", fontSize = 12.sp, color = TextSecondary)
                        }

                        // Premium Badge
                        Surface(
                            color = if (isPremium) EmeraldPrimary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(100),
                            border = BorderStroke(1.dp, if (isPremium) EmeraldPrimary.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f))
                        ) {
                            Text(
                                text = if (isPremium) "PREMIUM" else "FREE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPremium) EmeraldPrimary else TextSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Edit Profile Trigger Button
                    Button(
                        onClick = { /* Simulated Profile Edit Trigger */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.04f)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Modifica profilo", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // SECTION 2: PREFERENZE
            Text(
                text = "PREFERENZE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    // Row 1: Tema (Switch inline)
                    SettingsItemRow(
                        icon = Icons.Default.DarkMode,
                        title = "Tema Scuro (Amoled)",
                        value = if (isDarkTheme) "Attivo" else "Disattivo",
                        trailingContent = {
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { isDarkTheme = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = EmeraldPrimary,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = SurfaceElevation
                                )
                            )
                        }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Row 2: Valuta (Click opens selection dialog)
                    SettingsItemRow(
                        icon = Icons.Default.Paid,
                        title = "Valuta Principale",
                        value = when(currency) {
                            "€" -> "Euro (€)"
                            "$" -> "Dollaro ($)"
                            "£" -> "Sterlina (£)"
                            else -> currency
                        },
                        onClick = { showCurrencyDialog = true }
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Row 3: Lingua
                    SettingsItemRow(
                        icon = Icons.Default.Language,
                        title = "Lingua Applicazione",
                        value = "Italiano (IT)",
                        onClick = {}
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Row 4: Notifiche
                    SettingsItemRow(
                        icon = Icons.Default.NotificationsActive,
                        title = "Notifiche Push",
                        value = if (isNotificationsEnabled) "Abilitate" else "Silenziate",
                        trailingContent = {
                            Switch(
                                checked = isNotificationsEnabled,
                                onCheckedChange = { isNotificationsEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = EmeraldPrimary,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = SurfaceElevation
                                )
                            )
                        }
                    )
                }
            }

            // SECTION 3: FINANZA
            Text(
                text = "CONFIGURAZIONE FINANZE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    // Budgets Limits dialog trigger
                    SettingsItemRow(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "Configura Budget",
                        value = "Pianifica limiti",
                        onClick = { showBudgetDialog = true },
                        tag = "configure_budgets_row"
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Categorie
                    SettingsItemRow(
                        icon = Icons.Default.Category,
                        title = "Gestione Categorie",
                        value = "12 Attive",
                        onClick = {}
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Obiettivi
                    SettingsItemRow(
                        icon = Icons.Default.TrendingUp,
                        title = "Obiettivi di Risparmio",
                        value = "300 € / mese",
                        onClick = {}
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Metodo di pagamento
                    SettingsItemRow(
                        icon = Icons.Default.Payment,
                        title = "Metodi di Pagamento",
                        value = "Carta di Credito",
                        onClick = {}
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Regole Automatiche
                    SettingsItemRow(
                        icon = Icons.Default.Settings,
                        title = "Regole Automatiche",
                        value = "Gestisci regole",
                        onClick = onNavigateToAutomaticRules,
                        tag = "automatic_rules_row"
                    )

                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))

                    // Transazioni Ricorrenti
                    SettingsItemRow(
                        icon = Icons.Default.Sync,
                        title = "Transazioni Ricorrenti",
                        value = "Gestisci ricorrenze",
                        onClick = onNavigateToRecurringTransactions,
                        tag = "recurring_transactions_row"
                    )
                }
            }

            // SECTION: ECOSISTEMA CLOUD & BACKUP
            Text(
                text = "ECOSISTEMA CLOUD & BACKUP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth().testTag("cloud_settings_card")
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            val isConnected = connectivityStatus == com.example.data.remote.ConnectivityObserver.Status.Available
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isConnected) EmeraldPrimary else TextSecondary)
                            )
                            Column {
                                Text(
                                    text = if (isConnected) "Stato Cloud: Online" else "Stato Cloud: Offline",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val formattedBackupTime = remember(lastBackupTimestamp) {
                                    if (lastBackupTimestamp == 0L) {
                                        "Nessun backup cloud presente"
                                    } else {
                                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
                                        "Backup: " + sdf.format(java.util.Date(lastBackupTimestamp))
                                    }
                                }
                                Text(
                                    text = formattedBackupTime,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        // Auto-Sync Toggle
                        Switch(
                            checked = isAutoSyncEnabled,
                            onCheckedChange = { viewModel.setAutoSyncEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = EmeraldPrimary,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfaceElevation
                            ),
                            modifier = Modifier.testTag("auto_sync_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Sincronizza in tempo reale le tue transazioni, obiettivi e budget con il cloud.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Backup / Restore / Sync Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sincronizza Ora Button
                        Button(
                            onClick = {
                                viewModel.triggerManualSync(
                                    onSuccess = { Toast.makeText(context, "Sincronizzazione completata con successo!", Toast.LENGTH_SHORT).show() },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevation),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("settings_sync_now_button"),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Text("Sincronizza", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Backup Ora Button
                        Button(
                            onClick = {
                                viewModel.backupNow(
                                    onSuccess = { Toast.makeText(context, "Backup salvato nel cloud!", Toast.LENGTH_SHORT).show() },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevation),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("settings_backup_now_button"),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = LimeSecondary, modifier = Modifier.size(16.dp))
                                Text("Backup Ora", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Ripristina Ora Button
                        Button(
                            onClick = {
                                viewModel.restoreNow(
                                    onSuccess = { Toast.makeText(context, "Dati ripristinati con successo!", Toast.LENGTH_SHORT).show() },
                                    onError = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevation),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).testTag("settings_restore_backup_button"),
                            contentPadding = PaddingValues(vertical = 10.dp, horizontal = 4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                Text("Ripristina", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // SECTION 4: AI SUMMARY CARD
            Text(
                text = "INTELLIGENZA ARTIFICIALE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            Text("Stato Gemini AI", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Switches actual premium state binded with VM!
                        Switch(
                            checked = isPremium,
                            onCheckedChange = { /* Premium entitlement is read-only until billing is verified. */ },
                            enabled = false,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = EmeraldPrimary,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfaceElevation
                            ),
                            modifier = Modifier.testTag("premium_status_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Show active stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Piano Attuale", fontSize = 11.sp, color = TextSecondary)
                            Text(if (isPremium) "Pro Premium" else "Free Account", fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (isPremium) LimeSecondary else Color.White)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Chiamate Orarie", fontSize = 11.sp, color = TextSecondary)
                            Text(if (isPremium) "30 / ora" else "5 / ora", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Progress showing token consumption estimate
                    val maxEstimatedUsageValue = 250000f
                    val progressFraction = (totalEstimatedTokens / maxEstimatedUsageValue).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Token consumati questo mese", fontSize = 11.sp, color = TextSecondary)
                        Text("$totalEstimatedTokens / 250K", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(100))
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .clip(RoundedCornerShape(100))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(EmeraldPrimary, LimeSecondary)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Uso di token calcolato in base all'elaborazione locale e cloud dell'AI Coach.",
                        fontSize = 10.sp,
                        color = TextSecondary.copy(alpha = 0.6f),
                        lineHeight = 14.sp
                    )
                }
            }

            // SECTION 5: BACKUP
            Text(
                text = "DATI E BACKUP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    SettingsItemRow(
                        icon = Icons.Default.CloudUpload,
                        title = "Cloud Backup",
                        value = "Backup Attivo",
                        onClick = {}
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItemRow(
                        icon = Icons.Default.Download,
                        title = "Importa Dati",
                        value = "JSON / CSV",
                        onClick = {}
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItemRow(
                        icon = Icons.Default.Upload,
                        title = "Esporta Transazioni",
                        value = "Esporta CSV",
                        onClick = {}
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItemRow(
                        icon = Icons.Default.Sync,
                        title = "Sincronizza Ora",
                        value = "Aggiornato",
                        onClick = {}
                    )
                }
            }

            // SECTION 6: PRIVACY
            Text(
                text = "SICUREZZA E PRIVACY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    SettingsItemRow(
                        icon = Icons.Default.Fingerprint,
                        title = "Sblocco con Face ID / PIN",
                        value = if (isBiometricsEnabled) "Attivo" else "Disattivo",
                        trailingContent = {
                            Switch(
                                checked = isBiometricsEnabled,
                                onCheckedChange = { isBiometricsEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = EmeraldPrimary,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = SurfaceElevation
                                )
                            )
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItemRow(
                        icon = Icons.Default.Lock,
                        title = "Modifica Codice PIN",
                        value = "Configurato",
                        onClick = {}
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItemRow(
                        icon = Icons.Default.Security,
                        title = "Politica di Privacy",
                        value = "Leggi",
                        onClick = {}
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItemRow(
                        icon = Icons.Default.AppSettingsAlt,
                        title = "Permessi di Sistema",
                        value = "Gestisci",
                        onClick = {}
                    )
                }
            }

            // SECTION 7: INFO
            Text(
                text = "INFO E SUPPORTO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(6.dp)) {
                    SettingsItemRow(
                        icon = Icons.Default.Info,
                        title = "Versione Applicazione",
                        value = "v2.1.0-ai",
                        onClick = {}
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItemRow(
                        icon = Icons.Default.Receipt,
                        title = "Licenze Open Source",
                        value = "Visualizza",
                        onClick = {}
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItemRow(
                        icon = Icons.Default.ContactSupport,
                        title = "Servizio Clienti / Aiuto",
                        value = "Supporto",
                        onClick = {}
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.04f), modifier = Modifier.padding(horizontal = 16.dp))
                    SettingsItemRow(
                        icon = Icons.Default.StarRate,
                        title = "Valuta l'applicazione",
                        value = "Lascia 5 stelle",
                        onClick = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SECTION 8: RED LOGOUT BUTTON
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("logout_button")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Disconnetti Account", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
            }

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    value: String = "",
    tag: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }

    val taggedModifier = if (tag != null) {
        clickableModifier.testTag(tag)
    } else {
        clickableModifier
    }

    Row(
        modifier = taggedModifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Value Description
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            if (value.isNotEmpty() && trailingContent == null) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Custom trailing or default Chevron
        if (trailingContent != null) {
            trailingContent()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        } else if (value.isNotEmpty()) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
        }
    }
}
