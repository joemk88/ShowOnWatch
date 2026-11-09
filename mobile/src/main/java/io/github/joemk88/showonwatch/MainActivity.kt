package io.github.joemk88.showonwatch

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private val pickRequest = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (24 * resources.displayMetrics.density).toInt()

        // Progress notification while a video is being shrunk/sent.
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
        }

        val info = TextView(this).apply {
            setTextColor(Color.BLACK)
            textSize = 18f
            text = "Long-press any image in your browser (or open a photo or video in your gallery), " +
                "tap Share, then choose \"Show on watch\".\n\n" +
                "Videos are shrunk and trimmed to the first 60 seconds before sending.\n\n" +
                "Make sure the watch app is installed too."
        }
        val button = Button(this).apply {
            text = "Pick a photo or video to send"
            setOnClickListener {
                startActivityForResult(
                    Intent(Intent.ACTION_GET_CONTENT)
                        .setType("*/*")
                        .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*")),
                    pickRequest
                )
            }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(pad, pad, pad, pad)
            addView(info)
            addView(button, LinearLayout.LayoutParams(-1, -2).apply { topMargin = pad })
        })
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data ?: return
        if (requestCode == pickRequest && resultCode == RESULT_OK) {
            startActivity(
                Intent(this, ShareActivity::class.java)
                    .setAction(Intent.ACTION_SEND)
                    .setType(contentResolver.getType(uri) ?: "image/*")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            )
        }
    }
}
