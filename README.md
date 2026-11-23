# StudentWallet — Personal Finance & Group Expense Sharing (Android Native)

> **CSC13009 – Mobile Application Development**  
> Faculty of Information Technology, University of Science, VNU-HCM (`FIT@HCMUS`)  
> **Group 07 — FriendForever**

**StudentWallet** is an **offline-first (local-first)** personal finance and group expense-sharing Android application tailored for university students. Built **100% natively with Kotlin, Jetpack Compose, and Material Design 3** (no cross-platform frameworks), it helps students track daily spending quickly, scan receipts and payment QR codes, manage budgets, and split shared living/travel expenses without linking bank accounts or risking cloud privacy leaks.

---

## Team Members

| ID | Student ID | Full Name |
| :-: | :--- | :--- |
| 001 | `24120280` | Phan Huu Danh |
| 002 | `24120244` | Phan Van Viet |
| 003 | `24120248` | Phi Hoang Duc |
| 004 | `24120271` | Nguyen Trung Chinh |
| 005 | `24120224` | Nguyen Anh Thai |

---

## Core User Flows & Feature Modules

The project targets **30 feature slots** across five functional domains (see [docs/FEATURES.md](docs/FEATURES.md) for the complete breakdown; features **02, 06, 08, and 14** are currently marked `[TBD]` while undergoing redesign for higher technical depth):

1. **Transactions & Entry (`:feature:transaction`)**
   - Custom financial numeric keypad (`+`, `-`, `×`, `÷`, haptic feedback, VND formatting, range validation `0 < amount <= 999M`).
   - Recurring monthly expenses scheduled automatically with `WorkManager` (`PeriodicWorkRequest`).
   - Full-text search (`< 16ms`) powered by Room SQLite `FTS4` with keyword highlighting and multi-dimensional filters.
   - Bulk bank statement import from `.csv` and `.xlsx` with column mapping and duplicate detection.
2. **Computer Vision & Receipt Scanning (`:feature:scanner`)**
   - Automatic receipt capture with `CameraX` + Laplacian variance sharpness detection.
   - On-device receipt OCR using Google `ML Kit Text Recognition v2`, verification assistant, and WebP storage.
   - `VietQR` & `MoMo` EMVCo QR code reader via `ML Kit Barcode Scanning`.
   - Local high-resolution receipt image library (zoom, `90°` rotation, storage cleanup) with `Coil`.
3. **Budgets, Goals & Analytics (`:feature:budget`)**
   - Multi-period budget setup with Daily Safe-to-Spend calculation and tiered overspending alerts (`80%`, `90%`, `100%`).
   - Drag-and-drop budget reallocation tool and Saving Piggy Bank goals with `Lottie` celebrations.
   - Interactive spending charts (Donut, Stacked Bar, Trend Line via `Vico` / Compose Canvas), month-over-month variance insights, and a GitHub-style Financial Discipline Heatmap.
   - Home screen `Glance AppWidget` (`2x2`, `4x1`, `4x2`) with translucent Quick Add dialog.
4. **Group Expense Splitting & Debt Settlement (`:feature:group`)**
   - Room `N-N` group/member management (`GroupEntity`, `MemberEntity`, `GroupMemberCrossRef`).
   - 4-mode splitting toolkit: equal, percentage, fixed amount, and by item (validated to `0 VND` difference).
   - Greedy **Min-Cash Flow** debt optimization reducing settlement transfers from $O(N^2)$ to at most $N - 1$.
   - One-tap personal VietQR debt payment, group cash-flow timeline with safe revert, and settlement report export (`PDF`, `.xlsx`, `CSV`).
5. **Security, Backup & Settings (`:feature:security`)**
   - `BiometricPrompt` + 6-digit PIN hashed with `PBKDF2` in `Android Keystore`, with `30s` background auto-lock.
   - Full database and receipt image backup/restore to AES-256 CBC encrypted `.wallet` archives verified with `SHA-256` checksums.
   - Multi-currency (`VND`, `USD`, `EUR`), Dark/Light theme via `DataStore`, and multi-format personal report exports (`PDF`, `.xlsx`, `CSV`).

---

## Architecture & Tech Stack

### Software Architecture
- **Pattern:** Clean Architecture + Pragmatic MVI (Unidirectional Data Flow: `Intent -> ViewModel -> Domain/Data -> State/Effect`).
- **Modularization:** 7-module Gradle layout isolating shared infrastructure (`:core`), the application shell (`:app`), and five independent feature modules (`:feature:*`) so all five developers can work in parallel with minimal merge conflicts.

