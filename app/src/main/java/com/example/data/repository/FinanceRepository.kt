package com.example.data.repository

import com.example.ai.GeminiAiService
import com.example.data.local.BudgetDao
import com.example.data.local.SplitBillDao
import com.example.data.local.TransactionDao
import com.example.data.local.UserSettingsStore
import com.example.data.remote.FirebaseAuthService
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FinanceRepository(
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
        require(id != null && id > 0 && accountDao.getAccountByIdSync(id) != null) {
            "Seleziona un conto reale prima di salvare il movimento"
        }
        require(transaction.amount.isFinite() && transaction.amount > 0.0) { "Importo non valido" }
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(id: Int) {
        transactionDao.deleteTransactionById(id)
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

    val isAutoSyncEnabled: Flow<Boolean> = backupRestoreManager?.isAutoSyncEnabled ?: kotlinx.coroutines.flow.flowOf(true)
    val lastBackupTimestamp: Flow<Long> = backupRestoreManager?.lastBackupTimestamp ?: kotlinx.coroutines.flow.flowOf(0L)

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        backupRestoreManager?.setAutoSyncEnabled(enabled)
    }

    suspend fun backupNow(userId: String): Result<Unit> {
        if (!firebaseAuthService.isCloudSession(userId))
            return Result.failure(IllegalStateException("Backup cloud richiede un account Firebase verificato."))
        return backupRestoreManager?.backupNow(userId) ?: Result.failure(Exception("Backup manager non disponibile."))
    }

    suspend fun restoreNow(userId: String): Result<Unit> {
        if (!firebaseAuthService.isCloudSession(userId))
            return Result.failure(IllegalStateException("Ripristino cloud richiede un account Firebase verificato."))
        return backupRestoreManager?.restoreNow(userId) ?: Result.failure(Exception("Restore manager non disponibile."))
    }

    suspend fun syncAllBidirectional(userId: String): Result<Unit> {
        if (!firebaseAuthService.isCloudSession(userId))
            return Result.failure(IllegalStateException("Sincronizzazione cloud richiede un account Firebase verificato."))
        return syncManager?.syncAllBidirectional(userId) ?: Result.failure(Exception("Sync manager non disponibile."))
    }
}
