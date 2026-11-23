# Thư mục `feature/`

Chứa **5 module tính năng độc lập** chia cho **5 thành viên**:

| Module | Phụ trách chính |
| :--- | :--- |
| `:feature:transaction` | Giao dịch, Keypad, Search FTS4, Recurring, CSV/Excel Import |
| `:feature:scanner` | CameraX OCR, Review Assistant, VietQR/MoMo, Thư viện hóa đơn |
| `:feature:budget` | Ngân sách, Cảnh báo, Piggy Bank, Biểu đồ Vico, Audit Heatmap, Widget |
| `:feature:group` | Nhóm N-N, Chia tiền 4 mode, Min-Cash Flow, VietQR Pay, Export |
| `:feature:security` | Biometric + PIN, Backup AES-256, Cài đặt & Export cá nhân |

**Quy tắc:** Các module trong `feature/` chỉ phụ thuộc vào `:core`, **không** import chéo lẫn nhau.
Trong mỗi module, bạn có thể tự tổ chức theo chuẩn MVI (`XxxContract.kt`, `XxxViewModel.kt`, `XxxScreen.kt`).
