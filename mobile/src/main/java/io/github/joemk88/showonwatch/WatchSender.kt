package io.github.joemk88.showonwatch

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Blocking: call from a background thread. Each send returns a short status message. */
object WatchSender {

    const val IMAGE_PATH = "/image"
    const val VIDEO_PATH_PREFIX = "/video/"   // + "<id>/<byteCount>"
    const val KEY_IMAGE = "image"
    const val KEY_ID = "id"

    private const val NO_WATCH = "Watch not connected"

    // ---------- Images: Data Layer asset (v1.0 behaviour) ----------

    fun sendImage(context: Context, imageUri: Uri): String {
        val bytes = ImageShrinker.shrink(context.contentResolver, imageUri)
        val nodes = connectedNodes(context)
        if (nodes.isEmpty()) return NO_WATCH

        // The id makes every send a "change", even for the same picture.
        val id = System.currentTimeMillis()
        val request = PutDataMapRequest.create(IMAGE_PATH).apply {
            dataMap.putAsset(KEY_IMAGE, Asset.createFromBytes(bytes))
            dataMap.putLong(KEY_ID, id)
        }.asPutDataRequest().setUrgent()
        Tasks.await(Wearable.getDataClient(context).putDataItem(request), 15, TimeUnit.SECONDS)

        val opened = openViewer(context, nodes, id, "image")
        val kb = bytes.size / 1024
        return if (opened) "Sent to watch ($kb KB)"
        else "Sent ($kb KB) – open Show on watch on the watch"
    }

    // ---------- Videos: shrink, then stream over a channel ----------

    fun sendVideo(context: Context, videoUri: Uri, status: (String) -> Unit): String {
        val nodes = connectedNodes(context)
        if (nodes.isEmpty()) return NO_WATCH
        val target = nodes.firstOrNull { it.isNearby } ?: nodes.first()

        val file = File(context.cacheDir, "outgoing.mp4")
        file.delete()
        try {
            status("Shrinking video…")
            VideoShrinker.shrink(context, videoUri, file)
            val size = file.length()
            val mb = String.format(Locale.US, "%.1f", size / 1_048_576.0)

            val id = System.currentTimeMillis()
            // Open the viewer first so the watch shows "Receiving video…" during the transfer.
            val opened = openViewer(context, listOf(target), id, "video")

            status("Sending video ($mb MB)…")
            val channels = Wearable.getChannelClient(context)
            val channel = Tasks.await(
                channels.openChannel(target.id, "$VIDEO_PATH_PREFIX$id/$size"), 15, TimeUnit.SECONDS
            )
            val out = Tasks.await(channels.getOutputStream(channel), 15, TimeUnit.SECONDS)
            out.use { o -> file.inputStream().use { it.copyTo(o, 64 * 1024) } }
            // The watch closes the channel once it has the whole file.

            return if (opened) "Video sent ($mb MB)"
            else "Video sent ($mb MB) – open Show on watch on the watch"
        } finally {
            file.delete()
        }
    }

    // ---------- Shared helpers ----------

    private fun connectedNodes(context: Context): List<Node> =
        Tasks.await(Wearable.getNodeClient(context).connectedNodes, 10, TimeUnit.SECONDS)

    /** Wakes the watch and opens the viewer, which then waits for item [id]. */
    private fun openViewer(context: Context, nodes: List<Node>, id: Long, kind: String): Boolean {
        val viewIntent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse("showonwatch://show?id=$id&type=$kind"))
        val helper = RemoteActivityHelper(context, Executors.newSingleThreadExecutor())
        val targets = nodes.filter { it.isNearby }.ifEmpty { nodes }
        var opened = false
        for (node in targets) {
            try {
                helper.startRemoteActivity(viewIntent, node.id).get(10, TimeUnit.SECONDS)
                opened = true
            } catch (_: Exception) {
                // Watch app missing or watch unreachable for this node; try the next.
            }
        }
        return opened
    }
}
