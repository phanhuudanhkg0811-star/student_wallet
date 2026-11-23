package com.example.student_wallet.feature.security.crypto

/** Abstracts hardware/OS key-wrapping around serialized secret strings for testability. */
interface SecretProtector {
    fun encrypt(plaintext: String): String
    fun decrypt(payload: String): String?
}
