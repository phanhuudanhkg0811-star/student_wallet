package com.example.student_wallet.feature.security.backup

import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.domain.model.UserSettings
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal data class ReceiptBackupEntry(
    val archiveEntryName: String,
    val originalPath: String,
    val sourceFile: File,
)

internal data class ExtractedBackupPayload(
    val dbFile: File,
    val receiptsDir: File,
    val receiptEntryByOriginalPath: Map<String, String>,
    val settings: UserSettings,
    val createdAtEpochMillis: Long,
    val dbVersion: Int,
    val manifestTransactionCount: Int,
    val manifestCategoryCount: Int,
    val manifestReceiptCount: Int,
)

/**
 * Packs and extracts the inner ZIP payload containing SQLite DB snapshot,
 * user appearance/currency settings, and receipt images with Zip-Slip protection.
 */
internal object WalletBackupArchive {

    private const val ENTRY_MANIFEST = "manifest.properties"
    private const val ENTRY_SETTINGS = "settings.properties"
    private const val ENTRY_DATABASE = "db/student_wallet.db"
    private const val ENTRY_RECEIPTS_MAP = "receipts_map.properties"
    private const val RECEIPTS_PREFIX = "receipts/"

    private const val MAX_UNCOMPRESSED_BYTES = 128L * 1024L * 1024L
    private const val SQLITE_HEADER_PREFIX = "SQLite format 3\u0000"

    fun packToZipBytes(
        dbBytes: ByteArray,
        dbVersion: Int,
        createdAtEpochMillis: Long,
        transactionCount: Int,
        categoryCount: Int,
        settings: UserSettings,
        receiptEntries: List<ReceiptBackupEntry>,
    ): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            val manifestProps = Properties().apply {
                setProperty("formatVersion", "1")
                setProperty("createdAtEpochMillis", createdAtEpochMillis.toString())
                setProperty("dbVersion", dbVersion.toString())
                setProperty("transactionCount", transactionCount.toString())
                setProperty("categoryCount", categoryCount.toString())
                setProperty("receiptCount", receiptEntries.size.toString())
            }
            writePropertiesEntry(zip, ENTRY_MANIFEST, manifestProps)

            val settingsProps = Properties().apply {
                setProperty("themeMode", settings.themeMode.name)
                setProperty("currency", settings.currency.name)
                setProperty("dynamicColor", settings.dynamicColor.toString())
            }
            writePropertiesEntry(zip, ENTRY_SETTINGS, settingsProps)

            zip.putNextEntry(ZipEntry(ENTRY_DATABASE))
            zip.write(dbBytes)
            zip.closeEntry()

            val receiptMapProps = Properties()
            val writtenArchives = HashSet<String>()
            receiptEntries.forEach { entry ->
                val normalizedName = entry.archiveEntryName.removePrefix(RECEIPTS_PREFIX)
                receiptMapProps.setProperty(entry.originalPath, normalizedName)
                val fullEntryPath = "$RECEIPTS_PREFIX$normalizedName"
                if (writtenArchives.add(fullEntryPath) && entry.sourceFile.isFile) {
                    zip.putNextEntry(ZipEntry(fullEntryPath))
                    entry.sourceFile.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            writePropertiesEntry(zip, ENTRY_RECEIPTS_MAP, receiptMapProps)
        }
        return output.toByteArray()
    }

