package com.riccardopinato.cashmateai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.riccardopinato.cashmateai.ui.CashMateApp
import com.riccardopinato.cashmateai.ui.FinanceViewModel

class MainActivity : ComponentActivity() {
    private val model: FinanceViewModel by viewModels {
        val app = application as CashMateApplication
        FinanceViewModel.factory(app.repository, app.settings)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by model.state.collectAsState()
            val notice by model.notice.collectAsState()
            CashMateApp(
                state = state,
                notice = notice,
                onNoticeDismiss = model::clearNotice,
                onAddAccount = model::addAccount,
                onAddTransaction = model::addTransaction,
                onDeleteTransaction = model::deleteTransaction,
                onDarkThemeChange = model::setDarkTheme
            )
        }
    }
}
