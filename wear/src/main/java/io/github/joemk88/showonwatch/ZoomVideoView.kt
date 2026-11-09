package io.github.joemk88.showonwatch

import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView

/**
 * Looping video player whose picture can be zoomed and panned like a photo.
 * Uses a TextureView so the GPU scales each frame via [setTransform] — no extra
 * decoding work. Max 4× since clips arrive at ≤640 px.
 * Never requests audio focus, so it won't pause the user's music.
 */
class ZoomVideoView(context: Context) : TextureView(context), TextureView.SurfaceTextureListener {

    var onTap: (() -> Unit)? = null
    var onError: (() -> Unit)? = null

    var muted = true
        set(value) { field = value; applyVolume() }

    private val zoom = ZoomController(
        this, maxZoom = 4f,
        onChange = { applyTransform() },
        onSingleTap = { onTap?.invoke() }
    )
    private val transformMatrix = Matrix()

    private var path: String? = null
    private var surface: Surface? = null
    private var player: MediaPlayer? = null
    private var prepared = false
    private var playWhenReady = true
    private var resumeAt = 0

    init {
        surfaceTextureListener = this
        setOpaque(false)          // areas outside the zoomed picture show the black parent
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setVideo(p: String) {
        release()
        zoom.clearContent()
        resumeAt = 0
        playWhenReady = true
        path = p
        open()
    }

    fun stop() {
        release()
        path = null
    }

    fun pause() {
        playWhenReady = false
        val mp = player ?: return
        if (prepared && mp.isPlaying) {
            mp.pause()
            resumeAt = mp.currentPosition
        }
    }

    fun resume() {
        playWhenReady = true
        val mp = player
        when {
            mp == null -> open()
            prepared -> mp.start()
            // else: onPrepared will start it
        }
    }

    private fun open() {
        val p = path ?: return
        val s = surface ?: return     // opened again once the surface exists
        release()
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setDataSource(p)
            mp.setSurface(s)
            mp.isLooping = true
            mp.setOnVideoSizeChangedListener { _, w, h ->
                if (w > 0 && h > 0) zoom.setContentSize(w, h)
            }
            mp.setOnPreparedListener {
                prepared = true
                applyVolume()
                if (resumeAt > 0) it.seekTo(resumeAt)
                if (playWhenReady) it.start()
            }
            mp.setOnErrorListener { _, _, _ ->
                onError?.invoke()
                true
            }
            mp.prepareAsync()
        } catch (_: Exception) {
            release()
            onError?.invoke()
        }
    }

    private fun release() {
        val mp = player ?: return
        if (prepared) resumeAt = runCatching { mp.currentPosition }.getOrDefault(resumeAt)
        mp.release()
        player = null
        prepared = false
    }

    private fun applyVolume() {
        val v = if (muted) 0f else 1f
        runCatching { player?.setVolume(v, v) }
    }

    /** The texture is stretched to the view by default; undo that, then apply zoom/pan. */
    private fun applyTransform() {
        if (width == 0 || height == 0 || zoom.contentW == 0) return
        transformMatrix.setScale(
            zoom.contentW * zoom.scale / width,
            zoom.contentH * zoom.scale / height
        )
        transformMatrix.postTranslate(zoom.tx, zoom.ty)
        setTransform(transformMatrix)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        zoom.reset()
    }

    // --- Surface lifecycle ---
    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
        surface = Surface(st)
        open()
    }

    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = applyTransform()

    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        release()
        surface?.release()
        surface = null
        return true
    }

    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}

    // --- Input ---
    override fun onTouchEvent(event: MotionEvent) = zoom.onTouchEvent(event)

    override fun onGenericMotionEvent(event: MotionEvent) =
        zoom.onGenericMotionEvent(event) || super.onGenericMotionEvent(event)

    override fun canScrollHorizontally(direction: Int) = zoom.canScrollHorizontally(direction)
    override fun canScrollVertically(direction: Int) = zoom.canScrollVertically(direction)
}
