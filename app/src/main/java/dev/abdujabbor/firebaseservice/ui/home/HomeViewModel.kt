package dev.abdujabbor.firebaseservice.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import dev.abdujabbor.firebaseservice.R
import dev.abdujabbor.firebaseservice.data.AuthErrorMapper
import dev.abdujabbor.firebaseservice.data.AuthRepository
import dev.abdujabbor.firebaseservice.validation.AuthValidator
import dev.abdujabbor.firebaseservice.validation.PasswordError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val email: String = "",
    val isEmailVerified: Boolean = false,
    val isLoading: Boolean = false,
)

sealed interface HomeEvent {
    data class ShowMessage(@StringRes val message: Int) : HomeEvent
    object NavigateToAuth : HomeEvent
}

class HomeViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    init {
        val user = repository.currentUser
        if (user == null) {
            _events.trySend(HomeEvent.NavigateToAuth)
        } else {
            showUser(user)
        }
    }

    /** Reloads the user from Firebase, e.g. after they tap the verification link. */
    fun refresh(showResult: Boolean = false) {
        if (repository.currentUser == null) return
        launchWithLoading {
            val user = repository.reloadUser()
            showUser(user)
            if (showResult) {
                val message = if (user.isEmailVerified) R.string.message_email_verified
                else R.string.message_email_not_verified_yet
                _events.send(HomeEvent.ShowMessage(message))
            }
        }
    }

    fun resendVerification() = launchWithLoading {
        repository.sendEmailVerification()
        _events.send(HomeEvent.ShowMessage(R.string.message_verification_sent))
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        val error = when {
            currentPassword.isEmpty() -> R.string.error_current_password_required
            AuthValidator.validatePassword(newPassword) == PasswordError.EMPTY -> R.string.error_password_required
            AuthValidator.validatePassword(newPassword) == PasswordError.TOO_SHORT -> R.string.error_password_too_short
            currentPassword == newPassword -> R.string.error_password_unchanged
            else -> null
        }
        if (error != null) {
            _events.trySend(HomeEvent.ShowMessage(error))
            return
        }
        launchWithLoading {
            repository.changePassword(currentPassword, newPassword)
            _events.send(HomeEvent.ShowMessage(R.string.message_password_changed))
        }
    }

    fun deleteAccount(currentPassword: String) {
        if (currentPassword.isEmpty()) {
            _events.trySend(HomeEvent.ShowMessage(R.string.error_current_password_required))
            return
        }
        launchWithLoading {
            repository.deleteAccount(currentPassword)
            _events.send(HomeEvent.ShowMessage(R.string.message_account_deleted))
            _events.send(HomeEvent.NavigateToAuth)
        }
    }

    fun signOut() {
        repository.signOut()
        _events.trySend(HomeEvent.NavigateToAuth)
    }

    private fun showUser(user: FirebaseUser) {
        _state.update { it.copy(email = user.email.orEmpty(), isEmailVerified = user.isEmailVerified) }
    }

    private fun launchWithLoading(block: suspend () -> Unit) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseAuthInvalidUserException) {
                // The account was disabled or deleted elsewhere; the local session is stale.
                repository.signOut()
                _events.send(HomeEvent.ShowMessage(R.string.error_user_not_found))
                _events.send(HomeEvent.NavigateToAuth)
            } catch (e: Exception) {
                _events.send(HomeEvent.ShowMessage(AuthErrorMapper.toMessage(e)))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }
}
