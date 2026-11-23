package com.example.student_wallet.core.domain.format

import com.example.student_wallet.core.domain.model.CurrencyOption
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormatterTest {

    @Test
    fun `format VND groups thousands with dot and appends dong symbol`() {
        assertEquals("45.000 đ", MoneyFormatter.format(45_000L, CurrencyOption.VND))
        assertEquals("2.450.000 đ", MoneyFormatter.format(2_450_000L, CurrencyOption.VND))
        assertEquals("0 đ", MoneyFormatter.format(0L, CurrencyOption.VND))
    }

    @Test
    fun `format USD and EUR prepend currency symbol with comma grouping`() {
        assertEquals("$1,250", MoneyFormatter.format(1_250L, CurrencyOption.USD))
        assertEquals("€990", MoneyFormatter.format(990L, CurrencyOption.EUR))
    }

    @Test
    fun `formatSigned prepends plus for income and minus for expense`() {
        assertEquals("+5.000.000 đ", MoneyFormatter.formatSigned(5_000_000L, isIncome = true))
        assertEquals("−186.500 đ", MoneyFormatter.formatSigned(186_500L, isIncome = false))
    }
}
