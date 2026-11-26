package com.riccardopinato.cashmateai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.riccardopinato.cashmateai.data.FinanceRepository
import com.riccardopinato.cashmateai.data.MoneyTransactionEntity
import com.riccardopinato.cashmateai.data.PaymentAccountEntity
import com.riccardopinato.cashmateai.data.UserSettings
import com.riccardopinato.cashmateai.domain.TransactionKind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FinanceUiState(
    val accounts: List<PaymentAccountEntity> = emptyList(),
    val transactions: List<MoneyTransactionEntity> = emptyList(),
    val darkTheme: Boolean = false,
    val loaded: Boolean = false
)

class FinanceViewModel(
    private val repository: FinanceRepository,
    private val settings: UserSettings
) : ViewModel() {
    val state: StateFlow<FinanceUiState> =
        combine(repository.accounts, repository.transactions, settings.darkTheme) { accounts, transactions, dark ->
            FinanceUiState(accounts, transactions, dark, loaded = true)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceUiState())

    val notice = MutableStateFlow<String?>(null)

    fun clearNotice() { notice.value = null }

    fun addAccount(name: String, openingCents: Long) {
        viewModelScope.launch {
            runCatching { repository.addAccount(name, openingCents) }
                .onSuccess { notice.value = "Conto creato" }
                .onFailure { notice.value = it.message ?: "Impossibile creare il conto" }
        }
    }

    fun addTransaction(accountId: String?, kind: TransactionKind, cents: Long, category: String, note: String) {
        viewModelScope.launch {
            runCatching { repository.addTransaction(accountId, kind, cents, category, note) }
                .onSuccess { notice.value = "Movimento salvato" }
                .onFailure { notice.value = it.message ?: "Impossibile salvare" }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            runCatching { repository.deleteTransaction(id) }
                .onSuccess { notice.value = if (it) "Movimento eliminato" else "Movimento non trovato" }
                .onFailure { notice.value = "Impossibile eliminare" }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch {
            runCatching { settings.setDarkTheme(enabled) }
                .onFailure { notice.value = "Impossibile salvare l'impostazione" }
        }
    }

    companion object {
        fun factory(repository: FinanceRepository, settings: UserSettings): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(FinanceViewModel::class.java))
                    @Suppress("UNCHECKED_CAST")
                    return FinanceViewModel(repository, settings) as T
                }
            }
    }
}
