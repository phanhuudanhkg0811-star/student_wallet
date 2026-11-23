package com.example.student_wallet.feature.budget

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.student_wallet.core.ui.components.PlaceholderScreen
import com.example.student_wallet.core.ui.theme.Student_walletTheme

@Composable
fun BudgetScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Ngân sách & Thống kê (:feature:budget)",
        features = listOf(
            "Feature 05 & 27: Glance AppWidget & Quick Add",
            "Feature 07: Ngân sách nhiều kỳ & Daily Safe-to-Spend",
            "Feature 08: [TBD - Đang thiết kế lại]",
            "Feature 09: Hũ tiết kiệm (Piggy Bank) & Lottie",
            "Feature 10 & 11: Cảnh báo vượt mức & Kéo-thả điều chuyển",
            "Feature 12, 25, 26: Nhật ký Heatmap & Biểu đồ Vico"
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun BudgetScreenPreview() {
    Student_walletTheme {
        BudgetScreen()
    }
}
