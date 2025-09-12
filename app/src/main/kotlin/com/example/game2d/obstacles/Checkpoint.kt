package com.example.game2d.obstacles

import android.graphics.*
import com.example.game2d.Player
import com.example.game2d.resources.SpriteLoader
import kotlin.math.pow
import kotlin.math.sqrt

class Checkpoint(var x: Float, var y: Float) {
    var activated = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(canvas: Canvas) {
        val sprite = SpriteLoader.get("checkpoint")
        if (sprite != null) {
            val rect = RectF(x, y, x + 32f, y + 40f)
            val cf = if (activated) ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1.5f) }) else null
            paint.colorFilter = cf
            paint.isFilterBitmap = true
            paint.isDither = true
            canvas.drawBitmap(sprite, null, rect, paint)
            paint.colorFilter = null
        } else {
            paint.color = if (activated) Color.GREEN else Color.RED
            canvas.drawRect(x, y, x + 4f, y + 40f, paint)
            paint.color = if (activated) Color.YELLOW else Color.WHITE
            canvas.drawRect(x + 4f, y, x + 32f, y + 20f, paint)
        }
    }

    fun tryActivate(px: Float, py: Float, radius: Float = 40f): Boolean {
        val dx = px - x; val dy = py - y
        if (dx*dx + dy*dy < radius*radius) {
            activated = true
            return true
        }
        return false
    }
}
