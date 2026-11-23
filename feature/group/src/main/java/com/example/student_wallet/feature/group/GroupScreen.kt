package com.example.student_wallet.feature.group

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.student_wallet.core.ui.components.PlaceholderScreen
import com.example.student_wallet.core.ui.theme.Student_walletTheme

@Composable
fun GroupScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Chia tiền nhóm (:feature:group)",
        features = listOf(
            "Feature 19: Quản lý nhóm phòng trọ / du lịch (Room N-N)",
            "Feature 20: Chia tiền 4 chế độ (Đều / % / Cố định / Theo món)",
            "Feature 21: Thuật toán tối ưu trả nợ Min-Cash Flow",
            "Feature 22: Thanh toán nhanh qua VietQR cá nhân",
            "Feature 23: Xuất bảng tất toán PDF / Excel / CSV",
            "Feature 24: Lịch sử dòng tiền nhóm & Safe Revert"
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun GroupScreenPreview() {
    Student_walletTheme {
        GroupScreen()
    }
}
