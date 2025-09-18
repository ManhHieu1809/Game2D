package com.example.game2d

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class Bullet(
    var x: Float,
    var y: Float,
    private val direction: Int, // 1 = right, -1 = left
    private val speed: Float = 400f
) {
    private val width = 8f
    private val height = 4f
    private var isActive = true
    
    private val paint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }

    fun update(deltaMs: Long, worldWidth: Float) {
        if (!isActive) return
        
        val dt = deltaMs / 1000f
        x += direction * speed * dt
        
        // Deactivate if goes off screen
        if (x < -width || x > worldWidth + width) {
            isActive = false
        }
    }

    fun draw(canvas: Canvas) {
        if (!isActive) return
        
        canvas.drawRoundRect(
            x, y, x + width, y + height,
            2f, 2f, paint
        )
    }

    fun getBounds(): RectF {
        return RectF(x, y, x + width, y + height)
    }

    fun isActive(): Boolean = isActive
    
    fun deactivate() {
        isActive = false
    }
}