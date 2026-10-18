package com.endroid.codecanva

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.endroid.codecanva.record.RecordingService
import com.endroid.codecanva.ui.AppScreen
import com.endroid.codecanva.ui.AppThemeMode
import com.endroid.codecanva.ui.CanvaViewModel
import com.endroid.codecanva.ui.CodeCanvaApp
import com.endroid.codecanva.ui.theme.colorSchemeFromSeed

class MainActivity : ComponentActivity() {

    private val vm: CanvaViewModel by viewModels()
    /** When true, projection result should start typing after recorder starts. */
    private var startAfterProjection = false

    private val projectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val quality = vm.ui.value.videoQuality.name
                val intent = Intent(this, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START
                    putExtra(RecordingService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(RecordingService.EXTRA_DATA, result.data)
                    putExtra(RecordingService.EXTRA_QUALITY, quality)
                }
                ContextCompat.startForegroundService(this, intent)
                vm.onRecordingStarted()
                if (startAfterProjection) {
                    startAfterProjection = false
                    vm.startTyping()
                }
            } else {
                startAfterProjection = false
                vm.onRecordingDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            val state by vm.ui.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val dark = when (state.themeMode) {
                AppThemeMode.SYSTEM -> systemDark
                AppThemeMode.DARK, AppThemeMode.AMOLED -> true
                AppThemeMode.LIGHT -> false
            }
            val seed = Color(state.seedColorArgb)
            var scheme = colorSchemeFromSeed(seed, dark)
            if (state.themeMode == AppThemeMode.AMOLED) {
                scheme = scheme.copy(background = Color.Black, surface = Color.Black)
            }

            SideEffect {
                val immersive = state.screen == AppScreen.THEATER
                val controller = WindowInsetsControllerCompat(window, window.decorView)
                if (immersive) {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    controller.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    WindowCompat.setDecorFitsSystemWindows(window, true)
                    controller.show(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
                window.statusBarColor = scheme.surface.toArgb()
                window.navigationBarColor = scheme.surface.toArgb()
            }

            MaterialTheme(colorScheme = scheme) {
                CodeCanvaApp(
                    state = state,
                    onSourceChange = vm::setSource,
                    onLanguageChange = vm::setLanguage,
                    onSpeedChange = vm::setSpeedCps,
                    onModeChange = vm::setMode,
                    onRecordToggle = { enabled ->
                        // Preference only — does not start capture yet
                        vm.setRecordEnabled(enabled)
                    },
                    onThemeMode = vm::setThemeMode,
                    onSeedColor = vm::setSeedColor,
                    onVideoQuality = vm::setVideoQuality,
                    onStart = {
                        if (state.recordEnabled && !state.recording) {
                            startAfterProjection = true
                            requestRecording()
                        } else {
                            vm.startTyping()
                        }
                    },
                    onReset = {
                        stopRecording()
                        vm.reset()
                    },
                    onOpenPreview = vm::openPreview,
                    onBackTheater = {
                        // Stop recording only when leaving theater via Back
                        stopRecording()
                        vm.backFromTheater()
                    },
                    onBackPreview = vm::backFromPreview,
                    onFontSize = vm::setFontSize,
                )
            }
        }
    }

    private fun requestRecording() {
        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projectionLauncher.launch(mpm.createScreenCaptureIntent())
    }

    private fun stopRecording() {
        startService(Intent(this, RecordingService::class.java).apply {
            action = RecordingService.ACTION_STOP
        })
        vm.onRecordingStopped()
    }
}
