package com.example.data.remote

import android.util.Log
import com.example.domain.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreService {

    private val db: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("FirestoreService", "FirebaseFirestore initialization failed. Running in offline/fallback mode.", e)
            null
        }
    }

    // 1. User Profile Sync
    suspend fun uploadUserProfile(profile: UserProfile): Result<Unit> {
        val database = db ?: return Result.failure(Exception("Firestore non disponibile (Modalità Offline)"))
        return try {
            val userMap = mapOf(
                "id" to profile.id,
                "displayName" to profile.displayName,
                "email" to profile.email,
                "photoUrl" to profile.photoUrl,
                "provider" to profile.provider,
                "createdAt" to profile.createdAt,
                "updatedAt" to profile.updatedAt,
                "premiumPlan" to profile.premiumPlan,
                "currency" to profile.currency,
                "language" to profile.language,
                "isOnboardingCompleted" to profile.isOnboardingCompleted
            )
            database.collection("users").document(profile.id)
                .set(userMap, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error uploading user profile", e)
            Result.failure(e)
        }
    }

    suspend fun downloadUserProfile(userId: String): Result<UserProfile?> {
        val database = db ?: return Result.failure(Exception("Firestore non disponibile (Modalità Offline)"))
        return try {
            val doc = database.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val profile = UserProfile(
                    id = doc.getString("id") ?: userId,
                    displayName = doc.getString("displayName") ?: "Utente CashMate",
                    email = doc.getString("email") ?: "",
                    photoUrl = doc.getString("photoUrl"),
                    provider = doc.getString("provider") ?: "email",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                    premiumPlan = doc.getString("premiumPlan") ?: "FREE",
                    currency = doc.getString("currency") ?: "€",
                    language = doc.getString("language") ?: "it",
                    isOnboardingCompleted = doc.getBoolean("isOnboardingCompleted") ?: false,
                    isPremium = (doc.getString("premiumPlan") ?: "FREE") == "PREMIUM"
                )
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error downloading user profile", e)
            Result.failure(e)
        }
    }

    // 2. Generic upload and download for subcollections
    suspend fun uploadEntity(userId: String, collectionName: String, docId: String, data: Map<String, Any?>): Result<Unit> {
        val database = db ?: return Result.failure(Exception("Firestore non disponibile (Modalità Offline)"))
        return try {
            database.collection("users").document(userId)
                .collection(collectionName).document(docId)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error uploading entity to $collectionName", e)
            Result.failure(e)
        }
    }

    suspend fun downloadCollection(userId: String, collectionName: String): Result<List<Map<String, Any>>> {
        val database = db ?: return Result.failure(Exception("Firestore non disponibile (Modalità Offline)"))
        return try {
            val snapshot = database.collection("users").document(userId)
                .collection(collectionName).get().await()
            val list = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { data ->
                    val mut = data.toMutableMap()
                    mut["_id"] = doc.id
                    mut
                }
            }
            Result.success(list)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error downloading collection $collectionName", e)
            Result.failure(e)
        }
    }

    suspend fun deleteEntity(userId: String, collectionName: String, docId: String): Result<Unit> {
        val database = db ?: return Result.failure(Exception("Firestore non disponibile (Modalità Offline)"))
        return try {
            database.collection("users").document(userId)
                .collection(collectionName).document(docId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreService", "Error deleting entity in $collectionName", e)
            Result.failure(e)
        }
    }
}
