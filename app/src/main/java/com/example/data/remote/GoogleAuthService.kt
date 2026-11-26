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
    private var configured: Boolean = false

    init {
        // A real OAuth client ID is required for cloud Google authentication.
        val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val webClientId = if (resourceId != 0) context.getString(resourceId) else ""
        configured = webClientId.isNotBlank() && webClientId.endsWith(".apps.googleusercontent.com")

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
        if (configured) gso.requestIdToken(webClientId)

        googleSignInClient = GoogleSignIn.getClient(context, gso.build())
    }

    fun getSignInIntent(): Intent {
        check(configured) { "Google Sign-In non configurato. Scegli la modalità locale." }
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
