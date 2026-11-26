package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.UserSettingsStore
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BackupRestoreManager(
    private val context: Context,
    private val database: AppDatabase,
    private val firestoreService: FirestoreService,
    private val userSettingsStore: UserSettingsStore
) {

    // Status / Timestamp fields stored in shared preferences or datastore
    val isAutoSyncEnabled: Flow<Boolean> = userSettingsStore.autoSyncEnabled
    val lastBackupTimestamp: Flow<Long> = userSettingsStore.lastBackupTimestamp

    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        // Never allow callers to opt in to an unverified, non-atomic cloud sync.
        userSettingsStore.setAutoSync(false)
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        userSettingsStore.setLastBackup(timestamp)
    }

    // Forces a full upload of all local records to Firestore
    suspend fun backupNow(userId: String): Result<Unit> {
        return Result.failure(IllegalStateException(
            "Backup cloud non disponibile: ripristino non ancora certificato. Nessun dato trasmesso."
        ))
        /* Legacy uploader retained for audit only; MUST NOT execute.
        return try {
            // 1. Transactions
            val transactions = database.transactionDao().getAllTransactions().first()
            transactions.forEach { tx ->
                val map = mapOf<String, Any?>(
                    "id" to tx.id,
                    "amount" to tx.amount,
                    "type" to tx.type.name,
                    "category" to tx.category,
                    "dateMillis" to tx.dateMillis,
                    "note" to tx.note,
                    "paymentMethod" to tx.paymentMethod,
                    "accountId" to tx.accountId,
                    "userId" to userId,
                    "createdAt" to tx.createdAt,
                    "updatedAt" to tx.updatedAt,
                    "deletedAt" to tx.deletedAt,
                    "syncStatus" to "SYNCED",
                    "lastSyncedAt" to System.currentTimeMillis()
                )
                firestoreService.uploadEntity(userId, "transactions", tx.id.toString(), map).getOrThrow()
            }

            // 2. Accounts
            val accounts = database.accountDao().getAllAccounts().first()
            accounts.forEach { acc ->
                val map = mapOf<String, Any?>(
                    "id" to acc.id,
                    "nome" to acc.nome,
                    "tipo" to acc.tipo,
                    "saldo" to acc.saldo,
                    "colore" to acc.colore,
                    "icona" to acc.icona,
                    "ordine" to acc.ordine,
                    "visibile" to acc.visibile,
                    "creatoIl" to acc.creatoIl,
                    "aggiornatoIl" to acc.aggiornatoIl,
                    "userId" to userId,
                    "createdAt" to acc.createdAt,
                    "updatedAt" to acc.updatedAt,
                    "deletedAt" to acc.deletedAt,
                    "syncStatus" to "SYNCED",
                    "lastSyncedAt" to System.currentTimeMillis()
                )
                firestoreService.uploadEntity(userId, "accounts", acc.id.toString(), map).getOrThrow()
            }

            // 3. Split Bills
            val splitBills = database.splitBillDao().getAllSplitBills().first()
            splitBills.forEach { bill ->
                val map = mapOf<String, Any?>(
                    "id" to bill.id,
                    "title" to bill.title,
                    "totalAmount" to bill.totalAmount,
                    "divisionType" to bill.divisionType,
                    "dateMillis" to bill.dateMillis,
                    "participantsJson" to bill.participantsJson,
                    "userId" to userId,
                    "createdAt" to bill.createdAt,
                    "updatedAt" to bill.updatedAt,
                    "deletedAt" to bill.deletedAt,
                    "syncStatus" to "SYNCED",
                    "lastSyncedAt" to System.currentTimeMillis()
                )
                firestoreService.uploadEntity(userId, "splitBills", bill.id.toString(), map).getOrThrow()
            }

            // 4. Savings Goals
            val goals = database.savingsGoalDao().getAllSavingsGoals().first()
            goals.forEach { goal ->
                val map = mapOf<String, Any?>(
                    "id" to goal.id,
                    "name" to goal.name,
                    "targetAmount" to goal.targetAmount,
                    "currentAmount" to goal.currentAmount,
                    "emoji" to goal.emoji,
                    "targetDateMillis" to goal.targetDateMillis,
                    "createdAtMillis" to goal.createdAtMillis,
                    "description" to goal.description,
                    "status" to goal.status,
                    "color" to goal.color,
                    "notes" to goal.notes,
                    "userId" to userId,
                    "createdAt" to goal.createdAt,
                    "updatedAt" to goal.updatedAt,
                    "deletedAt" to goal.deletedAt,
                    "syncStatus" to "SYNCED",
                    "lastSyncedAt" to System.currentTimeMillis()
                )
                firestoreService.uploadEntity(userId, "goals", goal.id.toString(), map).getOrThrow()
            }

            // Update local and cloud backup metadata
            val now = System.currentTimeMillis()
            setLastBackupTimestamp(now)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BackupRestoreManager", "Backup failed", e)
            Result.failure(e)
        }
        */
    }

    /**
     * Disabled until all remote collections are validated and a rollback-safe
     * transactional restore is implemented. Historical implementation cleared
     * the local database before downloading every collection.
     */
    suspend fun restoreNow(userId: String): Result<Unit> = Result.failure(
        IllegalStateException("Ripristino cloud non ancora sicuro: dati locali preservati.")
    )
}
