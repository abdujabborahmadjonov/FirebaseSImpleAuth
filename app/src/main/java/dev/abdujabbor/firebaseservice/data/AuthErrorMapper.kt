package dev.abdujabbor.firebaseservice.data

import androidx.annotation.StringRes
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import dev.abdujabbor.firebaseservice.R

/** Turns Firebase exceptions into user-friendly messages. */
object AuthErrorMapper {

    @StringRes
    fun toMessage(error: Throwable): Int = when (error) {
        // Must come before its parent, FirebaseAuthInvalidCredentialsException.
        is FirebaseAuthWeakPasswordException -> R.string.error_weak_password
        is FirebaseAuthInvalidCredentialsException -> R.string.error_invalid_credentials
        is FirebaseAuthInvalidUserException -> R.string.error_user_not_found
        is FirebaseAuthUserCollisionException -> R.string.error_email_in_use
        is FirebaseAuthRecentLoginRequiredException -> R.string.error_recent_login
        is FirebaseTooManyRequestsException -> R.string.error_too_many_requests
        is FirebaseNetworkException -> R.string.error_network
        else -> R.string.error_generic
    }
}
