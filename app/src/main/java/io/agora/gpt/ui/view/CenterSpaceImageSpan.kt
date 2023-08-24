package io.agora.gpt.ui.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import java.lang.ref.WeakReference

class CenterSpaceImageSpan @JvmOverloads constructor(
    drawable: Drawable,
    private val mMarginLeft: Int = 0,
    private val mMarginRight: Int = 0
) : ImageSpan(drawable) {
    override fun draw(
        canvas: Canvas, text: CharSequence, start: Int, end: Int, x: Float, top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        var tempX = x
        val b = drawable
        val fm = paint.fontMetricsInt
        tempX += mMarginLeft
        val transY = (y + fm.descent + y + fm.ascent) / 2 - b.bounds.bottom / 2
        canvas.save()
        canvas.translate(tempX, transY.toFloat())
        b.draw(canvas)
        canvas.restore()
    }

    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: FontMetricsInt?): Int {
        return mMarginLeft + super.getSize(paint, text, start, end, fm) + mMarginRight
    }

    private fun getCachedDrawable():Drawable?{
        val wr = mDrawableRef
        var d: Drawable? = wr?.get()
        if (d == null) {
            d = drawable
            mDrawableRef = WeakReference(d)
        }
        return d
    }

    private var mDrawableRef: WeakReference<Drawable?>? = null
}