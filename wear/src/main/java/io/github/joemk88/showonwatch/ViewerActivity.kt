package io.github.joemk88.showonwatch

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView

class ViewerActivity : Activity() {

    private lateinit var imageView: ZoomImageView
    private lateinit var videoView: ZoomVideoView
    private lateinit var status: TextView
    private lateinit var hint: TextView

    private var wantedId = 0L
    private var wantedVideo = false
    private var shownId = -1L
    private var showingVideo = false

    private val handler = Handler(Looper.getMainLooper())
    private val onNewItem: () -> Unit = { refresh() }
    private val releaseScreen = Runnable {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    private val hideHint = Runnable { hint.visibility = View.GONE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val density = resources.displayMetrics.density

        imageView = ZoomImageView(this)

        // Pinch / crown / double-tap zoom like photos; single tap toggles sound.
        videoView = ZoomVideoView(this).apply {
            visibility = View.GONE
            onTap = {
                muted = !muted
                showHint(if (muted) "Muted" else "Sound on")
            }
            onError = { showStatus("Couldn't play video") }
        }

        status = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            val pad = (28 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }
        hint = TextView(this).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            setBackgroundColor(0x99000000.toInt())
            val p = (8 * density).toInt()
            setPadding(p * 2, p, p * 2, p)
            visibility = View.GONE
        }

        setContentView(FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
            addView(imageView, FrameLayout.LayoutParams(-1, -1))
            addView(videoView, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
            addView(status, FrameLayout.LayoutParams(-1, -1))
            addView(hint, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                .apply { bottomMargin = (28 * density).toInt() })
        })
        imageView.requestFocus()   // so the crown reaches the image view
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        wantedId = intent.data?.getQueryParameter("id")?.toLongOrNull() ?: 0L
        wantedVideo = intent.data?.getQueryParameter("type") == "video"
        refresh()
    }

    override fun onStart() {
        super.onStart()
        ImageStore.addListener(onNewItem)
        refresh()
        if (showingVideo) videoView.resume()
        keepScreenOnForAWhile()
    }

    override fun onStop() {
        ImageStore.removeListener(onNewItem)
        handler.removeCallbacks(releaseScreen)
        if (showingVideo) videoView.pause()
        super.onStop()
    }

    // Any touch keeps the screen awake a bit longer; then normal timeout resumes.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        keepScreenOnForAWhile()
        return super.dispatchTouchEvent(ev)
    }

    private fun keepScreenOnForAWhile() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        handler.removeCallbacks(releaseScreen)
        handler.postDelayed(releaseScreen, if (showingVideo) VIDEO_SCREEN_ON_MS else IMAGE_SCREEN_ON_MS)
    }

    private fun refresh() {
        val latest = ImageStore.latest(this)
        when {
            latest != null && latest.id >= wantedId -> show(latest)
            wantedId > 0 -> showStatus(if (wantedVideo) "Receiving video…" else "Receiving…")
            else -> showStatus(
                "Nothing yet.\n\nShare a picture or video from your phone and pick \"Show on watch\"."
            )
        }
    }

    private fun show(item: ImageStore.Latest) {
        if (item.id == shownId) { status.visibility = View.GONE; return }
        when (item.kind) {
            ImageStore.Kind.IMAGE -> {
                val bmp = BitmapFactory.decodeFile(item.file.path)
                    ?: return showStatus("Couldn't open image")
                stopVideo()
                imageView.visibility = View.VISIBLE
                imageView.setImage(bmp)
                imageView.requestFocus()
            }
            ImageStore.Kind.VIDEO -> {
                imageView.visibility = View.GONE
                videoView.visibility = View.VISIBLE
                showingVideo = true
                videoView.muted = true
                videoView.setVideo(item.file.path)
                videoView.requestFocus()   // so the crown zooms the video
                showHint("Tap for sound")
            }
        }
        shownId = item.id
        status.visibility = View.GONE
        keepScreenOnForAWhile()
    }

    private fun stopVideo() {
        if (!showingVideo) return
        videoView.stop()
        videoView.visibility = View.GONE
        showingVideo = false
    }

    private fun showHint(text: String) {
        hint.text = text
        hint.visibility = View.VISIBLE
        handler.removeCallbacks(hideHint)
        handler.postDelayed(hideHint, 1500)
    }

    private fun showStatus(text: String) {
        status.text = text
        status.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val IMAGE_SCREEN_ON_MS = 60_000L
        private const val VIDEO_SCREEN_ON_MS = 90_000L
    }
}
