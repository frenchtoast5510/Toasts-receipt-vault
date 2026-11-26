package com.frenchtoast.receiptvault

import android.app.Application
import com.frenchtoast.receiptvault.backup.EncryptedBackupManager
import com.frenchtoast.receiptvault.data.VaultRepository
import com.frenchtoast.receiptvault.data.demo.DemoDataSeeder
import com.frenchtoast.receiptvault.data.local.VaultDatabase
import com.frenchtoast.receiptvault.data.preferences.VaultPreferences
import com.frenchtoast.receiptvault.data.storage.VaultImageStorage
import com.frenchtoast.receiptvault.export.ClaimPacketGenerator
import com.frenchtoast.receiptvault.ocr.OnDeviceScanner
import com.frenchtoast.receiptvault.reminders.ReminderScheduler
import com.frenchtoast.receiptvault.security.AppLockController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(application: Application) {
    val database = VaultDatabase.getInstance(application)
    val preferences = VaultPreferences(application)
    val imageStorage = VaultImageStorage(application)
    val demoSeeder = DemoDataSeeder(imageStorage)
    val backupManager = EncryptedBackupManager(application, imageStorage)
    val reminderScheduler = ReminderScheduler(application)
    val scanner = OnDeviceScanner(application)
    val claimPacketGenerator = ClaimPacketGenerator(application, imageStorage)
    val appLockController = AppLockController()

    val repository = VaultRepository(
        dao = database.vaultItemDao(),
        preferences = preferences,
        imageStorage = imageStorage,
        demoSeeder = demoSeeder,
        backupManager = backupManager,
        reminderScheduler = reminderScheduler,
    )
}

class ToastsVaultApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.repository.ensureInitialDemoState()
        }
    }
}
