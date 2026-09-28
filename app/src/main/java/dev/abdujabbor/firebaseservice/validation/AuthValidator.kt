package dev.abdujabbor.firebaseservice.validation

enum class EmailError { EMPTY, INVALID }

enum class PasswordError { EMPTY, TOO_SHORT }

enum class PasswordStrength { WEAK, MEDIUM, STRONG }

/**
 * Pure-Kotlin input validation for the auth forms. It has no Android dependencies
 * so it can be covered by plain JVM unit tests.
 */
object AuthValidator {

    /** Firebase rejects passwords shorter than this. */
    const val MIN_PASSWORD_LENGTH = 6

    private val EMAIL_REGEX =
        Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    fun validateEmail(email: String): EmailError? {
        val trimmed = email.trim()
        return when {
            trimmed.isEmpty() -> EmailError.EMPTY
            !EMAIL_REGEX.matches(trimmed) -> EmailError.INVALID
            else -> null
        }
    }

    fun validatePassword(password: String): PasswordError? = when {
        password.isEmpty() -> PasswordError.EMPTY
        password.length < MIN_PASSWORD_LENGTH -> PasswordError.TOO_SHORT
        else -> null
    }

    fun passwordsMatch(password: String, confirmation: String): Boolean =
        password == confirmation

    /**
     * Scores a password on length and character variety. Anything shorter than
     * [MIN_PASSWORD_LENGTH] is always [PasswordStrength.WEAK].
     */
    fun passwordStrength(password: String): PasswordStrength {
        if (password.length < MIN_PASSWORD_LENGTH) return PasswordStrength.WEAK
        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isLowerCase() } && password.any { it.isUpperCase() }) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return when {
            score >= 4 -> PasswordStrength.STRONG
            score >= 2 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.WEAK
        }
    }
}
