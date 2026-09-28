package dev.abdujabbor.firebaseservice.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidatorTest {

    @Test
    fun emptyEmail_isRejected() {
        assertEquals(EmailError.EMPTY, AuthValidator.validateEmail(""))
        assertEquals(EmailError.EMPTY, AuthValidator.validateEmail("   "))
    }

    @Test
    fun malformedEmails_areRejected() {
        listOf("plainaddress", "no-at.example.com", "a@b", "a@b.", "@example.com", "a b@example.com")
            .forEach { assertEquals(it, EmailError.INVALID, AuthValidator.validateEmail(it)) }
    }

    @Test
    fun validEmails_areAccepted() {
        listOf("user@example.com", "first.last+tag@mail.co.uk", "  padded@example.org  ")
            .forEach { assertNull(it, AuthValidator.validateEmail(it)) }
    }

    @Test
    fun passwordValidation() {
        assertEquals(PasswordError.EMPTY, AuthValidator.validatePassword(""))
        assertEquals(PasswordError.TOO_SHORT, AuthValidator.validatePassword("12345"))
        assertNull(AuthValidator.validatePassword("123456"))
    }

    @Test
    fun passwordsMatch() {
        assertTrue(AuthValidator.passwordsMatch("secret1", "secret1"))
        assertFalse(AuthValidator.passwordsMatch("secret1", "Secret1"))
    }

    @Test
    fun passwordStrength() {
        assertEquals(PasswordStrength.WEAK, AuthValidator.passwordStrength("Ab1!"))
        assertEquals(PasswordStrength.WEAK, AuthValidator.passwordStrength("abcdef"))
        assertEquals(PasswordStrength.MEDIUM, AuthValidator.passwordStrength("abcdefgh1"))
        assertEquals(PasswordStrength.STRONG, AuthValidator.passwordStrength("Abcdefgh1!"))
        assertEquals(PasswordStrength.STRONG, AuthValidator.passwordStrength("Abcdefghijk1"))
    }
}
