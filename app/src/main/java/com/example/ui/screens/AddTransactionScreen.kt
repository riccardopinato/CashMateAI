package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import com.example.domain.model.Transaction
import com.example.domain.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.AccountViewModel
import com.example.domain.model.Account
import com.example.domain.model.TransactionRule
import com.example.ui.viewmodel.TransactionRuleViewModel
import com.example.ui.viewmodel.RecurringTransactionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

// Model for Favorite Transactions
data class FavoriteTx(
    val emoji: String,
    val amount: Double,
    val note: String,
    val category: String,
    val type: TransactionType,
    val paymentMethod: String
)

// Model for parsed transaction via Quick Add
data class ParsedTransaction(
    val amount: Double,
    val note: String,
    val type: TransactionType,
    val category: String,
    val paymentMethod: String,
    val account: Account?
)

// Helper to save a custom favorite in SharedPreferences
fun saveFavorite(context: android.content.Context, favorite: FavoriteTx) {
    val sharedPrefs = context.getSharedPreferences("cashmate_preferences", android.content.Context.MODE_PRIVATE)
    val currentSet = sharedPrefs.getStringSet("custom_favorites_v1", emptySet())?.toMutableSet() ?: mutableSetOf()
    val serialized = "${favorite.emoji}|${favorite.amount}|${favorite.note}|${favorite.category}|${favorite.type}|${favorite.paymentMethod}"
    currentSet.add(serialized)
    sharedPrefs.edit().putStringSet("custom_favorites_v1", currentSet).apply()
}

// Helper to retrieve custom favorites from SharedPreferences
fun getCustomFavorites(context: android.content.Context): List<FavoriteTx> {
    val sharedPrefs = context.getSharedPreferences("cashmate_preferences", android.content.Context.MODE_PRIVATE)
    val currentSet = sharedPrefs.getStringSet("custom_favorites_v1", emptySet()) ?: emptySet()
    return currentSet.mapNotNull { serialized ->
        val parts = serialized.split("|")
        if (parts.size >= 6) {
            FavoriteTx(
                emoji = parts[0],
                amount = parts[1].toDoubleOrNull() ?: 0.0,
                note = parts[2],
                category = parts[3],
                type = TransactionType.valueOf(parts[4]),
                paymentMethod = parts[5]
            )
        } else null
    }
}

