package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.RateLimitExceededException
import com.example.data.local.AppDatabase
import com.example.data.local.UserSettingsStore
import com.example.data.remote.FirebaseAuthService
import com.example.data.repository.FinanceRepository
import com.example.domain.model.*
import com.example.ai.GeminiAiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AiCoachUiState {
    object Idle : AiCoachUiState
    object Loading : AiCoachUiState
    data class Success(val response: AiCoachResponse) : AiCoachUiState
    data class Error(val message: String) : AiCoachUiState
}

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FinanceRepository
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // UI state flows
    val currentUser: StateFlow<UserProfile?>
    val transactions: StateFlow<List<Transaction>>
    val budgets: StateFlow<List<Budget>>
    val splitBills: StateFlow<List<SplitBill>>
    val isPremium: StateFlow<Boolean>
    val currency: StateFlow<String>
    val isAutoSyncEnabled: StateFlow<Boolean>
    val lastBackupTimestamp: StateFlow<Long>
    val connectivityStatus: StateFlow<com.example.data.remote.ConnectivityObserver.Status>

    private val _aiCoachState = MutableStateFlow<AiCoachUiState>(AiCoachUiState.Idle)
    val aiCoachState: StateFlow<AiCoachUiState> = _aiCoachState.asStateFlow()

    private val _aiUsageLogs = MutableStateFlow<List<AiUsageLog>>(emptyList())
    val aiUsageLogs: StateFlow<List<AiUsageLog>> = _aiUsageLogs.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        val userSettingsStore = UserSettingsStore(application)
        val firebaseAuthService = FirebaseAuthService()
        val geminiAiService = GeminiAiService(database.aiUsageLogDao())

        val connectivityObserver = com.example.data.remote.NetworkConnectivityObserver(application)
        val firestoreService = com.example.data.remote.FirestoreService()
        val syncManager = com.example.data.remote.SyncManager(application, database, firestoreService, connectivityObserver)
        val backupRestoreManager = com.example.data.remote.BackupRestoreManager(application, database, firestoreService, userSettingsStore)

        repository = FinanceRepository(
            transactionDao = database.transactionDao(),
            splitBillDao = database.splitBillDao(),
            budgetDao = database.budgetDao(),
            userSettingsStore = userSettingsStore,
            firebaseAuthService = firebaseAuthService,
            geminiAiService = geminiAiService,
            syncManager = syncManager,
            backupRestoreManager = backupRestoreManager
        )

        // Bind flows from repository
        currentUser = repository.currentUser.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        transactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        budgets = repository.allBudgets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        splitBills = repository.allSplitBills.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        isPremium = repository.isPremium.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

        currency = repository.currency.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "€"
        )

        isAutoSyncEnabled = repository.isAutoSyncEnabled.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

        lastBackupTimestamp = repository.lastBackupTimestamp.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

        connectivityStatus = connectivityObserver.connectivityState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = com.example.data.remote.ConnectivityObserver.Status.Available
        )

        // Automatically sync when online and auto-sync is enabled
        viewModelScope.launch {
            combine(currentUser, connectivityStatus, isAutoSyncEnabled) { user, status, autoSync ->
                Triple(user, status, autoSync)
            }.collect { (user, status, autoSync) ->
                if (user != null && status == com.example.data.remote.ConnectivityObserver.Status.Available && autoSync) {
                    android.util.Log.d("FinanceViewModel", "Device is online & Auto-sync is enabled - initiating bidirectional sync for: ${user.displayName}")
                    repository.syncAllBidirectional(user.id)
                }
            }
        }

        // Observe AI usage logs directly
        viewModelScope.launch {
            database.aiUsageLogDao().getAllUsageLogs().collect { logs ->
                _aiUsageLogs.value = logs
            }
        }

        // Seed initial budgets if empty
        viewModelScope.launch {
            repository.allBudgets.first().let { currentBudgets ->
                if (currentBudgets.isEmpty()) {
                    repository.insertBudget(Budget("Food", 200.0))
                    repository.insertBudget(Budget("Shopping", 150.0))
                    repository.insertBudget(Budget("Entertainment", 100.0))
                }
            }
        }
    }

    // AUTH ACTIONS
    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.login(email, password)
                .onSuccess { 
                    onSuccess()
                    // Sync upon successful login
                    triggerManualSync(onSuccess = {}, onError = {})
                }
                .onFailure { onError(it.message ?: "Errore sconosciuto") }
        }
    }

    fun register(email: String, displayName: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.register(email, displayName, password)
                .onSuccess { 
                    onSuccess()
                }
                .onFailure { onError(it.message ?: "Errore sconosciuto") }
        }
    }

    fun loginWithGoogle(credential: com.google.firebase.auth.AuthCredential, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            repository.loginWithGoogle(credential)
                .onSuccess { 
                    onSuccess()
                    // Sync upon successful login
                    triggerManualSync(onSuccess = {}, onError = {})
                }
                .onFailure { onError(it.message ?: "Errore sconosciuto") }
        }
    }

    fun loginOffline(onSuccess: () -> Unit) {
        repository.loginOffline()
            .onSuccess {
                onSuccess()
            }
    }

    fun loginOfflineWithDetails(email: String, displayName: String, onSuccess: () -> Unit) {
        repository.loginOfflineWithDetails(email, displayName)
            .onSuccess {
                onSuccess()
            }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.logout()
            onSuccess()
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSyncEnabled(enabled)
        }
    }

    fun backupNow(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onError("Devi aver effettuato l'accesso per eseguire il backup.")
            return
        }
        viewModelScope.launch {
            repository.backupNow(user.id)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Errore durante il backup cloud.") }
        }
    }

    fun restoreNow(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onError("Devi aver effettuato l'accesso per ripristinare il backup.")
            return
        }
        viewModelScope.launch {
            repository.restoreNow(user.id)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Errore durante il ripristino cloud.") }
        }
    }

    fun triggerManualSync(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onError("Nessun utente connesso.")
            return
        }
        viewModelScope.launch {
            repository.syncAllBidirectional(user.id)
                .onSuccess { onSuccess() }
                .onFailure { onError(it.message ?: "Sincronizzazione fallita.") }
        }
    }

    private fun autoSyncPending() {
        val user = currentUser.value
        if (user != null && isAutoSyncEnabled.value) {
            viewModelScope.launch {
                repository.syncManager?.syncPending(user.id)
            }
        }
    }

    // TRANSACTION ACTIONS
    fun addTransaction(
        amount: Double,
        type: TransactionType,
        category: String,
        dateMillis: Long,
        note: String,
        paymentMethod: String,
        accountId: Int? = null
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                Transaction(
                    amount = amount,
                    type = type,
                    category = category,
                    dateMillis = dateMillis,
                    note = note,
                    paymentMethod = paymentMethod,
                    accountId = accountId
                )
            )
            if (accountId != null) {
                val db = AppDatabase.getDatabase(getApplication())
                val account = db.accountDao().getAccountByIdSync(accountId)
                if (account != null) {
                    val delta = if (type == TransactionType.INCOME) amount else -amount
                    db.accountDao().updateAccount(
                        account.copy(
                            saldo = account.saldo + delta,
                            aggiornatoIl = System.currentTimeMillis()
                        )
                    )
                }
            }
            autoSyncPending()
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            val tx = transactions.value.find { it.id == id }
            if (tx?.accountId != null) {
                val account = db.accountDao().getAccountByIdSync(tx.accountId)
                if (account != null) {
                    val delta = if (tx.type == TransactionType.INCOME) -tx.amount else tx.amount
                    db.accountDao().updateAccount(
                        account.copy(
                            saldo = account.saldo + delta,
                            aggiornatoIl = System.currentTimeMillis()
                        )
                    )
                }
            }
            repository.deleteTransaction(id)
            autoSyncPending()
        }
    }

    // BUDGET ACTIONS
    fun addBudget(category: String, limitAmount: Double) {
        viewModelScope.launch {
            repository.insertBudget(Budget(category, limitAmount))
            autoSyncPending()
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudget(category)
            autoSyncPending()
        }
    }

    // SPLIT BILL ACTIONS
    fun addSplitBill(
        title: String,
        totalAmount: Double,
        divisionType: String,
        participants: List<SplitParticipant>
    ) {
        viewModelScope.launch {
            try {
                val listType = Types.newParameterizedType(List::class.java, SplitParticipant::class.java)
                val adapter = moshi.adapter<List<SplitParticipant>>(listType)
                val participantsJson = adapter.toJson(participants)

                repository.insertSplitBill(
                    SplitBill(
                        title = title,
                        totalAmount = totalAmount,
                        divisionType = divisionType,
                        participantsJson = participantsJson
                    )
                )
                autoSyncPending()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteSplitBill(id: Int) {
        viewModelScope.launch {
            repository.deleteSplitBill(id)
            autoSyncPending()
        }
    }

    fun parseParticipants(participantsJson: String): List<SplitParticipant> {
        return try {
            val listType = Types.newParameterizedType(List::class.java, SplitParticipant::class.java)
            val adapter = moshi.adapter<List<SplitParticipant>>(listType)
            adapter.fromJson(participantsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // SETTINGS ACTIONS
    private val _premiumUpgradeFeature = MutableStateFlow<com.example.premium.PremiumFeature?>(null)
    val premiumUpgradeFeature: StateFlow<com.example.premium.PremiumFeature?> = _premiumUpgradeFeature.asStateFlow()

    fun triggerPremiumUpgradeSheet(feature: com.example.premium.PremiumFeature) {
        _premiumUpgradeFeature.value = feature
    }

    fun dismissPremiumUpgradeSheet() {
        _premiumUpgradeFeature.value = null
    }

    fun togglePremiumStatus(isPremium: Boolean) {
        viewModelScope.launch {
            repository.setPremiumStatus(isPremium)
            // Synchronize with PremiumManager
            val plan = if (isPremium) com.example.premium.PremiumPlan.PREMIUM else com.example.premium.PremiumPlan.FREE
            com.example.premium.PremiumManager.setPlan(getApplication(), plan)
        }
    }

    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            repository.setCurrency(currency)
        }
    }

    // AI COACH ACTIONS
    fun askAiCoach() {
        viewModelScope.launch {
            _aiCoachState.value = AiCoachUiState.Loading

            // Compute summary of spending from local SQLite data
            val currentTransactions = transactions.value
            val totalIncome = currentTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val totalExpense = currentTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val netBalance = totalIncome - totalExpense

            val categoryExpenses = currentTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val categoriesStr = categoryExpenses.entries.joinToString { "${it.key}: ${it.value} ${currency.value}" }

            val spendingSummary = """
                Totale Entrate: $totalIncome ${currency.value}
                Totale Uscite: $totalExpense ${currency.value}
                Saldo Netto: $netBalance ${currency.value}
                Spese per Categoria: $categoriesStr
            """.trimIndent()

            repository.fetchAiCoachFeedback(spendingSummary)
                .onSuccess { response ->
                    _aiCoachState.value = AiCoachUiState.Success(response)
                }
                .onFailure { error ->
                    val userMessage = if (error is RateLimitExceededException) {
                        error.message ?: "Limite superato"
                    } else {
                        "Impossibile collegarsi all'IA. Controlla la tua connessione e riprova."
                    }
                    _aiCoachState.value = AiCoachUiState.Error(userMessage)
                }
        }
    }

    fun clearAiCoachError() {
        _aiCoachState.value = AiCoachUiState.Idle
    }
}
