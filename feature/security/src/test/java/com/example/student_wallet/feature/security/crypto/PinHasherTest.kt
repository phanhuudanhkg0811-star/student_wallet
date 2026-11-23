package com.example.student_wallet.feature.security.crypto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    private val hasher = PinHasher(defaultIterations = 1_000)

    @Test
    fun `isValidPin accepts 6 ASCII digits only`() {
        assertTrue(hasher.isValidPin("012345"))
        assertFalse(hasher.isValidPin("12345"))
        assertFalse(hasher.isValidPin("1234567"))
        assertFalse(hasher.isValidPin("12345a"))
        assertFalse(hasher.isValidPin("١٢٣٤٥٦"))
    }

    @Test
    fun `hash generates unique random salts and verify checks correctly`() {
        val first = hasher.hash("246810")
        val second = hasher.hash("246810")

        assertNotEquals(first, second)
        assertTrue(hasher.verify("246810", first))
        assertTrue(hasher.verify("246810", second))
        assertFalse(hasher.verify("246811", first))
        assertFalse(hasher.verify("123456", "malformed-payload"))
    }
}
