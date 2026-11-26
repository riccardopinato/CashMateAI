package com.example.data.local

import android.database.Cursor
import android.util.Base64
import androidx.room.withTransaction
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local, password-encrypted logical backup of the complete Room v6 ledger.
 * Restore is ONLY allowed onto a device with no user-created records. Existing user data
 * is never erased/replaced and unverified legacy schema versions cannot be imported.
 * Cloud sync and Google account access remain disabled independently of this component.
 */
class LocalVaultBackup(private val database: AppDatabase) {
    companion object {
        val magic = "CMVAULT1".toByteArray(Charsets.US_ASCII)
        const val saltLength = 16
        const val nonceLength = 12
        const val iterations = 310_000
        const val maxBackupSize = 32 * 1024 * 1024
        const val maxPlainSize = 24 * 1024 * 1024
        const val schemaVersion = 6
        const val packageId = "com.aistudio.cashmate.aqzwlx"
        const val aad = "CashMateAI-LocalVault-v1"
        fun readLimited(input: InputStream, limitBytes: Int = 32 * 1024 * 1024): ByteArray {
            val output = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var n = input.read(buf)
            while (n >= 0) {
                if (n > 0) {
                    require(output.size() + n <= limitBytes) { "File di backup troppo grande" }
                    output.write(buf, 0, n)
                }
                n = input.read(buf)
            }
            return output.toByteArray()
        }

        val tables = listOf(
            "accounts", "transactions", "split_bills", "budgets", "ai_usage_logs",
            "savings_goals", "savings_deposits", "transaction_rules", "recurring_transactions",
            "achievements", "timeline_events"
        )
        val names = Regex("[a-zA-Z_][a-zA-Z_0-9]*")
    }

    data class ResultSummary(val accounts: Int, val transactions: Int)

    /** Used by a system document picker. Never retains the passphrase or decrypted backup. */
    suspend fun exportEncrypted(password: CharArray): ByteArray = withContext(Dispatchers.IO) {
        validatePassword(password)
        val json = database.withTransaction { captureSnapshot() }.toString().toByteArray(Charsets.UTF_8)
        require(json.size <= maxPlainSize) { "Archivio troppo grande: esportazione annullata" }
        val salt = ByteArray(saltLength).also { SecureRandom().nextBytes(it) }
        val nonce = ByteArray(nonceLength).also { SecureRandom().nextBytes(it) }
        val key = derivedKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(json)
        key.fill(0)
        ByteArrayOutputStream().apply {
            write(magic)
            write(salt)
            write(nonce)
            write(encrypted)
        }.toByteArray()
    }

