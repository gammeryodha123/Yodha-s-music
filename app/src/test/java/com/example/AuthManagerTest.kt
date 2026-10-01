package com.example

import com.example.data.AuthManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthManagerTest {

    @Test
    fun `test email validation failure on invalid email`() {
        val result = AuthManager.signInWithEmail("invalidemail", "password123")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test password validation failure on short password`() {
        val result = AuthManager.signInWithEmail("user@example.com", "123")
        assertTrue(result.isFailure)
    }

    @Test
    fun `test seed account sign in success`() {
        val result = AuthManager.signInWithEmail("patchavasunitha@gmail.com", "password123")
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("patchavasunitha@gmail.com", user?.email)
        assertEquals("Sunitha Patchava", user?.name)
    }

    @Test
    fun `test sign in failure on incorrect password`() {
        val result = AuthManager.signInWithEmail("patchavasunitha@gmail.com", "wrongpassword")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Incorrect password") == true)
    }

    @Test
    fun `test sign up creates account and allows sign in`() {
        val newEmail = "testuser_${System.currentTimeMillis()}@test.com"
        val signUpResult = AuthManager.signUpWithEmail("Test Musician", newEmail, "mypassword123")
        assertTrue(signUpResult.isSuccess)

        val signInResult = AuthManager.signInWithEmail(newEmail, "mypassword123")
        assertTrue(signInResult.isSuccess)
        assertEquals("Test Musician", signInResult.getOrNull()?.name)
    }

    @Test
    fun `test google sign in creates authenticated google user`() {
        val googleUser = AuthManager.signInWithGoogleAccount(
            email = "patchavasunitha@gmail.com",
            name = "Sunitha Patchava"
        )
        assertEquals("patchavasunitha@gmail.com", googleUser.email)
        assertEquals("Sunitha Patchava", googleUser.name)
        assertEquals("google", googleUser.authProvider)
    }

    @Test
    fun `test password reset updates password`() {
        val resetResult = AuthManager.resetPassword("patchavasunitha@gmail.com", "newsecret999")
        assertTrue(resetResult.isSuccess)

        val signInWithNewPass = AuthManager.signInWithEmail("patchavasunitha@gmail.com", "newsecret999")
        assertTrue(signInWithNewPass.isSuccess)
    }
}
