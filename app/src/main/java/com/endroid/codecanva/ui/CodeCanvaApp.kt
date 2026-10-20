package com.endroid.codecanva.ui

import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.endroid.codecanva.R
import com.endroid.codecanva.editor.Language
import com.endroid.codecanva.editor.SyntaxHighlighter
import com.endroid.codecanva.ui.theme.PresetSeedColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CodeCanvaApp(
    state: CanvaUiState,
    onSourceChange: (String) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onModeChange: (TypeMode) -> Unit,
    onHumanTyping: (Boolean) -> Unit,
    onShowCursor: (Boolean) -> Unit,
    onSoundEnabled: (Boolean) -> Unit,
    onRecordToggle: (Boolean) -> Unit,
    onThemeMode: (AppThemeMode) -> Unit,
    onSeedColor: (Int) -> Unit,
    onVideoQuality: (VideoQuality) -> Unit,
    onIntro: (CardConfig) -> Unit,
    onOutro: (CardConfig) -> Unit,
    onSetupTab: (Int) -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onOpenPreview: () -> Unit,
    onBackTheater: () -> Unit,
    onBackPreview: () -> Unit,
    onBackCard: () -> Unit,
    onSkipCard: () -> Unit,
    onFontSize: (Float) -> Unit,
    onLibraryChanged: () -> Unit,
) {
    when (state.screen) {
        AppScreen.SETUP -> SetupScreen(
            state = state,
            onSourceChange = onSourceChange,
            onLanguageChange = onLanguageChange,
            onSpeedChange = onSpeedChange,
            onModeChange = onModeChange,
            onHumanTyping = onHumanTyping,
            onShowCursor = onShowCursor,
            onSoundEnabled = onSoundEnabled,
            onRecordToggle = onRecordToggle,
            onThemeMode = onThemeMode,
            onSeedColor = onSeedColor,
            onVideoQuality = onVideoQuality,
            onIntro = onIntro,
            onOutro = onOutro,
            onSetupTab = onSetupTab,
            onStart = onStart,
            onOpenPreview = onOpenPreview,
            onReset = onReset,
            onFontSize = onFontSize,
            onLibraryChanged = onLibraryChanged,
        )
        AppScreen.INTRO -> {
            BackHandler { onBackCard() }
            AnimatedCardScreen(config = state.intro, label = "Intro", onBack = onBackCard, onSkip = onSkipCard)
        }
        AppScreen.OUTRO -> {
            BackHandler { onBackCard() }
            AnimatedCardScreen(config = state.outro, label = "Outro", onBack = onBackCard, onSkip = onSkipCard)
        }
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
    onHumanTyping: (Boolean) -> Unit,
    onShowCursor: (Boolean) -> Unit,
    onSoundEnabled: (Boolean) -> Unit,
    onRecordToggle: (Boolean) -> Unit,
    onThemeMode: (AppThemeMode) -> Unit,
    onSeedColor: (Int) -> Unit,
    onVideoQuality: (VideoQuality) -> Unit,
    onIntro: (CardConfig) -> Unit,
    onOutro: (CardConfig) -> Unit,
    onSetupTab: (Int) -> Unit,
    onStart: () -> Unit,
    onOpenPreview: () -> Unit,
    onReset: () -> Unit,
    onFontSize: (Float) -> Unit,
    onLibraryChanged: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val tabs = listOf("Script", "Cards", "Library")
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Code Canva", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Shorts-ready type-out studio",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surface),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = state.setupTab) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = state.setupTab == i,
                        onClick = { onSetupTab(i) },
                        text = { Text(title) },
                    )
                }
            }
            when (state.setupTab) {
                0 -> ScriptTab(
                    state, onSourceChange, onLanguageChange, onSpeedChange, onModeChange,
                    onHumanTyping, onShowCursor, onSoundEnabled, onRecordToggle, onThemeMode,
                    onSeedColor, onVideoQuality, onStart, onOpenPreview, onReset, onFontSize,
                )
                1 -> CardsTab(state, onIntro, onOutro)
                else -> LibraryTab(state.libraryRefresh, onLibraryChanged)
            }
        }
    }
}

