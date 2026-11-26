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
import com.example.domain.model.TransactionRule
import com.example.domain.model.Transaction
import com.example.ui.theme.*
import com.example.ui.viewmodel.AccountViewModel
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.TransactionRuleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomaticRulesScreen(
    onNavigateBack: () -> Unit
) {
    val ruleViewModel: TransactionRuleViewModel = viewModel()
    val financeViewModel: FinanceViewModel = viewModel()
    val accountViewModel: AccountViewModel = viewModel()

    val rules by ruleViewModel.rules.collectAsStateWithLifecycle()
    val transactions by financeViewModel.transactions.collectAsStateWithLifecycle()
    val accounts by accountViewModel.accounts.collectAsStateWithLifecycle()
    val filteredAccounts = remember(accounts) {
        accounts.filter { acc ->
            val nameLower = acc.nome.trim().lowercase()
            nameLower != "tutti i conti" && nameLower != "tutto" && nameLower != "tutti" && nameLower != "all" && nameLower != "all_accounts"
        }
    }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedRuleForEdit by remember { mutableStateOf<TransactionRule?>(null) }

    // Dialog form state
    var merchantNameField by remember { mutableStateOf("") }
    var containsTextField by remember { mutableStateOf("") }
    var selectedCategoryField by remember { mutableStateOf("Food") }
    var selectedAccountField by remember { mutableStateOf<Account?>(null) }
    var priorityField by remember { mutableStateOf(1) }

    // AI suggestions calculation
    val aiSuggestions = remember(transactions, rules) {
        val nonSalaryTxs = transactions.filter { it.category != "Salary" && it.note.isNotBlank() }
        val grouped = nonSalaryTxs.groupBy { it.note.trim().lowercase() }
        
        grouped.mapNotNull { (noteLower, txList) ->
            if (txList.size >= 2) {
                // Find most frequent category for this note
                val catCounts = txList.groupBy { it.category }.mapValues { it.value.size }
                val maxCat = catCounts.maxByOrNull { it.value }?.key
                val count = catCounts[maxCat] ?: 0
                
                if (maxCat != null) {
                    // Check if a rule already exists for this note
                    val ruleExists = rules.any { rule ->
                        noteLower.contains(rule.merchantName.lowercase()) ||
                        noteLower.contains(rule.containsText.lowercase())
                    }
                    if (!ruleExists) {
                        val originalNote = txList.first().note
                        SuggestionInfo(
                            merchantName = originalNote,
                            category = maxCat,
                            count = count
                        )
                    } else null
                } else null
            } else null
        }
    }

    Scaffold(
        containerColor = BackgroundWarm,
        topBar = {
            TopAppBar(
                title = { Text("Regole Automatiche", fontWeight = FontWeight.Black, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("rules_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundWarm)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedRuleForEdit = null
                    merchantNameField = ""
                    containsTextField = ""
                    selectedCategoryField = "Food"
                    selectedAccountField = null
                    priorityField = 1
                    showAddEditDialog = true
                },
                containerColor = Color(0xFF00E676),
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_rule_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuova Regola")
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
            // AI SUGGESTIONS SECTION
            if (aiSuggestions.isNotEmpty()) {
                item {
                    Text(
                        text = "💡 Suggerimenti AI",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                items(aiSuggestions) { sug ->
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, Color(0xFF00E676).copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Hai classificato \"${sug.merchantName}\" come ${sug.category} ${sug.count} volte.",
                                    fontSize = 13.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Vuoi creare una regola automatica?",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    selectedRuleForEdit = null
                                    merchantNameField = sug.merchantName
                                    containsTextField = sug.merchantName
                                    selectedCategoryField = sug.category
                                    selectedAccountField = null
                                    priorityField = 1
                                    showAddEditDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Crea", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // MAIN RULES SECTION
            item {
                Text(
                    text = "Regole Attive",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            if (rules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Nessuna regola automatica configurata.",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Le regole imparano dalle tue scelte o puoi crearne di manuali.",
                                fontSize = 12.sp,
                                color = TextSecondary.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(rules, key = { it.id }) { rule ->
                    val (icon, tint) = getCategoryMeta(rule.categoryId, false)
                    val ruleAccount = filteredAccounts.find { it.id == rule.accountId }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
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
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(tint.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = tint,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = rule.merchantName,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "Categoria: ${rule.categoryId}",
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

                                Switch(
                                    checked = rule.enabled,
                                    onCheckedChange = { isChecked ->
                                        ruleViewModel.toggleRule(rule, isChecked)
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF00E676),
                                        checkedTrackColor = Color(0xFF00E676).copy(alpha = 0.3f),
                                        uncheckedThumbColor = TextSecondary,
                                        uncheckedTrackColor = Color.White.copy(alpha = 0.08f)
                                    )
                                )
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
                                        selectedRuleForEdit = rule
                                        merchantNameField = rule.merchantName
                                        containsTextField = rule.containsText
                                        selectedCategoryField = rule.categoryId
                                        selectedAccountField = filteredAccounts.find { it.id == rule.accountId }
                                        priorityField = rule.priority
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
                                        ruleViewModel.deleteRule(rule.id)
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

    // CREATE / EDIT DIALOG
    if (showAddEditDialog) {
        var categoryDropdownExpanded by remember { mutableStateOf(false) }
        var accountDropdownExpanded by remember { mutableStateOf(false) }

        val categoriesList = listOf(
            "Food", "Transport", "Shopping", "Bills", "Entertainment", "Health", "Travel", "Salary", "Other"
        )

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
                        text = if (selectedRuleForEdit == null) "Nuova Regola" else "Modifica Regola",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    // Merchant Name
                    OutlinedTextField(
                        value = merchantNameField,
                        onValueChange = { merchantNameField = it },
                        label = { Text("Nome Commerciante") },
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

                    // Contains Text
                    OutlinedTextField(
                        value = containsTextField,
                        onValueChange = { containsTextField = it },
                        label = { Text("Contiene Testo") },
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
                                val (icon, tint) = getCategoryMeta(selectedCategoryField, false)
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
                                val (icon, tint) = getCategoryMeta(cat, false)
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

                    // Account Selector (Optional)
                    Text("Account (Opzionale)", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BackgroundWarm),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
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
                                    Text(selectedAccountField?.nome ?: "Nessun Account automatico", color = Color.White, fontSize = 14.sp)
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
                            DropdownMenuItem(
                                text = { Text("Nessun Account automatico", color = TextSecondary) },
                                onClick = {
                                    selectedAccountField = null
                                    accountDropdownExpanded = false
                                }
                            )
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
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Priority
                    OutlinedTextField(
                        value = priorityField.toString(),
                        onValueChange = { priorityField = it.toIntOrNull() ?: 1 },
                        label = { Text("Priorità") },
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
                                if (merchantNameField.isNotBlank()) {
                                    val rule = selectedRuleForEdit
                                    if (rule != null) {
                                        ruleViewModel.updateRule(
                                            rule.copy(
                                                merchantName = merchantNameField,
                                                containsText = containsTextField.ifBlank { merchantNameField },
                                                categoryId = selectedCategoryField,
                                                accountId = selectedAccountField?.id,
                                                priority = priorityField
                                            )
                                        )
                                    } else {
                                        ruleViewModel.addRule(
                                            merchantName = merchantNameField,
                                            containsText = containsTextField.ifBlank { merchantNameField },
                                            categoryId = selectedCategoryField,
                                            accountId = selectedAccountField?.id,
                                            priority = priorityField
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

data class SuggestionInfo(
    val merchantName: String,
    val category: String,
    val count: Int
)
