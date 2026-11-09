package io.github.joemk88.showonwatch

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast

/** Invisible share target: works out what was shared, hands it to SendService, closes. */
class ShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = extractUri(intent)
        val mime = uri?.let { contentResolver.getType(it) } ?: intent.type
        val kind = when {
            mime == null -> null
            mime.startsWith("video/") -> "video/*"
            mime.startsWith("image/") -> "image/*"
            else -> null
        }

        if (uri == null || kind == null) {
            Toast.makeText(applicationContext, "No image or video found in share", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(
                applicationContext,
                if (kind == "video/*") "Preparing video for watch…" else "Sending to watch…",
                Toast.LENGTH_SHORT
            ).show()
            // Pass our read access to the service so it can keep reading after we close.
            startForegroundService(
                Intent(this, SendService::class.java)
                    .setDataAndType(uri, kind)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        }
        finish()
    }

    private fun extractUri(intent: Intent): Uri? {
        if (intent.action == Intent.ACTION_VIEW) return intent.data
        val stream: Uri? = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        return stream ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    }
}
