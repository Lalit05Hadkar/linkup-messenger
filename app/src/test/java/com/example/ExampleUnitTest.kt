package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testUserIdRegexRules() {
        val regex = Regex("^(?!\\.)[a-z0-9._]{4,20}(?<!\\.)$")

        // Valid user IDs
        assertTrue(regex.matches("alex_dev"))
        assertTrue(regex.matches("sarah.c"))
        assertTrue(regex.matches("user123"))
        assertTrue(regex.matches("john_doe_99"))

        // Invalid user IDs
        assertFalse(regex.matches(".alex"))      // leading dot
        assertFalse(regex.matches("alex."))      // trailing dot
        assertFalse(regex.matches("al"))         // too short (< 4 chars)
        assertFalse(regex.matches("a".repeat(21))) // too long (> 20 chars)
        assertFalse(regex.matches("Alex_Dev"))   // uppercase (must be lowercase)
        assertFalse(regex.matches("alex!dev"))   // disallowed special character
    }

    @Test
    fun testDeterministicChatIdGeneration() {
        fun getChatId(uidA: String, uidB: String): String {
            return if (uidA < uidB) "${uidA}_$uidB" else "${uidB}_$uidA"
        }

        val id1 = getChatId("uid_user1", "uid_user2")
        val id2 = getChatId("uid_user2", "uid_user1")

        assertEquals("uid_user1_uid_user2", id1)
        assertEquals(id1, id2) // Deterministic regardless of argument order
    }
}
