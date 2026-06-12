package com.nyora.hasan72341.sync.supabase

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.nyora.hasan72341.R

/**
 * Helper that launches a Google one-tap / popup sign-in and returns the id_token.
 * Called from SyncSettingsFragment when the user taps Sign in with Google.
 */
class SupabaseGoogleAuthHelper private constructor() {

    companion object {
        fun createIntent(context: Context): Intent {
            // Simple Google Sign-In (fallback; one-tap is preferable in production)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.server_client_id))
                .requestEmail()
                .build()
            return GoogleSignIn.getClient(context, gso).signInIntent
        }

        fun handleResult(data: Intent?): GoogleSignInResult = try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken.isNullOrBlank()) {
                GoogleSignInResult.Error("Google returned no ID token. Check server_client_id.")
            } else {
                GoogleSignInResult.Success(idToken)
            }
        } catch (e: ApiException) {
            val statusName = GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)
            GoogleSignInResult.Error("Google Sign-In failed: $statusName (${e.statusCode})")
        } catch (e: Exception) {
            GoogleSignInResult.Error(e.message ?: "Google Sign-In failed")
        }
    }

    sealed interface GoogleSignInResult {
        data class Success(val idToken: String) : GoogleSignInResult
        data class Error(val message: String) : GoogleSignInResult
    }
}