    /** Restoring into an already-used financial database fails before any modification. */
    suspend fun restoreIfEmpty(password: CharArray, input: ByteArray): ResultSummary = withContext(Dispatchers.IO) {
        validatePassword(password)
        require(input.size in (magic.size + saltLength + nonceLength + 17)..maxBackupSize) {
            "File backup mancante o troppo grande"
        }
        require(input.copyOfRange(0, magic.size).contentEquals(magic)) {
            "Formato backup non riconosciuto"
        }
        val salt = input.copyOfRange(magic.size, magic.size + saltLength)
        val nonce = input.copyOfRange(magic.size + saltLength, magic.size + saltLength + nonceLength)
        val payload = input.copyOfRange(magic.size + saltLength + nonceLength, input.size)
        val key = derivedKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        val decoded = try {
            cipher.doFinal(payload)
        } catch (_: javax.crypto.AEADBadTagException) {
            throw IllegalArgumentException("Password errata o archivio modificato: nessun dato è stato toccato")
        } finally {
            key.fill(0)
        }
        require(decoded.size <= maxPlainSize) { "Archivio troppo grande" }
        val parsed = JSONObject(String(decoded, Charsets.UTF_8))
        decoded.fill(0)
        require(parsed.getInt("format") == 1 && parsed.getInt("roomVersion") == schemaVersion) {
            "Versione backup non compatibile con Room v6"
        }
        require(parsed.getString("packageId") == packageId) { "Backup di un'altra applicazione" }
        val tableData = parsed.getJSONArray("tables")
        require(tableData.length() == tables.size) { "Backup incompleto" }
        val records = mutableListOf<Triple<String, List<String>, JSONArray>>()
        var totalRows = 0
        for ((i, expectedTable) in tables.withIndex()) {
            val entry = tableData.getJSONObject(i)
            require(entry.getString("name") == expectedTable) { "Ordine o nome tabella non valido" }
            val rawColumns = entry.getJSONArray("columns")
            val columns = (0 until rawColumns.length()).map { rawColumns.getString(it) }
            require(columns.isNotEmpty() && columns.distinct().size == columns.size && columns.all { names.matches(it) }) {
                "Colonne del backup non valide"
            }
            val rows = entry.getJSONArray("rows")
            totalRows += rows.length()
            require(totalRows <= 100_000) { "Archivio fuori limite" }
            for (j in 0 until rows.length()) {
                val row = rows.getJSONArray(j)
                require(row.length() == columns.size) { "Riga del backup incompleta" }
                for (k in 0 until row.length()) {
                    val v = row.get(k)
                    require(v == JSONObject.NULL || v is Number || v is String) { "Tipo dato non supportato" }
                    if (v is Number) require(v.toDouble().isFinite()) { "Numero non valido" }
                }
            }
            records.add(Triple(expectedTable, columns, rows))
        }
        database.withTransaction {
            val sqlite = database.openHelper.writableDatabase
            require(sqlite.version == schemaVersion) { "Database locale non compatibile" }
            // Validate every incoming column before touching any table.
            for ((table, columns, _) in records) {
                require(dbColumns(table) == columns) { "Schema differente per $table: nessuna modifica eseguita" }
            }
            for (table in tables) {
                if (table != "achievements") {
                    sqlite.query("SELECT COUNT(*) FROM \"$table\"").use { cursor ->
                        require(cursor.moveToFirst() && cursor.getLong(0) == 0L) {
                            "Sono già presenti dati: ripristino bloccato per non cancellarli"
                        }
                    }
                }
            }
            // 'achievements' can be pre-seeded on a new installation and is safe to replace
            // only because all other user-owned tables have passed the empty-database gate.
            sqlite.execSQL("DELETE FROM \"achievements\"")
            for ((table, columns, rows) in records) {
                val quoted = columns.joinToString(",") { "\"$it\"" }
                val parameters = List(columns.size) { "?" }.joinToString(",")
                val statement = sqlite.compileStatement("INSERT INTO \"$table\" ($quoted) VALUES ($parameters)")
                try {
                    for (i in 0 until rows.length()) {
                        val row = rows.getJSONArray(i)
                        statement.clearBindings()
                        for (j in 0 until row.length()) {
                            val pos = j + 1
                            when (val cell = row.get(j)) {
                                JSONObject.NULL -> statement.bindNull(pos)
                                is Int, is Long -> statement.bindLong(pos, (cell as Number).toLong())
                                is Number -> statement.bindDouble(pos, cell.toDouble())
                                is String -> statement.bindString(pos, cell)
                                else -> error("Tipo dato non valido")
                            }
                        }
                        statement.executeInsert()
                    }
                } finally { statement.close() }
            }
        }
        ResultSummary(
            accounts = records.first { it.first == "accounts" }.third.length(),
            transactions = records.first { it.first == "transactions" }.third.length()
        )
    }

    private fun captureSnapshot(): JSONObject {
        val sqlite = database.openHelper.writableDatabase
        require(sqlite.version == schemaVersion) { "Solo database Room v6 supportati" }
        val tablesJson = JSONArray()
        for (table in tables) {
            val columns = dbColumns(table)
            val rows = JSONArray()
            sqlite.query("SELECT * FROM \"$table\"").use { cursor ->
                while (cursor.moveToNext()) {
                    val row = JSONArray()
                    for (col in columns.indices) {
                        when (cursor.getType(col)) {
                            Cursor.FIELD_TYPE_NULL -> row.put(JSONObject.NULL)
                            Cursor.FIELD_TYPE_INTEGER -> row.put(cursor.getLong(col))
                            Cursor.FIELD_TYPE_FLOAT -> row.put(cursor.getDouble(col))
                            Cursor.FIELD_TYPE_STRING -> row.put(cursor.getString(col))
                            else -> throw IllegalStateException("Campo non esportabile in $table")
                        }
                    }
                    rows.put(row)
                }
            }
            tablesJson.put(JSONObject().put("name",table).put("columns",JSONArray(columns)).put("rows",rows))
        }
        return JSONObject()
            .put("format",1)
            .put("roomVersion",schemaVersion)
            .put("packageId",packageId)
            .put("createdAt",System.currentTimeMillis())
            .put("tables",tablesJson)
    }

    private fun dbColumns(table: String): List<String> {
        require(table in tables)
        val cursor = database.openHelper.writableDatabase.query("PRAGMA table_info(\"$table\")")
        return cursor.use {
            val cols = mutableListOf<String>()
            while (it.moveToNext()) cols.add(it.getString(1))
            cols
        }
    }

    private fun validatePassword(password: CharArray) {
        require(password.size >= 12) { "Password di backup: minimo 12 caratteri" }
    }

    private fun derivedKey(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, 256)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded
        } finally { spec.clearPassword() }
    }

}
