package com.endroid.codecanva.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.endroid.codecanva.editor.Language
import com.endroid.codecanva.ui.theme.DefaultSeedColor
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

enum class VideoQuality { LOW, MEDIUM, HIGH, ULTRA }

data class CanvaUiState(
    val screen: AppScreen = AppScreen.SETUP,
    val source: String = DEFAULT_HTML,
    val visible: String = "",
    val language: Language = Language.HTML,
    val mode: TypeMode = TypeMode.CHAR,
    val speedCps: Float = 32f,
    val running: Boolean = false,
    val finished: Boolean = false,
    val recordEnabled: Boolean = false,
    val recording: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val seedColorArgb: Int = DefaultSeedColor.toArgb(),
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
        val lang = detectLanguage(s)
        _ui.update {
            it.copy(
                source = s,
                visible = "",
                finished = false,
                progress = 0f,
                language = lang,
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
    fun setSeedColor(argb: Int) = _ui.update { it.copy(seedColorArgb = argb) }
    fun setVideoQuality(q: VideoQuality) = _ui.update { it.copy(videoQuality = q) }
    fun setFontSize(v: Float) = _ui.update { it.copy(fontSize = v.coerceIn(11f, 28f)) }

    fun onRecordingStarted() = _ui.update { it.copy(recording = true) }
    fun onRecordingDenied() = _ui.update {
        it.copy(recordEnabled = false, recording = false, status = "Recording permission denied")
    }
    fun onRecordingStopped() = _ui.update { it.copy(recording = false) }

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
                status = null,
            )
        }
        job = viewModelScope.launch {
            val units = when (_ui.value.mode) {
                TypeMode.CHAR -> source.map { it.toString() }
                TypeMode.WORD -> tokenizeWords(source)
                TypeMode.LINE -> {
                    val lines = source.split("\n")
                    lines.mapIndexed { i, line -> if (i < lines.lastIndex) line + "\n" else line }
                }
            }
            val total = units.size.coerceAtLeast(1)
            val acc = StringBuilder()
            for ((index, unit) in units.withIndex()) {
                if (!isActive) break
                val d = (1000f / _ui.value.speedCps).toLong().coerceIn(8L, 400L)
                acc.append(unit)
                _ui.update {
                    it.copy(visible = acc.toString(), progress = (index + 1).toFloat() / total)
                }
                delay(if (_ui.value.mode == TypeMode.CHAR) d else d * unit.length.coerceAtLeast(1))
            }
            _ui.update {
                it.copy(running = false, finished = true, visible = source, progress = 1f)
            }
        }
    }

    fun openPreview() {
        if (_ui.value.source.isBlank()) return
        _ui.update { it.copy(screen = AppScreen.PREVIEW) }
    }

    /** Theater back: stop type-out, leave theater (caller stops recording). */
    fun backFromTheater() {
        job?.cancel()
        job = null
        _ui.update {
            it.copy(screen = AppScreen.SETUP, running = false)
        }
    }

    fun backFromPreview() {
        _ui.update {
            it.copy(
                screen = if (it.visible.isNotEmpty() || it.finished) AppScreen.THEATER else AppScreen.SETUP,
            )
        }
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

    private fun detectLanguage(s: String): Language {
        val t = s.trimStart()
        return when {
            t.startsWith("<!DOCTYPE", true) || t.startsWith("<html", true) ||
                (t.startsWith("<") && (s.contains("<html", true) || s.contains("<style", true) || s.contains("<script", true))) ->
                Language.HTML
            t.startsWith("{") || t.startsWith("[") -> Language.JSON
            s.contains("fun ") && s.contains("(") -> Language.KOTLIN
            s.contains("def ") -> Language.PYTHON
            s.contains("function ") || s.contains("const ") || s.contains("=>") -> Language.JAVASCRIPT
            else -> _ui.value.language
        }
    }

    private fun tokenizeWords(s: String): List<String> {
        val out = ArrayList<String>()
        Regex("""\S+\s*|\s+""").findAll(s).forEach { out.add(it.value) }
        if (out.isEmpty() && s.isNotEmpty()) out.add(s)
        return out
    }
}
