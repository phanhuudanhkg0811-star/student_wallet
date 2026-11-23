# Phân công tính năng cho 5 thành viên (6 – 6 – 6 – 6 – 6)

Tài liệu này chia **đều 30 vị trí tính năng (mỗi người đúng 6 tính năng)** từ [FEATURES.md](FEATURES.md) cho **5 thành viên nhóm 07 – FriendForever**.
Trong đó, **4 tính năng `[TBD]` (`02`, `06`, `08`, `14`)** được chia đều **mỗi người 1 slot cho 4 thành viên còn lại tự đề xuất và điền bổ sung**.

---

## 1. Bảng phân công đều 6 tính năng / người

| STT | Thành viên (MSSV) | Module chính phụ trách | 6 tính năng được giao | Trong đó `[TBD]` | Trọng tâm công việc |
| :-: | :--- | :--- | :--- | :---: | :--- |
| 1 | **Phan Huu Danh** (`24120280`) — *Leader* | `:app`, `:core`, `:feature:security` | **05, 12, 27, 28, 29, 30** *(6)* | `0` | Dựng `:core` & `:app`, CI/PR Review, Biometric + PIN PBKDF2 (`28`), Backup AES-256 (`29`), Currency/Theme & Export cá nhân (`30`), Glance AppWidget (`05`, `27`), Nhật ký kỷ luật Heatmap (`12`). |
| 2 | **Phan Van Viet** (`24120244`) | `:feature:transaction` | **01, 02, 03, 04, 17, 26** *(6)* | **Feature `02`** *(1)* | Bàn phím số tài chính (`01`), `WorkManager` định kỳ (`03`), Room `FTS4` Search `< 16ms` (`04`), Import `.csv`/`.xlsx` (`17`), So sánh xu hướng chi tiêu theo kỳ MoM (`26`) + **Feature `02 [TBD]`**. |
| 3 | **Phi Hoang Duc** (`24120248`) | `:feature:scanner` | **13, 14, 15, 16, 18, 22** *(6)* | **Feature `14`** *(1)* | `CameraX` Laplacian (`13`), `ML Kit OCR` Review + WebP (`15`), Đọc QR VietQR/MoMo (`16`), Thư viện ảnh hóa đơn (`18`), Thanh toán trả nợ VietQR 1 chạm + ảnh biên lai (`22`) + **Feature `14 [TBD]`**. |
| 4 | **Nguyen Trung Chinh** (`24120271`) | `:feature:budget` | **07, 08, 09, 10, 11, 25** *(6)* | **Feature `08`** *(1)* | Ngân sách nhiều kỳ & Daily Safe-to-Spend (`07`), Hũ tiết kiệm `Lottie` (`09`), Cảnh báo `80/90/100%` (`10`), Kéo-thả điều chuyển (`11`), Biểu đồ tương tác Vico/Canvas (`25`) + **Feature `08 [TBD]`**. |
| 5 | **Nguyen Anh Thai** (`24120224`) | `:feature:group` | **06, 19, 20, 21, 23, 24** *(6)* | **Feature `06`** *(1)* | Quản lý nhóm Room `N-N` (`19`), Chia tiền 4 chế độ (`20`), Thuật toán **Min-Cash Flow** (`21`), Xuất tất toán PDF/`.xlsx`/CSV (`23`), Timeline nhóm & Safe Revert (`24`) + **Feature `06 [TBD]`**. |

---

## 2. Chi tiết 4 slot `[TBD]` để 4 bạn tự do đề xuất

- **Không ràng buộc theo module:** 4 bạn có thể **tự do chọn bất kỳ ý tưởng tính năng nào** (miễn phù hợp đồ án), không bắt buộc phải gắn với module hiện tại của mình.
- **Linh hoạt vị trí code:** Nếu tính năng mới khớp với module đang làm thì đặt chung luôn; nếu khác module thì cứ hoàn thành 5 tính năng chính trước rồi **làm phần này ở giai đoạn cuối** (đặt ở module phù hợp hoặc tách package/màn hình riêng).

### Slot Feature `02` — Phụ trách: Phan Van Viet
- **Tên tính năng mới:** `[Tự do đề xuất]`
- **Module dự kiến (nếu có):** `[Cùng :feature:transaction hoặc làm cuối / chuyển module phù hợp]`
- **Mô tả & Công nghệ sử dụng:** `[Điền mô tả ngắn & thư viện/thuật toán]`

### Slot Feature `06` — Phụ trách: Nguyen Anh Thai
- **Tên tính năng mới:** `[Tự do đề xuất]`
- **Module dự kiến (nếu có):** `[Cùng :feature:group hoặc làm cuối / chuyển module phù hợp]`
- **Mô tả & Công nghệ sử dụng:** `[Điền mô tả ngắn & thư viện/thuật toán]`

### Slot Feature `08` — Phụ trách: Nguyen Trung Chinh
- **Tên tính năng mới:** `[Tự do đề xuất]`
- **Module dự kiến (nếu có):** `[Cùng :feature:budget hoặc làm cuối / chuyển module phù hợp]`
- **Mô tả & Công nghệ sử dụng:** `[Điền mô tả ngắn & thư viện/thuật toán]`

### Slot Feature `14` — Phụ trách: Phi Hoang Duc
- **Tên tính năng mới:** `[Tự do đề xuất]`
- **Module dự kiến (nếu có):** `[Cùng :feature:scanner hoặc làm cuối / chuyển module phù hợp]`
- **Mô tả & Công nghệ sử dụng:** `[Điền mô tả ngắn & thư viện/thuật toán]`

---

## 3. Lưu ý phối hợp giữa các module

- **QR Code EMVCo (`16` & `22`):** Giao cho **Phi Hoang Duc** làm cả đọc (`16`) và sinh mã VietQR (`22`) để dùng chung 1 bộ parser/builder EMVCo, rồi cung cấp callback/component từ `:core` hoặc màn hình cho phần trả nợ nhóm.
- **Export PDF / Excel / CSV (`23` & `30`):** **Danh** (`30`) và **Thai** (`23`) dùng chung một Export helper đặt trong `:core` để tránh code trùng.
- **Tiểu tiết UX/UI gốc của `02`, `06`, `08`:** Dù slot `02, 06, 08` được nhường cho 4 bạn tự chọn tính năng mới, phần gợi ý danh mục cơ bản (`02` cũ), Snackbar Undo 5s khi xóa (`06` cũ) và Progress Bar đổi màu xanh/cam/đỏ (`08` cũ) vẫn nên được làm kèm trong `:feature:transaction` và `:feature:budget` để trải nghiệm app hoàn chỉnh (chi tiết xem trong [FEATURES.md](FEATURES.md)).
- **Đổi module:** Nếu 4 thành viên muốn hoán đổi cụm module với nhau, cập nhật lại bảng trên trước khi bắt đầu tạo nhánh code.
