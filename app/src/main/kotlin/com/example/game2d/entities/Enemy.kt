package com.example.game2d.entities

import android.graphics.*
import com.example.game2d.resources.SpriteLoader

class Enemy(val type: String, startX: Float, startY: Float) : Entity() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animTimer = 0f
    private var frame = 0
    var facingLeft = true

    init { x = startX; y = startY; vx = -50f }

    override fun getBounds(): RectF {
        val s = 48f
        return RectF(x - s/2, y - s/2, x + s/2, y + s/2)
    }

    override fun update(dtMs: Long) {
        val dt = dtMs / 1000f
        x += vx * dt
        facingLeft = vx < 0

        animTimer += dtMs
        if (animTimer >= 100f) {
            animTimer -= 100f
            val count = SpriteLoader.getFramesCount(type).coerceAtLeast(1)
            frame = (frame + 1) % count
        }
    }

    override fun draw(canvas: Canvas) {
        val frames = SpriteLoader.getFrames(type)
        val bmp = if (frames.isNotEmpty()) frames[frame % frames.size] else SpriteLoader.get(type)
        val s = 48f
        if (bmp != null) {
            val dest = RectF(x - s/2, y - s/2, x + s/2, y + s/2)
            canvas.save()
            if (!facingLeft) canvas.scale(-1f, 1f, x, y)
            paint.isFilterBitmap = true
            paint.isDither = true
            canvas.drawBitmap(bmp, Rect(0,0,bmp.width,bmp.height), dest, paint)
            canvas.restore()
        } else {
            paint.color = Color.MAGENTA
            canvas.drawCircle(x, y, s/2, paint)
        }
    }
}