    fun unpackToDirectory(zipBytes: ByteArray, destinationDir: File): ExtractedBackupPayload {
        if (!destinationDir.exists() && !destinationDir.mkdirs()) {
            throw WalletBackupException(
                WalletBackupErrorReason.IO_FAILURE,
                "Không thể tạo thư mục tạm để giải nén bản sao lưu.",
            )
        }
        val canonicalRoot = destinationDir.canonicalPath
        val receiptsDir = File(destinationDir, "receipts").apply { mkdirs() }
        var manifestProps: Properties? = null
        var settingsProps: Properties? = null
        var receiptMapProps: Properties? = null
        var dbFile: File? = null
        var totalBytesRead = 0L

        try {
            ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val safeName = entry.name.replace('\\', '/')
                        val targetFile = File(destinationDir, safeName)
                        val targetCanonical = targetFile.canonicalPath
                        if (!targetCanonical.startsWith("$canonicalRoot${File.separator}")) {
                            throw WalletBackupException(
                                WalletBackupErrorReason.INVALID_FORMAT,
                                "Tệp sao lưu chứa đường dẫn không hợp lệ.",
                            )
                        }

                        when {
                            safeName == ENTRY_MANIFEST -> manifestProps = readProperties(zip, totalBytesRead).also {
                                totalBytesRead += it.second
                            }.first
                            safeName == ENTRY_SETTINGS -> settingsProps = readProperties(zip, totalBytesRead).also {
                                totalBytesRead += it.second
                            }.first
                            safeName == ENTRY_RECEIPTS_MAP -> receiptMapProps = readProperties(zip, totalBytesRead).also {
                                totalBytesRead += it.second
                            }.first
                            safeName == ENTRY_DATABASE -> {
                                val outFile = File(destinationDir, "student_wallet.db")
                                totalBytesRead += copyBounded(zip, outFile, totalBytesRead)
                                dbFile = outFile
                            }
                            safeName.startsWith(RECEIPTS_PREFIX) -> {
                                targetFile.parentFile?.mkdirs()
                                totalBytesRead += copyBounded(zip, targetFile, totalBytesRead)
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } catch (e: WalletBackupException) {
            throw e
        } catch (e: Exception) {
            throw WalletBackupException(
                WalletBackupErrorReason.INVALID_FORMAT,
                "Không thể đọc gói dữ liệu bên trong tệp .wallet.",
                e,
            )
        }

        val resolvedDbFile = dbFile?.takeIf { it.isFile && hasSqliteHeader(it) }
            ?: throw WalletBackupException(
                WalletBackupErrorReason.DATABASE_CORRUPTED,
                "Tệp sao lưu không chứa cơ sở dữ liệu StudentWallet hợp lệ.",
            )

        val manifest = manifestProps ?: Properties()
        val settings = parseSettings(settingsProps ?: Properties())
        val mapEntries = buildMap {
            receiptMapProps?.forEach { k, v ->
                val key = k?.toString()?.takeIf { it.isNotBlank() } ?: return@forEach
                val value = v?.toString()?.takeIf { it.isNotBlank() } ?: return@forEach
                put(key, value)
            }
        }

        val createdAt = manifest.getProperty("createdAtEpochMillis")?.toLongOrNull() ?: 0L
        val dbVersion = manifest.getProperty("dbVersion")?.toIntOrNull() ?: 1
        val txCount = manifest.getProperty("transactionCount")?.toIntOrNull() ?: 0
        val catCount = manifest.getProperty("categoryCount")?.toIntOrNull() ?: 0
        val receiptCount = manifest.getProperty("receiptCount")?.toIntOrNull()
            ?: (receiptsDir.listFiles()?.count { it.isFile } ?: 0)

        return ExtractedBackupPayload(
            dbFile = resolvedDbFile,
            receiptsDir = receiptsDir,
            receiptEntryByOriginalPath = mapEntries,
            settings = settings,
            createdAtEpochMillis = createdAt,
            dbVersion = dbVersion,
            manifestTransactionCount = txCount,
            manifestCategoryCount = catCount,
            manifestReceiptCount = receiptCount,
        )
    }

    private fun parseSettings(props: Properties): UserSettings {
        val theme = props.getProperty("themeMode")
            ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
            ?: ThemeMode.SYSTEM
        val currency = props.getProperty("currency")
            ?.let { runCatching { CurrencyOption.valueOf(it) }.getOrNull() }
            ?: CurrencyOption.VND
        val dynamicColor = props.getProperty("dynamicColor")?.toBooleanStrictOrNull() ?: false
        return UserSettings(themeMode = theme, currency = currency, dynamicColor = dynamicColor)
    }

    private fun writePropertiesEntry(zip: ZipOutputStream, entryName: String, props: Properties) {
        val bytes = ByteArrayOutputStream().apply {
            OutputStreamWriter(this, StandardCharsets.UTF_8).use { writer ->
                props.store(writer, null)
            }
        }.toByteArray()
        zip.putNextEntry(ZipEntry(entryName))
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun readProperties(zip: ZipInputStream, alreadyRead: Long): Pair<Properties, Long> {
        val buffer = readBoundedBytes(zip, alreadyRead)
        val props = Properties()
        InputStreamReader(ByteArrayInputStream(buffer), StandardCharsets.UTF_8).use { reader ->
            props.load(reader)
        }
        return props to buffer.size.toLong()
    }

    private fun readBoundedBytes(zip: ZipInputStream, alreadyRead: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var written = 0L
        while (true) {
            val count = zip.read(chunk)
            if (count <= 0) break
            written += count
            if (alreadyRead + written > MAX_UNCOMPRESSED_BYTES) {
                throw WalletBackupException(
                    WalletBackupErrorReason.INVALID_FORMAT,
                    "Dung lượng giải nén vượt quá giới hạn an toàn.",
                )
            }
            out.write(chunk, 0, count)
        }
        return out.toByteArray()
    }

    private fun copyBounded(zip: ZipInputStream, dest: File, alreadyRead: Long): Long {
        val chunk = ByteArray(8192)
        var written = 0L
        dest.outputStream().use { out ->
            while (true) {
                val count = zip.read(chunk)
                if (count <= 0) break
                written += count
                if (alreadyRead + written > MAX_UNCOMPRESSED_BYTES) {
                    throw WalletBackupException(
                        WalletBackupErrorReason.INVALID_FORMAT,
                        "Dung lượng giải nén vượt quá giới hạn an toàn.",
                    )
                }
                out.write(chunk, 0, count)
            }
        }
        return written
    }

    private fun hasSqliteHeader(file: File): Boolean {
        if (file.length() < SQLITE_HEADER_PREFIX.length) return false
        val header = ByteArray(SQLITE_HEADER_PREFIX.length)
        file.inputStream().use { input ->
            if (input.read(header) < header.size) return false
        }
        return String(header, StandardCharsets.US_ASCII) == SQLITE_HEADER_PREFIX
    }
}
