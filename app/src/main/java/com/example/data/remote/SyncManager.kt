package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.domain.model.*
import kotlinx.coroutines.flow.first

class SyncManager(
    private val context: Context,
    private val database: AppDatabase,
    private val firestoreService: FirestoreService,
    private val connectivityObserver: NetworkConnectivityObserver
) {

    suspend fun syncPending(userId: String) {
        if (!connectivityObserver.isConnected()) {
            Log.d("SyncManager", "Skipping pending sync: Offline")
            return
        }

        try {
            // 1. Transactions
            val transactions = database.transactionDao().getAllTransactions().first()
            transactions.filter { it.syncStatus == "PENDING" }.forEach { tx ->
                val updatedTx = tx.copy(userId = userId, syncStatus = "SYNCED", lastSyncedAt = System.currentTimeMillis())
                val map = mapOf<String, Any?>(
                    "id" to updatedTx.id,
                    "amount" to updatedTx.amount,
                    "type" to updatedTx.type.name,
                    "category" to updatedTx.category,
                    "dateMillis" to updatedTx.dateMillis,
                    "note" to updatedTx.note,
                    "paymentMethod" to updatedTx.paymentMethod,
                    "accountId" to updatedTx.accountId,
                    "userId" to userId,
                    "createdAt" to updatedTx.createdAt,
                    "updatedAt" to updatedTx.updatedAt,
                    "deletedAt" to updatedTx.deletedAt,
                    "syncStatus" to "SYNCED",
                    "lastSyncedAt" to updatedTx.lastSyncedAt
                )
                firestoreService.uploadEntity(userId, "transactions", updatedTx.id.toString(), map).getOrThrow()
                database.transactionDao().insertTransaction(updatedTx)
            }

            // 2. Accounts
            val accounts = database.accountDao().getAllAccounts().first()
            accounts.filter { it.syncStatus == "PENDING" }.forEach { acc ->
                val updatedAcc = acc.copy(userId = userId, syncStatus = "SYNCED", lastSyncedAt = System.currentTimeMillis())
                val map = mapOf<String, Any?>(
                    "id" to updatedAcc.id,
                    "nome" to updatedAcc.nome,
                    "tipo" to updatedAcc.tipo,
                    "saldo" to updatedAcc.saldo,
                    "colore" to updatedAcc.colore,
                    "icona" to updatedAcc.icona,
                    "ordine" to updatedAcc.ordine,
                    "visibile" to updatedAcc.visibile,
                    "creatoIl" to updatedAcc.creatoIl,
                    "aggiornatoIl" to updatedAcc.aggiornatoIl,
                    "userId" to userId,
                    "createdAt" to updatedAcc.createdAt,
                    "updatedAt" to updatedAcc.updatedAt,
                    "deletedAt" to updatedAcc.deletedAt,
                    "syncStatus" to "SYNCED",
                    "lastSyncedAt" to updatedAcc.lastSyncedAt
                )
                firestoreService.uploadEntity(userId, "accounts", updatedAcc.id.toString(), map).getOrThrow()
                database.accountDao().updateAccount(updatedAcc)
            }

            // 3. Split Bills
            val bills = database.splitBillDao().getAllSplitBills().first()
            bills.filter { it.syncStatus == "PENDING" }.forEach { bill ->
                val updatedBill = bill.copy(userId = userId, syncStatus = "SYNCED", lastSyncedAt = System.currentTimeMillis())
                val map = mapOf<String, Any?>(
                    "id" to updatedBill.id,
                    "title" to updatedBill.title,
                    "totalAmount" to updatedBill.totalAmount,
                    "divisionType" to updatedBill.divisionType,
                    "dateMillis" to updatedBill.dateMillis,
                    "participantsJson" to updatedBill.participantsJson,
                    "userId" to userId,
                    "createdAt" to updatedBill.createdAt,
                    "updatedAt" to updatedBill.updatedAt,
                    "deletedAt" to updatedBill.deletedAt,
                    "syncStatus" to "SYNCED",
                    "lastSyncedAt" to updatedBill.lastSyncedAt
                )
                firestoreService.uploadEntity(userId, "splitBills", updatedBill.id.toString(), map).getOrThrow()
                database.splitBillDao().insertSplitBill(updatedBill)
            }

            // 4. Savings Goals
            val goals = database.savingsGoalDao().getAllSavingsGoals().first()
            goals.filter { it.syncStatus == "PENDING" }.forEach { goal ->
                val updatedGoal = goal.copy(userId = userId, syncStatus = "SYNCED", lastSyncedAt = System.currentTimeMillis())
                val map = mapOf<String, Any?>(
                    "id" to updatedGoal.id,
                    "name" to updatedGoal.name,
                    "targetAmount" to updatedGoal.targetAmount,
                    "currentAmount" to updatedGoal.currentAmount,
                    "emoji" to updatedGoal.emoji,
                    "targetDateMillis" to updatedGoal.targetDateMillis,
                    "createdAtMillis" to updatedGoal.createdAtMillis,
                    "description" to updatedGoal.description,
                    "status" to updatedGoal.status,
                    "color" to updatedGoal.color,
                    "notes" to updatedGoal.notes,
                    "userId" to userId,
                    "createdAt" to updatedGoal.createdAt,
                    "updatedAt" to updatedGoal.updatedAt,
                    "deletedAt" to updatedGoal.deletedAt,
                    "syncStatus" to "SYNCED",
                    "lastSyncedAt" to updatedGoal.lastSyncedAt
                )
                firestoreService.uploadEntity(userId, "goals", updatedGoal.id.toString(), map).getOrThrow()
                database.savingsGoalDao().insertSavingsGoal(updatedGoal)
            }

            // 5. Transaction Rules
            val rules = database.transactionRuleDao().getAllRules().first()
            rules.filter { it.syncStatus == "PENDING" }.forEach { rule ->
                val updatedRule = rule.copy(userId = userId, syncStatus = "SYNCED", lastSyncedAt = System.currentTimeMillis())
                val map = mapOf<String, Any?>(
                    "id" to updatedRule.id,
                    "merchantName" to updatedRule.merchantName,
                    "containsText" to updatedRule.containsText,
                    "categoryId" to updatedRule.categoryId,
                    "accountId" to updatedRule.accountId,
                    "priority" to updatedRule.priority,
                    "enabled" to updatedRule.enabled,
                    "createdAt" to updatedRule.createdAt,
                    "updatedAt" to updatedRule.updatedAt,
                    "userId" to userId,
                    "deletedAt" to updatedRule.deletedAt,
                    "syncStatus" to "SYNCED",
                    "lastSyncedAt" to updatedRule.lastSyncedAt
                )
                firestoreService.uploadEntity(userId, "rules", updatedRule.id.toString(), map).getOrThrow()
                database.transactionRuleDao().insertRule(updatedRule)
            }

            // 6. Recurring Transactions
            val recurring = database.recurringTransactionDao().getAllRecurring().first()
            recurring.filter { it.syncStatus == "PENDING" }.forEach { rt ->
                val updatedRt = rt.copy(userId = userId, syncStatus = "SYNCED", lastSyncedAt = System.currentTimeMillis())
                val map = mapOf<String, Any?>(
                    "id" to updatedRt.id,
                    "note" to updatedRt.note,
                    "amount" to updatedRt.amount,
                    "category" to updatedRt.category,
                    "type" to updatedRt.type.name,
                    "paymentMethod" to updatedRt.paymentMethod,
                    "accountId" to updatedRt.accountId,
                    "frequency" to updatedRt.frequency,
                    "enabled" to updatedRt.enabled,
                    "lastExecutedMillis" to updatedRt.lastExecutedMillis,
                    "createdAtMillis" to updatedRt.createdAtMillis,
                    "userId" to userId,
                    "createdAt" to updatedRt.createdAt,
                    "updatedAt" to updatedRt.updatedAt,
                    "deletedAt" to updatedRt.deletedAt,
                    "syncStatus" to "SYNCED",
                    "lastSyncedAt" to updatedRt.lastSyncedAt
                )
                firestoreService.uploadEntity(userId, "recurringTransactions", updatedRt.id.toString(), map).getOrThrow()
                database.recurringTransactionDao().insertRecurring(updatedRt)
            }

            // 7. Timeline Events
            val timeline = database.timelineEventDao().getAllTimelineEvents().first()
            timeline.filter { it.syncStatus == "PENDING" }.forEach { te ->
                val updatedTe = te.copy(userId = userId, syncStatus = "SYNCED", lastSyncedAt = System.currentTimeMillis())
                val map = mapOf<String, Any?>(
                    "id" to updatedTe.id,
                    "title" to updatedTe.title,
                    "description" to updatedTe.description,
                    "dateMillis" to updatedTe.dateMillis,
                    "type" to updatedTe.type,
                    "icon" to updatedTe.icon,
                    "userId" to userId,
                    "createdAt" to updatedTe.createdAt,
                    "updatedAt" to updatedTe.updatedAt,
                    "deletedAt" to updatedTe.deletedAt,
                    "syncStatus" to "SYNCED",
                    "lastSyncedAt" to updatedTe.lastSyncedAt
                )
                firestoreService.uploadEntity(userId, "timelineEvents", updatedTe.id.toString(), map).getOrThrow()
                database.timelineEventDao().insertEvent(updatedTe)
            }

            Log.d("SyncManager", "Sync pending completed successfully")
        } catch (e: Exception) {
            Log.e("SyncManager", "Error syncing pending items", e)
            throw e // do not report overall sync success after a failed upload
        }
    }

    suspend fun syncAllBidirectional(userId: String): Result<Unit> {
        if (!connectivityObserver.isConnected()) {
            return Result.failure(Exception("Rete assente."))
        }

        return try {
            syncPending(userId)

            // --- A. Transactions Bidirectional Sync ---
            val localTxList = database.transactionDao().getAllTransactions().first()
            val cloudTxList = firestoreService.downloadCollection(userId, "transactions").getOrThrow()

            cloudTxList.forEach { raw ->
                val id = (raw["id"] as? Number)?.toInt() ?: 0
                val updatedAt = (raw["updatedAt"] as? Number)?.toLong() ?: 0L
                val local = localTxList.find { it.id == id }

                if (local == null) {
                    // Download to local
                    val tx = Transaction(
                        id = id,
                        amount = (raw["amount"] as? Number)?.toDouble() ?: 0.0,
                        type = try { TransactionType.valueOf(raw["type"] as String) } catch(e:Exception) { TransactionType.EXPENSE },
                        category = raw["category"] as? String ?: "Altro",
                        dateMillis = (raw["dateMillis"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        note = raw["note"] as? String ?: "",
                        paymentMethod = raw["paymentMethod"] as? String ?: "Contanti",
                        accountId = (raw["accountId"] as? Number)?.toInt(),
                        userId = userId,
                        createdAt = (raw["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        updatedAt = updatedAt,
                        deletedAt = (raw["deletedAt"] as? Number)?.toLong(),
                        syncStatus = "SYNCED",
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    if (tx.deletedAt == null) {
                        database.transactionDao().insertTransaction(tx)
                    }
                } else if (updatedAt > local.updatedAt) {
                    // Cloud wins
                    val tx = local.copy(
                        amount = (raw["amount"] as? Number)?.toDouble() ?: local.amount,
                        type = try { TransactionType.valueOf(raw["type"] as String) } catch(e:Exception) { local.type },
                        category = raw["category"] as? String ?: local.category,
                        dateMillis = (raw["dateMillis"] as? Number)?.toLong() ?: local.dateMillis,
                        note = raw["note"] as? String ?: local.note,
                        paymentMethod = raw["paymentMethod"] as? String ?: local.paymentMethod,
                        accountId = (raw["accountId"] as? Number)?.toInt() ?: local.accountId,
                        updatedAt = updatedAt,
                        deletedAt = (raw["deletedAt"] as? Number)?.toLong(),
                        syncStatus = "SYNCED",
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    if (tx.deletedAt != null) {
                        database.transactionDao().deleteTransactionById(tx.id)
                    } else {
                        database.transactionDao().insertTransaction(tx)
                    }
                } else if (local.updatedAt > updatedAt) {
                    // Local wins, upload
                    val map = mapOf<String, Any?>(
                        "id" to local.id,
                        "amount" to local.amount,
                        "type" to local.type.name,
                        "category" to local.category,
                        "dateMillis" to local.dateMillis,
                        "note" to local.note,
                        "paymentMethod" to local.paymentMethod,
                        "accountId" to local.accountId,
                        "userId" to userId,
                        "createdAt" to local.createdAt,
                        "updatedAt" to local.updatedAt,
                        "deletedAt" to local.deletedAt,
                        "syncStatus" to "SYNCED",
                        "lastSyncedAt" to System.currentTimeMillis()
                    )
                    firestoreService.uploadEntity(userId, "transactions", local.id.toString(), map).getOrThrow()
                }
            }

            // Upload completely new local records that aren't on Cloud
            localTxList.forEach { local ->
                val cloudHasIt = cloudTxList.any { ((it["id"] as? Number)?.toInt() ?: -1) == local.id }
                if (!cloudHasIt) {
                    val map = mapOf<String, Any?>(
                        "id" to local.id,
                        "amount" to local.amount,
                        "type" to local.type.name,
                        "category" to local.category,
                        "dateMillis" to local.dateMillis,
                        "note" to local.note,
                        "paymentMethod" to local.paymentMethod,
                        "accountId" to local.accountId,
                        "userId" to userId,
                        "createdAt" to local.createdAt,
                        "updatedAt" to local.updatedAt,
                        "deletedAt" to local.deletedAt,
                        "syncStatus" to "SYNCED",
                        "lastSyncedAt" to System.currentTimeMillis()
                    )
                    firestoreService.uploadEntity(userId, "transactions", local.id.toString(), map).getOrThrow()
                }
            }

            // --- B. Accounts Bidirectional Sync ---
            val localAccList = database.accountDao().getAllAccounts().first()
            val cloudAccList = firestoreService.downloadCollection(userId, "accounts").getOrThrow()

            cloudAccList.forEach { raw ->
                val id = (raw["id"] as? Number)?.toInt() ?: 0
                val updatedAt = (raw["updatedAt"] as? Number)?.toLong() ?: 0L
                val local = localAccList.find { it.id == id }

                if (local == null) {
                    val acc = Account(
                        id = id,
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
                        updatedAt = updatedAt,
                        deletedAt = (raw["deletedAt"] as? Number)?.toLong(),
                        syncStatus = "SYNCED",
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    if (acc.deletedAt == null) {
                        database.accountDao().insertAccount(acc)
                    }
                } else if (updatedAt > local.updatedAt) {
                    val acc = local.copy(
                        nome = raw["nome"] as? String ?: local.nome,
                        tipo = raw["tipo"] as? String ?: local.tipo,
                        saldo = (raw["saldo"] as? Number)?.toDouble() ?: local.saldo,
                        colore = raw["colore"] as? String ?: local.colore,
                        icona = raw["icona"] as? String ?: local.icona,
                        ordine = (raw["ordine"] as? Number)?.toInt() ?: local.ordine,
                        visibile = raw["visibile"] as? Boolean ?: local.visibile,
                        aggiornatoIl = (raw["aggiornatoIl"] as? Number)?.toLong() ?: local.aggiornatoIl,
                        updatedAt = updatedAt,
                        deletedAt = (raw["deletedAt"] as? Number)?.toLong(),
                        syncStatus = "SYNCED",
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    if (acc.deletedAt != null) {
                        database.accountDao().deleteAccountById(acc.id)
                    } else {
                        database.accountDao().updateAccount(acc)
                    }
                } else if (local.updatedAt > updatedAt) {
                    val map = mapOf<String, Any?>(
                        "id" to local.id,
                        "nome" to local.nome,
                        "tipo" to local.tipo,
                        "saldo" to local.saldo,
                        "colore" to local.colore,
                        "icona" to local.icona,
                        "ordine" to local.ordine,
                        "visibile" to local.visibile,
                        "creatoIl" to local.creatoIl,
                        "aggiornatoIl" to local.aggiornatoIl,
                        "userId" to userId,
                        "createdAt" to local.createdAt,
                        "updatedAt" to local.updatedAt,
                        "deletedAt" to local.deletedAt,
                        "syncStatus" to "SYNCED",
                        "lastSyncedAt" to System.currentTimeMillis()
                    )
                    firestoreService.uploadEntity(userId, "accounts", local.id.toString(), map).getOrThrow()
                }
            }

            localAccList.forEach { local ->
                val cloudHasIt = cloudAccList.any { ((it["id"] as? Number)?.toInt() ?: -1) == local.id }
                if (!cloudHasIt) {
                    val map = mapOf<String, Any?>(
                        "id" to local.id,
                        "nome" to local.nome,
                        "tipo" to local.tipo,
                        "saldo" to local.saldo,
                        "colore" to local.colore,
                        "icona" to local.icona,
                        "ordine" to local.ordine,
                        "visibile" to local.visibile,
                        "creatoIl" to local.creatoIl,
                        "aggiornatoIl" to local.aggiornatoIl,
                        "userId" to userId,
                        "createdAt" to local.createdAt,
                        "updatedAt" to local.updatedAt,
                        "deletedAt" to local.deletedAt,
                        "syncStatus" to "SYNCED",
                        "lastSyncedAt" to System.currentTimeMillis()
                    )
                    firestoreService.uploadEntity(userId, "accounts", local.id.toString(), map).getOrThrow()
                }
            }

            // --- C. Savings Goals Bidirectional Sync ---
            val localGoalList = database.savingsGoalDao().getAllSavingsGoals().first()
            val cloudGoalList = firestoreService.downloadCollection(userId, "goals").getOrThrow()

            cloudGoalList.forEach { raw ->
                val id = (raw["id"] as? Number)?.toInt() ?: 0
                val updatedAt = (raw["updatedAt"] as? Number)?.toLong() ?: 0L
                val local = localGoalList.find { it.id == id }

                if (local == null) {
                    val goal = SavingsGoal(
                        id = id,
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
                        updatedAt = updatedAt,
                        deletedAt = (raw["deletedAt"] as? Number)?.toLong(),
                        syncStatus = "SYNCED",
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    if (goal.deletedAt == null) {
                        database.savingsGoalDao().insertSavingsGoal(goal)
                    }
                } else if (updatedAt > local.updatedAt) {
                    val goal = local.copy(
                        name = raw["name"] as? String ?: local.name,
                        targetAmount = (raw["targetAmount"] as? Number)?.toDouble() ?: local.targetAmount,
                        currentAmount = (raw["currentAmount"] as? Number)?.toDouble() ?: local.currentAmount,
                        emoji = raw["emoji"] as? String ?: local.emoji,
                        targetDateMillis = (raw["targetDateMillis"] as? Number)?.toLong() ?: local.targetDateMillis,
                        description = raw["description"] as? String ?: local.description,
                        status = raw["status"] as? String ?: local.status,
                        color = raw["color"] as? String ?: local.color,
                        notes = raw["notes"] as? String ?: local.notes,
                        updatedAt = updatedAt,
                        deletedAt = (raw["deletedAt"] as? Number)?.toLong(),
                        syncStatus = "SYNCED",
                        lastSyncedAt = System.currentTimeMillis()
                    )
                    if (goal.deletedAt != null) {
                        database.savingsGoalDao().deleteSavingsGoalById(goal.id)
                    } else {
                        database.savingsGoalDao().insertSavingsGoal(goal)
                    }
                } else if (local.updatedAt > updatedAt) {
                    val map = mapOf<String, Any?>(
                        "id" to local.id,
                        "name" to local.name,
                        "targetAmount" to local.targetAmount,
                        "currentAmount" to local.currentAmount,
                        "emoji" to local.emoji,
                        "targetDateMillis" to local.targetDateMillis,
                        "createdAtMillis" to local.createdAtMillis,
                        "description" to local.description,
                        "status" to local.status,
                        "color" to local.color,
                        "notes" to local.notes,
                        "userId" to userId,
                        "createdAt" to local.createdAt,
                        "updatedAt" to local.updatedAt,
                        "deletedAt" to local.deletedAt,
                        "syncStatus" to "SYNCED",
                        "lastSyncedAt" to System.currentTimeMillis()
                    )
                    firestoreService.uploadEntity(userId, "goals", local.id.toString(), map).getOrThrow()
                }
            }

            localGoalList.forEach { local ->
                val cloudHasIt = cloudGoalList.any { ((it["id"] as? Number)?.toInt() ?: -1) == local.id }
                if (!cloudHasIt) {
                    val map = mapOf<String, Any?>(
                        "id" to local.id,
                        "name" to local.name,
                        "targetAmount" to local.targetAmount,
                        "currentAmount" to local.currentAmount,
                        "emoji" to local.emoji,
                        "targetDateMillis" to local.targetDateMillis,
                        "createdAtMillis" to local.createdAtMillis,
                        "description" to local.description,
                        "status" to local.status,
                        "color" to local.color,
                        "notes" to local.notes,
                        "userId" to userId,
                        "createdAt" to local.createdAt,
                        "updatedAt" to local.updatedAt,
                        "deletedAt" to local.deletedAt,
                        "syncStatus" to "SYNCED",
                        "lastSyncedAt" to System.currentTimeMillis()
                    )
                    firestoreService.uploadEntity(userId, "goals", local.id.toString(), map).getOrThrow()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SyncManager", "Bidirectional sync failed", e)
            Result.failure(e)
        }
    }
}
