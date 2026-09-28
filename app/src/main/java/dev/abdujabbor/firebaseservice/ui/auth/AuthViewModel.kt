package dev.abdujabbor.firebaseservice.ui.auth

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.abdujabbor.firebaseservice.R
import dev.abdujabbor.firebaseservice.data.AuthErrorMapper
import dev.abdujabbor.firebaseservice.data.AuthRepository
import dev.abdujabbor.firebaseservice.validation.AuthValidator
import dev.abdujabbor.firebaseservice.validation.EmailError
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

enum class AuthMode { LOGIN, REGISTER }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val isLoading: Boolean = false,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmPasswordError: Int? = null,
)

sealed interface AuthEvent {
    data class ShowMessage(@StringRes val message: Int) : AuthEvent
    object NavigateHome : AuthEvent
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events: Flow<AuthEvent> = _events.receiveAsFlow()

    val isSignedIn: Boolean
        get() = repository.currentUser != null

    fun toggleMode() {
        _state.update {
            AuthUiState(
                mode = if (it.mode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN,
                isLoading = it.isLoading,
            )
        }
    }

    fun clearErrors() {
        _state.update { it.copy(emailError = null, passwordError = null, confirmPasswordError = null) }
    }

    fun submit(email: String, password: String, confirmPassword: String) {
        val mode = _state.value.mode
        val emailError = emailErrorMessage(AuthValidator.validateEmail(email))
        val passwordError = passwordErrorMessage(AuthValidator.validatePassword(password))
        val confirmError = if (
            mode == AuthMode.REGISTER && !AuthValidator.passwordsMatch(password, confirmPassword)
        ) R.string.error_passwords_do_not_match else null

        _state.update {
            it.copy(emailError = emailError, passwordError = passwordError, confirmPasswordError = confirmError)
        }
        if (emailError != null || passwordError != null || confirmError != null) return

        launchWithLoading {
            when (mode) {
                AuthMode.LOGIN -> repository.login(email.trim(), password)
                AuthMode.REGISTER -> {
                    repository.register(email.trim(), password)
                    _events.send(AuthEvent.ShowMessage(R.string.message_verification_sent))
                }
            }
            _events.send(AuthEvent.NavigateHome)
        }
    }

    fun sendPasswordReset(email: String) {
        val emailError = emailErrorMessage(AuthValidator.validateEmail(email))
        _state.update { it.copy(emailError = emailError, passwordError = null, confirmPasswordError = null) }
        if (emailError != null) return

        launchWithLoading {
            repository.sendPasswordReset(email.trim())
            _events.send(AuthEvent.ShowMessage(R.string.message_reset_email_sent))
        }
    }

    private fun launchWithLoading(block: suspend () -> Unit) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(AuthEvent.ShowMessage(AuthErrorMapper.toMessage(e)))
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun emailErrorMessage(error: EmailError?): Int? = when (error) {
        EmailError.EMPTY -> R.string.error_email_required
        EmailError.INVALID -> R.string.error_email_invalid
        null -> null
    }

    private fun passwordErrorMessage(error: PasswordError?): Int? = when (error) {
        PasswordError.EMPTY -> R.string.error_password_required
        PasswordError.TOO_SHORT -> R.string.error_password_too_short
        null -> null
    }
}
