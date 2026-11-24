package com.example.student_wallet.feature.security.backup

import com.example.student_wallet.core.domain.model.UserSettings
import java.io.File

enum class WalletBackupErrorReason {
    INVALID_FORMAT,
    CORRUPTED_CHECKSUM,
    WRONG_PASSWORD,
    INCOMPATIBLE_DB_VERSION,
    DATABASE_CORRUPTED,
    IO_FAILURE,
}

class WalletBackupException(
    val reason: WalletBackupErrorReason,
    override val message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

enum class BackupDestination {
    SAVE_TO_DEVICE,
    SHARE_FILE,
}

data class BackupStats(
    val transactionCount: Int,
    val categoryCount: Int,
    val receiptCount: Int,
)

data class BackupPreviewInfo(
    val fileName: String,
    val createdAtEpochMillis: Long,
    val dbVersion: Int,
    val transactionCount: Int,
    val categoryCount: Int,
    val receiptCount: Int,
    val checksumShortHex: String,
    val settings: UserSettings,
)

data class RestoreSummary(
    val transactionCount: Int,
    val categoryCount: Int,
    val receiptCount: Int,
)

internal class StagedRestoreSession(
    val stagingDir: File,
    val extracted: ExtractedBackupPayload,
    val preview: BackupPreviewInfo,
)
