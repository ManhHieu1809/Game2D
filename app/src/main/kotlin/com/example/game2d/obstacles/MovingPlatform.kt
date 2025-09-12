package com.example.game2d.obstacles

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class MovingPlatform(
    var x: Float,
    var y: Float,
    var startX: Float,
    var endX: Float,
    var speed: Float = 50f,
    var direction: Int = 1
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun update(dt: Float) {
        x += speed * direction * dt
        if (x <= startX || x >= endX) direction *= -1
    }

    fun rect(): RectF = RectF(x, y, x + 120f, y + 20f)

    fun draw(canvas: Canvas) {
        paint.color = Color.rgb(139,69,19)
        canvas.drawRoundRect(rect(), 4f, 4f, paint)
        paint.color = Color.rgb(160, 82, 45)
        canvas.drawRect(x + 4f, y + 4f, x + 116f, y + 16f, paint)
    }
}
