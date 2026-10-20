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
import androidx.lifecycle.lifecycleScope
import com.endroid.codecanva.audio.TypingSoundPlayer
import com.endroid.codecanva.record.RecordingService
import com.endroid.codecanva.ui.AppScreen
import com.endroid.codecanva.ui.AppThemeMode
import com.endroid.codecanva.ui.CanvaViewModel
import com.endroid.codecanva.ui.CodeCanvaApp
import com.endroid.codecanva.ui.theme.colorSchemeFromSeed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val vm: CanvaViewModel by viewModels()
    private var pendingProjectionData: Intent? = null
    private var pendingProjectionCode: Int = 0
    private val typingSound = TypingSoundPlayer()

    private val projectionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // Switch UI to intro/theater first so setup is never in the video
                pendingProjectionCode = result.resultCode
                pendingProjectionData = result.data
                vm.beginSession()
                lifecycleScope.launch {
                    // Short delay after immersive screen is up
                    delay(400)
                    val data = pendingProjectionData ?: return@launch
                    val quality = vm.ui.value.videoQuality.name
                    val intent = Intent(this@MainActivity, RecordingService::class.java).apply {
                        action = RecordingService.ACTION_START
                        putExtra(RecordingService.EXTRA_RESULT_CODE, pendingProjectionCode)
                        putExtra(RecordingService.EXTRA_DATA, data)
                        putExtra(RecordingService.EXTRA_QUALITY, quality)
                    }
                    ContextCompat.startForegroundService(this@MainActivity, intent)
                    vm.onRecordingStarted()
                    pendingProjectionData = null
                }
            } else {
                pendingProjectionData = null
                vm.onRecordingDenied()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        vm.onKeyClick = { typingSound.click() }
        vm.onSessionFullyEnded = { stopRecording() }

        setContent {
            val state by vm.ui.collectAsState()
            typingSound.setEnabled(state.soundEnabled)

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
                val immersive = state.screen != AppScreen.SETUP &&
                    state.screen != AppScreen.PREVIEW &&
                    state.screen != AppScreen.EDITOR
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
                    onHumanTyping = vm::setHumanTyping,
                    onShowCursor = vm::setShowCursor,
                    onSoundEnabled = { on ->
                        typingSound.setEnabled(on)
                        vm.setSoundEnabled(on)
                    },
                    onRecordToggle = { vm.setRecordEnabled(it) },
                    onThemeMode = vm::setThemeMode,
                    onSeedColor = vm::setSeedColor,
                    onVideoQuality = vm::setVideoQuality,
                    onIntro = vm::setIntro,
                    onOutro = vm::setOutro,
                    onSetupTab = vm::setSetupTab,
                    onStart = {
                        if (state.recordEnabled && !state.recording) {
                            requestRecording()
                        } else {
                            vm.beginSession()
                        }
                    },
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
                    onBackCard = {
                        stopRecording()
                        vm.backFromCard()
                    },
                    onSkipCard = vm::skipCard,
                    onFontSize = vm::setFontSize,
                    onEditorFont = vm::setEditorFont,
                    onLibraryChanged = vm::bumpLibrary,
                    onOpenEditor = vm::openEditor,
                    onTrimStart = vm::setTrimStart,
                    onTrimEnd = vm::setTrimEnd,
                    onCloseEditor = vm::closeEditor,
                    onEditBusy = vm::setEditBusy,
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
        typingSound.release()
        super.onDestroy()
    }
}
