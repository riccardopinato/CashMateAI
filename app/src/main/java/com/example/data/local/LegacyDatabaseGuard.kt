package com.example.data.local

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * A read-only preflight gate that stops Room from opening an unrecognized on-device schema.
 * It does not migrate, erase, copy or alter any database file.
 */
object LegacyDatabaseGuard {
    private val sqliteMagic = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun blockReason(context: Context): String? =
        inspect(context.getDatabasePath("cashmate_database"))

    fun inspect(file: File): String? {
        if (!file.exists()) return null // First installation; Room v6 will create it.
        return try {
            FileInputStream(file).use { stream ->
                val header = ByteArray(64)
                var count = 0
                while (count < header.size) {
                    val n = stream.read(header, count, header.size - count)
                    if (n <= 0) break
                    count += n
                }
                if (count < header.size || !header.copyOfRange(0, 16).contentEquals(sqliteMagic)) {
                    return "Database storico non riconosciuto. Nessuna cancellazione eseguita."
                }
                val version = ByteBuffer.wrap(header, 60, 4).order(ByteOrder.BIG_ENDIAN).int
                if (version == 6) null else {
                    "È presente un database CashMate in versione $version, diverso da Room v6. " +
                    "Per proteggere i movimenti non sarà aperto né modificato finché non sarà verificata una migrazione. " +
                    "Non disinstallare l'app e non cancellarne i dati."
                }
            }
        } catch (_: Exception) {
            "Impossibile verificare il database storico. Nessuna cancellazione eseguita."
        }
    }
}
