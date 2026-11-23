# Tổng hợp tính năng StudentWallet

Tổng hợp từ đề cương **CSC13009 — StudentWallet** và chia đều **6 tính năng / thành viên** (xem bảng phân công chi tiết tại [TEAM_ALLOCATION.md](TEAM_ALLOCATION.md)).
*(Các tính năng **02, 06, 08, 14** tạm đánh dấu **`[TBD]`** — 4 bạn được **tự do đề xuất ý tưởng, không bị ràng buộc phải khớp với module hiện tại** và có thể làm vào giai đoạn cuối).*

---

## 1. Module `:feature:transaction` — Phụ trách: Phan Van Viet (`6 tính năng`)

| ID | Tính năng | Mô tả ngắn & Công nghệ |
| :-: | :--- | :--- |
| **01** | Custom Financial Numeric Keypad | Phím số tài chính (`+`, `-`, `×`, `÷`), Haptic feedback, format VND, validate `0 < số tiền <= 999 triệu`. |
| **02** | `[TBD - Viet tự chọn]` | *Tự do đề xuất ý tưởng (không bắt buộc thuộc transaction; có thể làm cuối).* |
| **03** | Recurring Expense Management | `WorkManager` (`PeriodicWorkRequest`) tự tạo giao dịch cố định hàng tháng lúc `00:01` & thông báo xác nhận. |
| **04** | Full-text Search FTS4 & Multi-filter | Room FTS4 tìm kiếm `< 16ms` có highlight từ khóa; lọc theo Date Range, Range Slider, phương thức thanh toán. |
| **17** | Bulk Data Import (CSV / Excel) | Đọc sao kê `.csv`, `.xlsx`, bảng Preview, map cột, import trên Coroutine IO, lọc trùng lặp. |
| **26** | Periodic Trend Comparison | Phân tích biến động thu/chi theo từng tháng (MoM), tự sinh nhận xét xu hướng, tính Savings Rate. |

## 2. Module `:feature:scanner` — Phụ trách: Phi Hoang Duc (`6 tính năng`)

| ID | Tính năng | Mô tả ngắn & Công nghệ |
| :-: | :--- | :--- |
| **13** | Automatic Receipt Capture | CameraX + thuật toán Laplacian variance tự chụp khi ảnh nét nhất, hỗ trợ flash, tap-to-focus, chọn từ Gallery. |
| **14** | `[TBD - Duc tự chọn]` | *Tự do đề xuất ý tưởng (không bắt buộc thuộc scanner; có thể làm cuối).* |
| **15** | Receipt Verification & Matching | Review Assistant đối chiếu ảnh gốc + dữ liệu OCR, sửa nhanh, tự map danh mục (`WinMart -> Groceries`), lưu WebP. |
| **16** | VietQR & MoMo QR Reader | ML Kit Barcode Scanning giải mã chuẩn EMVCo, tách STK, ngân hàng, số tiền, nội dung chuyển khoản vào form. |
| **18** | Local Receipt Image Library | Xem ảnh hóa đơn độ phân giải cao với Coil, hỗ trợ xoay `90°`, zoom, xóa giải phóng bộ nhớ. |
| **22** | One-Tap Debt Payment (VietQR) | Sinh mã VietQR cá nhân để trả nợ nhóm, quét chuyển khoản, xác nhận đã trả và đính kèm ảnh biên lai. |

## 3. Module `:feature:budget` — Phụ trách: Nguyen Trung Chinh (`6 tính năng`)

| ID | Tính năng | Mô tả ngắn & Công nghệ |
| :-: | :--- | :--- |
| **07** | Flexible Multi-period Budget | Tạo hạn mức tháng/danh mục qua Slider, validate tổng hạn mức con `<=` thu nhập, tính Daily Safe-to-Spend. |
| **08** | `[TBD - Chinh tự chọn]` | *Tự do đề xuất ý tưởng (không bắt buộc thuộc budget; có thể làm cuối).* |
| **09** | Saving Piggy Bank Goals | Tạo mục tiêu tiết kiệm, nạp từ ví chính, dự báo ngày đạt mục tiêu, Lottie Confetti khi đạt 100%. |
| **10** | Tiered Overspending Alerts | Smart Notification khi vượt `80%`, `90%`, `100%` hạn mức, cho phép bật/tắt cảnh báo từng danh mục. |
| **11** | Drag-and-Drop Budget Reallocation | Kéo-thả card để chuyển phần ngân sách dư sang danh mục khác, lưu lịch sử điều chỉnh vào Room. |
| **25** | Interactive Spending Charts | Donut Chart, Stacked Bar, Trend Line bằng Vico / Compose Canvas; chạm lát Donut để lọc chi tiêu con. |

## 4. Module `:feature:group` — Phụ trách: Nguyen Anh Thai (`6 tính năng`)

