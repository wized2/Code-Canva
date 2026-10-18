package com.endroid.codecanva.record

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.endroid.codecanva.R
import java.io.File

/**
 * Records the display via MediaProjection.
 * Uses VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY when available so only this app's
 * windows are mirrored (not other apps). System bars are hidden by the activity immersive mode.
 */
class RecordingService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val code = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val data = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(EXTRA_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_DATA)
                }
                val quality = intent.getStringExtra(EXTRA_QUALITY) ?: "HIGH"
                if (data != null) startRecording(code, data, quality)
            }
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording(resultCode: Int, data: Intent, quality: String) {
        createChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setContentTitle("Code Canva")
            .setContentText("Recording typing session…")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(42, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(42, notification)
        }

        val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = mpm.getMediaProjection(resultCode, data)

        val metrics = DisplayMetrics()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        var width = metrics.widthPixels
        var height = metrics.heightPixels
        // Cap resolution by quality
        val maxW = when (quality) {
            "LOW" -> 720
            "MEDIUM" -> 1080
            "HIGH" -> 1440
            else -> 1920
        }
        if (width > maxW) {
            val scale = maxW.toFloat() / width
            width = maxW
            height = (height * scale).toInt().let { if (it % 2 == 1) it - 1 else it }
        }
        if (width % 2 == 1) width -= 1
        if (height % 2 == 1) height -= 1
        val density = metrics.densityDpi
        val bitrate = when (quality) {
            "LOW" -> 2_000_000
            "MEDIUM" -> 4_000_000
            "HIGH" -> 8_000_000
            else -> 12_000_000
        }

        val dir = File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "CodeCanva").apply { mkdirs() }
        outputFile = File(dir, "canva_${System.currentTimeMillis()}.mp4")

        val rec = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        rec.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        rec.setVideoEncodingBitRate(bitrate)
        rec.setVideoFrameRate(30)
        rec.setVideoSize(width, height)
        rec.setOutputFile(outputFile!!.absolutePath)
        rec.prepare()
        recorder = rec

        // Prefer own-content-only so other apps are not captured when the OS supports it.
        val flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or
            DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC

        virtualDisplay = projection?.createVirtualDisplay(
            "CodeCanva",
            width,
            height,
            density,
            flags,
            rec.surface,
            null,
            null,
        )
        rec.start()
    }

    private fun stopRecording() {
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
        virtualDisplay?.release()
        virtualDisplay = null
        projection?.stop()
        projection = null
        outputFile?.let { file ->
            if (file.exists()) {
                try {
                    val values = ContentValues().apply {
                        put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
                        put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                        put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/CodeCanva")
                    }
                    contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                        contentResolver.openOutputStream(uri)?.use { out ->
                            file.inputStream().use { it.copyTo(out) }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Recording", NotificationManager.IMPORTANCE_LOW),
            )
        }
    }

    companion object {
        const val ACTION_START = "com.endroid.codecanva.record.START"
        const val ACTION_STOP = "com.endroid.codecanva.record.STOP"
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        const val EXTRA_QUALITY = "quality"
        private const val CHANNEL = "codecanva_rec"
    }
}
