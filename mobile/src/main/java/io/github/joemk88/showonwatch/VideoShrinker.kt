package io.github.joemk88.showonwatch

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Re-encodes any video the phone can decode into a small watch-friendly MP4:
 * H.264 + AAC, long edge ≤ 640 px, ≤ 30 fps, ~1 Mbps, first 60 seconds only.
 * Blocking: call from a background thread.
 */
@OptIn(UnstableApi::class)
object VideoShrinker {

    const val MAX_DURATION_MS = 60_000L
    private const val LONG_EDGE = 640
    private const val VIDEO_BITRATE = 1_000_000
    private const val MAX_FPS = 30f

    fun shrink(context: Context, uri: Uri, out: File) {
        val (w, h) = displaySize(context, uri)
        val scale = minOf(1f, LONG_EDGE.toFloat() / maxOf(w, h))
        val targetW = even((w * scale).roundToInt())
        val targetH = even((h * scale).roundToInt())

        val done = CountDownLatch(1)
        var failure: Exception? = null

        // Transformer must be created and driven from a Looper thread: use main.
        Handler(Looper.getMainLooper()).post {
            try {
                val mediaItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setEndPositionMs(MAX_DURATION_MS)
                            .build()
                    )
                    .build()

                val videoEffects = listOf<Effect>(
                    FrameDropEffect.createDefaultFrameDropEffect(MAX_FPS),
                    Presentation.createForWidthAndHeight(
                        targetW, targetH, Presentation.LAYOUT_SCALE_TO_FIT
                    )
                )
                val edited = EditedMediaItem.Builder(mediaItem)
                    .setEffects(Effects(emptyList<AudioProcessor>(), videoEffects))
                    .build()

                val encoderFactory = DefaultEncoderFactory.Builder(context)
                    .setRequestedVideoEncoderSettings(
                        VideoEncoderSettings.Builder().setBitrate(VIDEO_BITRATE).build()
                    )
                    .build()

                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .setAudioMimeType(MimeTypes.AUDIO_AAC)
                    .setEncoderFactory(encoderFactory)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            done.countDown()
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            failure = exportException
                            done.countDown()
                        }
                    })
                    .build()

                transformer.start(edited, out.absolutePath)
            } catch (e: Exception) {
                failure = e
                done.countDown()
            }
        }

        if (!done.await(5, TimeUnit.MINUTES)) throw IllegalStateException("Video conversion timed out")
        failure?.let { throw it }
    }

    /** Width/height as displayed, i.e. with the rotation flag applied. */
    private fun displaySize(context: Context, uri: Uri): Pair<Int, Int> {
        val r = MediaMetadataRetriever()
        try {
            r.setDataSource(context, uri)
            val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: LONG_EDGE
            val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: LONG_EDGE
            val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            return if (rot == 90 || rot == 270) h to w else w to h
        } finally {
            r.release()
        }
    }

    private fun even(v: Int) = (v / 2 * 2).coerceAtLeast(2)
}
