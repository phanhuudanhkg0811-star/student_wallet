package com.example.student_wallet.feature.security.backup

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteStatement
import com.example.student_wallet.core.data.local.AppDatabase
import com.example.student_wallet.core.domain.model.UserSettings
import com.example.student_wallet.core.domain.repository.SettingsRepository
import com.example.student_wallet.feature.security.widget.WalletWidgetUpdater
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

interface WalletBackupCoordinator {
    suspend fun getCurrentBackupStats(): BackupStats
    suspend fun createBackupToStream(passphrase: String, outputStream: OutputStream)
    suspend fun createBackupCacheFile(passphrase: String, suggestedFileName: String): File
    suspend fun inspectBackupFile(
        fileName: String,
        passphrase: String,
        openInputStream: () -> InputStream?,
    ): BackupPreviewInfo

    suspend fun confirmStagedRestore(): RestoreSummary
    fun discardStagedRestore()
}

/**
 * Coordinates checkpointed SQLite snapshots, receipt image bundling, AES-256-CBC + SHA-256
 * `.wallet` creation, two-step decrypt/preview verification, and atomic live Room restoration.
 */
class AndroidWalletBackupManager(
    private val context: Context,
    private val appDatabase: AppDatabase,
    private val settingsRepository: SettingsRepository,
    private val walletCrypto: WalletCrypto = WalletCrypto(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : WalletBackupCoordinator {

    @Volatile
    private var stagedSession: StagedRestoreSession? = null

    override suspend fun getCurrentBackupStats(): BackupStats = withContext(ioDispatcher) {
        val db = appDatabase.openHelper.readableDatabase
        val txCount = queryTableRowCount(db, TABLE_TRANSACTIONS)
        val catCount = queryTableRowCount(db, TABLE_CATEGORIES)
        val receiptsCount = collectReceiptBackupEntries(db).size
        BackupStats(
            transactionCount = txCount,
            categoryCount = catCount,
            receiptCount = receiptsCount,
        )
    }

    override suspend fun createBackupToStream(passphrase: String, outputStream: OutputStream) {
        val encryptedBytes = buildEncryptedWalletBytes(passphrase)
        withContext(ioDispatcher) {
            outputStream.write(encryptedBytes)
            outputStream.flush()
        }
    }

    override suspend fun createBackupCacheFile(
        passphrase: String,
        suggestedFileName: String,
    ): File {
        val encryptedBytes = buildEncryptedWalletBytes(passphrase)
        return withContext(ioDispatcher) {
            val exportDir = File(context.cacheDir, CACHE_EXPORT_DIR).apply { mkdirs() }
            pruneOldCacheFiles(exportDir)
            val safeName = suggestedFileName
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .let { if (it.endsWith(".wallet")) it else "$it.wallet" }
            val file = File(exportDir, safeName)
            FileOutputStream(file).use { out ->
                out.write(encryptedBytes)
                out.flush()
            }
            file
        }
    }

    override suspend fun inspectBackupFile(
        fileName: String,
        passphrase: String,
        openInputStream: () -> InputStream?,
    ): BackupPreviewInfo {
        discardStagedRestore()
        return withContext(ioDispatcher) {
            val walletBytes = openInputStream()?.use { input ->
                readStreamBounded(input)
            } ?: throw WalletBackupException(
                WalletBackupErrorReason.IO_FAILURE,
                "Không thể mở tệp .wallet đã chọn.",
            )

            val decrypted = walletCrypto.decrypt(walletBytes, passphrase)
            val stagingDir = File(context.cacheDir, "$STAGING_ROOT_DIR/${System.currentTimeMillis()}").apply {
                mkdirs()
            }

            try {
                val extracted = WalletBackupArchive.unpackToDirectory(decrypted.zipBytes, stagingDir)
                val currentDbVersion = appDatabase.openHelper.readableDatabase.version
                val (stagedDbVersion, txCount, catCount) = verifyStagedDatabase(
                    dbFile = extracted.dbFile,
                    currentDbVersion = currentDbVersion,
                    fallbackTxCount = extracted.manifestTransactionCount,
                    fallbackCatCount = extracted.manifestCategoryCount,
                )

                val receiptCount = extracted.receiptsDir.listFiles()?.count { it.isFile }
                    ?: extracted.manifestReceiptCount

                val preview = BackupPreviewInfo(
                    fileName = fileName,
                    createdAtEpochMillis = extracted.createdAtEpochMillis,
                    dbVersion = stagedDbVersion,
                    transactionCount = txCount,
                    categoryCount = catCount,
                    receiptCount = receiptCount,
                    checksumShortHex = decrypted.outerSha256Hex.take(12),
                    settings = extracted.settings,
                )
                stagedSession = StagedRestoreSession(
                    stagingDir = stagingDir,
                    extracted = extracted,
                    preview = preview,
                )
                preview
            } catch (e: Throwable) {
                stagingDir.deleteRecursively()
                throw e
            }
        }
    }

    override suspend fun confirmStagedRestore(): RestoreSummary {
        val session = stagedSession ?: throw WalletBackupException(
            WalletBackupErrorReason.IO_FAILURE,
            "Phiên khôi phục đã hết hạn. Vui lòng chọn lại tệp .wallet.",
        )

        val previousSettings = settingsRepository.observeSettings().first()
        val rollbackDir = File(context.cacheDir, "$ROLLBACK_ROOT_DIR/${System.currentTimeMillis()}").apply {
            mkdirs()
        }
        val rollbackDbFile = File(rollbackDir, "rollback.db")
        val rollbackReceiptsDir = File(rollbackDir, "receipts").apply { mkdirs() }
        val targetReceiptsDir = File(context.filesDir, RECEIPTS_DIR_NAME)

        return try {
            withContext(ioDispatcher) {
                val writableDb = appDatabase.openHelper.writableDatabase
                writableDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }

                val liveDbFile = context.getDatabasePath(DATABASE_NAME)
                if (liveDbFile.isFile) {
                    liveDbFile.copyTo(rollbackDbFile, overwrite = true)
                }
                if (targetReceiptsDir.isDirectory) {
                    copyDirectoryContents(targetReceiptsDir, rollbackReceiptsDir)
                }

                replaceDatabaseFromFile(
                    writableDb = writableDb,
                    sourceDbFile = session.extracted.dbFile,
                )

                restoreReceiptFilesAndPaths(
                    writableDb = writableDb,
                    stagedReceiptsDir = session.extracted.receiptsDir,
                    targetReceiptsDir = targetReceiptsDir,
                    receiptMap = session.extracted.receiptEntryByOriginalPath,
                )
            }

            settingsRepository.setThemeMode(session.extracted.settings.themeMode)
            settingsRepository.setCurrency(session.extracted.settings.currency)
            settingsRepository.setDynamicColor(session.extracted.settings.dynamicColor)
            appDatabase.invalidationTracker.refreshAsync()
            WalletWidgetUpdater.refreshAll(context)

            RestoreSummary(
                transactionCount = session.preview.transactionCount,
                categoryCount = session.preview.categoryCount,
                receiptCount = session.preview.receiptCount,
            )
        } catch (e: Throwable) {
            runCatching {
                withContext(ioDispatcher) {
                    val writableDb = appDatabase.openHelper.writableDatabase
                    if (rollbackDbFile.isFile) {
                        replaceDatabaseFromFile(writableDb, rollbackDbFile)
                    }
                    if (targetReceiptsDir.isDirectory) {
                        targetReceiptsDir.deleteRecursively()
                    }
                    targetReceiptsDir.mkdirs()
                    copyDirectoryContents(rollbackReceiptsDir, targetReceiptsDir)
                }
                settingsRepository.setThemeMode(previousSettings.themeMode)
                settingsRepository.setCurrency(previousSettings.currency)
                settingsRepository.setDynamicColor(previousSettings.dynamicColor)
                appDatabase.invalidationTracker.refreshAsync()
            }
            if (e is WalletBackupException) throw e
            throw WalletBackupException(
                WalletBackupErrorReason.IO_FAILURE,
                "Khôi phục thất bại, dữ liệu trước đó đã được giữ nguyên: ${e.message ?: "Lỗi I/O"}",
                e,
            )
        } finally {
            rollbackDir.deleteRecursively()
            discardStagedRestore()
        }
    }

    override fun discardStagedRestore() {
        val current = stagedSession
        stagedSession = null
        current?.stagingDir?.deleteRecursively()
    }

    private suspend fun buildEncryptedWalletBytes(passphrase: String): ByteArray {
        val settings = settingsRepository.observeSettings().first()
        return withContext(ioDispatcher) {
            val writableDb = appDatabase.openHelper.writableDatabase
            writableDb.query("PRAGMA wal_checkpoint(TRUNCATE)").use { it.moveToFirst() }

            val dbFile = context.getDatabasePath(DATABASE_NAME)
            if (!dbFile.isFile) {
                throw WalletBackupException(
                    WalletBackupErrorReason.IO_FAILURE,
                    "Không tìm thấy tệp cơ sở dữ liệu để sao lưu.",
                )
            }

            val txCount = queryTableRowCount(writableDb, TABLE_TRANSACTIONS)
            val catCount = queryTableRowCount(writableDb, TABLE_CATEGORIES)
            val receiptEntries = collectReceiptBackupEntries(writableDb)
            val cleanDbBytes = createCleanDatabaseSnapshotBytes(dbFile)
            val zipBytes = WalletBackupArchive.packToZipBytes(
                dbBytes = cleanDbBytes,
                dbVersion = writableDb.version,
                createdAtEpochMillis = System.currentTimeMillis(),
                transactionCount = txCount,
                categoryCount = catCount,
                settings = settings,
                receiptEntries = receiptEntries,
            )
            walletCrypto.encrypt(zipBytes, passphrase)
        }
    }

    private fun createCleanDatabaseSnapshotBytes(liveDbFile: File): ByteArray {
        val walFile = File("${liveDbFile.path}-wal")
        val shmFile = File("${liveDbFile.path}-shm")
        if (!walFile.isFile || walFile.length() == 0L) {
            return liveDbFile.readBytes()
        }

        // If a UI Flow reader kept frames in -wal during checkpoint, consolidate an isolated copy.
        val tempDir = File(context.cacheDir, "$BACKUP_STAGING_DIR/${System.currentTimeMillis()}").apply {
            mkdirs()
        }
        return try {
            val copyDb = File(tempDir, DATABASE_NAME)
            liveDbFile.copyTo(copyDb, overwrite = true)
            walFile.copyTo(File("${copyDb.path}-wal"), overwrite = true)
            if (shmFile.isFile) {
                shmFile.copyTo(File("${copyDb.path}-shm"), overwrite = true)
            }
            SQLiteDatabase.openDatabase(copyDb.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.rawQuery("PRAGMA journal_mode = DELETE", null).use { it.moveToFirst() }
            }
            copyDb.readBytes()
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun verifyStagedDatabase(
        dbFile: File,
        currentDbVersion: Int,
        fallbackTxCount: Int,
        fallbackCatCount: Int,
    ): Triple<Int, Int, Int> {
        val sqliteDb = runCatching {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        }.getOrElse { error ->
            throw WalletBackupException(
                WalletBackupErrorReason.DATABASE_CORRUPTED,
                "Không thể mở cơ sở dữ liệu bên trong bản sao lưu.",
                error,
            )
        }

        return sqliteDb.use { db ->
            val quickCheck = db.rawQuery("PRAGMA quick_check", null).use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
            if (!quickCheck.equals("ok", ignoreCase = true)) {
                throw WalletBackupException(
                    WalletBackupErrorReason.DATABASE_CORRUPTED,
                    "Cơ sở dữ liệu trong bản sao lưu bị hỏng ($quickCheck).",
                )
            }

            val stagedVersion = db.version
            if (stagedVersion > currentDbVersion) {
                throw WalletBackupException(
                    WalletBackupErrorReason.INCOMPATIBLE_DB_VERSION,
                    "Bản sao lưu được tạo từ phiên bản app mới hơn (DB v$stagedVersion > v$currentDbVersion). Vui lòng cập nhật app trước khi khôi phục.",
                )
            }

            val txCount = querySqliteTableRowCount(db, TABLE_TRANSACTIONS)
            val catCount = querySqliteTableRowCount(db, TABLE_CATEGORIES)
            if (txCount == null && catCount == null) {
                throw WalletBackupException(
                    WalletBackupErrorReason.DATABASE_CORRUPTED,
                    "Tệp .wallet không chứa bảng dữ liệu hợp lệ của StudentWallet.",
                )
            }
            Triple(stagedVersion, txCount ?: fallbackTxCount, catCount ?: fallbackCatCount)
        }
    }

    private fun replaceDatabaseFromFile(
        writableDb: SupportSQLiteDatabase,
        sourceDbFile: File,
    ) {
        val sourceDb = SQLiteDatabase.openDatabase(
            sourceDbFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY,
        )
        sourceDb.use { backupDb ->
            val mainTables = listRestoreUserTables(writableDb)
            val backupTables = listSqliteRestoreUserTables(backupDb).toSet()
            val commonTables = mainTables.filter { it in backupTables }

            runCatching { writableDb.setForeignKeyConstraintsEnabled(false) }
            try {
                writableDb.beginTransaction()
                try {
                    // Replace-All clears all current user tables first so future modules never retain orphan FKs.
                    mainTables.forEach { table ->
                        writableDb.execSQL("DELETE FROM \"$table\"")
                    }

                    commonTables.forEach { table ->
                        val mainCols = listTableColumns(writableDb, table)
                        val backupCols = listSqliteTableColumns(backupDb, table).toSet()
                        val commonCols = mainCols.filter { it in backupCols }
                        if (commonCols.isNotEmpty()) {
                            copyTableRows(
                                sourceDb = backupDb,
                                targetDb = writableDb,
                                tableName = table,
                                columns = commonCols,
                            )
                        }
                    }

                    if (hasTable(writableDb, "sqlite_sequence") &&
                        hasSqliteTable(backupDb, "sqlite_sequence")
                    ) {
                        writableDb.execSQL("DELETE FROM sqlite_sequence")
                        val commonSet = commonTables.toSet()
                        writableDb.compileStatement(
                            "INSERT INTO sqlite_sequence(name, seq) VALUES (?, ?)",
                        ).use { seqStmt ->
                            backupDb.rawQuery("SELECT name, seq FROM sqlite_sequence", null).use { cursor ->
                                while (cursor.moveToNext()) {
                                    val name = cursor.getString(0) ?: continue
                                    if (name in commonSet) {
                                        seqStmt.clearBindings()
                                        seqStmt.bindString(1, name)
                                        seqStmt.bindLong(2, cursor.getLong(1))
                                        seqStmt.executeInsert()
                                    }
                                }
                            }
                        }
                    }

                    writableDb.setTransactionSuccessful()
                } finally {
                    writableDb.endTransaction()
                }
            } finally {
                runCatching { writableDb.setForeignKeyConstraintsEnabled(true) }
            }
        }
    }

    private fun copyTableRows(
        sourceDb: SQLiteDatabase,
        targetDb: SupportSQLiteDatabase,
        tableName: String,
        columns: List<String>,
    ) {
        val colsSql = columns.joinToString(",") { "\"$it\"" }
        val placeholders = columns.joinToString(",") { "?" }
        val insertSql = "INSERT INTO \"$tableName\" ($colsSql) VALUES ($placeholders)"

        targetDb.compileStatement(insertSql).use { stmt ->
            sourceDb.rawQuery("SELECT $colsSql FROM \"$tableName\"", null).use { cursor ->
                while (cursor.moveToNext()) {
                    stmt.clearBindings()
                    for (idx in columns.indices) {
                        bindCursorColumn(stmt = stmt, bindIndex = idx + 1, cursor = cursor, columnIndex = idx)
                    }
                    stmt.executeInsert()
                }
            }
        }
    }

    private fun bindCursorColumn(
        stmt: SupportSQLiteStatement,
        bindIndex: Int,
        cursor: Cursor,
        columnIndex: Int,
    ) {
        when (cursor.getType(columnIndex)) {
            Cursor.FIELD_TYPE_NULL -> stmt.bindNull(bindIndex)
            Cursor.FIELD_TYPE_INTEGER -> stmt.bindLong(bindIndex, cursor.getLong(columnIndex))
            Cursor.FIELD_TYPE_FLOAT -> stmt.bindDouble(bindIndex, cursor.getDouble(columnIndex))
            Cursor.FIELD_TYPE_STRING -> stmt.bindString(bindIndex, cursor.getString(columnIndex))
            Cursor.FIELD_TYPE_BLOB -> stmt.bindBlob(bindIndex, cursor.getBlob(columnIndex))
            else -> stmt.bindNull(bindIndex)
        }
    }

    private fun restoreReceiptFilesAndPaths(
        writableDb: SupportSQLiteDatabase,
        stagedReceiptsDir: File,
        targetReceiptsDir: File,
        receiptMap: Map<String, String>,
    ) {
        if (targetReceiptsDir.isDirectory) {
            targetReceiptsDir.deleteRecursively()
        }
        targetReceiptsDir.mkdirs()
        if (stagedReceiptsDir.isDirectory) {
            copyDirectoryContents(stagedReceiptsDir, targetReceiptsDir)
        }

        if (!hasTable(writableDb, TABLE_TRANSACTIONS)) return
        val columns = listTableColumns(writableDb, TABLE_TRANSACTIONS)
        if ("receiptImagePath" !in columns) return

        writableDb.beginTransaction()
        try {
            receiptMap.forEach { (originalPath, archiveName) ->
                val resolvedFile = File(targetReceiptsDir, archiveName)
                if (resolvedFile.isFile) {
                    writableDb.execSQL(
                        "UPDATE $TABLE_TRANSACTIONS SET receiptImagePath = ? WHERE receiptImagePath = ?",
                        arrayOf<Any>(resolvedFile.absolutePath, originalPath),
                    )
                }
            }

            // Fallback remapping by filename if a restored path pointed to a previous sandbox path.
            val pathsToRemap = mutableListOf<Pair<String, String>>()
            writableDb.query(
                "SELECT DISTINCT receiptImagePath FROM $TABLE_TRANSACTIONS " +
                    "WHERE receiptImagePath IS NOT NULL AND receiptImagePath != ''",
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val existingPath = cursor.getString(0) ?: continue
                    if (!File(existingPath).isFile) {
                        val candidate = File(targetReceiptsDir, File(existingPath).name)
                        if (candidate.isFile) {
                            pathsToRemap += existingPath to candidate.absolutePath
                        }
                    }
                }
            }
            pathsToRemap.forEach { (oldPath, newPath) ->
                writableDb.execSQL(
                    "UPDATE $TABLE_TRANSACTIONS SET receiptImagePath = ? WHERE receiptImagePath = ?",
                    arrayOf<Any>(newPath, oldPath),
                )
            }

            writableDb.setTransactionSuccessful()
        } finally {
            writableDb.endTransaction()
        }
    }

    private fun collectReceiptBackupEntries(db: SupportSQLiteDatabase): List<ReceiptBackupEntry> {
        val entries = mutableListOf<ReceiptBackupEntry>()
        val seenCanonicalPaths = HashSet<String>()
        val usedArchiveNames = HashSet<String>()

        fun addFile(originalPath: String, file: File) {
            if (!file.isFile || !file.canRead()) return
            val canonical = runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
            if (!seenCanonicalPaths.add(canonical)) return
            val baseName = file.name.ifBlank { "receipt_${entries.size + 1}.jpg" }
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            var uniqueName = baseName
            var suffix = 1
            while (!usedArchiveNames.add(uniqueName)) {
                uniqueName = "${suffix++}_$baseName"
            }
            entries += ReceiptBackupEntry(
                archiveEntryName = uniqueName,
                originalPath = originalPath,
                sourceFile = file,
            )
        }

        if (hasTable(db, TABLE_TRANSACTIONS)) {
            val columns = listTableColumns(db, TABLE_TRANSACTIONS)
            if ("receiptImagePath" in columns) {
                db.query(
                    "SELECT DISTINCT receiptImagePath FROM $TABLE_TRANSACTIONS " +
                        "WHERE receiptImagePath IS NOT NULL AND receiptImagePath != ''",
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(0) ?: continue
                        addFile(originalPath = path, file = File(path))
                    }
                }
            }
        }

        val receiptsDir = File(context.filesDir, RECEIPTS_DIR_NAME)
        if (receiptsDir.isDirectory) {
            receiptsDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    addFile(originalPath = file.absolutePath, file = file)
                }
            }
        }
        return entries
    }

    private fun listRestoreUserTables(db: SupportSQLiteDatabase): List<String> =
        db.query("SELECT name, sql FROM sqlite_master WHERE type = 'table'").use(::extractRestoreTables)

    private fun listSqliteRestoreUserTables(db: SQLiteDatabase): List<String> =
        db.rawQuery("SELECT name, sql FROM sqlite_master WHERE type = 'table'", null).use(::extractRestoreTables)

    private fun extractRestoreTables(cursor: Cursor): List<String> {
        val ftsVirtualTables = mutableSetOf<String>()
        val allTables = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val name = cursor.getString(0) ?: continue
            val createSql = cursor.getString(1).orEmpty()
            if (createSql.contains("VIRTUAL TABLE", ignoreCase = true) &&
                createSql.contains("fts", ignoreCase = true)
            ) {
                ftsVirtualTables += name
            }
            allTables += name
        }

        val ftsShadowNames = ftsVirtualTables.flatMapTo(HashSet()) { vtable ->
            listOf(
                "${vtable}_content",
                "${vtable}_segments",
                "${vtable}_segdir",
                "${vtable}_docsize",
                "${vtable}_stat",
            )
        }

        return allTables.filter { name ->
            !name.startsWith("sqlite_") &&
                !name.startsWith("android_") &&
                name != "room_master_table" &&
                name !in ftsShadowNames
        }
    }

    private fun listTableColumns(db: SupportSQLiteDatabase, tableName: String): List<String> =
        db.query("PRAGMA table_info(\"$tableName\")").use(::extractColumnNames)

    private fun listSqliteTableColumns(db: SQLiteDatabase, tableName: String): List<String> =
        db.rawQuery("PRAGMA table_info(\"$tableName\")", null).use(::extractColumnNames)

    private fun extractColumnNames(cursor: Cursor): List<String> {
        val cols = mutableListOf<String>()
        val nameIdx = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIdx >= 0) {
                cursor.getString(nameIdx)?.let(cols::add)
            }
        }
        return cols
    }

    private fun hasTable(db: SupportSQLiteDatabase, tableName: String): Boolean =
        db.query(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(tableName),
        ).use { it.moveToFirst() }

    private fun hasSqliteTable(db: SQLiteDatabase, tableName: String): Boolean =
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ? LIMIT 1",
            arrayOf(tableName),
        ).use { it.moveToFirst() }

    private fun queryTableRowCount(db: SupportSQLiteDatabase, tableName: String): Int {
        if (!hasTable(db, tableName)) return 0
        return db.query("SELECT COUNT(*) FROM \"$tableName\"").use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun querySqliteTableRowCount(db: SQLiteDatabase, tableName: String): Int? {
        if (!hasSqliteTable(db, tableName)) return null
        return db.rawQuery("SELECT COUNT(*) FROM \"$tableName\"", null).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    private fun readStreamBounded(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var total = 0L
        while (true) {
            val count = input.read(chunk)
            if (count <= 0) break
            total += count
            if (total > MAX_WALLET_FILE_BYTES) {
                throw WalletBackupException(
                    WalletBackupErrorReason.INVALID_FORMAT,
                    "Tệp .wallet vượt quá giới hạn dung lượng cho phép.",
                )
            }
            out.write(chunk, 0, count)
        }
        return out.toByteArray()
    }

    private fun copyDirectoryContents(sourceDir: File, destDir: File) {
        destDir.mkdirs()
        sourceDir.listFiles()?.forEach { child ->
            val target = File(destDir, child.name)
            if (child.isDirectory) {
                copyDirectoryContents(child, target)
            } else if (child.isFile) {
                child.copyTo(target, overwrite = true)
            }
        }
    }

    private fun pruneOldCacheFiles(exportDir: File) {
        val cutoff = System.currentTimeMillis() - CACHE_TTL_MILLIS
        exportDir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    companion object {
        private const val DATABASE_NAME = "student_wallet.db"
        private const val TABLE_TRANSACTIONS = "transactions"
        private const val TABLE_CATEGORIES = "categories"
        private const val RECEIPTS_DIR_NAME = "receipts"
        private const val CACHE_EXPORT_DIR = "exports"
        private const val BACKUP_STAGING_DIR = "backup_staging"
        private const val STAGING_ROOT_DIR = "restore_staging"
        private const val ROLLBACK_ROOT_DIR = "restore_rollback"
        private const val MAX_WALLET_FILE_BYTES = 128L * 1024L * 1024L
        private const val CACHE_TTL_MILLIS = 24L * 60L * 60L * 1000L
    }
}
