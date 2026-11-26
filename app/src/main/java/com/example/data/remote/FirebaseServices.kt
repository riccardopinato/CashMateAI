package com.example.data.remote

import android.util.Log
import com.example.domain.model.UserProfile
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseAuthService {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private var auth: FirebaseAuth? = null
    private val firestoreService = FirestoreService()

    init {
        try {
            auth = FirebaseAuth.getInstance()
            auth?.addAuthStateListener { firebaseAuth ->
                val fbUser = firebaseAuth.currentUser
                if (fbUser != null) {
                    val profile = UserProfile(
                        id = fbUser.uid,
                        displayName = fbUser.displayName ?: fbUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Utente CashMate",
                        email = fbUser.email ?: "",
                        photoUrl = fbUser.photoUrl?.toString(),
                        provider = fbUser.providerData.firstOrNull()?.providerId ?: "email",
                        createdAt = fbUser.metadata?.creationTimestamp ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        premiumPlan = "FREE", // default
                        currency = "€",
                        language = "it",
                        isOnboardingCompleted = false,
                        isPremium = false
                    )
                    _currentUser.value = profile
                } else {
                    val currentProvider = _currentUser.value?.provider
                    if (currentProvider != "offline" && currentProvider != "google_offline") {
                        _currentUser.value = null
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseAuthService", "Firebase initialization failed - falling back to offline", e)
            // Seed a default mock/offline user to avoid app crashes when google-services is missing or in emulator
            _currentUser.value = UserProfile(
                id = "offline_user_id",
                displayName = "Utente Offline",
                email = "user@cashmate.ai",
                provider = "offline"
            )
        }
    }

    suspend fun login(email: String, password: String): Result<UserProfile> {
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
        val profile = UserProfile(
            id = "offline_user_id",
            displayName = "Utente Demo",
            email = "demo@cashmate.ai",
            provider = "offline",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        _currentUser.value = profile
        return Result.success(profile)
    }

    fun loginOfflineWithDetails(email: String, displayName: String): Result<UserProfile> {
        val userId = "google_offline_" + java.util.UUID.nameUUIDFromBytes(email.toByteArray()).toString()
        val profile = UserProfile(
            id = userId,
            displayName = displayName,
            email = email,
            provider = "google_offline",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        _currentUser.value = profile
        return Result.success(profile)
    }

    suspend fun register(email: String, displayName: String, password: String): Result<UserProfile> {
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
        val current = _currentUser.value ?: return
        val updated = current.copy(
            isPremium = isPremium,
            premiumPlan = if (isPremium) "PREMIUM" else "FREE",
            updatedAt = System.currentTimeMillis()
        )
        _currentUser.value = updated
        try {
            firestoreService.uploadUserProfile(updated)
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
        return Result.success(Unit)
    }
}

class FirestoreSplitBillService {
    suspend fun syncSplitBills(bills: List<com.example.domain.model.SplitBill>): Result<Unit> {
        return Result.success(Unit)
    }
}
