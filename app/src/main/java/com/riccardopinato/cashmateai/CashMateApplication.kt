package com.riccardopinato.cashmateai

import android.app.Application
import androidx.room.Room
import com.riccardopinato.cashmateai.data.FinanceDatabase
import com.riccardopinato.cashmateai.data.FinanceRepository
import com.riccardopinato.cashmateai.data.UserSettings

class CashMateApplication : Application() {
    private val database: FinanceDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            FinanceDatabase::class.java,
            "cashmate_local.db"
        ).build()
    }

    val repository: FinanceRepository by lazy { FinanceRepository(database) }
    val settings: UserSettings by lazy { UserSettings(applicationContext) }
}
