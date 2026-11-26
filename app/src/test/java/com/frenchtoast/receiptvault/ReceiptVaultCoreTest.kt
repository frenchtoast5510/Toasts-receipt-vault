package com.frenchtoast.receiptvault

import com.frenchtoast.receiptvault.backup.BackupManifest
import com.frenchtoast.receiptvault.data.local.VaultItemEntity
import com.frenchtoast.receiptvault.model.EntryType
import com.frenchtoast.receiptvault.model.HomeFilterChip
import com.frenchtoast.receiptvault.model.ItemCategory
import com.frenchtoast.receiptvault.model.VaultDate
import com.frenchtoast.receiptvault.model.VaultItem
import com.frenchtoast.receiptvault.ocr.CardMasker
import com.frenchtoast.receiptvault.ocr.ReceiptParser
import com.frenchtoast.receiptvault.security.AppSecurityManager
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

class ReceiptVaultCoreTest {

    @Test
    fun cardMaskerMasksCardsAndPreservesSerialNumbers() {
        val sample = """
            BEST BUY #0412
            VISA CREDIT 4532 1234 5678 9012
            AMEX 3782-822463-10005
            S/N: 12345678901234
            TOTAL: $349.99
        """.trimIndent()

        val result = CardMasker.maskCardNumbers(sample)
        assertEquals(2, result.maskedCount)
        assertFalse(result.maskedText.contains("4532"))
        assertFalse(result.maskedText.contains("822463"))
        assertTrue(result.maskedText.contains("9012"))
        assertTrue(result.maskedText.contains("0005"))
        assertFalse(result.maskedText.contains("10005"))
        assertTrue(result.maskedText.contains("12345678901234"))
    }

    @Test
    fun receiptParserExtractsCoreFields() {
        val raw = """
            MICRO CENTER
            DATE: 2026-10-12
            ITEM: OLED HANDHELD 512GB
            30 DAY RETURN POLICY
            1 YEAR WARRANTY
            TOTAL: $549.00
            MASTERCARD 5412 7512 3456 7890
            SERIAL NO: FVXX41200984B
        """.trimIndent()

        val parsed = ReceiptParser.parse(raw)
        assertEquals("MICRO CENTER", parsed.storeName)
        assertEquals(54900L, parsed.priceCents)
        assertEquals("FVXX41200984B", parsed.serialNumber)
        assertTrue(parsed.didMaskCard)
        assertNotNull(parsed.purchaseDateEpochDay)
        assertNotNull(parsed.returnDeadlineEpochDay)
        assertNotNull(parsed.warrantyExpirationEpochDay)
    }

