package com.endroid.codecanva.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.endroid.codecanva.editor.Language
import com.endroid.codecanva.editor.SyntaxHighlighter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeCanvaApp(
    state: CanvaUiState,
    onSourceChange: (String) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onModeChange: (TypeMode) -> Unit,
    onRecordToggle: (Boolean) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onShowPreview: (Boolean) -> Unit,
    onFontSize: (Float) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val displayText = if (state.running || state.finished || state.visible.isNotEmpty()) state.visible else state.source
    val highlighted = remember(displayText, state.language) {
        SyntaxHighlighter.highlight(displayText, state.language, isDark = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Code Canva", fontWeight = FontWeight.SemiBold)
                        Text(
                            state.status ?: "Type-out · preview · record",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surface),
                actions = {
                    if (state.finished || state.visible.isNotEmpty()) {
                        IconButton(onClick = { onShowPreview(true) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Preview")
                        }
                    }
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
        ) {
            // Settings row
            Text("Speed (${state.speedCps.toInt()} units/s)", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = state.speedCps,
                onValueChange = onSpeedChange,
                valueRange = 4f..90f,
                enabled = !state.running,
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TypeMode.entries.forEach { m ->
                    FilterChip(
                        selected = state.mode == m,
                        onClick = { onModeChange(m) },
                        enabled = !state.running,
                        label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("Record", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = state.recordEnabled,
                    onCheckedChange = onRecordToggle,
                    enabled = !state.running,
                )
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(Language.XML, Language.KOTLIN, Language.JAVASCRIPT, Language.PYTHON, Language.JSON, Language.PLAIN).forEach { lang ->
                    FilterChip(
                        selected = state.language == lang,
                        onClick = { onLanguageChange(lang) },
                        label = { Text(lang.displayName) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onStart, enabled = !state.running && state.source.isNotBlank()) {
                    Text(if (state.visible.isEmpty()) "Start" else "Restart")
                }
                OutlinedButton(onClick = onPause, enabled = state.running) { Text("Pause") }
                OutlinedButton(
                    onClick = { onShowPreview(true) },
                    enabled = state.source.isNotBlank(),
                ) { Text("Run preview") }
            }
            if (state.running || state.progress > 0f) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                )
            }
            Spacer(Modifier.height(10.dp))

            // Editor surface
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(12.dp),
            ) {
                val scroll = rememberScrollState()
                val mono = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = state.fontSize.sp,
                    color = scheme.onSurface,
                    lineHeight = (state.fontSize * 1.45f).sp,
                )
                if (state.running || state.finished || state.visible.isNotEmpty()) {
                    Text(
                        text = highlighted,
                        style = mono,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .horizontalScroll(rememberScrollState()),
                    )
                } else {
                    OutlinedTextField(
                        value = state.source,
                        onValueChange = onSourceChange,
                        textStyle = mono,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = {
                            Text(
                                "Paste HTML, JS, Kotlin, Python…",
                                style = mono.copy(color = scheme.onSurfaceVariant.copy(alpha = 0.5f)),
                            )
                        },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Font",
                style = MaterialTheme.typography.labelMedium,
            )
            Slider(value = state.fontSize, onValueChange = onFontSize, valueRange = 12f..24f)
        }
    }

    AnimatedVisibility(visible = state.showPreview) {
        PreviewSheet(
            html = state.source,
            onClose = { onShowPreview(false) },
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PreviewSheet(html: String, onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.scrim.copy(alpha = 0.5f)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.surface),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Preview", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
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
                update = { wv ->
                    wv.loadDataWithBaseURL(null, html, "text/html", "utf-8", null)
                },
                modifier = Modifier.fillMaxSize().weight(1f),
            )
        }
    }
}
