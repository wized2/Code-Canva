package com.endroid.codecanva.audio

import android.media.AudioManager
import android.media.ToneGenerator
import kotlin.concurrent.thread

/** Lightweight key-click using system tones (no asset files). */
class TypingSoundPlayer {
    @Volatile private var enabled = false
    private var tone: ToneGenerator? = null

    fun setEnabled(on: Boolean) {
        enabled = on
        if (on && tone == null) {
            try {
                tone = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
            } catch (_: Exception) {
                tone = null
            }
        }
        if (!on) {
            try { tone?.release() } catch (_: Exception) {}
            tone = null
        }
    }

    fun click() {
        if (!enabled) return
        val t = tone ?: return
        thread(name = "key-click", isDaemon = true) {
            try {
                t.startTone(ToneGenerator.TONE_PROP_BEEP, 18)
            } catch (_: Exception) {}
        }
    }

    fun release() {
        enabled = false
        try { tone?.release() } catch (_: Exception) {}
        tone = null
    }
}
