package dev.abdujabbor.firebaseservice.data

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Suspending wrapper around [FirebaseAuth]. Every call awaits the underlying
 * Firebase task, so callers see the real outcome (or the real exception).
 */
class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    /** Creates the account and sends a verification email to the new address. */
    suspend fun register(email: String, password: String): FirebaseUser {
        val user = auth.createUserWithEmailAndPassword(email, password).await().user
            ?: throw IllegalStateException("Registration returned no user")
        // The account exists at this point; a failed verification email should not fail sign-up.
        runCatching { user.sendEmailVerification().await() }
        return user
    }

    suspend fun login(email: String, password: String): FirebaseUser =
        auth.signInWithEmailAndPassword(email, password).await().user
            ?: throw IllegalStateException("Sign-in returned no user")

    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun sendEmailVerification() {
        requireUser().sendEmailVerification().await()
    }

    /** Refreshes the cached user from the server, e.g. to pick up email verification. */
    suspend fun reloadUser(): FirebaseUser {
        val user = requireUser()
        user.reload().await()
        return auth.currentUser ?: user
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val user = reauthenticate(currentPassword)
        user.updatePassword(newPassword).await()
    }

    suspend fun deleteAccount(currentPassword: String) {
        val user = reauthenticate(currentPassword)
        user.delete().await()
    }

    fun signOut() = auth.signOut()

    /** Sensitive operations require a recent sign-in, so confirm the password first. */
    private suspend fun reauthenticate(currentPassword: String): FirebaseUser {
        val user = requireUser()
        val email = user.email ?: throw IllegalStateException("Signed-in user has no email")
        user.reauthenticate(EmailAuthProvider.getCredential(email, currentPassword)).await()
        return user
    }

    private fun requireUser(): FirebaseUser =
        auth.currentUser ?: throw IllegalStateException("No signed-in user")
}
