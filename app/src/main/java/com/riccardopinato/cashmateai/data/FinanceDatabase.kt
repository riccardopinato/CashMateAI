package com.riccardopinato.cashmateai.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "payment_accounts")
data class PaymentAccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val openingBalanceCents: Long,
    val createdAtMillis: Long
)

@Entity(
    tableName = "money_transactions",
    foreignKeys = [
        ForeignKey(
            entity = PaymentAccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("accountId"), Index("occurredAtMillis")]
)
data class MoneyTransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val kind: String,
    val amountCents: Long,
    val category: String,
    val note: String,
    val occurredAtMillis: Long
)

@Dao
interface PaymentAccountDao {
    @Query("SELECT * FROM payment_accounts ORDER BY createdAtMillis ASC")
    fun observeAll(): Flow<List<PaymentAccountEntity>>

    @Query("SELECT * FROM payment_accounts WHERE id = :id LIMIT 1")
    suspend fun find(id: String): PaymentAccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: PaymentAccountEntity)
}

@Dao
interface MoneyTransactionDao {
    @Query("SELECT * FROM money_transactions ORDER BY occurredAtMillis DESC, id DESC")
    fun observeAll(): Flow<List<MoneyTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: MoneyTransactionEntity)

    @Query("DELETE FROM money_transactions WHERE id = :id")
    suspend fun delete(id: String): Int
}

@Database(
    entities = [PaymentAccountEntity::class, MoneyTransactionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun accounts(): PaymentAccountDao
    abstract fun transactions(): MoneyTransactionDao
}
