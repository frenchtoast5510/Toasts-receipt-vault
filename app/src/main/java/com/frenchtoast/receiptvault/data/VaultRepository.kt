package com.frenchtoast.receiptvault.data

import android.net.Uri
import com.frenchtoast.receiptvault.backup.BackupRestoreSummary
import com.frenchtoast.receiptvault.backup.EncryptedBackupManager
import com.frenchtoast.receiptvault.data.demo.DemoDataSeeder
import com.frenchtoast.receiptvault.data.local.VaultItemDao
import com.frenchtoast.receiptvault.data.local.VaultItemEntity
import com.frenchtoast.receiptvault.data.preferences.AppLockMode
import com.frenchtoast.receiptvault.data.preferences.AutoLockTimeout
import com.frenchtoast.receiptvault.data.preferences.VaultPreferences
import com.frenchtoast.receiptvault.data.preferences.VaultSettings
import com.frenchtoast.receiptvault.data.storage.VaultImageStorage
import com.frenchtoast.receiptvault.model.VaultItem
import com.frenchtoast.receiptvault.reminders.ReminderScheduler
import com.frenchtoast.receiptvault.security.AppSecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class VaultRepository(
    private val dao: VaultItemDao,
    private val preferences: VaultPreferences,
    private val imageStorage: VaultImageStorage,
    private val demoSeeder: DemoDataSeeder,
    private val backupManager: EncryptedBackupManager,
    private val reminderScheduler: ReminderScheduler,
) {
    val itemsFlow: Flow<List<VaultItem>> = dao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    val settingsFlow: Flow<VaultSettings> = preferences.settingsFlow

    fun observeItem(id: String): Flow<VaultItem?> = dao.observeById(id).map { it?.toDomain() }

    suspend fun getItem(id: String): VaultItem? = dao.getById(id)?.toDomain()

    suspend fun getSettingsSnapshot(): VaultSettings = preferences.getSnapshot()

    suspend fun ensureInitialDemoState() = withContext(Dispatchers.IO) {
        val settings = preferences.getSnapshot()
        if (!settings.demoSeededOnce) {
            if (settings.demoModeEnabled && dao.getAllSnapshot().isEmpty()) {
                seedDemoItems()
            }
            preferences.markDemoSeededOnce()
        } else if (settings.demoModeEnabled && dao.countDemoItems() > 0) {
            // Ensure bundled demo PDF/images exist if upgrading from v1
            val existingDemo = dao.getAllSnapshot().filter { it.isDemo }.map { it.toDomain() }
            if (existingDemo.none { it.documentPath != null }) {
                clearDemoItemsOnly()
                seedDemoItems()
            }
        }
    }

    suspend fun setDemoModeEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        preferences.setDemoModeEnabled(enabled)
        if (enabled) {
            if (dao.countDemoItems() == 0) {
                seedDemoItems()
            }
        } else {
            clearDemoItemsOnly()
        }
    }

    suspend fun clearDemoItemsToEmptyVault() = withContext(Dispatchers.IO) {
        preferences.setDemoModeEnabled(false)
        clearDemoItemsOnly()
    }

    suspend fun resetSampleData() = withContext(Dispatchers.IO) {
        clearDemoItemsOnly()
        seedDemoItems()
        preferences.setDemoModeEnabled(true)
    }

    private suspend fun seedDemoItems() {
        val samples = demoSeeder.buildSampleItems()
        demoSeeder.generateAndStoreDemoImages(samples)
        dao.upsertAll(samples.map { VaultItemEntity.fromDomain(it) })
    }

    private suspend fun clearDemoItemsOnly() {
        val all = dao.getAllSnapshot()
        val demoItems = all.filter { it.isDemo }
        val userImages = all.filter { !it.isDemo }
            .flatMap { listOfNotNull(it.receiptImagePath, it.serialImagePath) }
            .toSet()
        val userDocs = all.filter { !it.isDemo }
            .mapNotNull { it.documentPath }
            .toSet()

        dao.deleteAllDemoItems()
        demoItems.forEach { item ->
            item.receiptImagePath?.takeIf { it !in userImages }?.let { imageStorage.deleteImage(it) }
            item.serialImagePath?.takeIf { it !in userImages }?.let { imageStorage.deleteImage(it) }
            item.documentPath?.takeIf { it !in userDocs }?.let { imageStorage.deleteDocument(it) }
        }
    }

    suspend fun saveItem(item: VaultItem) = withContext(Dispatchers.IO) {
        val existing = dao.getById(item.id)
        dao.upsert(VaultItemEntity.fromDomain(item))

        val allAfter = dao.getAllSnapshot()
        val remainingImages = allAfter
            .flatMap { listOfNotNull(it.receiptImagePath, it.serialImagePath) }
            .toSet()
        val remainingDocs = allAfter.mapNotNull { it.documentPath }.toSet()

        listOfNotNull(existing?.receiptImagePath, existing?.serialImagePath)
            .filter { it !in remainingImages }
            .forEach { imageStorage.deleteImage(it) }
        existing?.documentPath
            ?.takeIf { it !in remainingDocs }
            ?.let { imageStorage.deleteDocument(it) }
    }

    suspend fun deleteItem(id: String) = withContext(Dispatchers.IO) {
        val existing = dao.getById(id) ?: return@withContext
        dao.deleteById(id)
        val allAfter = dao.getAllSnapshot()
        val remainingImages = allAfter
            .flatMap { listOfNotNull(it.receiptImagePath, it.serialImagePath) }
            .toSet()
        val remainingDocs = allAfter.mapNotNull { it.documentPath }.toSet()

        listOfNotNull(existing.receiptImagePath, existing.serialImagePath)
            .filter { it !in remainingImages }
            .forEach { imageStorage.deleteImage(it) }
        existing.documentPath
            ?.takeIf { it !in remainingDocs }
            ?.let { imageStorage.deleteDocument(it) }
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        preferences.setRemindersEnabled(enabled)
        if (enabled) {
            reminderScheduler.scheduleDailyReminders()
        } else {
            reminderScheduler.cancelReminders()
        }
    }

    suspend fun updateLeadDays(returnDays: Int, warrantyDays: Int) {
        preferences.setLeadDays(returnDays, warrantyDays)
        val settings = preferences.getSnapshot()
        if (settings.remindersEnabled) {
            reminderScheduler.scheduleDailyReminders()
        }
    }

    fun triggerImmediateReminderCheck() {
        reminderScheduler.runImmediateCheck()
    }

    suspend fun setAppLockMode(mode: AppLockMode) {
        preferences.setAppLockMode(mode)
    }

    suspend fun configureCustomPin(secret: String) {
        val (salt, hash) = AppSecurityManager.hashSecret(secret)
        preferences.saveCustomPin(salt, hash)
    }

    suspend fun clearCustomPin() {
        preferences.clearCustomPin()
    }

    suspend fun setAllowBiometricWithCustomPin(allowed: Boolean) {
        preferences.setAllowBiometricWithCustomPin(allowed)
    }

    suspend fun setAutoLockTimeout(timeout: AutoLockTimeout) {
        preferences.setAutoLockTimeout(timeout)
    }

    suspend fun setHideInRecents(hide: Boolean) {
        preferences.setHideInRecents(hide)
    }

    suspend fun exportEncryptedBackup(destinationUri: Uri, password: String): Result<Int> {
        val items = dao.getAllSnapshot()
        return backupManager.exportEncryptedBackup(destinationUri, password, items)
    }

    suspend fun importEncryptedBackup(
        sourceUri: Uri,
        password: String,
        replaceExisting: Boolean,
    ): Result<BackupRestoreSummary> = withContext(Dispatchers.IO) {
        backupManager.importEncryptedBackup(sourceUri, password).map { archive ->
            if (replaceExisting) {
                dao.deleteAll()
            }
            var savedImages = 0
            archive.images.forEach { (fileName, bytes) ->
                imageStorage.saveBytes(fileName, bytes)
                savedImages++
            }
            var savedDocs = 0
            archive.documents.forEach { (fileName, bytes) ->
                imageStorage.saveDocBytes(fileName, bytes)
                savedDocs++
            }
            dao.upsertAll(archive.manifest.items)
            if (replaceExisting) {
                val referencedImages = archive.manifest.items
                    .flatMap { listOfNotNull(it.receiptImagePath, it.serialImagePath) }
                    .map { File(it).name }
                    .toSet()
                val referencedDocs = archive.manifest.items
                    .mapNotNull { it.documentPath }
                    .map { File(it).name }
                    .toSet()
                imageStorage.pruneUnreferenced(referencedImages, referencedDocs)
            }
            val hasDemo = archive.manifest.items.any { it.isDemo }
            if (!hasDemo && replaceExisting) {
                preferences.setDemoModeEnabled(false)
            }
            BackupRestoreSummary(
                restoredItemsCount = archive.manifest.items.size,
                restoredImagesCount = savedImages,
                restoredDocsCount = savedDocs,
            )
        }
    }

    fun localStorageBytes(): Long = imageStorage.totalStorageBytes()
}
