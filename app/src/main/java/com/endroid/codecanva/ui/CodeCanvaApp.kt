package com.endroid.codecanva.ui

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.endroid.codecanva.R
import com.endroid.codecanva.editor.Language
import com.endroid.codecanva.editor.SyntaxHighlighter
import com.endroid.codecanva.ui.theme.PresetSeedColors

@Composable
fun CodeCanvaApp(
    state: CanvaUiState,
    onSourceChange: (String) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onModeChange: (TypeMode) -> Unit,
    onRecordToggle: (Boolean) -> Unit,
    onThemeMode: (AppThemeMode) -> Unit,
    onSeedColor: (Int) -> Unit,
    onVideoQuality: (VideoQuality) -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onOpenPreview: () -> Unit,
    onBackTheater: () -> Unit,
    onBackPreview: () -> Unit,
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
            onSeedColor = onSeedColor,
            onVideoQuality = onVideoQuality,
            onStart = onStart,
            onOpenPreview = onOpenPreview,
            onReset = onReset,
            onFontSize = onFontSize,
        )
        AppScreen.THEATER -> {
            BackHandler { onBackTheater() }
            TheaterScreen(state = state, onBack = onBackTheater, onRun = onOpenPreview)
        }
        AppScreen.PREVIEW -> {
            BackHandler { onBackPreview() }
            PreviewScreen(html = state.source, onBack = onBackPreview)
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
    onSeedColor: (Int) -> Unit,
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
                            "Type-out · preview · optional record",
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
                .padding(horizontal = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SectionLabel("Appearance")
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppThemeMode.entries.forEach { m ->
                    FilterChip(
                        selected = state.themeMode == m,
                        onClick = { onThemeMode(m) },
                        label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            Text("Color seed", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PresetSeedColors.forEach { c ->
                    val selected = state.seedColorArgb == c.toArgbCompat()
                    Box(
                        Modifier
                            .size(if (selected) 36.dp else 30.dp)
                            .clip(CircleShape)
                            .background(c)
                            .then(
                                if (selected) Modifier.border(2.dp, scheme.onSurface, CircleShape)
                                else Modifier.border(1.dp, scheme.outlineVariant, CircleShape),
                            )
                            .clickable { onSeedColor(c.toArgbCompat()) },
                    )
                }
            }

            SectionLabel("Typing")
            Text("Speed  ${state.speedCps.toInt()}/s", style = MaterialTheme.typography.labelMedium)
            Slider(value = state.speedCps, onValueChange = onSpeedChange, valueRange = 8f..80f)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TypeMode.entries.forEach { m ->
                    FilterChip(
                        selected = state.mode == m,
                        onClick = { onModeChange(m) },
                        label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(Language.HTML, Language.KOTLIN, Language.JAVASCRIPT, Language.PYTHON, Language.JSON, Language.PLAIN).forEach { lang ->
                    FilterChip(
                        selected = state.language == lang,
                        onClick = { onLanguageChange(lang) },
                        label = { Text(lang.displayName) },
                    )
                }
            }
            Text("Font size", style = MaterialTheme.typography.labelMedium)
            Slider(value = state.fontSize, onValueChange = onFontSize, valueRange = 12f..22f)

            SectionLabel("Recording")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Record after Start")
                    Text(
                        "Starts when you press Start. Stops when you leave the editor (Back).",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.recordEnabled, onCheckedChange = onRecordToggle)
            }
            if (state.recordEnabled) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoQuality.entries.forEach { q ->
                        FilterChip(
                            selected = state.videoQuality == q,
                            onClick = { onVideoQuality(q) },
                            label = { Text(q.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        )
                    }
                }
            }

            SectionLabel("Source")
            OutlinedTextField(
                value = state.source,
                onValueChange = onSourceChange,
                modifier = Modifier.fillMaxWidth().height(200.dp),
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                shape = RoundedCornerShape(16.dp),
            )
            state.status?.let {
                Text(it, color = scheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStart,
                    enabled = state.source.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("Start") }
                OutlinedButton(onClick = onOpenPreview, enabled = state.source.isNotBlank(), shape = RoundedCornerShape(14.dp)) {
                    Text("Preview")
                }
                OutlinedButton(onClick = onReset, shape = RoundedCornerShape(14.dp)) { Text("Reset") }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun TheaterScreen(
    state: CanvaUiState,
    onBack: () -> Unit,
    onRun: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val text = state.visible
    val isDark = scheme.background.luminance() < 0.5f
    val highlighted = remember(text, state.language, isDark) {
        SyntaxHighlighter.highlight(text, state.language, isDark)
    }
    val mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = state.fontSize.sp,
        color = scheme.onSurface,
        lineHeight = (state.fontSize * 1.5f).sp,
    )
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val gutterScroll = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    var viewportWidthPx by remember { mutableIntStateOf(0) }
    var codeAreaHeightPx by remember { mutableIntStateOf(0) }

    /*
     * Scroll the *current line* into view by measured pixel width vs viewport.
     * Independent of whether an earlier line was longer (that was the 1.2.2 bug).
     * Newline → smooth reset to x=0.
     */
    LaunchedEffect(text, viewportWidthPx, state.fontSize, hScroll.maxValue) {
        // Vertical follow latest line
        vScroll.animateScrollTo(vScroll.maxValue)
        gutterScroll.scrollTo(vScroll.value)

        if (text.isEmpty() || viewportWidthPx <= 0) {
            hScroll.animateScrollTo(0)
            return@LaunchedEffect
        }
        if (text.endsWith("\n")) {
            hScroll.animateScrollTo(0)
            return@LaunchedEffect
        }

        val currentLine = text.substringAfterLast('\n')
        val lineWidth = textMeasurer.measure(
            text = currentLine,
            style = mono,
            softWrap = false,
            maxLines = 1,
        ).size.width

        // Keep ~48px padding past the caret end so the last chars aren't glued to the edge
        val target = (lineWidth - viewportWidthPx + 48).coerceAtLeast(0)

        // Layout may lag one frame behind text growth — wait until maxValue can reach target
        var frames = 0
        while (hScroll.maxValue < target && frames < 8) {
            delay(8)
            frames++
        }
        val dest = target.coerceAtMost(hScroll.maxValue.coerceAtLeast(0))
        hScroll.animateScrollTo(dest)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding(),
    ) {
        Surface(color = scheme.surface, tonalElevation = 1.dp) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back),
                        contentDescription = "Back",
                        tint = scheme.onSurface,
                    )
                }
                Text(
                    when (state.language) {
                        Language.HTML -> "index.html"
                        Language.KOTLIN -> "Main.kt"
                        Language.JAVASCRIPT -> "main.js"
                        Language.PYTHON -> "main.py"
                        Language.JSON -> "data.json"
                        else -> "file.txt"
                    },
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onRun) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play),
                        contentDescription = "Run preview",
                        tint = scheme.primary,
                    )
                }
            }
        }
        if (state.running && state.progress < 1f) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
            )
        }
        Row(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(scheme.surfaceVariant.copy(alpha = 0.2f)),
        ) {
            val lines = if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
            val gutter = remember(lines) { (1..lines).joinToString("\n") { it.toString() } }
            Text(
                gutter,
                style = mono.copy(
                    color = scheme.onSurfaceVariant.copy(alpha = 0.4f),
                    fontSize = (state.fontSize * 0.85f).sp,
                ),
                modifier = Modifier
                    .padding(start = 10.dp, top = 14.dp, end = 8.dp)
                    .verticalScroll(gutterScroll, enabled = false),
            )
            // Viewport Box reports visible width; Text content may be wider (no wrap)
            Box(
                Modifier
                    .weight(1f)
                    .padding(top = 14.dp, end = 14.dp, bottom = 14.dp)
                    .onSizeChanged {
                        viewportWidthPx = it.width
                        codeAreaHeightPx = it.height
                    },
            ) {
                Text(
                    text = highlighted,
                    style = mono,
                    softWrap = false,
                    maxLines = Int.MAX_VALUE,
                    modifier = Modifier
                        .horizontalScroll(hScroll)
                        .verticalScroll(vScroll),
                )
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
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = "Back",
                            tint = scheme.onSurface,
                        )
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
        return if (v <= 0.03928f) v / 12.92f
        else Math.pow(((v + 0.055) / 1.055).toDouble(), 2.4).toFloat()
    }
    return 0.2126f * c(red) + 0.7152f * c(green) + 0.0722f * c(blue)
}

private fun Color.toArgbCompat(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
    )
