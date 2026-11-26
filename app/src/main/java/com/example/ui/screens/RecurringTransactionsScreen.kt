package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.Account
import com.example.domain.model.RecurringTransaction
import com.example.domain.model.TransactionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.AccountViewModel
import com.example.ui.viewmodel.RecurringTransactionViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTransactionsScreen(
    onNavigateBack: () -> Unit
) {
    val recurringViewModel: RecurringTransactionViewModel = viewModel()
    val accountViewModel: AccountViewModel = viewModel()

    val recurringList by recurringViewModel.recurringTransactions.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val filteredAccounts = remember(accounts) {
        accounts.filter { acc ->
            val nameLower = acc.nome.trim().lowercase()
            nameLower != "tutti i conti" && nameLower != "tutto" && nameLower != "tutti" && nameLower != "all" && nameLower != "all_accounts"
        }
    }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedForEdit by remember { mutableStateOf<RecurringTransaction?>(null) }

    // Dialog state fields
    var noteField by remember { mutableStateOf("") }
    var amountField by remember { mutableStateOf("") }
    var selectedCategoryField by remember { mutableStateOf("Food") }
    var selectedTypeField by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedPaymentMethodField by remember { mutableStateOf("Card") }
    var selectedAccountField by remember { mutableStateOf<Account?>(null) }
    var selectedFrequencyField by remember { mutableStateOf("Mensile") }
    var accountFieldError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = BackgroundWarm,
        topBar = {
            TopAppBar(
                title = { Text("Transazioni Ricorrenti", fontWeight = FontWeight.Black, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("recurring_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWarm)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedForEdit = null
                    noteField = ""
                    amountField = ""
                    selectedCategoryField = "Food"
                    selectedTypeField = TransactionType.EXPENSE
                    selectedPaymentMethodField = "Card"
                    selectedAccountField = filteredAccounts.firstOrNull()
                    selectedFrequencyField = "Mensile"
                    accountFieldError = null
                    showAddEditDialog = true
                },
                containerColor = Color(0xFF00E676),
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_recurring_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuova Ricorrenza")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Movimenti Ricorrenti offline",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (recurringList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Nessun movimento ricorrente.",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Puoi pianificarne uno da qui o spuntando \"Ricorrente\" quando aggiungi una nuova transazione.",
                                fontSize = 12.sp,
                                color = TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(recurringList, key = { it.id }) { item ->
                    val (icon, tint) = getCategoryMeta(item.category, item.type == TransactionType.INCOME)
                    val ruleAccount = filteredAccounts.find { it.id == item.accountId }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(tint.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = tint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = item.note,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${item.frequency} • ${item.category}",
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                            if (ruleAccount != null) {
                                                Text("•", color = TextSecondary, fontSize = 11.sp)
                                                Text(
                                                    text = "${ruleAccount.icona} ${ruleAccount.nome}",
                                                    fontSize = 11.sp,
                                                    color = TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    val formattedAmount = String.format(Locale.getDefault(), "%,.2f", item.amount)
                                    val sign = if (item.type == TransactionType.INCOME) "+" else "-"
                                    val amtColor = if (item.type == TransactionType.INCOME) Color(0xFF00E676) else Color.White
                                    Text(
                                        text = "$sign $formattedAmount €",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = amtColor
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Switch(
                                        checked = item.enabled,
                                        onCheckedChange = { isChecked ->
                                            recurringViewModel.toggleRecurring(item, isChecked)
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF00E676),
                                            checkedTrackColor = Color(0xFF00E676).copy(alpha = 0.3f),
                                            uncheckedThumbColor = TextSecondary,
                                            uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = Color.White.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = {
                                        selectedForEdit = item
                                        noteField = item.note
                                        amountField = item.amount.toString()
                                        selectedCategoryField = item.category
                                        selectedTypeField = item.type
                                        selectedPaymentMethodField = item.paymentMethod
                                        selectedAccountField = filteredAccounts.find { it.id == item.accountId } ?: filteredAccounts.firstOrNull()
                                        selectedFrequencyField = item.frequency
                                        accountFieldError = null
                                        showAddEditDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Modifica", tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Modifica", color = Color(0xFF00E676), fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                TextButton(
                                    onClick = {
                                        recurringViewModel.deleteRecurring(item.id)
                                    }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Elimina", color = ExpenseRed, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showAddEditDialog) {
        var categoryDropdownExpanded by remember { mutableStateOf(false) }
        var accountDropdownExpanded by remember { mutableStateOf(false) }
        var frequencyDropdownExpanded by remember { mutableStateOf(false) }

        val categoriesList = listOf(
            "Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Travel", "Salary", "Other"
        )
        val frequenciesList = listOf("Giornaliera", "Settimanale", "Mensile", "Annuale", "Personalizzata")
        val paymentMethods = listOf("Card", "Cash", "Bank Transfer")

        Dialog(onDismissRequest = { showAddEditDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (selectedForEdit == null) "Nuova Ricorrenza" else "Modifica Ricorrenza",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Note description
                    OutlinedTextField(
                        value = noteField,
                        onValueChange = { noteField = it },
                        label = { Text("Nota / Descrizione") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            focusedLabelColor = Color(0xFF00E676),
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Amount
                    OutlinedTextField(
                        value = amountField,
                        onValueChange = { amountField = it },
                        label = { Text("Importo") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00E676),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                            focusedLabelColor = Color(0xFF00E676),
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Entry / Exit Type
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BackgroundWarm)
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTypeField == TransactionType.EXPENSE) Color(0xFF00E676).copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { selectedTypeField = TransactionType.EXPENSE }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Uscita", color = if (selectedTypeField == TransactionType.EXPENSE) Color(0xFF00E676) else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selectedTypeField == TransactionType.INCOME) Color(0xFF00E676).copy(alpha = 0.15f) else Color.Transparent)
                                .clickable { selectedTypeField = TransactionType.INCOME }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Entrata", color = if (selectedTypeField == TransactionType.INCOME) Color(0xFF00E676) else TextSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Frequency Selector
                    Text("Frequenza", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { frequencyDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(selectedFrequencyField, color = Color.White, fontSize = 14.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        }

                        DropdownMenu(
                            expanded = frequencyDropdownExpanded,
                            onDismissRequest = { frequencyDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .background(SurfaceWhite)
                        ) {
                            frequenciesList.forEach { freq ->
                                DropdownMenuItem(
                                    text = { Text(freq, color = Color.White) },
                                    onClick = {
                                        selectedFrequencyField = freq
                                        frequencyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Category Selector
                    Text("Categoria", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { categoryDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val (icon, tint) = getCategoryMeta(selectedCategoryField, selectedTypeField == TransactionType.INCOME)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(selectedCategoryField, color = Color.White, fontSize = 14.sp)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        }

                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .background(SurfaceWhite)
                        ) {
                            categoriesList.forEach { cat ->
                                val (icon, tint) = getCategoryMeta(cat, selectedTypeField == TransactionType.INCOME)
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(cat, color = Color.White, fontSize = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        selectedCategoryField = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Account Selector
                    Text("Account", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
                            border = BorderStroke(
                                width = if (accountFieldError != null) 1.6.dp else 1.dp,
                                color = if (accountFieldError != null) ExpenseRed else Color.White.copy(alpha = 0.08f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { accountDropdownExpanded = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(selectedAccountField?.icona ?: "🏦", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(selectedAccountField?.nome ?: "Seleziona Account", color = Color.White, fontSize = 14.sp)
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                            }
                        }

                        DropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.75f)
                                .background(SurfaceWhite)
                        ) {
                            filteredAccounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(acc.icona, fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(acc.nome, color = Color.White)
                                        }
                                    },
                                    onClick = {
                                        selectedAccountField = acc
                                        accountFieldError = null
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    if (accountFieldError != null) {
                        Text(accountFieldError!!, color = ExpenseRed, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    // Payment method selection
                    Text("Metodo di Pagamento", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paymentMethods.forEach { method ->
                            val isSelected = selectedPaymentMethodField == method
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF00E676).copy(alpha = 0.15f) else SurfaceWhite)
                                    .border(1.dp, if (isSelected) Color(0xFF00E676).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .clickable { selectedPaymentMethodField = method }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = method,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF00E676) else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showAddEditDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Annulla", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                val amountVal = amountField.toDoubleOrNull() ?: 0.0
                                var hasError = false
                                if (noteField.isBlank() || amountVal <= 0.0 || selectedAccountField == null) {
                                    if (selectedAccountField == null) {
                                        accountFieldError = "L'account è obbligatorio."
                                    }
                                    hasError = true
                                }
                                if (!hasError) {
                                    val item = selectedForEdit
                                    if (item != null) {
                                        recurringViewModel.updateRecurring(
                                            item.copy(
                                                note = noteField,
                                                amount = amountVal,
                                                category = selectedCategoryField,
                                                type = selectedTypeField,
                                                paymentMethod = selectedPaymentMethodField,
                                                accountId = selectedAccountField?.id,
                                                frequency = selectedFrequencyField
                                            )
                                        )
                                    } else {
                                        recurringViewModel.addRecurring(
                                            note = noteField,
                                            amount = amountVal,
                                            category = selectedCategoryField,
                                            type = selectedTypeField,
                                            paymentMethod = selectedPaymentMethodField,
                                            accountId = selectedAccountField?.id,
                                            frequency = selectedFrequencyField
                                        )
                                    }
                                    showAddEditDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Salva", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
