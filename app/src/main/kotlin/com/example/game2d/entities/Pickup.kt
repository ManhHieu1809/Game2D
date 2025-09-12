package com.example.game2d.entities

import android.graphics.*
import com.example.game2d.resources.SpriteLoader
import kotlin.math.sin

class Pickup(val type: String, startX: Float, startY: Float) : Entity() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    var collected = false

    init { x = startX; y = startY }

    override fun getBounds(): RectF {
        val s = size()
        return RectF(x - s/2, y - s/2, x + s/2, y + s/2)
    }

    private fun size(): Float = when(type) {
        "cherry" -> 28f
        "strawberry" -> 32f
        "apple","orange" -> 36f
        "banana" -> 40f
        else -> 32f
    }

    override fun update(dtMs: Long) {
        if (collected) { alive = false; return }
        t += dtMs * 0.001f
        vy = (sin(t * 4f) * 6f).toFloat()   // bobbing
    }

    override fun draw(canvas: Canvas) {
        if (collected) return
        val bmp = SpriteLoader.get(type)
        val s = size()
        val dest = RectF(x - s/2, y + vy - s/2, x + s/2, y + vy + s/2)
        if (bmp != null) {
            paint.isFilterBitmap = true
            paint.isDither = true
            canvas.drawBitmap(bmp, null, dest, paint)
        } else {
            paint.color = Color.RED
            canvas.drawCircle(x, y + vy, s/2, paint)
        }
    }

    override fun onCollide(other: Entity) {
        collected = true
        alive = false
    }
}
