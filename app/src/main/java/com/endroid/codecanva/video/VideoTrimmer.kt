package com.endroid.codecanva.video

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import java.io.File
import java.nio.ByteBuffer

object VideoTrimmer {

    fun durationMs(context: Context, uri: Uri): Long {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, uri)
            r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
    }

    /**
     * Trim [startMs, endMs] into [outFile] using sample copy (fast, no re-encode).
     * Returns null on success, error message on failure.
     */
    fun trim(
        context: Context,
        input: Uri,
        outFile: File,
        startMs: Long,
        endMs: Long,
    ): String? {
        if (endMs <= startMs) return "End must be after start"
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        return try {
            extractor.setDataSource(context, input, null)
            if (outFile.exists()) outFile.delete()
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackCount = extractor.trackCount
            val indexMap = HashMap<Int, Int>(trackCount)
            var hasVideo = false
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    val dst = muxer.addTrack(format)
                    indexMap[i] = dst
                    if (mime.startsWith("video/")) hasVideo = true
                }
            }
            if (!hasVideo) return "No video track"
            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            val buffer = ByteBuffer.allocate(1024 * 1024)
            val info = MediaCodec.BufferInfo()

            for ((srcTrack, dstTrack) in indexMap) {
                extractor.selectTrack(srcTrack)
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break
                    val sampleTime = extractor.sampleTime
                    if (sampleTime < 0 || sampleTime > endUs) break
                    if (sampleTime >= startUs) {
                        info.offset = 0
                        info.size = sampleSize
                        info.presentationTimeUs = sampleTime - startUs
                        info.flags = extractor.sampleFlags
                        muxer.writeSampleData(dstTrack, buffer, info)
                    }
                    extractor.advance()
                }
                extractor.unselectTrack(srcTrack)
            }
            null
        } catch (e: Exception) {
            try { if (outFile.exists()) outFile.delete() } catch (_: Exception) {}
            e.message ?: "Trim failed"
        } finally {
            try { extractor.release() } catch (_: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (_: Exception) {
            }
        }
    }
}
