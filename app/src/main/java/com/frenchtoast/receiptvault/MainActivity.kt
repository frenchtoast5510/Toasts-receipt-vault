package com.frenchtoast.receiptvault

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.frenchtoast.receiptvault.data.preferences.VaultSettings
import com.frenchtoast.receiptvault.ui.components.LockScreenOverlay
import com.frenchtoast.receiptvault.ui.navigation.VaultAppRoot
import com.frenchtoast.receiptvault.ui.theme.ToastsReceiptVaultTheme
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val appContainer: AppContainer
        get() = (application as ToastsVaultApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = appContainer
        lifecycleScope.launch {
            val initialSettings = container.repository.getSettingsSnapshot()
            container.appLockController.initializeOnLaunch(initialSettings.appLockMode)
        }

        setContent {
            val settings by container.repository.settingsFlow
                .collectAsStateWithLifecycle(initialValue = VaultSettings())
            val isLocked by container.appLockController.isLocked.collectAsStateWithLifecycle()

            LaunchedEffect(settings.hideInRecents) {
                if (settings.hideInRecents) {
                    window.setFlags(
                        WindowManager.LayoutParams.FLAG_SECURE,
                        WindowManager.LayoutParams.FLAG_SECURE,
                    )
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            ToastsReceiptVaultTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    VaultAppRoot(container = container)

                    AnimatedVisibility(
                        visible = isLocked,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        LockScreenOverlay(
                            settings = settings,
                            appLockController = container.appLockController,
                            onUnlocked = { container.appLockController.unlock() },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val container = appContainer
        lifecycleScope.launch {
            val settings = container.repository.getSettingsSnapshot()
            container.appLockController.onAppStarted(settings.appLockMode, settings.autoLockTimeout)
        }
    }

    override fun onStop() {
        super.onStop()
        val container = appContainer
        lifecycleScope.launch {
            val settings = container.repository.getSettingsSnapshot()
            container.appLockController.onAppStopped(settings.appLockMode)
        }
    }
}
