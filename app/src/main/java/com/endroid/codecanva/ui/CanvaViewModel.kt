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

data class CanvaUiState(
    val source: String = DEFAULT_HTML,
    val visible: String = "",
    val language: Language = Language.XML,
    val mode: TypeMode = TypeMode.CHAR,
    val speedCps: Float = 28f,
    val running: Boolean = false,
    val finished: Boolean = false,
    val recordEnabled: Boolean = false,
    val recording: Boolean = false,
    val showPreview: Boolean = false,
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
  <script>
    console.log("Code Canva preview ready");
  </script>
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

    fun setLanguage(lang: Language) {
        _ui.update { it.copy(language = lang) }
    }

    fun setSpeedCps(v: Float) {
        _ui.update { it.copy(speedCps = v.coerceIn(4f, 120f)) }
    }

    fun setMode(mode: TypeMode) {
        if (_ui.value.running) return
        _ui.update { it.copy(mode = mode) }
    }

    fun setRecordEnabled(v: Boolean) {
        _ui.update { it.copy(recordEnabled = v) }
    }

    fun setShowPreview(v: Boolean) {
        _ui.update { it.copy(showPreview = v) }
    }

    fun setFontSize(v: Float) {
        _ui.update { it.copy(fontSize = v.coerceIn(11f, 28f)) }
    }

    fun onRecordingStarted() {
        _ui.update { it.copy(recording = true, status = "Recording…") }
    }

    fun onRecordingDenied() {
        _ui.update { it.copy(recordEnabled = false, recording = false, status = "Recording permission denied") }
    }

    fun onRecordingStopped() {
        _ui.update {
            it.copy(
                recording = false,
                status = if (it.recordEnabled) "Recording saved to Movies/CodeCanva" else it.status,
            )
        }
    }

    fun startTyping() {
        if (_ui.value.running) return
        val source = _ui.value.source
        if (source.isEmpty()) {
            _ui.update { it.copy(status = "Paste or type some code first") }
            return
        }
        job?.cancel()
        _ui.update {
            it.copy(running = true, finished = false, visible = "", progress = 0f, status = "Typing…", showPreview = false)
        }
        job = viewModelScope.launch {
            val units = when (_ui.value.mode) {
                TypeMode.CHAR -> source.map { it.toString() }
                TypeMode.WORD -> tokenizeWords(source)
                TypeMode.LINE -> source.split("\n").mapIndexed { i, line ->
                    if (i < source.split("\n").lastIndex) line + "\n" else line
                }
            }
            val total = units.size.coerceAtLeast(1)
            val delayMs = (1000f / _ui.value.speedCps).toLong().coerceIn(8L, 400L)
            val acc = StringBuilder()
            for ((index, unit) in units.withIndex()) {
                if (!isActive) break
                // While running, re-read speed for live adjust
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
                    status = "Done",
                )
            }
        }
    }

    fun pauseTyping() {
        job?.cancel()
        job = null
        _ui.update { it.copy(running = false, status = "Paused") }
    }

    fun reset() {
        job?.cancel()
        job = null
        _ui.update {
            it.copy(
                visible = "",
                running = false,
                finished = false,
                progress = 0f,
                status = null,
                showPreview = false,
                recording = false,
            )
        }
    }

    private fun tokenizeWords(s: String): List<String> {
        val out = ArrayList<String>()
        val re = Regex("""\S+\s*|\s+""")
        re.findAll(s).forEach { out.add(it.value) }
        if (out.isEmpty() && s.isNotEmpty()) out.add(s)
        return out
    }
}
