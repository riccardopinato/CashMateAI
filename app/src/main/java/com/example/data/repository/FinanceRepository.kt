package com.example.data.repository

import com.example.ai.GeminiAiService
import com.example.data.local.BudgetDao
import com.example.data.local.SplitBillDao
import com.example.data.local.TransactionDao
import com.example.data.local.AppDatabase
import androidx.room.withTransaction
import com.example.data.local.UserSettingsStore
import com.example.data.remote.FirebaseAuthService
import com.example.domain.model.*
import com.example.domain.model.MoneyAmount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FinanceRepository(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: com.example.data.local.AccountDao,
    private val splitBillDao: SplitBillDao,
    private val budgetDao: BudgetDao,
    private val userSettingsStore: UserSettingsStore,
    private val firebaseAuthService: FirebaseAuthService,
    private val geminiAiService: GeminiAiService,
    val syncManager: com.example.data.remote.SyncManager? = null,
    val backupRestoreManager: com.example.data.remote.BackupRestoreManager? = null
) {
    // Current user flow from firebase auth
    val currentUser: Flow<UserProfile?> = firebaseAuthService.currentUser

    // Transactions
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insertTransaction(transaction: Transaction) {
        val id = transaction.accountId
        require(id != null && id > 0) { "Seleziona un conto reale prima di salvare il movimento" }
        require(transaction.id == 0) { "Un movimento esistente non può essere inserito nuovamente: usare una modifica verificata" }
        val amountCents = MoneyAmount.cents(transaction.amount)
        require(amountCents > 0L) { "Importo non valido" }
        database.withTransaction {
            val account = requireNotNull(accountDao.getAccountByIdSync(id)) { "Conto inesistente" }
            val deltaCents = if (transaction.type == TransactionType.INCOME) amountCents else -amountCents
            val newBalance = MoneyAmount.applyDelta(account.saldo, deltaCents)
            // An existing ID is not a new expense. All changes commit or roll back together.
            transactionDao.insertTransaction(transaction.copy(amount = MoneyAmount.euros(amountCents)))
            accountDao.updateAccount(account.copy(
                saldo = newBalance, aggiornatoIl = System.currentTimeMillis()
            ))
        }
    }

    suspend fun deleteTransaction(id: Int) {
        database.withTransaction {
            val tx = transactionDao.getTransactionByIdSync(id) ?: return@withTransaction
            require(tx.paymentMethod != "Transfer") {
                "Questa voce appartiene a un trasferimento: eliminare una sola metà renderebbe i saldi incoerenti"
            }
            val account = requireNotNull(tx.accountId?.let { accountDao.getAccountByIdSync(it) }) {
                "Movimento storico senza conto valido: richiede riconciliazione prima dell'eliminazione"
            }
            val amountCents = MoneyAmount.cents(tx.amount)
            val reversalCents = if (tx.type == TransactionType.INCOME) -amountCents else amountCents
            val newBalance = MoneyAmount.applyDelta(account.saldo, reversalCents)
            accountDao.updateAccount(account.copy(
                saldo = newBalance, aggiornatoIl = System.currentTimeMillis()
            ))
            transactionDao.deleteTransactionById(id)
        }
    }

    // Split bills
    val allSplitBills: Flow<List<SplitBill>> = splitBillDao.getAllSplitBills()

    suspend fun insertSplitBill(splitBill: SplitBill) {
        splitBillDao.insertSplitBill(splitBill)
    }

    suspend fun deleteSplitBill(id: Int) {
        splitBillDao.deleteSplitBillById(id)
    }

    // Budgets
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()

    suspend fun insertBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }

    suspend fun deleteBudget(category: String) {
        budgetDao.deleteBudgetByCategory(category)
    }

    // Preferences
    val isPremium: Flow<Boolean> = kotlinx.coroutines.flow.flowOf(false) // no verified billing integration
    val currency: Flow<String> = userSettingsStore.currency

    suspend fun setPremiumStatus(isPremium: Boolean) {
        if (isPremium) return // never grant an unverified entitlement
        userSettingsStore.setPremium(false)
        firebaseAuthService.setPremiumStatus(isPremium)
    }

    suspend fun setCurrency(currency: String) {
        userSettingsStore.setCurrency(currency)
    }

    // AI Coach call
    suspend fun fetchAiCoachFeedback(spendingSummary: String): Result<AiCoachResponse> {
        val premium = isPremium.first()
        return geminiAiService.generateCoachFeedback(spendingSummary, premium)
    }

    // Auth actions
    suspend fun login(email: String, password: String): Result<UserProfile> {
        return firebaseAuthService.login(email, password)
    }

    fun loginOffline(): Result<UserProfile> {
        return firebaseAuthService.loginOffline()
    }

    fun loginOfflineWithDetails(email: String, displayName: String): Result<UserProfile> {
        return firebaseAuthService.loginOfflineWithDetails(email, displayName)
    }

    suspend fun register(email: String, displayName: String, password: String): Result<UserProfile> {
        return firebaseAuthService.register(email, displayName, password)
    }

    suspend fun logout() {
        firebaseAuthService.logout()
    }

    suspend fun loginWithGoogle(credential: com.google.firebase.auth.AuthCredential): Result<UserProfile> {
        return firebaseAuthService.loginWithGoogleCredential(credential)
    }

    // Recovery gate: old cloud sync/restore flows lack atomicity and could overwrite local records.
    // Keep source algorithms for review, but do not expose them as active capabilities.
    val isAutoSyncEnabled: Flow<Boolean> = kotlinx.coroutines.flow.flowOf(false)
    val lastBackupTimestamp: Flow<Long> = backupRestoreManager?.lastBackupTimestamp
        ?: kotlinx.coroutines.flow.flowOf(0L)

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        if (!enabled) backupRestoreManager?.setAutoSyncEnabled(false)
    }

    suspend fun backupNow(userId: String): Result<Unit> = Result.failure(
        IllegalStateException("Backup cloud sospeso: verifica sicurezza e ripristino non completata.")
    )

    suspend fun restoreNow(userId: String): Result<Unit> = Result.failure(
        IllegalStateException("Ripristino cloud sospeso per proteggere i dati locali.")
    )

    suspend fun syncAllBidirectional(userId: String): Result<Unit> = Result.failure(
        IllegalStateException("Sincronizzazione cloud sospesa fino al collaudo con dati di test.")
    )
}
