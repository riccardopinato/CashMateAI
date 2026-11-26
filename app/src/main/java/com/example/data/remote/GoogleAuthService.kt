package com.example.data.remote

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class GoogleAuthService(private val context: Context) {

    private val googleSignInClient: GoogleSignInClient

    init {
        // Find web_client_id from resources if present, otherwise use a placeholder.
        val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val webClientId = if (resourceId != 0) {
            context.getString(resourceId)
        } else {
            // Standard client ID placeholder or from BuildConfig if provided
            "103456789012-placeholder.apps.googleusercontent.com"
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        // Sign out from Google first so that the user is always prompted to choose an account
        googleSignInClient.signOut()
        return googleSignInClient.signInIntent
    }

    fun getSignInResult(data: Intent?): Result<GoogleSignInAccount> {
        return try {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                Result.success(account)
            } else {
                Result.failure(Exception("Google Sign-In account is null"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        googleSignInClient.signOut()
    }
}
