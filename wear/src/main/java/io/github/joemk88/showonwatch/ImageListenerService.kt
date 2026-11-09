package io.github.joemk88.showonwatch

import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.ChannelClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Receives images (Data Layer assets) and videos (channel streams) from the phone. */
class ImageListenerService : WearableListenerService() {

    // Runs on a background thread, so blocking calls are fine here.
    override fun onDataChanged(events: DataEventBuffer) {
        for (event in events) {
            if (event.type != DataEvent.TYPE_CHANGED) continue
            val item = event.dataItem
            if (item.uri.path != IMAGE_PATH) continue

            val map = DataMapItem.fromDataItem(item).dataMap
            val asset = map.getAsset(KEY_IMAGE) ?: continue
            val id = map.getLong(KEY_ID)

            try {
                val fd = Tasks.await(
                    Wearable.getDataClient(this).getFdForAsset(asset), 30, TimeUnit.SECONDS
                )
                val bytes = fd.inputStream.use { it.readBytes() }
                ImageStore.saveImage(applicationContext, bytes, id)
            } catch (_: Exception) {
                // Transfer failed; the next share will try again.
            }
        }
    }

    // Path is "/video/<id>/<byteCount>"; the byte count lets us reject truncated transfers.
    override fun onChannelOpened(channel: ChannelClient.Channel) {
        val parts = channel.path.removePrefix(VIDEO_PATH_PREFIX).split("/")
        val id = parts.getOrNull(0)?.toLongOrNull()
        val expected = parts.getOrNull(1)?.toLongOrNull()
        val channels = Wearable.getChannelClient(applicationContext)
        if (id == null || expected == null) {
            channels.close(channel)
            return
        }

        val ctx = applicationContext
        thread(name = "receive-video-$id") {
            val incoming = ImageStore.incomingVideoFile(ctx, id)
            try {
                val input = Tasks.await(channels.getInputStream(channel), 15, TimeUnit.SECONDS)
                input.use { i -> incoming.outputStream().use { i.copyTo(it, 64 * 1024) } }
                if (incoming.length() == expected) ImageStore.commitVideo(ctx, id)
                else incoming.delete()
            } catch (_: Exception) {
                incoming.delete()
            } finally {
                channels.close(channel)
            }
        }
    }

    companion object {
        const val IMAGE_PATH = "/image"
        const val VIDEO_PATH_PREFIX = "/video/"
        const val KEY_IMAGE = "image"
        const val KEY_ID = "id"
    }
}
