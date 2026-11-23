package com.example.student_wallet.core.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Test

class BudgetLevelTest {

    @Test
    fun `budgetLevel returns OK below 60 percent`() {
        assertEquals(BudgetLevel.OK, budgetLevel(spent = 0L, limit = 1_000_000L))
        assertEquals(BudgetLevel.OK, budgetLevel(spent = 590_000L, limit = 1_000_000L))
    }

    @Test
    fun `budgetLevel returns WARN from 60 to 85 percent inclusive`() {
        assertEquals(BudgetLevel.WARN, budgetLevel(spent = 600_000L, limit = 1_000_000L))
        assertEquals(BudgetLevel.WARN, budgetLevel(spent = 850_000L, limit = 1_000_000L))
    }

    @Test
    fun `budgetLevel returns BAD above 85 percent or when overspent`() {
        assertEquals(BudgetLevel.BAD, budgetLevel(spent = 860_000L, limit = 1_000_000L))
        assertEquals(BudgetLevel.BAD, budgetLevel(spent = 420_000L, limit = 400_000L))
        assertEquals(BudgetLevel.BAD, budgetLevel(spent = 100L, limit = 0L))
    }

    @Test
    fun `budgetProgress clamps between 0 and 1`() {
        assertEquals(0f, budgetProgress(spent = -50L, limit = 1_000L), 0.0001f)
        assertEquals(0.55f, budgetProgress(spent = 1_100_000L, limit = 2_000_000L), 0.0001f)
        assertEquals(1f, budgetProgress(spent = 420_000L, limit = 400_000L), 0.0001f)
    }
}
