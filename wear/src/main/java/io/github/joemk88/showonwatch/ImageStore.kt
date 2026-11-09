package io.github.joemk88.showonwatch

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Keeps only the most recent item (image or video), so reopening the app
 * shows the last thing sent.
 */
object ImageStore {

    enum class Kind { IMAGE, VIDEO }

    class Latest(val id: Long, val kind: Kind, val file: File)

    private val listeners = CopyOnWriteArraySet<() -> Unit>()
    private val main = Handler(Looper.getMainLooper())

    private fun imageFile(ctx: Context) = File(ctx.filesDir, "latest.jpg")
    private fun videoFile(ctx: Context) = File(ctx.filesDir, "latest.mp4")
    private fun metaFile(ctx: Context) = File(ctx.filesDir, "latest.meta")   // "<id> <KIND>"
    private fun legacyIdFile(ctx: Context) = File(ctx.filesDir, "latest.id") // v1.0

    fun incomingVideoFile(ctx: Context, id: Long) = File(ctx.filesDir, "incoming-$id.mp4")

    fun latest(ctx: Context): Latest? {
        val meta = metaFile(ctx).takeIf { it.exists() }?.readText()?.trim()?.split(" ")
        if (meta != null && meta.size == 2) {
            val id = meta[0].toLongOrNull() ?: return null
            val kind = runCatching { Kind.valueOf(meta[1]) }.getOrNull() ?: return null
            val file = if (kind == Kind.VIDEO) videoFile(ctx) else imageFile(ctx)
            return if (file.exists()) Latest(id, kind, file) else null
        }
        // Image saved by v1.0
        val legacyId = legacyIdFile(ctx).takeIf { it.exists() }?.readText()?.trim()?.toLongOrNull()
        return if (legacyId != null && imageFile(ctx).exists()) Latest(legacyId, Kind.IMAGE, imageFile(ctx)) else null
    }

    fun latestId(ctx: Context): Long = latest(ctx)?.id ?: 0L

    @Synchronized
    fun saveImage(ctx: Context, bytes: ByteArray, id: Long) {
        if (id <= latestId(ctx)) return            // stale or duplicate delivery
        val tmp = File(ctx.filesDir, "latest.jpg.tmp")
        tmp.writeBytes(bytes)
        tmp.renameTo(imageFile(ctx))
        videoFile(ctx).delete()
        commit(ctx, id, Kind.IMAGE)
    }

    @Synchronized
    fun commitVideo(ctx: Context, id: Long) {
        val incoming = incomingVideoFile(ctx, id)
        if (id <= latestId(ctx)) { incoming.delete(); return }
        videoFile(ctx).delete()
        incoming.renameTo(videoFile(ctx))
        imageFile(ctx).delete()
        commit(ctx, id, Kind.VIDEO)
    }

    private fun commit(ctx: Context, id: Long, kind: Kind) {
        metaFile(ctx).writeText("$id ${kind.name}")
        legacyIdFile(ctx).delete()
        main.post { listeners.forEach { it() } }
    }

    fun addListener(l: () -> Unit) = listeners.add(l)
    fun removeListener(l: () -> Unit) = listeners.remove(l)
}
