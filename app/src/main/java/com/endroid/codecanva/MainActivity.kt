package com.endroid.codecanva

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.endroid.codecanva.record.RecordingService
import com.endroid.codecanva.ui.CodeCanvaApp
import com.endroid.codecanva.ui.CanvaViewModel

class MainActivity : ComponentActivity() {

    private val vm: CanvaViewModel by viewModels()

    private val projectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val intent = Intent(this, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START
                    putExtra(RecordingService.EXTRA_RESULT_CODE, result.resultCode)
                    putExtra(RecordingService.EXTRA_DATA, result.data)
                }
                ContextCompat.startForegroundService(this, intent)
                vm.onRecordingStarted()
            } else {
                vm.onRecordingDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val dark = isSystemInDarkTheme()
            val scheme = if (dark) {
                darkColorScheme(
                    primary = Color(0xFFB39DFF),
                    secondary = Color(0xFF80CBC4),
                    surface = Color(0xFF121212),
                    background = Color(0xFF0D0D0D),
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF5E35B1),
                    secondary = Color(0xFF00897B),
                )
            }
            MaterialTheme(colorScheme = scheme) {
                val state by vm.ui.collectAsState()
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
                    onStart = vm::startTyping,
                    onPause = vm::pauseTyping,
                    onReset = {
                        stopRecording()
                        vm.reset()
                    },
                    onShowPreview = vm::setShowPreview,
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

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}
