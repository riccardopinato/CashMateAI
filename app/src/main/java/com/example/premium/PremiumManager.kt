package com.example.premium

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PremiumFeature(val title: String, val description: String) {
    AI_CHAT("Richieste AI Coach", "Chiedi consigli finanziari personalizzati alimentati da Gemini AI"),
    OCR("Scansione Scontrini OCR", "Digitalizza all'istante scontrini e fatture con la fotocamera"),
    UNLIMITED_SPLITS("Split Bill Illimitati", "Crea infiniti album condivisi per dividere le spese con gli amici"),
    MULTI_ACCOUNT("Multi-Account & Wallet", "Gestisci conti multipli, carte e contanti separatamente"),
    EXPORT("Esportazione Dati", "Esporta l'intera cronologia delle transazioni in formato CSV o JSON"),
    CLOUD_SYNC("Sincronizzazione Cloud", "Backup automatico e sincronizzazione in tempo reale su tutti i dispositivi"),
    WIDGET("Widget Schermata Home", "Monitora il tuo saldo e aggiungi transazioni rapide dalla Home"),
    SMART_RULES("Regole Automatiche Pro", "Crea regole intelligenti avanzate basate sui merchant"),
    GOALS("Obiettivi di Risparmio", "Pianifica e monitora molteplici traguardi di risparmio mensili")
}

enum class PremiumPlan {
    FREE,
    PREMIUM,
    LIFETIME
}

object PremiumManager {
    // Limits easily modifiable in one place
    const val LIMIT_AI_REQUESTS_FREE = 5
    const val LIMIT_OCR_FREE = 5
    const val LIMIT_GOALS_FREE = 3
    const val LIMIT_WALLETS_FREE = 2
    const val LIMIT_SPLITS_FREE = 5

    private const val PREFS_NAME = "cashmate_premium_preferences_v1"
    private const val KEY_PLAN = "premium_plan"
    private const val KEY_EXPIRATION = "premium_expiration"

    private val _currentPlan = MutableStateFlow(PremiumPlan.FREE)
    val currentPlan: StateFlow<PremiumPlan> = _currentPlan.asStateFlow()

    private val _expirationDate = MutableStateFlow("Nessuna")
    val expirationDate: StateFlow<String> = _expirationDate.asStateFlow()

    fun init(context: Context) {
        // Historical SharedPreferences may hold values set by demo buttons.
        // Keep them untouched for forensic recovery, but do not grant paid access.
        _currentPlan.value = PremiumPlan.FREE
        _expirationDate.value = "Non verificata"
    }

    fun setPlan(context: Context, plan: PremiumPlan, expiration: String = "Nessuna") {
        // No Play Billing/RevenueCat verified entitlement exists in this snapshot.
        // Explicitly refuse upgrades. Free is the only effective state.
        _currentPlan.value = PremiumPlan.FREE
        _expirationDate.value = "Non verificata"
    }

    fun isPremiumActive(): Boolean = false

    /**
     * Centralized function to verify if a feature is accessible.
     * @param currentCount optional count of existing items (e.g. goals count, wallet count)
     */
    fun canUseFeature(feature: PremiumFeature, currentCount: Int = 0): Boolean {
        if (isPremiumActive()) return true

        return when (feature) {
            PremiumFeature.AI_CHAT -> currentCount < LIMIT_AI_REQUESTS_FREE
            PremiumFeature.OCR -> currentCount < LIMIT_OCR_FREE
            PremiumFeature.UNLIMITED_SPLITS -> currentCount < LIMIT_SPLITS_FREE
            PremiumFeature.MULTI_ACCOUNT -> currentCount < LIMIT_WALLETS_FREE
            PremiumFeature.GOALS -> currentCount < LIMIT_GOALS_FREE
            PremiumFeature.EXPORT -> false
            PremiumFeature.WIDGET -> false
            PremiumFeature.CLOUD_SYNC -> false
            PremiumFeature.SMART_RULES -> false // Smart rules limitate in Free
        }
    }

    /**
     * Get descriptive limit text for Free plan to display in UI comparison
     */
    fun getLimitDescription(feature: PremiumFeature): String {
        return when (feature) {
            PremiumFeature.AI_CHAT -> "$LIMIT_AI_REQUESTS_FREE / ora"
            PremiumFeature.OCR -> "$LIMIT_OCR_FREE / mese"
            PremiumFeature.UNLIMITED_SPLITS -> "Max $LIMIT_SPLITS_FREE album"
            PremiumFeature.MULTI_ACCOUNT -> "Max $LIMIT_WALLETS_FREE conti"
            PremiumFeature.GOALS -> "Max $LIMIT_GOALS_FREE obiettivi"
            PremiumFeature.EXPORT -> "Non disponibile"
            PremiumFeature.WIDGET -> "Non disponibile"
            PremiumFeature.CLOUD_SYNC -> "Solo locale"
            PremiumFeature.SMART_RULES -> "Non disponibile"
        }
    }
}