### Technology Stack

| Layer / Capability | Technologies (Configured & Planned) |
| :--- | :--- |
| **Language & UI** | 100% Kotlin (`2.2.10`), Jetpack Compose (`BOM 2026.02.01`), Material Design 3, `Lottie Compose`, `Coil` |
| **Build Toolchain** | Android Gradle Plugin `9.4.1` (Built-in Kotlin), Gradle `9.6.0`, JDK `25` Daemon Toolchain, Version Catalog (`libs.versions.toml`) |
| **Android SDK Targets** | `minSdk = 24`, `compileSdk = 37`, `targetSdk = 37`, Java `11` bytecode compatibility |
| **Local Persistence** | Room Persistence Library (SQLite `FTS4`, `1-N` / `N-N` Relations, Database Views) + Encrypted `DataStore` |
| **Background & Widgets** | `WorkManager` (`PeriodicWorkRequest`), `androidx.glance` AppWidget |
| **Computer Vision** | `CameraX`, Google `ML Kit Text Recognition v2` (On-device OCR), `ML Kit Barcode Scanning` |
| **Charting & Export** | `Vico` Charting Library, Compose `Canvas`, Android `Canvas` PDF Generator, CSV/Excel (`.xlsx`) |
| **Security** | `androidx.biometric`, `Android Keystore`, `PBKDF2`, `AES-256 CBC`, `SHA-256` integrity checks |
| **CI/CD** | GitHub Actions (`testDebugUnitTest`, `assembleDebug`, APK artifact upload) |

---

## Repository Structure

> **Android Studio Tip:** Switch the Project tool window dropdown from **Android** to **Project** view to browse `docs/`, `.github/`, and module-level `README.md` files directly.

```text
student_wallet/
├── .github/
│   └── workflows/
│       └── ci.yml                  # GitHub Actions CI: unit tests + debug APK build
├── docs/
│   ├── README.md                   # Documentation index
│   └── FEATURES.md                 # Feature catalog (26 confirmed + 4 TBD slots)
├── app/                            # Application entry point & root 5-tab navigation shell
├── core/                           # Single shared module across all features
│   └── src/main/java/com/example/student_wallet/core/
│       ├── data/                   # Room DB, DAOs, Entities, DataStore, Crypto/Backup
│       ├── domain/                 # Shared domain models, Repository interfaces, algorithms
│       └── ui/                     # Material 3 theme, colors, typography & shared UI
├── feature/                        # 5 isolated feature modules (1 per team member)
│   ├── transaction/                # Member 1: Entries, Keypad, FTS4 Search, Recurring, Import
│   ├── scanner/                    # Member 2: CameraX OCR, Review Assistant, VietQR, Gallery
│   ├── budget/                     # Member 3: Budgets, Alerts, Piggy Bank, Charts, Widget
│   ├── group/                      # Member 4: Groups N-N, 4-Mode Split, Min-Cash Flow, Export
│   └── security/                   # Member 5: Biometric/PIN, AES-256 Backup, Settings, Export
├── CONTRIBUTING.md                 # Git branching, Conventional Commits & PR workflow rules
├── README.md                       # Project documentation
├── build.gradle.kts                # Root Gradle build configuration
└── settings.gradle.kts             # Module declarations (:app, :core, :feature:*)
```

---

## Getting Started

### Prerequisites
- **Android Studio** supporting AGP `9.4.1` and Gradle `9.6.0`
- **JDK 25** (configured via `gradle/gradle-daemon-jvm.properties` for the Gradle Daemon)
- **Android SDK Platform 37**

### Useful Commands

```bash
# Run JVM unit tests across all modules
./gradlew testDebugUnitTest

# Build the debug APK (output: app/build/outputs/apk/debug/app-debug.apk)
./gradlew assembleDebug

# Run both unit tests and debug APK build (matches CI)
./gradlew testDebugUnitTest assembleDebug
```

---

## CI/CD Pipeline

Every push or pull request targeting `main` or `dev` triggers [ci.yml](.github/workflows/ci.yml), which:
1. Sets up JDK 25 (`Temurin`) and Gradle caching.
2. Runs `./gradlew testDebugUnitTest --no-daemon`.
3. Builds `./gradlew assembleDebug --no-daemon`.
4. Uploads `app-debug.apk` as a workflow artifact (`student-wallet-debug-apk`, retained for 7 days).

---

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) before writing code. All team members must work on dedicated `feat/...` branches, follow **Conventional Commits**, and pass CI checks before merging into the main branch.
