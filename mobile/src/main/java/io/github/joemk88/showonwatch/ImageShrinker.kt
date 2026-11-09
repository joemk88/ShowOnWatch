package io.github.joemk88.showonwatch

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.ByteArrayOutputStream

/**
 * Decodes any image the system understands (JPEG, PNG, WebP, HEIC, GIF first frame...)
 * straight to a downscaled bitmap, then re-encodes as JPEG. Keeps the Bluetooth
 * transfer small (~100-300 KB) while leaving headroom for zooming on the watch.
 */
object ImageShrinker {

    fun shrink(cr: ContentResolver, uri: Uri, maxEdge: Int = 1280, quality: Int = 85): ByteArray {
        val source = ImageDecoder.createSource(cr, uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val w = info.size.width
            val h = info.size.height
            val scale = maxEdge.toFloat() / maxOf(w, h)
            if (scale < 1f) {
                decoder.setTargetSize(
                    (w * scale).toInt().coerceAtLeast(1),
                    (h * scale).toInt().coerceAtLeast(1)
                )
            }
        }

        // JPEG has no alpha: flatten transparent images onto black (the watch background).
        val bitmap = if (decoded.hasAlpha()) {
            Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888).also {
                Canvas(it).apply {
                    drawColor(Color.BLACK)
                    drawBitmap(decoded, 0f, 0f, null)
                }
            }
        } else decoded

        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }
    }
}