    @Test
    fun encryptedBackupEnvelopeV2RoundTripAndWrongPasswordRejection() {
        val item = VaultItemEntity(
            id = "test-1",
            entryType = EntryType.DOCUMENT.name,
            title = "Home Insurance Policy",
            storeName = "Pacific Mutual",
            category = ItemCategory.OTHER.name,
            purchaseDateEpochDay = VaultDate.fromYmd(2026, 11, 1),
            priceCents = 14999L,
            currencyCode = "USD",
            serialNumber = "POL-99812",
            returnDeadlineEpochDay = null,
            warrantyExpirationEpochDay = VaultDate.fromYmd(2027, 11, 1),
            notes = "Keep copy",
            rawOcrText = "",
            receiptImagePath = null,
            serialImagePath = null,
            documentPath = "doc_policy.pdf",
            documentDisplayName = "policy_2026.pdf",
            documentMimeType = "application/pdf",
            isDemo = false,
            createdAtMillis = 1000L,
            updatedAtMillis = 1000L,
        )

        val manifest = BackupManifest(formatVersion = 2, exportedAtMillis = 12345L, items = listOf(item))
        val json = Json { ignoreUnknownKeys = true }
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry("vault_manifest.json"))
            zip.write(json.encodeToString(manifest).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("docs/doc_policy.pdf"))
            zip.write("%PDF-1.4\n%stub".toByteArray(Charsets.US_ASCII))
            zip.closeEntry()
        }
        val plainZip = baos.toByteArray()

        val rng = SecureRandom()
        val salt = ByteArray(16).apply { rng.nextBytes(this) }
        val iv = ByteArray(12).apply { rng.nextBytes(this) }
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(PBEKeySpec("secret-toast".toCharArray(), salt, 120_000, 256)).encoded
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plainZip)

        val decCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(keyBytes, "AES"), GCMParameterSpec(128, iv))
        val decryptedZip = decCipher.doFinal(ciphertext)

        var restored: BackupManifest? = null
        var hasDocEntry = false
        ZipInputStream(ByteArrayInputStream(decryptedZip)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                when (entry.name) {
                    "vault_manifest.json" -> {
                        restored = json.decodeFromString<BackupManifest>(zis.readBytes().toString(Charsets.UTF_8))
                    }
                    "docs/doc_policy.pdf" -> {
                        hasDocEntry = zis.readBytes().isNotEmpty()
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        assertEquals(2, restored?.formatVersion)
        assertEquals(1, restored?.items?.size)
        assertEquals("Home Insurance Policy", restored?.items?.first()?.title)
        assertEquals("policy_2026.pdf", restored?.items?.first()?.documentDisplayName)
        assertTrue(hasDocEntry)

        val wrongKey = factory.generateSecret(PBEKeySpec("wrong-pass".toCharArray(), salt, 120_000, 256)).encoded
        var badTag = false
        try {
            val badCipher = Cipher.getInstance("AES/GCM/NoPadding")
            badCipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(wrongKey, "AES"), GCMParameterSpec(128, iv))
            badCipher.doFinal(ciphertext)
        } catch (_: AEADBadTagException) {
            badTag = true
        }
        assertTrue(badTag)
    }

    @Test
    fun homeFilterChipsCategorizeTypesExpiringAndExpired() {
        val today = VaultDate.fromYmd(2026, 11, 25)
        val receiptItem = VaultItem(
            id = "1",
            entryType = EntryType.RECEIPT,
            title = "Monitor",
            storeName = "Store",
            category = ItemCategory.ELECTRONICS,
            purchaseDateEpochDay = VaultDate.plusDays(today, -5),
            priceCents = 1000,
            returnDeadlineEpochDay = VaultDate.plusDays(today, 20),
            warrantyExpirationEpochDay = VaultDate.plusDays(today, 300),
            receiptImagePath = "receipt_1.jpg",
        )
        val warrantyItem = receiptItem.copy(
            id = "2",
            entryType = EntryType.WARRANTY,
            returnDeadlineEpochDay = VaultDate.plusDays(today, 2),
        )
        val documentItem = receiptItem.copy(
            id = "3",
            entryType = EntryType.DOCUMENT,
            returnDeadlineEpochDay = null,
            warrantyExpirationEpochDay = VaultDate.plusDays(today, -1),
            receiptImagePath = null,
            documentPath = "doc_1.pdf",
            documentDisplayName = "policy.pdf",
        )

        assertTrue(receiptItem.matchesHomeFilter(HomeFilterChip.ALL, today, 5, 30))
        assertTrue(receiptItem.matchesHomeFilter(HomeFilterChip.RECEIPTS, today, 5, 30))
        assertFalse(receiptItem.matchesHomeFilter(HomeFilterChip.WARRANTIES, today, 5, 30))

        assertTrue(warrantyItem.matchesHomeFilter(HomeFilterChip.WARRANTIES, today, 5, 30))
        assertTrue(warrantyItem.matchesHomeFilter(HomeFilterChip.EXPIRING_SOON, today, 5, 30))

        assertTrue(documentItem.matchesHomeFilter(HomeFilterChip.DOCUMENTS, today, 5, 30))
        assertTrue(documentItem.matchesHomeFilter(HomeFilterChip.EXPIRED, today, 5, 30))
        assertFalse(receiptItem.matchesHomeFilter(HomeFilterChip.EXPIRED, today, 5, 30))

        assertTrue(documentItem.hasOriginalFiles())
        assertEquals(1, documentItem.attachedFileCount())
    }

    @Test
    fun saltedPinHashVerifiesCorrectSecretAndRejectsWrongSecret() {
        val (salt1, hash1) = AppSecurityManager.hashSecret("849201")
        val (salt2, hash2) = AppSecurityManager.hashSecret("849201")

        assertNotEquals(salt1, salt2)
        assertNotEquals(hash1, hash2)
        assertTrue(AppSecurityManager.verifySecret("849201", salt1, hash1))
        assertTrue(AppSecurityManager.verifySecret("849201", salt2, hash2))
        assertFalse(AppSecurityManager.verifySecret("000000", salt1, hash1))
        assertFalse(AppSecurityManager.verifySecret("849201", "", hash1))
    }

    @Test
    fun vaultDateEpochConversionIsConsistent() {
        val epochDay = VaultDate.fromYmd(2026, 11, 25)
        val (y, m, d) = VaultDate.toYmd(epochDay)
        assertEquals(2026, y)
        assertEquals(11, m)
        assertEquals(25, d)
        assertEquals("2026-11-25", VaultDate.formatIso(epochDay))
    }
}
