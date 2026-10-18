package com.endroid.codecanva.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.endroid.codecanva.editor.Language
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TypeMode { CHAR, WORD, LINE }

enum class AppScreen { SETUP, THEATER, PREVIEW }

enum class AppThemeMode { SYSTEM, DARK, LIGHT, AMOLED }

enum class VideoQuality {
    LOW,    // 720p-ish bitrate
    MEDIUM,
    HIGH,
    ULTRA,
}

data class CanvaUiState(
    val screen: AppScreen = AppScreen.SETUP,
    val source: String = DEFAULT_HTML,
    val visible: String = "",
    val language: Language = Language.XML,
    val mode: TypeMode = TypeMode.CHAR,
    val speedCps: Float = 28f,
    val running: Boolean = false,
    val finished: Boolean = false,
    val recordEnabled: Boolean = false,
    val recording: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val videoQuality: VideoQuality = VideoQuality.HIGH,
    val fontSize: Float = 15f,
    val status: String? = null,
    val progress: Float = 0f,
)

private val DEFAULT_HTML = """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8"/>
  <meta name="viewport" content="width=device-width, initial-scale=1"/>
  <title>Code Canva</title>
  <style>
    body { font-family: system-ui, sans-serif; margin: 2rem; background: #0f0f12; color: #f2f2f2; }
    h1 { color: #b39dff; }
    .card { padding: 1rem 1.25rem; border-radius: 12px; background: #1a1a22; max-width: 420px; }
  </style>
</head>
<body>
  <div class="card">
    <h1>Hello from Code Canva</h1>
    <p>Edit this HTML, press Start, watch it type out — then Run to preview.</p>
  </div>
  <script>console.log("Code Canva preview ready");</script>
</body>
</html>
""".trimIndent()

class CanvaViewModel : ViewModel() {
    private val _ui = MutableStateFlow(CanvaUiState())
    val ui: StateFlow<CanvaUiState> = _ui.asStateFlow()

    private var job: Job? = null

    fun setSource(s: String) {
        if (_ui.value.running) return
        val lang = Language.fromFileName(
            when {
                s.trimStart().startsWith("<!") || s.contains("<html", true) -> "index.html"
                s.trimStart().startsWith("{") -> "a.json"
                s.contains("fun ") && s.contains("(") -> "Main.kt"
                s.contains("def ") -> "main.py"
                else -> "file.txt"
            }
        )
        _ui.update {
            it.copy(
                source = s,
                visible = "",
                finished = false,
                progress = 0f,
                language = if (it.language == Language.PLAIN) lang else it.language,
                status = null,
            )
        }
    }

    fun setLanguage(lang: Language) = _ui.update { it.copy(language = lang) }
    fun setSpeedCps(v: Float) = _ui.update { it.copy(speedCps = v.coerceIn(4f, 120f)) }
    fun setMode(mode: TypeMode) {
        if (_ui.value.running) return
        _ui.update { it.copy(mode = mode) }
    }
    fun setRecordEnabled(v: Boolean) = _ui.update { it.copy(recordEnabled = v) }
    fun setThemeMode(m: AppThemeMode) = _ui.update { it.copy(themeMode = m) }
    fun setVideoQuality(q: VideoQuality) = _ui.update { it.copy(videoQuality = q) }
    fun setFontSize(v: Float) = _ui.update { it.copy(fontSize = v.coerceIn(11f, 28f)) }

    fun onRecordingStarted() {
        _ui.update { it.copy(recording = true, status = "Recording app…") }
    }

    fun onRecordingDenied() {
        _ui.update {
            it.copy(recordEnabled = false, recording = false, status = "Recording permission denied")
        }
    }

    fun onRecordingStopped() {
        _ui.update {
            it.copy(
                recording = false,
                status = "Recording saved (Movies/CodeCanva)",
            )
        }
    }

    /** Enter full-screen theater and begin type-out. */
    fun startTyping() {
        if (_ui.value.running) return
        val source = _ui.value.source
        if (source.isBlank()) {
            _ui.update { it.copy(status = "Paste or type some code first") }
            return
        }
        job?.cancel()
        _ui.update {
            it.copy(
                screen = AppScreen.THEATER,
                running = true,
                finished = false,
                visible = "",
                progress = 0f,
                status = "Typing…",
            )
        }
        job = viewModelScope.launch {
            val units = when (_ui.value.mode) {
                TypeMode.CHAR -> source.map { it.toString() }
                TypeMode.WORD -> tokenizeWords(source)
                TypeMode.LINE -> {
                    val lines = source.split("\n")
                    lines.mapIndexed { i, line ->
                        if (i < lines.lastIndex) line + "\n" else line
                    }
                }
            }
            val total = units.size.coerceAtLeast(1)
            val acc = StringBuilder()
            for ((index, unit) in units.withIndex()) {
                if (!isActive) break
                val d = (1000f / _ui.value.speedCps).toLong().coerceIn(8L, 400L)
                acc.append(unit)
                _ui.update {
                    it.copy(
                        visible = acc.toString(),
                        progress = (index + 1).toFloat() / total,
                    )
                }
                delay(if (_ui.value.mode == TypeMode.CHAR) d else d * unit.length.coerceAtLeast(1))
            }
            _ui.update {
                it.copy(
                    running = false,
                    finished = true,
                    visible = source,
                    progress = 1f,
                    status = "Done — open Preview or go back",
                )
            }
        }
    }

    fun pauseTyping() {
        job?.cancel()
        job = null
        _ui.update { it.copy(running = false, status = "Paused") }
    }

    fun openPreview() {
        if (_ui.value.source.isBlank()) return
        _ui.update { it.copy(screen = AppScreen.PREVIEW) }
    }

    fun backFromTheater() {
        job?.cancel()
        job = null
        _ui.update {
            it.copy(
                screen = AppScreen.SETUP,
                running = false,
                // keep visible/finished so user sees result on setup if wanted
            )
        }
    }

    fun backFromPreview() {
        // Return to theater if we were watching, else setup
        val target = if (_ui.value.visible.isNotEmpty() || _ui.value.finished) {
            AppScreen.THEATER
        } else {
            AppScreen.SETUP
        }
        _ui.update { it.copy(screen = target) }
    }

    fun goSetup() {
        job?.cancel()
        job = null
        _ui.update { it.copy(screen = AppScreen.SETUP, running = false) }
    }

    fun reset() {
        job?.cancel()
        job = null
        _ui.update {
            it.copy(
                screen = AppScreen.SETUP,
                visible = "",
                running = false,
                finished = false,
                progress = 0f,
                status = null,
                recording = false,
            )
        }
    }

    private fun tokenizeWords(s: String): List<String> {
        val out = ArrayList<String>()
        Regex("""\S+\s*|\s+""").findAll(s).forEach { out.add(it.value) }
        if (out.isEmpty() && s.isNotEmpty()) out.add(s)
        return out
    }
}
