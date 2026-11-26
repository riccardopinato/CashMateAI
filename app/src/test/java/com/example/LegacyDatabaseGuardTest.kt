package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.LegacyDatabaseGuard
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LegacyDatabaseGuardTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun sqliteHeader(version: Int): ByteArray = ByteArray(64).also { header ->
        "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII).copyInto(header)
        ByteBuffer.wrap(header, 60, 4).order(ByteOrder.BIG_ENDIAN).putInt(version)
    }

    @Test fun missingDatabaseIsSafeForFreshInstall() {
        val f = File(context.cacheDir, "cashmate_missing_test_${System.nanoTime()}.db")
        assertNull(LegacyDatabaseGuard.inspect(f))
    }

    @Test fun olderSchemaBlocksOpeningAndNeverModifiesFile() {
        val f = File.createTempFile("legacy_guard", ".db", context.cacheDir)
        try {
            val bytes = sqliteHeader(5)
            f.writeBytes(bytes)
            val result = LegacyDatabaseGuard.inspect(f)
            assertNotNull(result)
            assertTrue(result!!.contains("versione 5"))
            assertArrayEquals(bytes, f.readBytes())
        } finally { f.delete() }
    }

    @Test fun supportedVersionPassesAndBrokenHeaderFailsClosed() {
        val f = File.createTempFile("legacy_guard", ".db", context.cacheDir)
        try {
            f.writeBytes(sqliteHeader(6))
            assertNull(LegacyDatabaseGuard.inspect(f))
            f.writeText("invalid")
            assertNotNull(LegacyDatabaseGuard.inspect(f))
        } finally { f.delete() }
    }
}
