package com.example.student_wallet.feature.transaction

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.student_wallet.core.ui.components.PlaceholderScreen
import com.example.student_wallet.core.ui.theme.Student_walletTheme

@Composable
fun TransactionScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Giao dịch (:feature:transaction)",
        features = listOf(
            "Feature 01: Bàn phím số tài chính (Numeric Keypad)",
            "Feature 02: [TBD - Đang thiết kế lại]",
            "Feature 03: Giao dịch định kỳ (WorkManager)",
            "Feature 04: Tìm kiếm FTS4 & Bộ lọc đa chiều",
            "Feature 06: [TBD - Đang thiết kế lại]",
            "Feature 17: Nhập sao kê từ CSV / Excel"
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun TransactionScreenPreview() {
    Student_walletTheme {
        TransactionScreen()
    }
}
