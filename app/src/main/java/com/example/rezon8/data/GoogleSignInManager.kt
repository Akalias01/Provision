package com.mossglen.reverie.data

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleUser(
    val id: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val account: GoogleSignInAccount? = null
)

@Singleton
class GoogleSignInManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _currentUser = MutableStateFlow<GoogleUser?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _isSignedIn = MutableStateFlow(false)
    val isSignedIn = _isSignedIn.asStateFlow()

    // Google Sign-In configuration with Drive scopes
    // DRIVE: Full read/write access to browse, download, and upload
    // DRIVE_APPDATA: App's hidden data folder for backups
    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestScopes(Scope(DriveScopes.DRIVE))
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
        .build()

    val signInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    init {
        checkExistingSignIn()
    }

    private fun checkExistingSignIn() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            updateUserState(account)
        }
    }

    fun getSignInIntent(): Intent = signInClient.signInIntent

    fun handleSignInResult(result: ActivityResult): Result<GoogleUser> {
        return try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            updateUserState(account)
            Result.success(_currentUser.value!!)
        } catch (e: ApiException) {
            Result.failure(e)
        }
    }

    private fun updateUserState(account: GoogleSignInAccount) {
        val user = GoogleUser(
            id = account.id ?: "",
            email = account.email ?: "",
            displayName = account.displayName,
            photoUrl = account.photoUrl?.toString(),
            account = account
        )
        _currentUser.value = user
        _isSignedIn.value = true
    }

    fun getAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun hasDrivePermission(): Boolean {
        val account = getAccount() ?: return false
        return GoogleSignIn.hasPermissions(
            account,
            Scope(DriveScopes.DRIVE),
            Scope(DriveScopes.DRIVE_APPDATA)
        )
    }

    fun signOut(onComplete: () -> Unit = {}) {
        signInClient.signOut().addOnCompleteListener {
            _currentUser.value = null
            _isSignedIn.value = false
            onComplete()
        }
    }

    fun revokeAccess(onComplete: () -> Unit = {}) {
        signInClient.revokeAccess().addOnCompleteListener {
            _currentUser.value = null
            _isSignedIn.value = false
            onComplete()
        }
    }
}
