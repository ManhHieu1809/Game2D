package com.example.game2d

import android.content.Context
import android.graphics.*
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat

enum class Edge { LEFT, RIGHT, TOP, BOTTOM }

class ToggleButton(
    private val context: Context,
    @DrawableRes private val onRes: Int,
    @DrawableRes private val offRes: Int,
    private val sizeDp: Float = 48f,
    private val marginDp: Float = 8f
) {
    private var bmpOn: Bitmap = decode(onRes)
    private var bmpOff: Bitmap = decode(offRes)
    private val rect = RectF()
    var isOn: Boolean = false
        private set

    private fun decode(@DrawableRes res: Int): Bitmap {
        val d = ResourcesCompat.getDrawable(context.resources, res, null)!!
        val w = d.intrinsicWidth.coerceAtLeast(1)
        val h = d.intrinsicHeight.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.setBounds(0, 0, w, h)
        d.draw(c)
        return bmp
    }

    fun placeAtEdge(edge: Edge, screenW: Int, screenH: Int, density: Float) {
        val size = sizeDp * density
        val margin = marginDp * density
        val cx: Float
        val cy: Float
        when (edge) {
            Edge.TOP -> { cx = screenW / 2f; cy = margin + size / 2f }
            Edge.BOTTOM -> { cx = screenW / 2f; cy = screenH - margin - size / 2f }
            Edge.LEFT -> { cx = margin + size / 2f; cy = screenH / 2f }
            Edge.RIGHT -> { cx = screenW - margin - size / 2f; cy = screenH / 2f }
        }
        rect.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f)
    }

    fun setState(on: Boolean) { isOn = on }
    fun toggle(): Boolean { isOn = !isOn; return isOn }

    fun draw(canvas: Canvas, paint: Paint) {
        val bmp = if (isOn) bmpOn else bmpOff
        val src = Rect(0, 0, bmp.width, bmp.height)
        canvas.drawBitmap(bmp, src, rect, paint)
    }

    fun hitTest(x: Float, y: Float): Boolean = rect.contains(x, y)
}
