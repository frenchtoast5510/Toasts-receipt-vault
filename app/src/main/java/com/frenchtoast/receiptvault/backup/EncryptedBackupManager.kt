package com.frenchtoast.receiptvault.backup

import android.content.Context
import android.net.Uri
import com.frenchtoast.receiptvault.data.local.VaultItemEntity
import com.frenchtoast.receiptvault.data.storage.VaultImageStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
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

@Serializable
data class BackupManifest(
    val formatVersion: Int = 2,
    val exportedAtMillis: Long,
    val items: List<VaultItemEntity>,
)

data class BackupRestoreSummary(
    val restoredItemsCount: Int,
    val restoredImagesCount: Int,
    val restoredDocsCount: Int,
)

data class DecryptedArchiveData(
    val manifest: BackupManifest,
    val images: Map<String, ByteArray>,
    val documents: Map<String, ByteArray>,
)

class EncryptedBackupManager(
    private val context: Context,
    private val imageStorage: VaultImageStorage,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    suspend fun exportEncryptedBackup(
        destinationUri: Uri,
        password: String,
        items: List<VaultItemEntity>,
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            require(password.length >= 6) { "Password must be at least 6 characters." }
            val plainZipBytes = buildZipArchive(items)
            val encryptedBytes = encrypt(plainZipBytes, password)
            context.contentResolver.openOutputStream(destinationUri)?.use { os ->
                os.write(encryptedBytes)
                os.flush()
            } ?: error("Unable to open destination file for writing.")
            items.size
        }
    }

    suspend fun importEncryptedBackup(
        sourceUri: Uri,
        password: String,
    ): Result<DecryptedArchiveData> = withContext(Dispatchers.IO) {
        runCatching {
            require(password.isNotEmpty()) { "Enter the backup password." }
            val encryptedBytes = context.contentResolver.openInputStream(sourceUri)?.use { input ->
                input.readBytes()
            } ?: error("Unable to read selected backup file.")

            val plainZipBytes = try {
                decrypt(encryptedBytes, password)
            } catch (_: AEADBadTagException) {
                error("Incorrect password or corrupted backup archive.")
            }

            parseZipArchive(plainZipBytes)
        }
    }

    internal fun buildZipArchive(items: List<VaultItemEntity>): ByteArray {
        val manifest = BackupManifest(
            formatVersion = 2,
            exportedAtMillis = System.currentTimeMillis(),
            items = items,
        )
        val manifestBytes = json.encodeToString(manifest).toByteArray(Charsets.UTF_8)

        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifestBytes)
            zip.closeEntry()

            val imageNames = items.flatMap { listOfNotNull(it.receiptImagePath, it.serialImagePath) }
                .map { File(it).name }
                .distinct()

            imageNames.forEach { fileName ->
                val file = imageStorage.resolveImageFile(fileName)
                if (file != null && file.exists()) {
                    zip.putNextEntry(ZipEntry("$IMAGES_DIR_PREFIX$fileName"))
                    zip.write(file.readBytes())
                    zip.closeEntry()
                }
            }

            val docNames = items.mapNotNull { it.documentPath }
                .map { File(it).name }
                .distinct()

            docNames.forEach { fileName ->
                val file = imageStorage.resolveDocumentFile(fileName)
                if (file != null && file.exists()) {
                    zip.putNextEntry(ZipEntry("$DOCS_DIR_PREFIX$fileName"))
                    zip.write(file.readBytes())
                    zip.closeEntry()
                }
            }
        }
        return baos.toByteArray()
    }

    internal fun parseZipArchive(zipBytes: ByteArray): DecryptedArchiveData {
        var manifest: BackupManifest? = null
        val images = mutableMapOf<String, ByteArray>()
        val documents = mutableMapOf<String, ByteArray>()

        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && !name.contains("..")) {
                    when {
                        name == MANIFEST_ENTRY -> {
                            val content = zis.readBytes().toString(Charsets.UTF_8)
                            manifest = json.decodeFromString<BackupManifest>(content)
                        }
                        name.startsWith(IMAGES_DIR_PREFIX) -> {
                            val safeFileName = File(name.removePrefix(IMAGES_DIR_PREFIX)).name
                            if (safeFileName.isNotEmpty()) {
                                images[safeFileName] = zis.readBytes()
                            }
                        }
                        name.startsWith(DOCS_DIR_PREFIX) -> {
                            val safeFileName = File(name.removePrefix(DOCS_DIR_PREFIX)).name
                            if (safeFileName.isNotEmpty()) {
                                documents[safeFileName] = zis.readBytes()
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val parsedManifest = manifest ?: error("Backup archive is missing vault_manifest.json.")
        return DecryptedArchiveData(parsedManifest, images, documents)
    }

    internal fun encrypt(plaintext: ByteArray, password: String): ByteArray {
        val rng = SecureRandom()
        val salt = ByteArray(SALT_LENGTH).apply { rng.nextBytes(this) }
        val iv = ByteArray(IV_LENGTH).apply { rng.nextBytes(this) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)

        val out = ByteArrayOutputStream(MAGIC.size + SALT_LENGTH + IV_LENGTH + ciphertext.size)
        out.write(MAGIC)
        out.write(salt)
        out.write(iv)
        out.write(ciphertext)
        return out.toByteArray()
    }

    internal fun decrypt(payload: ByteArray, password: String): ByteArray {
        val headerSize = MAGIC.size + SALT_LENGTH + IV_LENGTH
        require(payload.size > headerSize) { "Invalid backup file format." }
        val magic = payload.copyOfRange(0, MAGIC.size)
        require(magic.contentEquals(MAGIC)) { "Unsupported backup file format." }

        val salt = payload.copyOfRange(MAGIC.size, MAGIC.size + SALT_LENGTH)
        val iv = payload.copyOfRange(MAGIC.size + SALT_LENGTH, headerSize)
        val ciphertext = payload.copyOfRange(headerSize, payload.size)
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(keyBytes, "AES")
    }

    companion object {
        private val MAGIC = byteArrayOf('T'.code.toByte(), 'R'.code.toByte(), 'V'.code.toByte(), '1'.code.toByte())
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 12
        private const val GCM_TAG_BITS = 128
        private const val KEY_LENGTH_BITS = 256
        private const val PBKDF2_ITERATIONS = 120_000
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val MANIFEST_ENTRY = "vault_manifest.json"
        private const val IMAGES_DIR_PREFIX = "images/"
        private const val DOCS_DIR_PREFIX = "docs/"
    }
}
