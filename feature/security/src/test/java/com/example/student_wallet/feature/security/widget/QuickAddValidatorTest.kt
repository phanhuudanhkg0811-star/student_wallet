package com.example.student_wallet.feature.security.widget

import com.example.student_wallet.core.domain.model.Category
import com.example.student_wallet.core.domain.model.PaymentMethod
import com.example.student_wallet.core.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickAddValidatorTest {

    @Test
    fun `sanitizeDigits removes symbols and caps digit count`() {
        assertEquals("35000", QuickAddValidator.sanitizeDigits("35,000đ abc"))
        assertEquals("12345678901", QuickAddValidator.sanitizeDigits("123456789012345"))
    }

    @Test
    fun `parseAndValidateAmount rejects zero invalid and over max 999M`() {
        assertNull(QuickAddValidator.parseAndValidateAmount(""))
        assertNull(QuickAddValidator.parseAndValidateAmount("0"))
        assertNull(QuickAddValidator.parseAndValidateAmount("999000001"))

        assertEquals(1L, QuickAddValidator.parseAndValidateAmount("1"))
        assertEquals(45_000L, QuickAddValidator.parseAndValidateAmount("45000"))
        assertEquals(999_000_000L, QuickAddValidator.parseAndValidateAmount("999000000"))
    }

    @Test
    fun `buildTransaction maps category and defaults blank note`() {
        val category = Category(
            id = 3L,
            name = "Di chuyển",
            emoji = "🚌",
            type = TransactionType.EXPENSE,
            colorHex = 0xFF4C6359L,
        )
        val built = QuickAddValidator.buildTransaction(
            amount = 15_000L,
            type = TransactionType.EXPENSE,
            category = category,
            note = "   ",
            paymentMethod = PaymentMethod.MOMO,
            timestampEpochMillis = 12345L,
        )

        assertEquals(15_000L, built.amount)
        assertEquals(3L, built.categoryId)
        assertEquals("Di chuyển", built.categoryName)
        assertEquals("🚌", built.categoryEmoji)
        assertEquals("Chi nhanh từ Widget", built.note)
        assertEquals(PaymentMethod.MOMO, built.paymentMethod)
        assertEquals(12345L, built.timestampEpochMillis)
    }
}
