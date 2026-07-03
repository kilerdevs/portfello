package com.portfello.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.portfello.data.AppPrefs
import com.portfello.data.crypto.AppLockStatus
import com.portfello.data.crypto.LockState
import com.portfello.data.sync.SyncScheduler
import com.portfello.ui.lock.LockScreen
import com.portfello.ui.navigation.AppNavGraph
import com.portfello.ui.onboarding.OnboardingScreen
import com.portfello.ui.theme.PortfelloTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// AppCompatActivity (a FragmentActivity, so BiometricPrompt still works) is
// required for per-app locale switching via AppCompatDelegate
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject lateinit var lockState: LockState
    @Inject lateinit var syncScheduler: SyncScheduler
    @Inject lateinit var prefs: AppPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        if (prefs.onboardingComplete) {
            syncScheduler.schedule(prefs.syncIntervalMin)
        }
        setContent {
            PortfelloTheme {
                val status by lockState.status.collectAsStateWithLifecycle()
                when (status) {
                    AppLockStatus.NEEDS_SETUP,
                    AppLockStatus.LOCKED -> LockScreen()
                    AppLockStatus.UNLOCKED -> {
                        var onboardingDone by remember { mutableStateOf(prefs.onboardingComplete) }
                        if (!onboardingDone) {
                            OnboardingScreen(prefs) {
                                onboardingDone = true
                                syncScheduler.schedule(prefs.syncIntervalMin)
                            }
                        } else {
                            val navController = rememberNavController()
                            AppNavGraph(navController = navController)
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lockState.onAppBackground()
    }

    override fun onStart() {
        super.onStart()
        lockState.onAppForeground()
    }
}
