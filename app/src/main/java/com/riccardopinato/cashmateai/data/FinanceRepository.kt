package com.riccardopinato.cashmateai.data

import com.riccardopinato.cashmateai.domain.FinanceMath
import com.riccardopinato.cashmateai.domain.TransactionKind
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val database: FinanceDatabase) {
    val accounts: Flow<List<PaymentAccountEntity>> = database.accounts().observeAll()
    val transactions: Flow<List<MoneyTransactionEntity>> = database.transactions().observeAll()

    suspend fun addAccount(name: String, openingBalanceCents: Long): String {
        val cleanName = name.trim()
        require(cleanName.length in 2..60) { "Nome conto: 2–60 caratteri" }
        require(openingBalanceCents >= 0L) { "Saldo di apertura non negativo" }
        val id = UUID.randomUUID().toString()
        database.accounts().insert(
            PaymentAccountEntity(
                id = id,
                name = cleanName,
                openingBalanceCents = openingBalanceCents,
                createdAtMillis = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun addTransaction(
        accountId: String?,
        kind: TransactionKind,
        amountCents: Long,
        category: String,
        note: String
    ): String {
        // Never allow "Tutti i conti" (null) or an orphan/unknown account in a write.
        require(!accountId.isNullOrBlank() && database.accounts().find(accountId) != null) {
            "Seleziona un conto reale prima di salvare"
        }
        require(amountCents > 0) { "Importo positivo obbligatorio" }
        val cleanCategory = category.trim()
        require(cleanCategory.isNotEmpty() && cleanCategory.length <= 60) { "Categoria non valida" }
        require(note.length <= 280) { "Nota troppo lunga" }
        val id = UUID.randomUUID().toString()
        database.transactions().insert(
            MoneyTransactionEntity(
                id = id,
                accountId = accountId,
                kind = kind.name,
                amountCents = amountCents,
                category = cleanCategory,
                note = note.trim(),
                occurredAtMillis = System.currentTimeMillis()
            )
        )
        return id
    }

    suspend fun deleteTransaction(id: String): Boolean =
        database.transactions().delete(id) == 1
}
