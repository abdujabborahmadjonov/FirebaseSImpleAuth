package dev.abdujabbor.firebaseservice.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dev.abdujabbor.firebaseservice.MainActivity
import dev.abdujabbor.firebaseservice.R
import dev.abdujabbor.firebaseservice.databinding.ActivityHomeBinding
import dev.abdujabbor.firebaseservice.databinding.DialogPasswordBinding
import dev.abdujabbor.firebaseservice.util.collectWhenStarted

/** Account screen for a signed-in user. */
class HomeActivity : AppCompatActivity() {

    private val binding by lazy { ActivityHomeBinding.inflate(layoutInflater) }
    private val viewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.apply {
            btnRefresh.setOnClickListener { viewModel.refresh(showResult = true) }
            btnResendVerification.setOnClickListener { viewModel.resendVerification() }
            btnChangePassword.setOnClickListener { showChangePasswordDialog() }
            btnDeleteAccount.setOnClickListener { showDeleteAccountDialog() }
            btnSignOut.setOnClickListener { viewModel.signOut() }
        }

        collectWhenStarted(viewModel.state) { render(it) }
        collectWhenStarted(viewModel.events) { event ->
            when (event) {
                is HomeEvent.ShowMessage ->
                    Toast.makeText(this, event.message, Toast.LENGTH_LONG).show()
                HomeEvent.NavigateToAuth -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Pick up verification done in the mail app while we were in the background.
        viewModel.refresh()
    }

    private fun render(state: HomeUiState) = binding.run {
        tvEmail.text = getString(R.string.signed_in_as, state.email)
        tvVerificationStatus.setText(
            if (state.isEmailVerified) R.string.status_verified else R.string.status_not_verified
        )
        tvVerificationStatus.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (state.isEmailVerified) R.drawable.ic_verified else R.drawable.ic_warning, 0, 0, 0
        )
        verificationActions.visibility = if (state.isEmailVerified) View.GONE else View.VISIBLE

        progress.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        listOf(btnRefresh, btnResendVerification, btnChangePassword, btnDeleteAccount, btnSignOut)
            .forEach { it.isEnabled = !state.isLoading }
    }

    private fun showChangePasswordDialog() {
        val dialogBinding = DialogPasswordBinding.inflate(layoutInflater)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_change_password)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_save) { _, _ ->
                viewModel.changePassword(
                    dialogBinding.etCurrentPassword.text.toString(),
                    dialogBinding.etNewPassword.text.toString(),
                )
            }
            .show()
    }

    private fun showDeleteAccountDialog() {
        val dialogBinding = DialogPasswordBinding.inflate(layoutInflater)
        dialogBinding.tilNewPassword.visibility = View.GONE
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_delete_account)
            .setMessage(R.string.delete_account_warning)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteAccount(dialogBinding.etCurrentPassword.text.toString())
            }
            .show()
    }
}
