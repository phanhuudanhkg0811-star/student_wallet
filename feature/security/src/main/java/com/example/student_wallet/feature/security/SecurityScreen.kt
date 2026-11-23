package com.example.student_wallet.feature.security

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.student_wallet.core.ui.components.PlaceholderScreen
import com.example.student_wallet.core.ui.theme.Student_walletTheme

@Composable
fun SecurityScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Bảo mật & Cài đặt (:feature:security)",
        features = listOf(
            "Feature 28: Khóa app bằng Vân tay & PIN 6 số (Auto-lock 30s)",
            "Feature 29: Sao lưu & khôi phục mã hóa AES-256 (.wallet)",
            "Feature 30: Đổi tiền tệ, Theme & Xuất báo cáo PDF/Excel/CSV"
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun SecurityScreenPreview() {
    Student_walletTheme {
        SecurityScreen()
    }
}
