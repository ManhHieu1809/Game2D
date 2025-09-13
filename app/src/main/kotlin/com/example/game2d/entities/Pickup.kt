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
        "cherry" -> 70f
        "strawberry" -> 70f
        "apple","orange" -> 70f
        "banana" -> 70f
        else -> 50f
    }

    override fun update(dtMs: Long) {
        if (collected) { alive = false; return }
        t += dtMs * 0.001f
        vy = (sin(t * 4f) * 6f).toFloat()   // bobbing
    }

    override fun draw(canvas: Canvas) {
        if (collected) return

        // try frames first
        val frames = SpriteLoader.getFrames(type)
        val s = size()
        val dest = RectF(x - s/2, y + vy - s/2, x + s/2, y + vy + s/2)

        if (frames.isNotEmpty()) {
            // animate using t (t được tăng trong update)
            val fps = 12f                       // tốc độ frame: chỉnh nếu muốn
            val idx = ((t * fps).toInt() % frames.size).coerceAtLeast(0)
            val bmp = frames.getOrNull(idx) ?: SpriteLoader.get(type)
            bmp?.let {
                paint.isFilterBitmap = false   // pixel-art: tắt filtering
                paint.isDither = false
                canvas.drawBitmap(it, null, dest, paint)
            }
        } else {
            // fallback: single image (existing behavior) but don't use filtering for pixel-art
            val bmp = SpriteLoader.get(type)
            if (bmp != null) {
                paint.isFilterBitmap = false
                paint.isDither = false
                canvas.drawBitmap(bmp, null, dest, paint)
            } else {
                paint.color = Color.RED
                canvas.drawCircle(x, y + vy, s/2, paint)
            }
        }
    }


    override fun onCollide(other: Entity) {
        collected = true
        alive = false
    }
}
