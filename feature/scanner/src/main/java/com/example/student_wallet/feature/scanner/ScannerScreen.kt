package com.example.student_wallet.feature.scanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.student_wallet.core.ui.components.PlaceholderScreen
import com.example.student_wallet.core.ui.theme.Student_walletTheme

@Composable
fun ScannerScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        title = "Quét hóa đơn & QR (:feature:scanner)",
        features = listOf(
            "Feature 13: Chụp hóa đơn tự động bằng CameraX",
            "Feature 14: [TBD - Đang bổ sung mới]",
            "Feature 15: Đối chiếu OCR & lưu ảnh WebP",
            "Feature 16: Đọc mã VietQR & MoMo (EMVCo)",
            "Feature 18: Thư viện ảnh hóa đơn (Zoom / Xoay 90°)"
        ),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
private fun ScannerScreenPreview() {
    Student_walletTheme {
        ScannerScreen()
    }
}
