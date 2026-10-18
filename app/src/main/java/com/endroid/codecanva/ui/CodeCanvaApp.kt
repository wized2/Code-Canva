package com.endroid.codecanva.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.endroid.codecanva.editor.Language
import com.endroid.codecanva.editor.SyntaxHighlighter

@Composable
fun CodeCanvaApp(
    state: CanvaUiState,
    onSourceChange: (String) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onModeChange: (TypeMode) -> Unit,
    onRecordToggle: (Boolean) -> Unit,
    onThemeMode: (AppThemeMode) -> Unit,
    onVideoQuality: (VideoQuality) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onOpenPreview: () -> Unit,
    onBackTheater: () -> Unit,
    onBackPreview: () -> Unit,
    onStopRecording: () -> Unit,
    onFontSize: (Float) -> Unit,
) {
    when (state.screen) {
        AppScreen.SETUP -> SetupScreen(
            state = state,
            onSourceChange = onSourceChange,
            onLanguageChange = onLanguageChange,
            onSpeedChange = onSpeedChange,
            onModeChange = onModeChange,
            onRecordToggle = onRecordToggle,
            onThemeMode = onThemeMode,
            onVideoQuality = onVideoQuality,
            onStart = onStart,
            onOpenPreview = onOpenPreview,
            onReset = onReset,
            onFontSize = onFontSize,
        )
        AppScreen.THEATER -> {
            BackHandler { onBackTheater() }
            TheaterScreen(
                state = state,
                onPause = onPause,
                onResume = onStart,
                onOpenPreview = onOpenPreview,
                onBack = onBackTheater,
                onStopRecording = onStopRecording,
            )
        }
        AppScreen.PREVIEW -> {
            BackHandler { onBackPreview() }
            PreviewScreen(
                html = state.source,
                onBack = onBackPreview,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupScreen(
    state: CanvaUiState,
    onSourceChange: (String) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onModeChange: (TypeMode) -> Unit,
    onRecordToggle: (Boolean) -> Unit,
    onThemeMode: (AppThemeMode) -> Unit,
    onVideoQuality: (VideoQuality) -> Unit,
    onStart: () -> Unit,
    onOpenPreview: () -> Unit,
    onReset: () -> Unit,
    onFontSize: (Float) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Code Canva", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Configure, then Start for full-screen type-out",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surface),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("Theme", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppThemeMode.entries.forEach { m ->
                    FilterChip(
                        selected = state.themeMode == m,
                        onClick = { onThemeMode(m) },
                        label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Speed (${state.speedCps.toInt()} /s)", style = MaterialTheme.typography.labelMedium)
            Slider(value = state.speedCps, onValueChange = onSpeedChange, valueRange = 4f..90f)
            Text("Type mode", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeMode.entries.forEach { m ->
                    FilterChip(
                        selected = state.mode == m,
                        onClick = { onModeChange(m) },
                        label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Language highlight", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Language.XML, Language.KOTLIN, Language.JAVASCRIPT, Language.PYTHON, Language.JSON, Language.PLAIN).forEach { lang ->
                    FilterChip(
                        selected = state.language == lang,
                        onClick = { onLanguageChange(lang) },
                        label = { Text(lang.displayName) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Record session", modifier = Modifier.weight(1f))
                Switch(checked = state.recordEnabled, onCheckedChange = onRecordToggle)
            }
            if (state.recordEnabled) {
                Text("Video quality", style = MaterialTheme.typography.labelMedium)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoQuality.entries.forEach { q ->
                        FilterChip(
                            selected = state.videoQuality == q,
                            onClick = { onVideoQuality(q) },
                            label = { Text(q.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
                Text(
                    "Records this app only (immersive). A stop control is overlaid in the video.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("Font size", style = MaterialTheme.typography.labelMedium)
            Slider(value = state.fontSize, onValueChange = onFontSize, valueRange = 12f..24f)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.source,
                onValueChange = onSourceChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                label = { Text("Source (HTML / code)") },
            )
            if (!state.status.isNullOrBlank()) {
                Text(state.status!!, color = scheme.primary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, enabled = state.source.isNotBlank()) { Text("Start") }
                OutlinedButton(onClick = onOpenPreview, enabled = state.source.isNotBlank()) { Text("Preview") }
                OutlinedButton(onClick = onReset) { Text("Reset") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TheaterScreen(
    state: CanvaUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onOpenPreview: () -> Unit,
    onBack: () -> Unit,
    onStopRecording: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val text = if (state.visible.isNotEmpty()) state.visible else ""
    val isDark = scheme.background.luminance() < 0.5f
    val highlighted = remember(text, state.language, isDark) {
        SyntaxHighlighter.highlight(text, state.language, isDark)
    }
    val mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = state.fontSize.sp,
        color = scheme.onSurface,
        lineHeight = (state.fontSize * 1.45f).sp,
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Editor chrome (looks like IDE, not system bars)
            Surface(color = scheme.surface, tonalElevation = 2.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("main.${state.language.extensions.firstOrNull() ?: "txt"}", fontWeight = FontWeight.Medium)
                        Text(
                            when {
                                state.running -> "Typing…"
                                state.finished -> "Finished"
                                else -> "Paused"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    if (state.finished || state.visible.isNotEmpty()) {
                        IconButton(onClick = onOpenPreview) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Preview")
                        }
                    }
                }
            }
            if (state.running || state.progress > 0f) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp),
                )
            }
            // Read-only code area
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(scheme.surfaceVariant.copy(alpha = 0.25f)),
            ) {
                // Line numbers
                val lines = text.count { it == '\n' } + 1
                val gutter = remember(lines) {
                    (1..lines).joinToString("\n") { it.toString() }
                }
                Text(
                    gutter,
                    style = mono.copy(color = scheme.onSurfaceVariant.copy(alpha = 0.45f), fontSize = (state.fontSize * 0.9f).sp),
                    modifier = Modifier
                        .padding(start = 8.dp, top = 12.dp, end = 8.dp)
                        .verticalScroll(rememberScrollState()),
                )
                Text(
                    text = highlighted,
                    style = mono,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 12.dp, end = 12.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                )
            }
            // Bottom controls (minimal)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(scheme.surface)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (state.running) {
                    OutlinedButton(onClick = onPause) { Text("Pause") }
                } else if (!state.finished) {
                    Button(onClick = onResume) { Text("Resume") }
                }
                if (state.finished) {
                    Button(onClick = onOpenPreview) { Text("Preview") }
                    OutlinedButton(onClick = onBack) { Text("Done") }
                }
            }
        }

        // Recording stop overlay — appears in the video
        if (state.recording || state.recordEnabled) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 56.dp, end = 12.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xCCB71C1C),
                shadowElevation = 6.dp,
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                    )
                    Text("REC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    IconButton(
                        onClick = onStopRecording,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop recording", tint = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PreviewScreen(html: String, onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surface),
            )
        },
    ) { padding ->
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                }
            },
            update = { it.loadDataWithBaseURL(null, html, "text/html", "utf-8", null) },
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

private fun Color.luminance(): Float {
    fun c(v: Float): Float {
        val x = if (v <= 0.03928f) v / 12.92f else Math.pow(((v + 0.055) / 1.055).toDouble(), 2.4).toFloat()
        return x
    }
    return 0.2126f * c(red) + 0.7152f * c(green) + 0.0722f * c(blue)
}