| ID | Tính năng | Mô tả ngắn & Công nghệ |
| :-: | :--- | :--- |
| **06** | `[TBD - Thai tự chọn]` | *Tự do đề xuất ý tưởng (không bắt buộc thuộc group; có thể làm cuối).* |
| **19** | Roommate / Travel Group Management | Room N-N (`GroupEntity`, `MemberEntity`, `GroupMemberCrossRef`), avatar màu, quy tắc chia mặc định, quyền admin local. |
| **20** | 4-Mode Expense Splitting Toolkit | 4 chế độ chia: Đều, %, Số tiền cố định, Theo món; kiểm tra tổng chia = `100%` hóa đơn (lệch = `0 VND`). |
| **21** | Debt Path Optimization (Min-Cash Flow) | Thuật toán Greedy Min-Cash Flow tối ưu giao dịch trả nợ từ $O(N^2)$ xuống còn tối đa $N - 1$ giao dịch. |
| **23** | Group Settlement Export | Xuất bảng tất toán (ai trả, ai nợ ai, số dư cuối) ra PDF, `.xlsx`, CSV và chia sẻ qua Zalo/Messenger. |
| **24** | Group Cash Flow Timeline | Timeline toàn bộ chi tiêu & trả nợ của nhóm, lọc theo thành viên, Safe Revert không làm lệch đồ thị nợ. |

## 5. Module `:feature:security` (+ Widget & Audit) — Phụ trách: Phan Huu Danh (`6 tính năng`)

| ID | Tính năng | Mô tả ngắn & Công nghệ |
| :-: | :--- | :--- |
| **05** | Quick Expense Entry từ Widget | Nút "Quick Add" trên Widget mở Translucent Dialog nhập nhanh số tiền, lưu DB và cập nhật Widget. |
| **12** | Financial Discipline Audit Journal | Tổng kết tháng kèm Financial Score `0–100`, Heatmap Calendar kiểu GitHub Contribution Graph. |
| **27** | Glance AppWidget | `androidx.glance` hỗ trợ `2x2`, `4x1`, `4x2` hiển thị số dư, chi tiêu hôm nay, cảnh báo ngân sách. |
| **28** | Biometric & PIN App Lock | `BiometricPrompt` + PIN 6 số băm PBKDF2 trong Android Keystore; tự khóa sau `30 giây` ở chế độ nền. |
| **29** | AES-256 Encrypted Backup & Restore | Xuất DB & ảnh hóa đơn ra file `.wallet` mã hóa AES-256 CBC; xác thực SHA-256 checksum khi restore. |
| **30** | Currency, Theme & Personal Export | Đổi tiền tệ (`VND/USD/EUR`), Dark/Light Theme (DataStore); xuất báo cáo tài chính cá nhân ra PDF, `.xlsx`, CSV. |

---

## Các tiểu tiết UX/UI gốc vẫn giữ để làm kèm (từ Proposal cũ của mục 02, 06, 08)

Ba ý cũ của mục **02, 06, 08** tuy quá nhỏ để tính thành một tính năng đồ án độc lập, nhưng đều **rất cần cho trải nghiệm app và dễ cài đặt**, nên vẫn được làm lồng ghép trực tiếp vào các màn hình chính (nhường slot `02, 06, 08` cho tính năng mới xứng tầm hơn):

- **Gợi ý danh mục tự động cơ bản** *(ý cũ của `02`)* → Làm kèm trong **Feature `01` (`:feature:transaction`)**: Gợi ý nhanh danh mục từ ghi chú (Rule Engine / từ khóa cơ bản hoặc Naive Bayes đơn giản) để người dùng đỡ chọn tay nhiều lần.
- **Undo 5 giây khi xóa giao dịch** *(ý cũ của `06`)* → Làm kèm trong **Feature `01` / `04` (`:feature:transaction`)**: Soft-delete (`is_deleted = true`) kèm Snackbar đếm ngược 5 giây để hoàn tác trước khi xóa thật & tính lại ngân sách.
- **Progress Bar đổi màu động cho Ngân sách** *(ý cũ của `08`)* → Làm kèm trong **Feature `07` (`:feature:budget`)**: Thanh tiến độ Canvas/Compose với `animateColorAsState`: xanh (`< 60%`), cam (`60–85%`), đỏ nhấp nháy (`> 85%`).

---

## Tech Stack tham khảo từ Proposal

- **UI:** Jetpack Compose, Material Design 3, Lottie Compose, Vico Charting Library, Coil
- **Local Storage:** Room (FTS4, 1-N & N-N Relations, Database Views) + Encrypted DataStore
- **Background & OS:** WorkManager, Glance AppWidget, Android Canvas PDF Generator
- **Computer Vision:** CameraX + Google ML Kit Text Recognition v2 + ML Kit Barcode Scanning
- **Security:** `BiometricPrompt` + PIN 6 chữ số (PBKDF2), Android Keystore, AES-256 CBC
