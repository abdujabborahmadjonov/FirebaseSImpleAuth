package dev.abdujabbor.firebaseservice

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import dev.abdujabbor.firebaseservice.databinding.ActivityMainBinding
import dev.abdujabbor.firebaseservice.ui.auth.AuthEvent
import dev.abdujabbor.firebaseservice.ui.auth.AuthMode
import dev.abdujabbor.firebaseservice.ui.auth.AuthUiState
import dev.abdujabbor.firebaseservice.ui.auth.AuthViewModel
import dev.abdujabbor.firebaseservice.ui.home.HomeActivity
import dev.abdujabbor.firebaseservice.util.collectWhenStarted
import dev.abdujabbor.firebaseservice.validation.AuthValidator
import dev.abdujabbor.firebaseservice.validation.PasswordStrength

/** Sign-in / registration screen. Signed-in users go straight to [HomeActivity]. */
class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (viewModel.isSignedIn) {
            openHome()
            return
        }
        setContentView(binding.root)

        binding.apply {
            btnSubmit.setOnClickListener { submit() }
            btnToggleMode.setOnClickListener { viewModel.toggleMode() }
            btnForgotPassword.setOnClickListener {
                viewModel.sendPasswordReset(etEmail.text.toString())
            }
            etEmail.doAfterTextChanged { viewModel.clearErrors() }
            etConfirmPassword.doAfterTextChanged { viewModel.clearErrors() }
            etPassword.doAfterTextChanged {
                viewModel.clearErrors()
                renderPasswordStrength(it?.toString().orEmpty())
            }
            etConfirmPassword.setOnEditorActionListener { _, _, _ -> submit(); true }
        }

        collectWhenStarted(viewModel.state) { render(it) }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is AuthEvent.ShowMessage ->
                    Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
                AuthEvent.NavigateHome -> openHome()
            }
        }
    }

    private fun submit() = binding.run {
        viewModel.submit(
            etEmail.text.toString(),
            etPassword.text.toString(),
            etConfirmPassword.text.toString(),
        )
    }

    private fun render(state: AuthUiState) = binding.run {
        val isRegister = state.mode == AuthMode.REGISTER
        tvTitle.setText(if (isRegister) R.string.title_register else R.string.title_login)
        btnSubmit.setText(if (isRegister) R.string.action_register else R.string.action_login)
        btnToggleMode.setText(if (isRegister) R.string.action_have_account else R.string.action_need_account)
        tilConfirmPassword.visibility = if (isRegister) View.VISIBLE else View.GONE
        tvPasswordStrength.visibility = if (isRegister) View.VISIBLE else View.GONE
        btnForgotPassword.visibility = if (isRegister) View.GONE else View.VISIBLE
        renderPasswordStrength(etPassword.text?.toString().orEmpty())

        tilEmail.error = state.emailError?.let { getString(it) }
        tilPassword.error = state.passwordError?.let { getString(it) }
        tilConfirmPassword.error = state.confirmPasswordError?.let { getString(it) }

        progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        listOf(etEmail, etPassword, etConfirmPassword, btnSubmit, btnToggleMode, btnForgotPassword)
            .forEach { it.isEnabled = !state.isLoading }
    }

    private fun renderPasswordStrength(password: String) = binding.tvPasswordStrength.run {
        if (password.isEmpty()) {
            text = ""
            return@run
        }
        val (label, color) = when (AuthValidator.passwordStrength(password)) {
            PasswordStrength.WEAK -> R.string.password_strength_weak to R.color.strength_weak
            PasswordStrength.MEDIUM -> R.string.password_strength_medium to R.color.strength_medium
            PasswordStrength.STRONG -> R.string.password_strength_strong to R.color.strength_strong
        }
        text = getString(R.string.password_strength, getString(label))
        setTextColor(ContextCompat.getColor(this@MainActivity, color))
    }

    private fun openHome() {
        startActivity(Intent(this, HomeActivity::class.java))
        finish()
    }
}
