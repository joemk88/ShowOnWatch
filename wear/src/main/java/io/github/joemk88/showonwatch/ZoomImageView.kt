package io.github.joemk88.showonwatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

/** Photo view: fit-to-screen with the shared zoom gestures (up to 8×) and double-tap callback. */
class ZoomImageView(context: Context) : View(context) {

    private var bitmap: Bitmap? = null
    private val drawMatrix = Matrix()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    var onDoubleTap: (() -> Unit)? = null

    var flipped: Boolean
        get() = zoom.flipped
        set(value) { zoom.flipped = value }

    private val zoom = ZoomController(
        this, maxZoom = 8f,
        onChange = { invalidate() },
        onDoubleTap = { onDoubleTap?.invoke() }
    )

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun setImage(b: Bitmap) {
        bitmap = b
        zoom.clearContent()
        zoom.setContentSize(b.width, b.height)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) = zoom.reset()

    override fun onDraw(canvas: Canvas) {
        val b = bitmap ?: return
        drawMatrix.setScale(zoom.scale, zoom.scale)
        drawMatrix.postTranslate(zoom.tx, zoom.ty)
        canvas.drawBitmap(b, drawMatrix, paint)
    }

    override fun onTouchEvent(event: MotionEvent) = zoom.onTouchEvent(event)

    // Single taps arrive here via ZoomController; nothing extra to do for photos.
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent) =
        zoom.onGenericMotionEvent(event) || super.onGenericMotionEvent(event)

    override fun canScrollHorizontally(direction: Int) = zoom.canScrollHorizontally(direction)
    override fun canScrollVertically(direction: Int) = zoom.canScrollVertically(direction)
}
