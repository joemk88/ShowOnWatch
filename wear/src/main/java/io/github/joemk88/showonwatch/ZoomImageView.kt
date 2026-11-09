package io.github.joemk88.showonwatch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

/** Photo view: fit-to-screen with the shared zoom gestures (up to 8×). */
class ZoomImageView(context: Context) : View(context) {

    private var bitmap: Bitmap? = null
    private val drawMatrix = Matrix()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    private val zoom = ZoomController(this, maxZoom = 8f, onChange = { invalidate() })

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

    override fun onGenericMotionEvent(event: MotionEvent) =
        zoom.onGenericMotionEvent(event) || super.onGenericMotionEvent(event)

    override fun canScrollHorizontally(direction: Int) = zoom.canScrollHorizontally(direction)
    override fun canScrollVertically(direction: Int) = zoom.canScrollVertically(direction)
}
