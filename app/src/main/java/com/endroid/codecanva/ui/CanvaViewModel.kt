package com.endroid.codecanva.ui

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
import kotlin.random.Random

enum class TypeMode { CHAR, WORD, LINE }

enum class AppScreen { SETUP, INTRO, THEATER, OUTRO, PREVIEW }

enum class AppThemeMode { SYSTEM, DARK, LIGHT, AMOLED }

enum class VideoQuality { LOW, MEDIUM, HIGH, ULTRA }

enum class CardAnim { FADE, SCALE, SLIDE_UP, TYPEWRITER }

enum class EditorFont {
    MONO,      // platform monospace
    SANS,      // default sans
    SERIF,     // serif
    ROUNDED,   // default sans, slightly larger tracking feel
}

data class CardConfig(
    val enabled: Boolean = true,
    val title: String = "",
    val subtitle: String = "",
    val durationSec: Float = 3f,
    val anim: CardAnim = CardAnim.SCALE,
)

data class CanvaUiState(
    val screen: AppScreen = AppScreen.SETUP,
    val setupTab: Int = 0,
    val source: String = DEFAULT_HTML,
    val visible: String = "",
    val language: Language = Language.HTML,
    val mode: TypeMode = TypeMode.CHAR,
    val speedCps: Float = 28f,
    val humanTyping: Boolean = true,
    val showCursor: Boolean = true,
    val soundEnabled: Boolean = false,
    val running: Boolean = false,
    val finished: Boolean = false,
    val recordEnabled: Boolean = false,
    val recording: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val seedColorArgb: Int = DefaultSeedColor.toArgb(),
    val videoQuality: VideoQuality = VideoQuality.HIGH,
    val fontSize: Float = 15f,
    val editorFont: EditorFont = EditorFont.MONO,
    val status: String? = null,
    val progress: Float = 0f,
    val intro: CardConfig = CardConfig(
        enabled = true,
        title = "Building this in 30 seconds",
        subtitle = "Watch me code it live",
        durationSec = 3f,
        anim = CardAnim.SCALE,
    ),
    val outro: CardConfig = CardConfig(
        enabled = true,
        title = "Like & Subscribe!",
        subtitle = "Full code link in bio",
        durationSec = 3.5f,
        anim = CardAnim.FADE,
    ),
    val libraryRefresh: Int = 0,
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
    var onKeyClick: (() -> Unit)? = null

    fun setSetupTab(tab: Int) = _ui.update { it.copy(setupTab = tab.coerceIn(0, 2)) }
    fun setSource(s: String) {
        if (_ui.value.running) return
        val lang = detectLanguage(s)
        _ui.update {
            it.copy(source = s, visible = "", finished = false, progress = 0f, language = lang, status = null)
        }
    }
    fun setLanguage(lang: Language) = _ui.update { it.copy(language = lang) }
    fun setSpeedCps(v: Float) = _ui.update { it.copy(speedCps = v.coerceIn(4f, 120f)) }
    fun setMode(mode: TypeMode) {
        if (_ui.value.running) return
        _ui.update { it.copy(mode = mode) }
    }
    fun setHumanTyping(v: Boolean) = _ui.update { it.copy(humanTyping = v) }
    fun setShowCursor(v: Boolean) = _ui.update { it.copy(showCursor = v) }
    fun setSoundEnabled(v: Boolean) = _ui.update { it.copy(soundEnabled = v) }
    fun setRecordEnabled(v: Boolean) = _ui.update { it.copy(recordEnabled = v) }
    fun setThemeMode(m: AppThemeMode) = _ui.update { it.copy(themeMode = m) }
    fun setSeedColor(argb: Int) = _ui.update { it.copy(seedColorArgb = argb) }
    fun setVideoQuality(q: VideoQuality) = _ui.update { it.copy(videoQuality = q) }
    fun setFontSize(v: Float) = _ui.update { it.copy(fontSize = v.coerceIn(11f, 28f)) }
    fun setEditorFont(f: EditorFont) = _ui.update { it.copy(editorFont = f) }
    fun setIntro(c: CardConfig) = _ui.update { it.copy(intro = c) }
    fun setOutro(c: CardConfig) = _ui.update { it.copy(outro = c) }
    fun bumpLibrary() = _ui.update { it.copy(libraryRefresh = it.libraryRefresh + 1) }

    fun onRecordingStarted() = _ui.update { it.copy(recording = true) }
    fun onRecordingDenied() = _ui.update {
        it.copy(recordEnabled = false, recording = false, status = "Recording permission denied")
    }
    fun onRecordingStopped() = _ui.update {
        it.copy(recording = false, libraryRefresh = it.libraryRefresh + 1)
    }

    fun beginSession() {
        val source = _ui.value.source
        if (source.isBlank()) {
            _ui.update { it.copy(status = "Paste or type some code first") }
            return
        }
        job?.cancel()
        val intro = _ui.value.intro
        if (intro.enabled && intro.title.isNotBlank()) {
            _ui.update {
                it.copy(screen = AppScreen.INTRO, running = false, finished = false, visible = "", progress = 0f, status = null)
            }
            job = viewModelScope.launch {
                delay((intro.durationSec * 1000).toLong().coerceIn(800L, 15_000L))
                if (isActive) startTypingInternal()
            }
        } else {
            startTypingInternal()
        }
    }

    private fun startTypingInternal() {
        job?.cancel()
        val source = _ui.value.source
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
            val base = (1000f / _ui.value.speedCps).toLong().coerceIn(10L, 400L)
            for ((index, unit) in units.withIndex()) {
                if (!isActive) break
                acc.append(unit)
                _ui.update {
                    it.copy(visible = acc.toString(), progress = (index + 1).toFloat() / total)
                }
                if (_ui.value.soundEnabled) onKeyClick?.invoke()
                val wait = if (_ui.value.humanTyping) humanDelay(unit, base)
                else if (_ui.value.mode == TypeMode.CHAR) base
                else base * unit.length.coerceAtLeast(1)
                delay(wait)
            }
            _ui.update {
                it.copy(running = false, finished = true, visible = source, progress = 1f)
            }
            // Outro runs only after leaving Preview (see leavePreview)
        }
    }

    fun openPreview() {
        if (_ui.value.source.isBlank()) return
        _ui.update { it.copy(screen = AppScreen.PREVIEW) }
    }

    fun backFromTheater() {
        job?.cancel()
        job = null
        _ui.update { it.copy(screen = AppScreen.SETUP, running = false) }
    }

    /** Leave preview → outro (if enabled) for N seconds, then setup. Caller stops rec after outro. */
    var onSessionFullyEnded: (() -> Unit)? = null

    fun leavePreview() {
        job?.cancel()
        val outro = _ui.value.outro
        if (outro.enabled && outro.title.isNotBlank()) {
            _ui.update { it.copy(screen = AppScreen.OUTRO) }
            job = viewModelScope.launch {
                delay((outro.durationSec * 1000).toLong().coerceIn(800L, 15_000L))
                if (!isActive) return@launch
                _ui.update { it.copy(screen = AppScreen.SETUP, running = false) }
                onSessionFullyEnded?.invoke()
            }
        } else {
            _ui.update { it.copy(screen = AppScreen.SETUP, running = false) }
            onSessionFullyEnded?.invoke()
        }
    }

    fun backFromPreview() = leavePreview()

    fun backFromCard() {
        // Intro has no buttons; kept for system back during intro only
        if (_ui.value.screen == AppScreen.INTRO) {
            job?.cancel()
            startTypingInternal()
            return
        }
        job?.cancel()
        job = null
        _ui.update { it.copy(screen = AppScreen.SETUP, running = false) }
        onSessionFullyEnded?.invoke()
    }

    fun skipCard() {
        // No UI skip buttons; system path only
        when (_ui.value.screen) {
            AppScreen.INTRO -> {
                job?.cancel()
                startTypingInternal()
            }
            AppScreen.OUTRO -> {
                job?.cancel()
                _ui.update { it.copy(screen = AppScreen.SETUP, running = false) }
                onSessionFullyEnded?.invoke()
            }
            else -> {}
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

    private fun humanDelay(unit: String, base: Long): Long {
        if (unit.isEmpty()) return base
        val c = unit.last()
        val jitter = Random.nextLong(-base / 3, base / 2 + 1)
        val core = when {
            c == '\n' -> base * 3 + Random.nextLong(40, 160)
            c in ".,;:!" -> base * 2 + Random.nextLong(20, 100)
            c == ' ' -> (base * 0.85).toLong()
            unit.length > 1 -> base * unit.length.coerceAtMost(8)
            else -> base
        }
        return (core + jitter).coerceAtLeast(12L)
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
