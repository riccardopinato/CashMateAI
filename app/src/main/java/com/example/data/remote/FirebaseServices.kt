package com.example.data.remote

import android.util.Log
import android.content.Context
import java.util.UUID
import com.example.domain.model.UserProfile
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthService(private val context: Context) {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private var auth: FirebaseAuth? = null
    private val firestoreService = FirestoreService()

    init {
        // Recovery mode: never auto-load cloud identities over a device-local ledger.
        // The v6 Room database is device-scoped, not partitioned by Firebase UID.
        _currentUser.value = null
    }

    suspend fun login(email: String, password: String): Result<UserProfile> {
        return Result.failure(IllegalStateException(
            "Accesso cloud sospeso: isolamento dei dati finanziari per account non ancora verificato"
        ))
    }

    /** Historical Firebase implementation is intentionally unreachable during recovery. */
    private suspend fun historicalLogin(email: String, password: String): Result<UserProfile> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Servizio Firebase non disponibile."))
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val fbUser = result.user ?: throw Exception("Utente nullo.")
            
            // Check if there is a profile in firestore
            val existingProfile = firestoreService.downloadUserProfile(fbUser.uid).getOrNull()
            val profile = existingProfile ?: UserProfile(
                id = fbUser.uid,
                displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Utente CashMate",
                email = fbUser.email ?: "",
                photoUrl = fbUser.photoUrl?.toString(),
                provider = "password",
                createdAt = fbUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            if (existingProfile == null) {
                firestoreService.uploadUserProfile(profile)
            }
            _currentUser.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun loginOffline(): Result<UserProfile> {
        // A per-installation local identity is genuine device state, not Google/Firebase auth.
        val prefs = context.applicationContext.getSharedPreferences("cashmate_local_identity", Context.MODE_PRIVATE)
        val id = prefs.getString("local_id", null) ?: UUID.randomUUID().toString().also {
            prefs.edit().putString("local_id", it).apply()
        }
        val created = prefs.getLong("created_at", 0L).let { old ->
            if (old > 0) old else System.currentTimeMillis().also { prefs.edit().putLong("created_at", it).apply() }
        }
        val profile = UserProfile(
            id = "local_$id", displayName = "Il mio dispositivo", email = "",
            provider = "local", createdAt = created, updatedAt = created
        )
        _currentUser.value = profile
        return Result.success(profile)
    }

    /** A failed Google sign-in must never impersonate a Google user offline. */
    fun loginOfflineWithDetails(email: String, displayName: String): Result<UserProfile> =
        Result.failure(IllegalStateException("Accesso Google non riuscito. Usa esplicitamente la modalità locale."))

    fun isCloudSession(userId: String): Boolean {
        val provider = _currentUser.value?.provider
        return provider != "local" && provider != null && auth?.currentUser?.uid == userId
    }

    suspend fun register(email: String, displayName: String, password: String): Result<UserProfile> {
        return Result.failure(IllegalStateException(
            "Accesso cloud sospeso: isolamento dei dati finanziari per account non ancora verificato"
        ))
    }

    /** Historical Firebase implementation is intentionally unreachable during recovery. */
    private suspend fun historicalRegister(email: String, displayName: String, password: String): Result<UserProfile> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Servizio Firebase non disponibile."))
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val fbUser = result.user ?: throw Exception("Utente nullo.")
            
            // Update profile display name
            try {
                val profileUpdates = userProfileChangeRequest {
                    this.displayName = displayName
                }
                fbUser.updateProfile(profileUpdates).await()
            } catch (ex: Exception) {
                Log.e("FirebaseAuthService", "Failed to update firebase display name", ex)
            }
            
            val profile = UserProfile(
                id = fbUser.uid,
                displayName = displayName.ifBlank { "Utente CashMate" },
                email = fbUser.email ?: email,
                photoUrl = fbUser.photoUrl?.toString(),
                provider = "password",
                createdAt = fbUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            firestoreService.uploadUserProfile(profile)
            _currentUser.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogleCredential(credential: AuthCredential): Result<UserProfile> {
        return Result.failure(IllegalStateException(
            "Accesso cloud sospeso: isolamento dei dati finanziari per account non ancora verificato"
        ))
    }

    /** Historical Firebase implementation is intentionally unreachable during recovery. */
    private suspend fun historicalLoginWithGoogleCredential(credential: AuthCredential): Result<UserProfile> {
        val firebaseAuth = auth ?: return Result.failure(Exception("Servizio Firebase non disponibile."))
        return try {
            val result = firebaseAuth.signInWithCredential(credential).await()
            val fbUser = result.user ?: throw Exception("Utente nullo.")
            
            val existingProfile = firestoreService.downloadUserProfile(fbUser.uid).getOrNull()
            val profile = existingProfile ?: UserProfile(
                id = fbUser.uid,
                displayName = fbUser.displayName ?: "Utente CashMate",
                email = fbUser.email ?: "",
                photoUrl = fbUser.photoUrl?.toString(),
                provider = "google.com",
                createdAt = fbUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            
            if (existingProfile == null) {
                firestoreService.uploadUserProfile(profile)
            }
            _currentUser.value = profile
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        try {
            auth?.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseAuthService", "Error during signout", e)
        }
        _currentUser.value = null
    }

    suspend fun setPremiumStatus(isPremium: Boolean) {
        if (isPremium) return // no verified billing provider is configured
        val current = _currentUser.value ?: return
        val updated = current.copy(
            isPremium = isPremium,
            premiumPlan = if (isPremium) "PREMIUM" else "FREE",
            updatedAt = System.currentTimeMillis()
        )
        _currentUser.value = updated
        try {
            if (isCloudSession(updated.id)) firestoreService.uploadUserProfile(updated)
        } catch (e: Exception) {
            Log.e("FirebaseAuthService", "Failed to update premium on firestore", e)
        }
    }

    fun updateProfile(profile: UserProfile) {
        _currentUser.value = profile
    }
}

// Retaining compatibility for old service references
class FirestoreUserService {
    suspend fun uploadUserProfile(profile: UserProfile): Result<Unit> {
        return FirestoreService().uploadUserProfile(profile)
    }
}

class FirestoreTransactionService {
    suspend fun syncTransactions(transactions: List<com.example.domain.model.Transaction>): Result<Unit> {
        return Result.failure(IllegalStateException("La sincronizzazione di transazioni non è implementata."))
    }
}

class FirestoreSplitBillService {
    suspend fun syncSplitBills(bills: List<com.example.domain.model.SplitBill>): Result<Unit> {
        return Result.failure(IllegalStateException("La sincronizzazione di spese condivise non è implementata."))
    }
}
