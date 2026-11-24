package com.example.student_wallet.feature.security.backup

import com.example.student_wallet.core.domain.model.CurrencyOption
import com.example.student_wallet.core.domain.model.ThemeMode
import com.example.student_wallet.core.domain.model.UserSettings
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class WalletBackupArchiveTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `pack and unpack preserve sqlite header settings and receipt mapping`() {
        val fakeDbBytes = "SQLite format 3\u0000".toByteArray(StandardCharsets.US_ASCII) +
            ByteArray(128) { idx -> (idx % 17).toByte() }
        val receiptSource = File(tempFolder.newFolder("src_receipts"), "hoa_don_01.webp").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        }
        val settings = UserSettings(
            themeMode = ThemeMode.DARK,
            currency = CurrencyOption.USD,
            dynamicColor = true,
        )

        val zipBytes = WalletBackupArchive.packToZipBytes(
            dbBytes = fakeDbBytes,
            dbVersion = 1,
            createdAtEpochMillis = 1_764_000_000_000L,
            transactionCount = 14,
            categoryCount = 7,
            settings = settings,
            receiptEntries = listOf(
                ReceiptBackupEntry(
                    archiveEntryName = "hoa_don_01.webp",
                    originalPath = receiptSource.absolutePath,
                    sourceFile = receiptSource,
                ),
            ),
        )

        val extractDir = tempFolder.newFolder("extracted")
        val unpacked = WalletBackupArchive.unpackToDirectory(zipBytes, extractDir)

        assertArrayEquals(fakeDbBytes, unpacked.dbFile.readBytes())
        assertEquals(settings, unpacked.settings)
        assertEquals(1_764_000_000_000L, unpacked.createdAtEpochMillis)
        assertEquals(1, unpacked.dbVersion)
        assertEquals(14, unpacked.manifestTransactionCount)
        assertEquals(7, unpacked.manifestCategoryCount)
        assertEquals(1, unpacked.manifestReceiptCount)
        assertEquals("hoa_don_01.webp", unpacked.receiptEntryByOriginalPath[receiptSource.absolutePath])

        val restoredReceipt = File(unpacked.receiptsDir, "hoa_don_01.webp")
        assertTrue(restoredReceipt.isFile)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6), restoredReceipt.readBytes())
    }

    @Test
    fun `unpack blocks zip slip path traversal entries`() {
        val maliciousZip = ByteArrayOutputStream().apply {
            ZipOutputStream(this).use { zip ->
                zip.putNextEntry(ZipEntry("../../outside_escape.txt"))
                zip.write("evil".toByteArray())
                zip.closeEntry()
            }
        }.toByteArray()

        val extractDir = tempFolder.newFolder("safe_extract")
        try {
            WalletBackupArchive.unpackToDirectory(maliciousZip, extractDir)
            fail("Expected WalletBackupException for Zip-Slip entry")
        } catch (expected: WalletBackupException) {
            assertEquals(WalletBackupErrorReason.INVALID_FORMAT, expected.reason)
        }
    }
}
