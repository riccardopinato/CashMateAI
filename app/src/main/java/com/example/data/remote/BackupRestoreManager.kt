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
        userSettingsStore.setAutoSync(enabled)
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        userSettingsStore.setLastBackup(timestamp)
    }

    // Forces a full upload of all local records to Firestore
    suspend fun backupNow(userId: String): Result<Unit> {
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
    }

    // Erases local DB and imports everything from Firestore
    suspend fun restoreNow(userId: String): Result<Unit> {
        return try {
            // 1. Download and restore Transactions
            val cloudTx = firestoreService.downloadCollection(userId, "transactions").getOrThrow()
            database.transactionDao().clearAllTransactions()
            cloudTx.forEach { raw ->
                val tx = Transaction(
                    id = (raw["id"] as? Number)?.toInt() ?: 0,
                    amount = (raw["amount"] as? Number)?.toDouble() ?: 0.0,
                    type = try { TransactionType.valueOf(raw["type"] as String) } catch (e: Exception) { TransactionType.EXPENSE },
                    category = raw["category"] as? String ?: "Altro",
                    dateMillis = (raw["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    note = raw["note"] as? String ?: "",
                    paymentMethod = raw["paymentMethod"] as? String ?: "Contanti",
                    accountId = (raw["accountId"] as? Number)?.toInt(),
                    userId = userId,
                    createdAt = (raw["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (raw["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    deletedAt = (raw["deletedAt"] as? Number)?.toLong(),
                    syncStatus = "SYNCED",
                    lastSyncedAt = System.currentTimeMillis()
                )
                if (tx.deletedAt == null) {
                    database.transactionDao().insertTransaction(tx)
                }
            }

            // 2. Download and restore Accounts
            val cloudAcc = firestoreService.downloadCollection(userId, "accounts").getOrThrow()
            database.accountDao().clearAllAccounts()
            cloudAcc.forEach { raw ->
                val acc = Account(
                    id = (raw["id"] as? Number)?.toInt() ?: 0,
                    nome = raw["nome"] as? String ?: "Conto",
                    tipo = raw["tipo"] as? String ?: "Conto Corrente",
                    saldo = (raw["saldo"] as? Number)?.toDouble() ?: 0.0,
                    colore = raw["colore"] as? String ?: "#00E676",
                    icona = raw["icona"] as? String ?: "💳",
                    ordine = (raw["ordine"] as? Number)?.toInt() ?: 0,
                    visibile = raw["visibile"] as? Boolean ?: true,
                    creatoIl = (raw["creatoIl"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    aggiornatoIl = (raw["aggiornatoIl"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    userId = userId,
                    createdAt = (raw["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (raw["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    deletedAt = (raw["deletedAt"] as? Number)?.toLong(),
                    syncStatus = "SYNCED",
                    lastSyncedAt = System.currentTimeMillis()
                )
                if (acc.deletedAt == null) {
                    database.accountDao().insertAccount(acc)
                }
            }

            // 3. Download and restore Split Bills
            val cloudBills = firestoreService.downloadCollection(userId, "splitBills").getOrThrow()
            database.splitBillDao().getAllSplitBills().first().forEach { bill ->
                database.splitBillDao().deleteSplitBillById(bill.id)
            }
            cloudBills.forEach { raw ->
                val bill = SplitBill(
                    id = (raw["id"] as? Number)?.toInt() ?: 0,
                    title = raw["title"] as? String ?: "Spesa",
                    totalAmount = (raw["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                    divisionType = raw["divisionType"] as? String ?: "EQUAL",
                    dateMillis = (raw["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    participantsJson = raw["participantsJson"] as? String ?: "[]",
                    userId = userId,
                    createdAt = (raw["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (raw["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    deletedAt = (raw["deletedAt"] as? Number)?.toLong(),
                    syncStatus = "SYNCED",
                    lastSyncedAt = System.currentTimeMillis()
                )
                if (bill.deletedAt == null) {
                    database.splitBillDao().insertSplitBill(bill)
                }
            }

            // 4. Download and restore Goals
            val cloudGoals = firestoreService.downloadCollection(userId, "goals").getOrThrow()
            database.savingsGoalDao().getAllSavingsGoals().first().forEach { goal ->
                database.savingsGoalDao().deleteSavingsGoalById(goal.id)
            }
            cloudGoals.forEach { raw ->
                val goal = SavingsGoal(
                    id = (raw["id"] as? Number)?.toInt() ?: 0,
                    name = raw["name"] as? String ?: "Obiettivo",
                    targetAmount = (raw["targetAmount"] as? Number)?.toDouble() ?: 100.0,
                    currentAmount = (raw["currentAmount"] as? Number)?.toDouble() ?: 0.0,
                    emoji = raw["emoji"] as? String ?: "🎯",
                    targetDateMillis = (raw["targetDateMillis"] as? Number)?.toLong(),
                    createdAtMillis = (raw["createdAtMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    description = raw["description"] as? String ?: "",
                    status = raw["status"] as? String ?: "ACTIVE",
                    color = raw["color"] as? String ?: "#00E676",
                    notes = raw["notes"] as? String ?: "",
                    userId = userId,
                    createdAt = (raw["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    updatedAt = (raw["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                    deletedAt = (raw["deletedAt"] as? Number)?.toLong(),
                    syncStatus = "SYNCED",
                    lastSyncedAt = System.currentTimeMillis()
                )
                if (goal.deletedAt == null) {
                    database.savingsGoalDao().insertSavingsGoal(goal)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BackupRestoreManager", "Restore failed", e)
            Result.failure(e)
        }
    }
}