@Composable
private fun ScriptTab(
    state: CanvaUiState,
    onSourceChange: (String) -> Unit,
    onLanguageChange: (Language) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onModeChange: (TypeMode) -> Unit,
    onHumanTyping: (Boolean) -> Unit,
    onShowCursor: (Boolean) -> Unit,
    onSoundEnabled: (Boolean) -> Unit,
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
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SectionLabel("Appearance")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppThemeMode.entries.forEach { m ->
                FilterChip(selected = state.themeMode == m, onClick = { onThemeMode(m) }, label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) })
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PresetSeedColors.forEach { c ->
                val selected = state.seedColorArgb == c.toArgbCompat()
                Box(
                    Modifier
                        .size(if (selected) 34.dp else 28.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(if (selected) 2.dp else 1.dp, if (selected) scheme.onSurface else scheme.outlineVariant, CircleShape)
                        .clickable { onSeedColor(c.toArgbCompat()) },
                )
            }
        }

        SectionLabel("Typing feel")
        Text("Speed  ${state.speedCps.toInt()}/s", style = MaterialTheme.typography.labelMedium)
        Slider(value = state.speedCps, onValueChange = onSpeedChange, valueRange = 8f..80f)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeMode.entries.forEach { m ->
                FilterChip(selected = state.mode == m, onClick = { onModeChange(m) }, label = { Text(m.name.lowercase().replaceFirstChar { it.uppercase() }) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Human typing")
                Text("Jitter + pauses on punctuation", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Switch(checked = state.humanTyping, onCheckedChange = onHumanTyping)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Blinking cursor", modifier = Modifier.weight(1f))
            Switch(checked = state.showCursor, onCheckedChange = onShowCursor)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Typing sounds")
                Text("Soft clicks while typing", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Switch(checked = state.soundEnabled, onCheckedChange = onSoundEnabled)
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(Language.HTML, Language.KOTLIN, Language.JAVASCRIPT, Language.PYTHON, Language.JSON, Language.PLAIN).forEach { lang ->
                FilterChip(selected = state.language == lang, onClick = { onLanguageChange(lang) }, label = { Text(lang.displayName) })
            }
        }
        Text("Font size", style = MaterialTheme.typography.labelMedium)
        Slider(value = state.fontSize, onValueChange = onFontSize, valueRange = 12f..22f)

        SectionLabel("Recording")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Record after Start")
                Text("Captures intro → code → outro", style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
            Switch(checked = state.recordEnabled, onCheckedChange = onRecordToggle)
        }
        if (state.recordEnabled) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VideoQuality.entries.forEach { q ->
                    FilterChip(selected = state.videoQuality == q, onClick = { onVideoQuality(q) }, label = { Text(q.name.lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }
        }

        SectionLabel("Source")
        OutlinedTextField(
            value = state.source,
            onValueChange = onSourceChange,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp),
            shape = RoundedCornerShape(16.dp),
        )
        state.status?.let { Text(it, color = scheme.error, style = MaterialTheme.typography.bodySmall) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onStart, enabled = state.source.isNotBlank(), shape = RoundedCornerShape(14.dp)) { Text("Start") }
            OutlinedButton(onClick = onOpenPreview, enabled = state.source.isNotBlank(), shape = RoundedCornerShape(14.dp)) { Text("Preview") }
            OutlinedButton(onClick = onReset, shape = RoundedCornerShape(14.dp)) { Text("Reset") }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun CardsTab(state: CanvaUiState, onIntro: (CardConfig) -> Unit, onOutro: (CardConfig) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Start & end cards for YouTube Shorts — shown while recording.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CardEditor(title = "Start screen (intro)", config = state.intro, onChange = onIntro)
        CardEditor(title = "End screen (outro)", config = state.outro, onChange = onOutro)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CardEditor(title: String, config: CardConfig, onChange: (CardConfig) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(checked = config.enabled, onCheckedChange = { onChange(config.copy(enabled = it)) })
            }
            OutlinedTextField(
                value = config.title,
                onValueChange = { onChange(config.copy(title = it)) },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = config.subtitle,
                onValueChange = { onChange(config.copy(subtitle = it)) },
                label = { Text("Subtitle") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Text("Duration ${"%.1f".format(config.durationSec)}s", style = MaterialTheme.typography.labelMedium)
            Slider(
                value = config.durationSec,
                onValueChange = { onChange(config.copy(durationSec = it)) },
                valueRange = 1.5f..8f,
            )
            Text("Animation", style = MaterialTheme.typography.labelMedium)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardAnim.entries.forEach { a ->
                    FilterChip(
                        selected = config.anim == a,
                        onClick = { onChange(config.copy(anim = a)) },
                        label = { Text(a.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) },
                    )
                }
            }
        }
    }
}

data class LibraryItem(val id: Long, val name: String, val uri: Uri, val dateMs: Long, val sizeBytes: Long)

@Composable
private fun LibraryTab(refreshKey: Int, onLibraryChanged: () -> Unit) {
    val context = LocalContext.current
    var items by remember(refreshKey) { mutableStateOf(loadLibrary(context)) }
    LaunchedEffect(refreshKey) { items = loadLibrary(context) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Your recordings. Open in the system editor to trim/cut, or share to YouTube.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (items.isEmpty()) {
            Text("No videos yet — enable Record and press Start.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items.forEach { item ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.name, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        formatMeta(item),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { openVideo(context, item.uri) }) { Text("Open") }
                        OutlinedButton(onClick = { shareVideo(context, item.uri) }) { Text("Share") }
                        OutlinedButton(onClick = {
                            try {
                                context.contentResolver.delete(item.uri, null, null)
                            } catch (_: Exception) {
                            }
                            // also try app files dir
                            try {
                                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "CodeCanva")
                                dir.listFiles()?.filter { it.name == item.name }?.forEach { it.delete() }
                            } catch (_: Exception) {
                            }
                            onLibraryChanged()
                            items = loadLibrary(context)
                        }) { Text("Delete") }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun loadLibrary(context: Context): List<LibraryItem> {
    val out = ArrayList<LibraryItem>()
    // App-private movies
    try {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "CodeCanva")
        dir.listFiles()?.filter { it.extension.equals("mp4", true) }?.sortedByDescending { it.lastModified() }?.forEach { f ->
            val uri = FileProvider.getUriForFile(context, context.packageName + ".files", f)
            out.add(LibraryItem(-f.lastModified(), f.name, uri, f.lastModified(), f.length()))
        }
    } catch (_: Exception) {
    }
    // MediaStore Movies/CodeCanva
    try {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.RELATIVE_PATH,
        )
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Video.Media.RELATIVE_PATH} LIKE ?",
            arrayOf("%CodeCanva%"),
            "${MediaStore.Video.Media.DATE_ADDED} DESC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol) ?: "video.mp4"
                if (out.any { it.name == name }) continue
                out.add(
                    LibraryItem(
                        id = id,
                        name = name,
                        uri = ContentUris.withAppendedId(collection, id),
                        dateMs = c.getLong(dateCol) * 1000L,
                        sizeBytes = c.getLong(sizeCol),
                    ),
                )
            }
        }
    } catch (_: Exception) {
    }
    return out.sortedByDescending { it.dateMs }
}

private fun formatMeta(item: LibraryItem): String {
    val df = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    val sizeMb = item.sizeBytes / (1024.0 * 1024.0)
    return "${df.format(Date(item.dateMs))} · ${"%.1f".format(sizeMb)} MB"
}

private fun openVideo(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Open video"))
    } catch (_: Exception) {
    }
}

private fun shareVideo(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share video"))
    } catch (_: Exception) {
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
private fun AnimatedCardScreen(
    config: CardConfig,
    label: String,
    onBack: () -> Unit,
    onSkip: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(config) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween((config.durationSec * 1000).toInt(), easing = FastOutSlowInEasing))
    }
    val enter by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "enter",
    )
    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse",
    )

    val scale = when (config.anim) {
        CardAnim.SCALE -> 0.85f + 0.15f * enter
        CardAnim.FADE -> 1f
        CardAnim.SLIDE_UP -> 1f
        CardAnim.TYPEWRITER -> 1f
    }
    val alpha = when (config.anim) {
        CardAnim.FADE, CardAnim.TYPEWRITER -> enter
        else -> 0.4f + 0.6f * enter
    }
    val translateY = when (config.anim) {
        CardAnim.SLIDE_UP -> (1f - enter) * 80f
        else -> 0f
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding(),
    ) {
        // subtle progress
        LinearProgressIndicator(
            progress = { progress.value },
            modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(28.dp)
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale * if (config.anim == CardAnim.SCALE) pulse else 1f
                    scaleY = scale * if (config.anim == CardAnim.SCALE) pulse else 1f
                    translationY = translateY
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.primary)
            Spacer(Modifier.height(16.dp))
            Text(
                text = if (config.anim == CardAnim.TYPEWRITER) {
                    val n = (config.title.length * enter).toInt().coerceIn(0, config.title.length)
                    config.title.take(n)
                } else config.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = scheme.onBackground,
            )
            if (config.subtitle.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    config.subtitle,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(onClick = onSkip) { Text("Skip") }
        }
    }
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
    val mono = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = state.fontSize.sp,
        color = scheme.onSurface,
        lineHeight = (state.fontSize * 1.5f).sp,
    )
    val vScroll = rememberScrollState()
    val activeHScroll = rememberScrollState()

    val splitLines = remember(text) { text.split("\n") }
    val activeIndex = splitLines.lastIndex
    val activeLine = splitLines.getOrElse(activeIndex) { "" }

    val blink = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by blink.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(530), RepeatMode.Reverse),
        label = "cursorA",
    )

    LaunchedEffect(text, vScroll.maxValue) {
        vScroll.scrollTo(vScroll.maxValue)
    }
    LaunchedEffect(activeLine, activeHScroll.maxValue) {
        if (activeLine.isEmpty()) activeHScroll.scrollTo(0)
        else activeHScroll.scrollTo(activeHScroll.maxValue)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(scheme.background)
            .statusBarsPadding(),
    ) {
        Surface(color = scheme.surface, tonalElevation = 1.dp) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_back), contentDescription = "Back", tint = scheme.onSurface)
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
                    Icon(painterResource(R.drawable.ic_play), contentDescription = "Run preview", tint = scheme.primary)
                }
            }
        }
        if (state.running && state.progress < 1f) {
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(2.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(scheme.surfaceVariant.copy(alpha = 0.2f))
                .padding(top = 14.dp, end = 14.dp, bottom = 14.dp)
                .verticalScroll(vScroll),
        ) {
            splitLines.forEachIndexed { index, line ->
                val lineHighlight = remember(line, state.language, isDark) {
                    SyntaxHighlighter.highlight(line, state.language, isDark)
                }
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        text = "${index + 1}",
                        style = mono.copy(
                            color = scheme.onSurfaceVariant.copy(alpha = 0.4f),
                            fontSize = (state.fontSize * 0.85f).sp,
                        ),
                        maxLines = 1,
                        modifier = Modifier.padding(start = 10.dp, end = 8.dp).width(28.dp),
                    )
                    if (index == activeIndex) {
                        Row(Modifier.weight(1f).horizontalScroll(activeHScroll), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = lineHighlight, style = mono, softWrap = false, maxLines = 1)
                            if (state.showCursor && (state.running || !state.finished)) {
                                Box(
                                    Modifier
                                        .padding(start = 1.dp)
                                        .width(2.dp)
                                        .height((state.fontSize * 1.2f).dp)
                                        .alpha(if (state.running) cursorAlpha else 1f)
                                        .background(scheme.primary),
                                )
                            }
                        }
                    } else {
                        Text(
                            text = lineHighlight,
                            style = mono,
                            softWrap = false,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f),
                        )
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
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back", tint = scheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = scheme.surface),
            )
        },
    ) { padding ->
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
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
    fun c(v: Float): Float =
        if (v <= 0.03928f) v / 12.92f
        else Math.pow(((v + 0.055) / 1.055).toDouble(), 2.4).toFloat()
    return 0.2126f * c(red) + 0.7152f * c(green) + 0.0722f * c(blue)
}

private fun Color.toArgbCompat(): Int =
    android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt(),
    )
