package com.riccardopinato.cashmateai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.riccardopinato.cashmateai.data.MoneyTransactionEntity
import com.riccardopinato.cashmateai.data.PaymentAccountEntity
import com.riccardopinato.cashmateai.domain.FinanceMath
import com.riccardopinato.cashmateai.domain.TransactionKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val accent = Color(0xFF0D9279)
private val neutral = Color(0xFF456075)
private val softSurface = Color(0xFFE7F6F1)
private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ITALY)

private enum class Tab(val title: String, val marker: String) {
    HOME("Home", "⌂"),
    TRANSACTIONS("Movimenti", "≡"),
    ANALYTICS("Analisi", "▥"),
    SPLIT("Dividi", "÷"),
    SETTINGS("Altro", "⚙")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashMateApp(
    state: FinanceUiState,
    notice: String?,
    onNoticeDismiss: () -> Unit,
    onAddAccount: (String, Long) -> Unit,
    onAddTransaction: (String?, TransactionKind, Long, String, String) -> Unit,
    onDeleteTransaction: (String) -> Unit,
    onDarkThemeChange: (Boolean) -> Unit
) {
    val scheme = if (state.darkTheme) darkColorScheme(primary = Color(0xFF68E0BE))
        else lightColorScheme(primary = accent, secondary = neutral, surface = Color(0xFFF9FBFA))
    MaterialTheme(colorScheme = scheme) {
        var currentTabIndex by rememberSaveable { mutableIntStateOf(0) }
        var filterAccountId by rememberSaveable { mutableStateOf<String?>(null) }
        var addMovement by remember { mutableStateOf(false) }
        var addAccount by remember { mutableStateOf(false) }
        var deleteId by remember { mutableStateOf<String?>(null) }
        val hostState = remember { SnackbarHostState() }
        val tab = Tab.entries[currentTabIndex]
        val validAccountIds = state.accounts.map { it.id }.toSet()
        LaunchedEffect(validAccountIds) {
            if (filterAccountId != null && filterAccountId !in validAccountIds) filterAccountId = null
        }
        LaunchedEffect(notice) {
            if (!notice.isNullOrBlank()) {
                hostState.showSnackbar(notice)
                onNoticeDismiss()
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("CashMate", fontWeight = FontWeight.Bold)
                            Text("Il tuo denaro, senza complicazioni", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(hostState) },
            floatingActionButton = {
                if (tab == Tab.HOME || tab == Tab.TRANSACTIONS) {
                    FloatingActionButton(onClick = {
                        if (state.accounts.isEmpty()) addAccount = true else addMovement = true
                    }) { Text("+", style = MaterialTheme.typography.headlineSmall) }
                }
            },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = currentTabIndex == index,
                            onClick = { currentTabIndex = index },
                            icon = { Text(item.marker) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        ) { inset ->
            if (!state.loaded) {
                Box(Modifier.fillMaxSize().padding(inset), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(Modifier.fillMaxSize().padding(inset)) {
                    when (tab) {
                        Tab.HOME -> HomeTab(state, filterAccountId, { filterAccountId = it }, { addAccount = true })
                        Tab.TRANSACTIONS -> TransactionsTab(state, filterAccountId, { filterAccountId = it }, { deleteId = it })
                        Tab.ANALYTICS -> AnalyticsTab(state, filterAccountId, { filterAccountId = it })
                        Tab.SPLIT -> SplitTab()
                        Tab.SETTINGS -> SettingsTab(state, { addAccount = true }, onDarkThemeChange)
                    }
                }
            }
        }

        if (addMovement) {
            NewMovementDialog(
                accounts = state.accounts,
                onClose = { addMovement = false },
                onSave = onAddTransaction
            )
        }
        if (addAccount) {
            NewAccountDialog(onClose = { addAccount = false }, onSave = onAddAccount)
        }
        if (deleteId != null) {
            AlertDialog(
                onDismissRequest = { deleteId = null },
                title = { Text("Eliminare il movimento?") },
                text = { Text("L'operazione non può essere annullata. I saldi verranno ricalcolati.") },
                confirmButton = {
                    TextButton(onClick = {
                        deleteId?.let(onDeleteTransaction)
                        deleteId = null
                    }) { Text("Elimina") }
                },
                dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Annulla") } }
            )
        }
    }
}

@Composable
private fun AccountFilter(
    accounts: List<PaymentAccountEntity>,
    selectedAccountId: String?,
    onChange: (String?) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedAccountId == null,
            onClick = { onChange(null) },
            label = { Text("Tutti i conti") }
        )
        accounts.forEach { account ->
            FilterChip(
                selected = selectedAccountId == account.id,
                onClick = { onChange(account.id) },
                label = { Text(account.name, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyHint(title: String, description: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BalancePanel(
    accounts: List<PaymentAccountEntity>,
    transactions: List<MoneyTransactionEntity>,
    selectedAccountId: String?
) {
    val balance = FinanceMath.balanceCents(accounts, transactions, selectedAccountId)
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.primary == accent) softSurface else MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Saldo disponibile", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                FinanceMath.euro(balance),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (selectedAccountId == null) "Riepilogo di tutti i conti" else "Saldo del conto selezionato",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun MonthSummary(transactions: List<MoneyTransactionEntity>, accountId: String?) {
    val month = FinanceMath.monthTransactions(transactions, accountId)
    val income = month.filter { it.kind == TransactionKind.INCOME.name }.sumOf { it.amountCents }
    val expense = month.filter { it.kind == TransactionKind.EXPENSE.name }.sumOf { it.amountCents }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Entrate del mese", FinanceMath.euro(income), Modifier.weight(1f), accent)
        StatCard("Uscite del mese", FinanceMath.euro(expense), Modifier.weight(1f), Color(0xFFB15152))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Card(modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeTab(
    state: FinanceUiState,
    selectedAccountId: String?,
    onFilterChange: (String?) -> Unit,
    onAddAccount: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AccountFilter(state.accounts, selectedAccountId, onFilterChange)
            Spacer(Modifier.height(8.dp))
            if (state.accounts.isEmpty()) {
                EmptyHint("Inizia dal tuo primo conto", "Aggiungi un conto reale (per esempio carta o contanti) prima di registrare una transazione.")
                Spacer(Modifier.height(10.dp))
                Button(onClick = onAddAccount) { Text("Crea il primo conto") }
            } else {
                BalancePanel(state.accounts, state.transactions, selectedAccountId)
            }
        }
        if (state.accounts.isNotEmpty()) item { MonthSummary(state.transactions, selectedAccountId) }
        item { SectionTitle("Ultimi movimenti") }
        val visible = state.transactions.filter { selectedAccountId == null || it.accountId == selectedAccountId }.take(6)
        if (visible.isEmpty()) {
            item { EmptyHint("Nessun movimento", "Usa il pulsante + per aggiungere una spesa o un'entrata.") }
        } else {
            items(visible, key = { it.id }) { tx ->
                TransactionItem(tx, state.accounts)
            }
        }
        item { Spacer(Modifier.height(40.dp)) }
    }
}

@Composable
private fun TransactionItem(tx: MoneyTransactionEntity, accounts: List<PaymentAccountEntity>, onDelete: (() -> Unit)? = null) {
    val isIncome = tx.kind == TransactionKind.INCOME.name
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(tx.category, fontWeight = FontWeight.SemiBold, maxLines = 1)
                val accountName = accounts.firstOrNull { it.id == tx.accountId }?.name ?: "Conto non disponibile"
                val date = Instant.ofEpochMilli(tx.occurredAtMillis).atZone(ZoneId.systemDefault()).format(dateFormatter)
                Text(accountName + " · " + date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (tx.note.isNotBlank()) Text(tx.note, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (isIncome) "+" else "−") + FinanceMath.euro(tx.amountCents),
                    color = if (isIncome) accent else Color(0xFFB15152),
                    fontWeight = FontWeight.Bold
                )
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Elimina") }
            }
        }
    }
}

@Composable
private fun TransactionsTab(
    state: FinanceUiState,
    accountId: String?,
    onFilterChange: (String?) -> Unit,
    onDelete: (String) -> Unit
) {
    val visible = state.transactions.filter { accountId == null || it.accountId == accountId }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { AccountFilter(state.accounts, accountId, onFilterChange) }
        item { SectionTitle("Movimenti (" + visible.size + ")") }
        if (visible.isEmpty()) item { EmptyHint("Non ci sono movimenti", "Seleziona un conto o premi + per iniziare.") }
        items(visible, key = { it.id }) { tx ->
            TransactionItem(tx, state.accounts) { onDelete(tx.id) }
        }
        item { Spacer(Modifier.height(50.dp)) }
    }
}

@Composable
private fun AnalyticsTab(
    state: FinanceUiState,
    accountId: String?,
    onFilterChange: (String?) -> Unit
) {
    val current = FinanceMath.monthTransactions(state.transactions, accountId)
    val expenses = current.filter { it.kind == TransactionKind.EXPENSE.name }
    val byCategory = expenses.groupBy { it.category }.mapValues { (_, entries) -> entries.sumOf { it.amountCents } }
        .toList().sortedByDescending { it.second }
    val totalExpenses = expenses.sumOf { it.amountCents }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { AccountFilter(state.accounts, accountId, onFilterChange) }
        item { SectionTitle("Questo mese") }
        item { MonthSummary(state.transactions, accountId) }
        item { SectionTitle("Dove vanno i soldi") }
        if (byCategory.isEmpty()) {
            item { EmptyHint("Ancora nessuna analisi", "Inserisci alcune uscite per vedere la distribuzione per categoria.") }
        } else {
            items(byCategory) { (category, cents) ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(category, fontWeight = FontWeight.SemiBold)
                            Text(FinanceMath.euro(cents))
                        }
                        LinearProgressIndicator(
                            progress = { if (totalExpenses == 0L) 0f else (cents.toDouble() / totalExpenses).toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
        item {
            Text(
                "Dati calcolati esclusivamente dai movimenti locali. Non costituiscono consulenza finanziaria.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SplitTab() {
    var amount by rememberSaveable { mutableStateOf("") }
    var people by rememberSaveable { mutableStateOf("2") }
    val cents = FinanceMath.parsePositiveCents(amount)
    val count = people.toIntOrNull()
    val splits = if (cents != null && count != null && count in 2..100) FinanceMath.splitEqually(cents, count) else emptyList()
    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle("Dividi una spesa")
        Text("Calcola le quote senza arrotondamenti nascosti. I movimenti non vengono registrati automaticamente.")
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("Totale in €") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        OutlinedTextField(
            value = people,
            onValueChange = { people = it },
            label = { Text("Numero persone (2–100)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        if (splits.isNotEmpty()) {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionTitle("Quote calcolate")
                    Text("Quota principale: " + FinanceMath.euro(splits.first()))
                    val grouped = splits.groupingBy { it }.eachCount().toList().sortedByDescending { it.first }
                    grouped.forEach { (share, quantity) ->
                        Text(quantity.toString() + " persone × " + FinanceMath.euro(share))
                    }
                    HorizontalDivider()
                    Text("Controllo: " + FinanceMath.euro(splits.sum()), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    state: FinanceUiState,
    onAddAccount: () -> Unit,
    onDarkChange: (Boolean) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SectionTitle("Conti e impostazioni") }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Conti personali", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    state.accounts.forEach { account ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(account.name)
                            Text(FinanceMath.euro(FinanceMath.balanceCents(state.accounts, state.transactions, account.id)))
                        }
                    }
                    OutlinedButton(onClick = onAddAccount) { Text("Aggiungi conto") }
                    Text("«Tutti i conti» è solo una vista di riepilogo, non un metodo di pagamento.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Tema scuro", Modifier.weight(1f))
                    Switch(checked = state.darkTheme, onCheckedChange = onDarkChange)
                }
            }
        }
        item {
            EmptyHint(
                "Account e backup cloud",
                "Non configurati in questa build. Nessun accesso fittizio, nessun invio di dati e nessuna sincronizzazione dichiarata."
            )
        }
        item {
            EmptyHint(
                "AI Coach e Premium",
                "Non attivi: mancano provider, credenziali, verifiche privacy e integrazione acquisti. Decision Studio è ancora un'idea."
            )
        }
        item {
            Text("I dati finanziari sono conservati localmente su questo dispositivo. Disinstallare l'app può eliminarli. Non utilizzare per dati importanti senza un backup verificato.", style = MaterialTheme.typography.bodySmall)
        }
        item { Spacer(Modifier.height(30.dp)) }
    }
}

@Composable
private fun NewAccountDialog(
    onClose: () -> Unit,
    onSave: (String, Long) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var opening by remember { mutableStateOf("") }
    val cents = if (opening.isBlank() || opening.trim() == "0" || opening.trim() == "0,00") 0L
        else FinanceMath.parsePositiveCents(opening)
    val valid = name.trim().length in 2..60 && cents != null
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Nuovo conto reale") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Registra carta, conto corrente o contanti. Questo conto potrà ricevere i movimenti.")
                OutlinedTextField(name, { name = it }, label = { Text("Nome conto") }, singleLine = true)
                OutlinedTextField(
                    opening, { opening = it }, label = { Text("Saldo iniziale in € (facoltativo)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val value = cents ?: return@Button
                onSave(name, value)
                onClose()
            }, enabled = valid) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Annulla") } }
    )
}

@Composable
private fun NewMovementDialog(
    accounts: List<PaymentAccountEntity>,
    onClose: () -> Unit,
    onSave: (String?, TransactionKind, Long, String, String) -> Unit
) {
    var accountId by remember { mutableStateOf<String?>(null) }
    var kind by remember { mutableStateOf(TransactionKind.EXPENSE) }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val cents = FinanceMath.parsePositiveCents(amount)
    val validAccount = FinanceMath.validAccountId(accounts, accountId)
    val valid = cents != null && validAccount != null && category.trim().isNotBlank() &&
        category.length <= 60 && note.length <= 280

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Nuovo movimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = kind == TransactionKind.EXPENSE,
                        onClick = { kind = TransactionKind.EXPENSE },
                        label = { Text("Uscita") }
                    )
                    FilterChip(
                        selected = kind == TransactionKind.INCOME,
                        onClick = { kind = TransactionKind.INCOME },
                        label = { Text("Entrata") }
                    )
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Importo in €") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (facoltativa)") },
                    minLines = 1,
                    maxLines = 2
                )
                Text("Metodo di pagamento obbligatorio", fontWeight = FontWeight.SemiBold)
                Column(
                    Modifier.height(130.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    accounts.forEach { account ->
                        FilterChip(
                            selected = accountId == account.id,
                            onClick = { accountId = account.id },
                            label = { Text(account.name) }
                        )
                    }
                }
                if (validAccount == null) {
                    Text("Seleziona un conto reale; non puoi registrare su «Tutti i conti».",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    val value = cents ?: return@Button
                    onSave(validAccount, kind, value, category, note)
                    onClose()
                }
            ) { Text("Salva") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Annulla") } }
    )
}
