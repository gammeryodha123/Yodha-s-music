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
    fun `test valid email sign in creates user`() {
        val result = AuthManager.signInWithEmail("john.doe@example.com", "securePassword123")
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals("john.doe@example.com", user?.email)
        assertEquals("John.doe", user?.name)
    }

    @Test
    fun `test demo sign in creates VIP user`() {
        val demoUser = AuthManager.signInDemoUser()
        assertEquals("vip@yodhamusic.app", demoUser.email)
        assertEquals("Yodha VIP", demoUser.name)
        assertEquals(false, demoUser.isGuest)
    }

    @Test
    fun `test guest sign in creates guest profile`() {
        val guest = AuthManager.signInAsGuest()
        assertTrue(guest.isGuest)
        assertEquals("Guest Listener", guest.name)
    }
}