// Helper to parse Quick Add queries locally offline
fun parseQuickAddInput(
    input: String,
    accountsList: List<Account>,
    transactionsList: List<Transaction>
): ParsedTransaction? {
    if (input.isBlank()) return null
    
    // Pattern looking for number sequence (decimals supported)
    val doubleRegex = Regex("""\b\d+(?:[.,]\d+)?\b""")
    val matchResult = doubleRegex.find(input) ?: return null
    val amountStr = matchResult.value.replace(',', '.')
    val parsedAmount = amountStr.toDoubleOrNull() ?: return null
    
    val textWithNoAmount = input.replace(matchResult.value, "").trim()
    if (textWithNoAmount.isBlank()) {
        return ParsedTransaction(parsedAmount, "", TransactionType.EXPENSE, "Other", "Card", null)
    }
    
    val cleanedText = textWithNoAmount.trim()
    var category = "Other"
    var type = TransactionType.EXPENSE
    val lowercaseText = cleanedText.lowercase(Locale.getDefault())
    
    if (lowercaseText.contains("esselunga") || lowercaseText.contains("conad") || lowercaseText.contains("coop") || lowercaseText.contains("spesa") || lowercaseText.contains("cibo") || lowercaseText.contains("ristorante") || lowercaseText.contains("cena") || lowercaseText.contains("mcdonald") || lowercaseText.contains("pizza") || lowercaseText.contains("mcdonald's")) {
        category = "Food"
    } else if (lowercaseText.contains("eni") || lowercaseText.contains("q8") || lowercaseText.contains("benzina") || lowercaseText.contains("treno") || lowercaseText.contains("carburante") || lowercaseText.contains("auto") || lowercaseText.contains("parcheggio") || lowercaseText.contains("taxi")) {
        category = "Transport"
    } else if (lowercaseText.contains("amazon") || lowercaseText.contains("shopping") || lowercaseText.contains("zara") || lowercaseText.contains("vestiti") || lowercaseText.contains("regalo")) {
        category = "Shopping"
    } else if (lowercaseText.contains("netflix") || lowercaseText.contains("spotify") || lowercaseText.contains("bolletta") || lowercaseText.contains("luce") || lowercaseText.contains("gas") || lowercaseText.contains("affitto") || lowercaseText.contains("abbonamento") || lowercaseText.contains("prime")) {
        category = "Bills"
    } else if (lowercaseText.contains("cinema") || lowercaseText.contains("gioco") || lowercaseText.contains("steam") || lowercaseText.contains("bar") || lowercaseText.contains("birra") || lowercaseText.contains("pub")) {
        category = "Entertainment"
    } else if (lowercaseText.contains("farmacia") || lowercaseText.contains("medico") || lowercaseText.contains("salute") || lowercaseText.contains("ospedale") || lowercaseText.contains("dentista")) {
        category = "Health"
    } else if (lowercaseText.contains("volo") || lowercaseText.contains("hotel") || lowercaseText.contains("viaggio") || lowercaseText.contains("vacanza") || lowercaseText.contains("flight") || lowercaseText.contains("airbnb")) {
        category = "Travel"
    } else if (lowercaseText.contains("stipendio") || lowercaseText.contains("bonifico") || lowercaseText.contains("salary") || lowercaseText.contains("entrata")) {
        category = "Salary"
        type = TransactionType.INCOME
    }
    
    var suggestedAccount: Account? = null
    var suggestedPaymentMethod = "Card"
    
    val matchingPastTransactions = transactionsList.filter { 
        it.note.contains(cleanedText, ignoreCase = true) 
    }
    
    if (matchingPastTransactions.isNotEmpty()) {
        val mostFrequentAccountId = matchingPastTransactions
            .groupBy { it.accountId }
            .maxByOrNull { it.value.size }?.key
            
        suggestedAccount = accountsList.find { it.id == mostFrequentAccountId }
        suggestedPaymentMethod = matchingPastTransactions
            .groupBy { it.paymentMethod }
            .maxByOrNull { it.value.size }?.key ?: "Card"
    } else {
        if (lowercaseText.contains("esselunga") || lowercaseText.contains("amazon") || lowercaseText.contains("mcdonald") || lowercaseText.contains("netflix") || lowercaseText.contains("spotify")) {
            suggestedPaymentMethod = "Card"
        } else if (lowercaseText.contains("bar") || lowercaseText.contains("benzina") || lowercaseText.contains("pizza")) {
            suggestedPaymentMethod = "Cash"
        } else if (lowercaseText.contains("affitto") || lowercaseText.contains("stipendio")) {
            suggestedPaymentMethod = "Bank Transfer"
        }
    }
    
    if (suggestedAccount == null && accountsList.isNotEmpty()) {
        suggestedAccount = accountsList.firstOrNull()
    }
    
    return ParsedTransaction(
        amount = parsedAmount,
        note = cleanedText.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
        type = type,
        category = category,
        paymentMethod = suggestedPaymentMethod,
        account = suggestedAccount
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit
) {
    val currency by viewModel.currency.collectAsState()
    val accountViewModel: AccountViewModel = viewModel()
    val accountsList by accountViewModel.accounts.collectAsState()
    val filteredAccounts = remember(accountsList) {
        accountsList.filter { acc ->
            val nameLower = acc.nome.trim().lowercase()
            nameLower != "tutti i conti" && nameLower != "tutto" && nameLower != "tutti" && nameLower != "all" && nameLower != "all_accounts"
        }
    }
    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var showAccountDropdown by remember { mutableStateOf(false) }

    val ruleViewModel: TransactionRuleViewModel = viewModel()
    val recurringViewModel: RecurringTransactionViewModel = viewModel()
    val rulesList by ruleViewModel.rules.collectAsState()
    var autoAppliedRule by remember { mutableStateOf<TransactionRule?>(null) }
    var showSaveRuleDialog by remember { mutableStateOf(false) }
    var lastNoteChecked by remember { mutableStateOf("") }

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf<String?>(null) }
    var noteError by remember { mutableStateOf<String?>(null) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf("Food") }
    var paymentMethod by remember { mutableStateOf("Card") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }

    // Smart Local States for Sprint Requirements
    var quickAddText by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var isRecurringSelected by remember { mutableStateOf(false) }
    var recurringFrequency by remember { mutableStateOf("Mensile") }
    var saveAsFavorite by remember { mutableStateOf(false) }
    var customFavoritesList by remember { mutableStateOf(emptyList<FavoriteTx>()) }

    val transactionsList by viewModel.transactions.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        customFavoritesList = getCustomFavorites(context)
    }

    LaunchedEffect(filteredAccounts, transactionsList) {
        if (selectedAccount == null && filteredAccounts.isNotEmpty()) {
            val lastUsedAccountId = transactionsList.maxByOrNull { it.dateMillis }?.accountId
            val lastUsedAccount = lastUsedAccountId?.let { id -> filteredAccounts.find { it.id == id } }
            if (lastUsedAccount != null) {
                selectedAccount = lastUsedAccount
            } else {
                val mostUsedAccountId = transactionsList
                    .filter { it.accountId != null }
                    .groupBy { it.accountId }
                    .maxByOrNull { it.value.size }?.key
                val mostUsedAccount = mostUsedAccountId?.let { id -> filteredAccounts.find { it.id == id } }
                selectedAccount = mostUsedAccount ?: filteredAccounts.firstOrNull()
            }
        }
    }

    LaunchedEffect(note, rulesList) {
        val trimmedNote = note.trim()
        if (trimmedNote.isNotBlank() && trimmedNote != lastNoteChecked) {
            val matchingRule = rulesList.filter { it.enabled }.firstOrNull { rule ->
                (rule.merchantName.isNotBlank() && trimmedNote.contains(rule.merchantName, ignoreCase = true)) ||
                (rule.containsText.isNotBlank() && trimmedNote.contains(rule.containsText, ignoreCase = true))
            }
            if (matchingRule != null) {
                selectedCategory = matchingRule.categoryId
                autoAppliedRule = matchingRule
                if (matchingRule.accountId != null) {
                    val acc = filteredAccounts.find { it.id == matchingRule.accountId }
                    if (acc != null) {
                        selectedAccount = acc
                    }
                }
            } else {
                autoAppliedRule = null
                // Apply offline smart merchant predefined rules
                val lowercaseNote = trimmedNote.lowercase(Locale.getDefault())
                var suggestionCategory: String? = null
                var suggestionPaymentMethod: String? = null
                
                if (lowercaseNote.contains("esselunga") || lowercaseNote.contains("conad") || lowercaseNote.contains("coop") || lowercaseNote.contains("spesa") || lowercaseNote.contains("cibo")) {
                    suggestionCategory = "Food"
                } else if (lowercaseNote.contains("amazon") || lowercaseNote.contains("zara")) {
                    suggestionCategory = "Shopping"
                } else if (lowercaseNote.contains("mcdonald") || lowercaseNote.contains("mcdonald's") || lowercaseNote.contains("pizza") || lowercaseNote.contains("ristorante") || lowercaseNote.contains("cena")) {
                    suggestionCategory = "Food"
                } else if (lowercaseNote.contains("eni") || lowercaseNote.contains("q8") || lowercaseNote.contains("benzina")) {
                    suggestionCategory = "Transport"
                    suggestionPaymentMethod = "Card"
                } else if (lowercaseNote.contains("netflix") || lowercaseNote.contains("spotify") || lowercaseNote.contains("prime")) {
                    suggestionCategory = "Bills"
                    suggestionPaymentMethod = "Card"
                } else if (lowercaseNote.contains("affitto")) {
                    suggestionCategory = "Bills"
                    suggestionPaymentMethod = "Bank Transfer"
                } else if (lowercaseNote.contains("stipendio") || lowercaseNote.contains("salary")) {
                    suggestionCategory = "Salary"
                    type = TransactionType.INCOME
                    suggestionPaymentMethod = "Bank Transfer"
                }
                
                if (suggestionCategory != null) {
                    selectedCategory = suggestionCategory
                    if (suggestionPaymentMethod != null) {
                        paymentMethod = suggestionPaymentMethod
                    }
                    val matchingPast = transactionsList.filter { it.note.contains(trimmedNote, ignoreCase = true) }
                    if (matchingPast.isNotEmpty()) {
                        val mostFreqAccountId = matchingPast.groupBy { it.accountId }.maxByOrNull { it.value.size }?.key
                        val acc = filteredAccounts.find { it.id == mostFreqAccountId }
                        if (acc != null) {
                            selectedAccount = acc
                        }
                    }
                }
            }
            lastNoteChecked = trimmedNote
        }
    }

    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

    val categories = listOf(
        "Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Travel", "Salary", "Other"
    )

    val paymentMethods = listOf("Card", "Cash", "Bank Transfer")

    Scaffold(
        containerColor = BackgroundWarm,
        topBar = {
            TopAppBar(
                title = { Text("Nuova Transazione", fontWeight = FontWeight.Black, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("add_tx_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.White)
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
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section: Preferiti (Favorites)
            val defaultFavorites = listOf(
                FavoriteTx("☕", 2.50, "Colazione al bar", "Food", TransactionType.EXPENSE, "Cash"),
                FavoriteTx("🍕", 15.00, "Cena Pizza", "Food", TransactionType.EXPENSE, "Card"),
                FavoriteTx("⛽", 50.00, "Benzina auto", "Transport", TransactionType.EXPENSE, "Card"),
                FavoriteTx("🎬", 17.99, "Netflix", "Bills", TransactionType.EXPENSE, "Card"),
                FavoriteTx("🛍️", 45.00, "Spesa Esselunga", "Food", TransactionType.EXPENSE, "Card")
            )
            val allFavorites = remember(customFavoritesList) {
                defaultFavorites + customFavoritesList
            }

            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    text = "PREFERITI (1 TAP PER INSERIRE)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allFavorites) { fav ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(SurfaceWhite)
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .clickable {
                                    amount = fav.amount.toString()
                                    note = fav.note
                                    selectedCategory = fav.category
                                    type = fav.type
                                    paymentMethod = fav.paymentMethod
                                    if (selectedAccount == null && filteredAccounts.isNotEmpty()) {
                                        selectedAccount = filteredAccounts.firstOrNull()
                                    }
                                    accountError = null
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(fav.emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(fav.note, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%,.2f", fav.amount)} $currency",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section: Quick Add (Inserimento Rapido)
            val keyboardController = LocalSoftwareKeyboardController.current
            val parsedQuickResult = remember(quickAddText, filteredAccounts, transactionsList) {
                if (quickAddText.isNotBlank()) {
                    parseQuickAddInput(quickAddText, filteredAccounts, transactionsList)
                } else null
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceElevation),
                border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "INSERIMENTO RAPIDO SMART",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676),
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "es. \"25 Esselunga\" o \"42 benzina\"",
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quickAddText,
                        onValueChange = { quickAddText = it },
                        placeholder = { Text("Scrivi qui l'importo e la nota...", fontSize = 13.sp, color = TextSecondary.copy(alpha = 0.7f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676).copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                            focusedContainerColor = SurfaceWhite,
                            unfocusedContainerColor = SurfaceWhite,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )

                    // Quick Add Parsing Results Action Chip
                    AnimatedVisibility(
                        visible = parsedQuickResult != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        parsedQuickResult?.let { parsed ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF00E676).copy(alpha = 0.08f))
                                    .border(1.dp, Color(0xFF00E676).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        amount = parsed.amount.toString()
                                        note = parsed.note
                                        selectedCategory = parsed.category
                                        type = parsed.type
                                        paymentMethod = parsed.paymentMethod
                                        if (parsed.account != null) {
                                            selectedAccount = parsed.account
                                        } else if (selectedAccount == null && filteredAccounts.isNotEmpty()) {
                                            selectedAccount = filteredAccounts.firstOrNull()
                                        }
                                        accountError = null
                                        quickAddText = ""
                                        keyboardController?.hide()
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(
                                            text = "Tocca per confermare ed applicare:",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val categoryIcon = getCategoryMeta(parsed.category, parsed.type == TransactionType.INCOME).first
                                            Icon(
                                                imageVector = categoryIcon,
                                                contentDescription = null,
                                                tint = Color(0xFF00E676),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${parsed.note} • ${String.format(Locale.getDefault(), "%,.2f", parsed.amount)} $currency (${parsed.category})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Conferma",
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Transaction Type Selector (Tab Pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceElevation)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                // Expense Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (type == TransactionType.EXPENSE) Color(0xFF00E676).copy(alpha = 0.15f) else Color.Transparent)
                        .clickable {
                            type = TransactionType.EXPENSE
                            if (selectedCategory == "Salary") selectedCategory = "Food"
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Uscita",
                        fontWeight = FontWeight.Bold,
                        color = if (type == TransactionType.EXPENSE) Color(0xFF00E676) else TextSecondary,
                        fontSize = 14.sp
                    )
                }

                // Income Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (type == TransactionType.INCOME) Color(0xFF00E676).copy(alpha = 0.15f) else Color.Transparent)
                        .clickable {
                            type = TransactionType.INCOME
                            selectedCategory = "Salary"
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Entrata",
                        fontWeight = FontWeight.Bold,
                        color = if (type == TransactionType.INCOME) Color(0xFF00E676) else TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }

            // Amount Input Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("IMPORTO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text(
                            text = currency,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = amount,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.toDoubleOrNull() != null || input == ".") {
                                    amount = input
                                }
                            },
                            placeholder = { Text("0.00", color = Color.White.copy(alpha = 0.2f)) },
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color(0xFF00E676)
                            ),
                            modifier = Modifier
                                .width(200.dp)
                                .testTag("transaction_amount_field")
                        )
                    }
                }
            }

            // Category Selection Label
            Text("Categoria", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            // Category Icons Selector Grid
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val filteredCategories = if (type == TransactionType.INCOME) {
                        listOf("Salary", "Other")
                    } else {
                        categories.filter { it != "Salary" }
                    }

                    val chunks = filteredCategories.chunked(4)
                    chunks.forEach { rowCategories ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowCategories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                val (icon, tint) = getCategoryMeta(cat, type == TransactionType.INCOME)

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) tint.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { selectedCategory = cat }
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) tint else Color.White.copy(alpha = 0.05f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = cat,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) tint else TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                            if (rowCategories.size < 4) {
                                repeat(4 - rowCategories.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = autoAppliedRule != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                autoAppliedRule?.let { rule ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF00E676).copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Categoria proposta automaticamente",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                                Text(
                                    text = "Regola applicata per \"${rule.merchantName}\" → $selectedCategory",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Da quale account? selection
            Text("Da quale account?", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    border = BorderStroke(
                        width = if (accountError != null) 1.6.dp else 1.dp,
                        color = if (accountError != null) ExpenseRed else Color.White.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showAccountDropdown = true }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        selectedAccount?.colore?.let { Color(android.graphics.Color.parseColor(it)) }?.copy(alpha = 0.15f)
                                            ?: Color.White.copy(alpha = 0.05f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedAccount?.icona ?: "🏦",
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = selectedAccount?.nome ?: "Seleziona metodo di pagamento",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = selectedAccount?.let { "${it.saldo} $currency" } ?: "Seleziona un metodo di pagamento per continuare.",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = Color.White
                        )
                    }
                }

                DropdownMenu(
                    expanded = showAccountDropdown,
                    onDismissRequest = { showAccountDropdown = false },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .background(SurfaceWhite)
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                ) {
                    filteredAccounts.forEach { acc ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(android.graphics.Color.parseColor(acc.colore)).copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = acc.icona, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(acc.nome, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                        Text("${acc.saldo} $currency", color = TextSecondary, fontSize = 11.sp)
                                    }
                                }
                            },
                            onClick = {
                                selectedAccount = acc
                                accountError = null
                                showAccountDropdown = false
                            },
                            modifier = Modifier.background(SurfaceWhite)
                        )
                    }
                }
            }

            // Note field
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota / Descrizione") },
                    placeholder = { Text("es. Spesa Esselunga") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00E676),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                        focusedLabelColor = Color(0xFF00E676),
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_note_field")
                )

                // 1. Smart Note Auto-Suggestions (when input is short or empty)
                if (note.trim().length < 2) {
                    val quickSuggs = listOf("Esselunga", "Amazon", "McDonald's", "Eni", "Q8", "Netflix", "Spotify", "Affitto", "Stipendio")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Suggeriti:",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(quickSuggs) { sugg ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceWhite)
                                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .clickable { note = sugg }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(sugg, fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // 2. Ultime Transazioni Simili (Duplicate Past similar transaction in 1 tap)
                val similarPast = remember(note, transactionsList) {
                    val query = note.trim()
                    if (query.length >= 2) {
                        transactionsList.filter { 
                            it.note.contains(query, ignoreCase = true) 
                        }.distinctBy { it.note }.take(3)
                    } else {
                        emptyList()
                    }
                }

                if (similarPast.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "DUPLICA TRANSAZIONE PASSATA (1 TAP):",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                            letterSpacing = 0.8.sp
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(similarPast) { tx ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF00E676).copy(alpha = 0.05f))
                                        .border(1.dp, Color(0xFF00E676).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .clickable {
                                            amount = tx.amount.toString()
                                            note = tx.note
                                            selectedCategory = tx.category
                                            type = tx.type
                                            paymentMethod = tx.paymentMethod
                                            if (tx.accountId != null) {
                                                val matchingAcc = filteredAccounts.find { it.id == tx.accountId }
                                                if (matchingAcc != null) {
                                                    selectedAccount = matchingAcc
                                                }
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = getCategoryMeta(tx.category, tx.type == TransactionType.INCOME).first,
                                            contentDescription = null,
                                            tint = Color(0xFF00E676),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(tx.note, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            Text("${tx.amount} $currency (${tx.category})", fontSize = 9.sp, color = TextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Smart Tags: Optional Tags Selector
                val availableTags = listOf("Lavoro", "Vacanza", "Casa", "Auto", "Regalo")
                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text(
                        text = "TAG OPZIONALI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        availableTags.forEach { tag ->
                            val isSelected = selectedTags.contains(tag)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100))
                                    .background(if (isSelected) Color(0xFF00E676).copy(alpha = 0.15f) else SurfaceWhite)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(100)
                                    )
                                    .clickable {
                                        selectedTags = if (isSelected) {
                                            selectedTags - tag
                                        } else {
                                            selectedTags + tag
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF00E676) else TextSecondary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "#$tag",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF00E676) else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Recurring suggestion check & picker
                val isRecurringKeyword = remember(note) {
                    val ln = note.trim().lowercase(Locale.getDefault())
                    ln.contains("netflix") || ln.contains("spotify") || ln.contains("affitto") || ln.contains("stipendio") || ln.contains("bolletta") || ln.contains("bollette") || ln.contains("prime") || ln.contains("assicurazione")
                }

                if (isRecurringKeyword || isRecurringSelected) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceElevation),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Vuoi rendere questa transazione ricorrente?",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Switch(
                                    checked = isRecurringSelected,
                                    onCheckedChange = { isRecurringSelected = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = Color(0xFF00E676)
                                    )
                                )
                            }
                            
                            AnimatedVisibility(
                                visible = isRecurringSelected,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                                    Text("Frequenza di ricorrenza:", fontSize = 11.sp, color = TextSecondary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val frequencies = listOf("Giornaliera", "Settimanale", "Mensile", "Annuale", "Personalizzata")
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(frequencies) { freq ->
                                            val isFreqSelected = recurringFrequency == freq
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isFreqSelected) Color(0xFF00E676).copy(alpha = 0.15f) else SurfaceWhite)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isFreqSelected) Color(0xFF00E676) else Color.White.copy(alpha = 0.05f),
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable { recurringFrequency = freq }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = freq,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isFreqSelected) Color(0xFF00E676) else TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Date picker
            OutlinedTextField(
                value = sdf.format(Date(selectedDate)),
                onValueChange = {},
                label = { Text("Data") },
                readOnly = true,
                leadingIcon = {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "Calendario",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.clickable {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(year, month, dayOfMonth)
                                    selectedDate = calendar.timeInMillis
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E676),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                    focusedLabelColor = Color(0xFF00E676),
                    unfocusedLabelColor = TextSecondary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                calendar.set(year, month, dayOfMonth)
                                selectedDate = calendar.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
            )

            // Payment method selection
            Text("Metodo di Pagamento", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentMethods.forEach { method ->
                    val isSelected = paymentMethod == method
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) Color(0xFF00E676).copy(alpha = 0.15f) else SurfaceWhite)
                            .border(1.dp, if (isSelected) Color(0xFF00E676).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                            .clickable { paymentMethod = method }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = method,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF00E676) else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Validation Feedback Banners
            if (amountError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(amountError!!, fontSize = 12.sp, color = ExpenseRed, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (noteError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(noteError!!, fontSize = 12.sp, color = ExpenseRed, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (accountError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ExpenseRed.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(accountError!!, fontSize = 12.sp, color = ExpenseRed, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // Save as Favorite custom option
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceWhite)
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                    .clickable { saveAsFavorite = !saveAsFavorite }
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (saveAsFavorite) Color(0xFF00E676) else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Salva nei Preferiti", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Mostra questa transazione nella barra rapida", fontSize = 10.sp, color = TextSecondary)
                    }
                }
                Checkbox(
                    checked = saveAsFavorite,
                    onCheckedChange = { saveAsFavorite = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF00E676),
                        checkmarkColor = Color.Black
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val doubleAmount = amount.toDoubleOrNull()
                    var hasError = false
                    if (doubleAmount == null || doubleAmount <= 0) {
                        amountError = "Inserisci un importo valido maggiore di zero."
                        hasError = true
                    } else {
                        amountError = null
                    }
                    if (note.trim().isEmpty()) {
                        noteError = "La descrizione della transazione è obbligatoria."
                        hasError = true
                    } else {
                        noteError = null
                    }
                    if (selectedAccount == null) {
                        accountError = "L'account della transazione è obbligatorio."
                        hasError = true
                    } else {
                        accountError = null
                    }
                    if (hasError) return@Button

                    val tagsSuffix = if (selectedTags.isNotEmpty()) {
                        " " + selectedTags.joinToString(" ") { "#$it" }
                    } else ""
                    val recurringSuffix = if (isRecurringSelected) {
                        " [Ricorrente: $recurringFrequency]"
                    } else ""
                    val finalNote = "${note.trim()}$tagsSuffix$recurringSuffix"

                    if (saveAsFavorite) {
                        val emoji = when (selectedCategory) {
                            "Food" -> "🍔"
                            "Transport" -> "🚗"
                            "Shopping" -> "🛍️"
                            "Bills" -> "💳"
                            "Entertainment" -> "🎬"
                            "Health" -> "🏥"
                            "Travel" -> "✈️"
                            "Salary" -> "💰"
                            else -> "🏷️"
                        }
                        val newFav = FavoriteTx(
                            emoji = emoji,
                            amount = doubleAmount!!,
                            note = note.trim(),
                            category = selectedCategory,
                            type = type,
                            paymentMethod = paymentMethod
                        )
                        saveFavorite(context, newFav)
                    }

                    val matchingRule = rulesList.filter { it.enabled }.firstOrNull { rule ->
                        (rule.merchantName.isNotBlank() && note.trim().contains(rule.merchantName, ignoreCase = true)) ||
                        (rule.containsText.isNotBlank() && note.trim().contains(rule.containsText, ignoreCase = true))
                    }

                    if (matchingRule != null && matchingRule.categoryId == selectedCategory) {
                        if (isRecurringSelected) {
                            recurringViewModel.addRecurring(
                                note = note.trim(),
                                amount = doubleAmount!!,
                                category = selectedCategory,
                                type = type,
                                paymentMethod = paymentMethod,
                                accountId = selectedAccount?.id,
                                frequency = recurringFrequency
                            )
                        }
                        viewModel.addTransaction(
                            amount = doubleAmount!!,
                            type = type,
                            category = selectedCategory,
                            dateMillis = selectedDate,
                            note = finalNote,
                            paymentMethod = paymentMethod,
                            accountId = selectedAccount?.id
                        )
                        onNavigateBack()
                    } else {
                        showSaveRuleDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button")
            ) {
                Text("Salva Transazione", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showSaveRuleDialog) {
        AlertDialog(
            onDismissRequest = {
                showSaveRuleDialog = false
            },
            containerColor = SurfaceWhite,
            title = {
                Text(
                    text = "Ricorda questa scelta?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "La prossima volta che inserirai \"${note.trim()}\", CashMate proporrà automaticamente la categoria \"$selectedCategory\".",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                    onClick = {
                        val doubleAmount = amount.toDoubleOrNull() ?: 0.0
                        ruleViewModel.addRule(
                            merchantName = note.trim(),
                            containsText = note.trim(),
                            categoryId = selectedCategory,
                            accountId = selectedAccount?.id,
                            priority = 1,
                            enabled = true
                        )
                        val tagsSuffix = if (selectedTags.isNotEmpty()) {
                            " " + selectedTags.joinToString(" ") { "#$it" }
                        } else ""
                        val recurringSuffix = if (isRecurringSelected) {
                            " [Ricorrente: $recurringFrequency]"
                        } else ""
                        val finalNote = "${note.trim()}$tagsSuffix$recurringSuffix"

                        if (isRecurringSelected && doubleAmount > 0) {
                            recurringViewModel.addRecurring(
                                note = note.trim(),
                                amount = doubleAmount,
                                category = selectedCategory,
                                type = type,
                                paymentMethod = paymentMethod,
                                accountId = selectedAccount?.id,
                                frequency = recurringFrequency
                            )
                        }

                        viewModel.addTransaction(
                            amount = doubleAmount,
                            type = type,
                            category = selectedCategory,
                            dateMillis = selectedDate,
                            note = finalNote,
                            paymentMethod = paymentMethod,
                            accountId = selectedAccount?.id
                        )
                        showSaveRuleDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Sempre", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val doubleAmount = amount.toDoubleOrNull() ?: 0.0
                        val tagsSuffix = if (selectedTags.isNotEmpty()) {
                            " " + selectedTags.joinToString(" ") { "#$it" }
                        } else ""
                        val recurringSuffix = if (isRecurringSelected) {
                            " [Ricorrente: $recurringFrequency]"
                        } else ""
                        val finalNote = "${note.trim()}$tagsSuffix$recurringSuffix"

                        if (isRecurringSelected && doubleAmount > 0) {
                            recurringViewModel.addRecurring(
                                note = note.trim(),
                                amount = doubleAmount,
                                category = selectedCategory,
                                type = type,
                                paymentMethod = paymentMethod,
                                accountId = selectedAccount?.id,
                                frequency = recurringFrequency
                            )
                        }

                        viewModel.addTransaction(
                            amount = doubleAmount,
                            type = type,
                            category = selectedCategory,
                            dateMillis = selectedDate,
                            note = finalNote,
                            paymentMethod = paymentMethod,
                            accountId = selectedAccount?.id
                        )
                        showSaveRuleDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Solo questa volta", color = TextSecondary)
                }
            }
        )
    }
}
