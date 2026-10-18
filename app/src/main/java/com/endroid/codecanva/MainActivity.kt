package com.endroid.codecanva

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import com.endroid.codecanva.ui.VideoQuality

class MainActivity : ComponentActivity() {

    private val vm: CanvaViewModel by viewModels()

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
            } else {
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
            val amoled = state.themeMode == AppThemeMode.AMOLED
            val scheme = if (dark) {
                darkColorScheme(
                    primary = Color(0xFFB39DFF),
                    secondary = Color(0xFF80CBC4),
                    surface = if (amoled) Color.Black else Color(0xFF121212),
                    background = if (amoled) Color.Black else Color(0xFF0D0D0D),
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF5E35B1),
                    secondary = Color(0xFF00897B),
                )
            }

            // Immersive in theater so recording / UI hide system action bars as much as possible
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
                        if (enabled) requestRecording()
                        else stopRecording()
                        vm.setRecordEnabled(enabled)
                    },
                    onThemeMode = vm::setThemeMode,
                    onVideoQuality = vm::setVideoQuality,
                    onStart = {
                        // If record was requested but projection not yet running, still start typing
                        vm.startTyping()
                    },
                    onPause = vm::pauseTyping,
                    onReset = {
                        stopRecording()
                        vm.reset()
                    },
                    onOpenPreview = vm::openPreview,
                    onBackTheater = {
                        stopRecording()
                        vm.backFromTheater()
                    },
                    onBackPreview = vm::backFromPreview,
                    onStopRecording = {
                        stopRecording()
                    },
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
