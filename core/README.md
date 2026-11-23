# Module `:core`

Module dùng chung duy nhất cho toàn bộ 5 module trong `feature/`.

## Cấu trúc bên trong `src/main/java/com/example/student_wallet/core/`

- `data/` — Room Database (`AppDatabase`), Entity, DAO, DataStore, Backup AES-256.
- `domain/` — Model dùng chung (`Transaction`, `Category`, `Budget`...), Interface Repository & Thuật toán.
- `ui/` — Material 3 Theme (`Student_walletTheme`), màu sắc, Typography và Component dùng chung.
