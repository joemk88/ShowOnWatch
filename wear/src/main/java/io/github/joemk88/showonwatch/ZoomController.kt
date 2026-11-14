package io.github.joemk88.showonwatch

import android.view.GestureDetector
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.exp
import kotlin.math.min

/**
 * Shared zoom/pan state and gestures for photos and videos: pinch to zoom,
 * drag to pan, crown (rotary) to zoom, plus an optional double-tap callback.
 * A single tap is reported as a normal click (view.performClick()), so it also
 * works with accessibility services. The view applies [scale], [tx], [ty] however it draws.
 */
class ZoomController(
    private val view: View,
    private val maxZoom: Float,
    private val onChange: () -> Unit,
    private val onDoubleTap: (() -> Unit)? = null
) {
    /**
     * True while the screen is turned upside down (the parent is rotated 180°).
     * Touch input is already mapped by Android, but the swipe-to-dismiss check
     * asks in screen directions, so those answers have to be reversed.
     */
    var flipped = false

    var contentW = 0; private set
    var contentH = 0; private set
    var scale = 1f; private set
    var tx = 0f; private set
    var ty = 0f; private set
    private var minScale = 1f

    private val scaleDetector = ScaleGestureDetector(view.context,
        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(d: ScaleGestureDetector): Boolean {
                zoomBy(d.scaleFactor, d.focusX, d.focusY); return true
            }
        })

    private val gestureDetector = GestureDetector(view.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent) = true
            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, dx: Float, dy: Float): Boolean {
                tx -= dx; ty -= dy; clamp(); onChange(); return true
            }
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleTap?.invoke()
                return onDoubleTap != null
            }
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                view.performClick()
                return true
            }
        })

    private val rotaryFactor = ViewConfiguration.get(view.context).scaledVerticalScrollFactor

    /** Sets the content's natural size. Re-fits only if the size actually changed. */
    fun setContentSize(w: Int, h: Int) {
        if (w == contentW && h == contentH) return
        contentW = w
        contentH = h
        reset()
    }

    /** Forget the content so the next [setContentSize] always re-fits (new item). */
    fun clearContent() {
        contentW = 0
        contentH = 0
    }

    /** Fit to screen and centre. Also call when the view's size changes. */
    fun reset() {
        if (contentW == 0 || contentH == 0 || view.width == 0 || view.height == 0) return
        minScale = min(view.width.toFloat() / contentW, view.height.toFloat() / contentH)
        scale = minScale
        tx = (view.width - contentW * scale) / 2f
        ty = (view.height - contentH * scale) / 2f
        onChange()
    }

    private fun zoomBy(factor: Float, fx: Float, fy: Float) {
        if (contentW == 0) return
        val newScale = (scale * factor).coerceIn(minScale, minScale * maxZoom)
        val k = newScale / scale
        tx = fx - (fx - tx) * k
        ty = fy - (fy - ty) * k
        scale = newScale
        clamp()
        onChange()
    }

    private fun clamp() {
        val w = contentW * scale
        val h = contentH * scale
        val vw = view.width.toFloat()
        val vh = view.height.toFloat()
        tx = if (w <= vw) (vw - w) / 2f else tx.coerceIn(vw - w, 0f)
        ty = if (h <= vh) (vh - h) / 2f else ty.coerceIn(vh - h, 0f)
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        if (!scaleDetector.isInProgress) gestureDetector.onTouchEvent(event)
        return true
    }

    /** Crown: rotate to zoom around the centre. */
    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL &&
            event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)
        ) {
            val delta = -event.getAxisValue(MotionEvent.AXIS_SCROLL) * rotaryFactor
            zoomBy(exp(delta / 400f), view.width / 2f, view.height / 2f)
            return true
        }
        return false
    }

    // Lets Wear's swipe-to-dismiss only trigger once you're at the left edge.
    fun canScrollHorizontally(screenDirection: Int): Boolean {
        val direction = if (flipped) -screenDirection else screenDirection
        val w = contentW * scale
        if (contentW == 0 || w <= view.width + 0.5f) return false
        return if (direction < 0) tx < -0.5f else tx + w > view.width + 0.5f
    }

    fun canScrollVertically(screenDirection: Int): Boolean {
        val direction = if (flipped) -screenDirection else screenDirection
        val h = contentH * scale
        if (contentH == 0 || h <= view.height + 0.5f) return false
        return if (direction < 0) ty < -0.5f else ty + h > view.height + 0.5f
    }
}
