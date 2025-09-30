package com.example.game2d

import android.graphics.*
import kotlin.math.*

object PotionEffects {

    fun drawJumpEffect(canvas: Canvas, player: Player, paint: Paint) {
        // Removed jump effect particles - now using countdown timer instead
    }

    fun drawSpeedEffect(canvas: Canvas, player: Player, paint: Paint) {

    }



    fun drawShieldEffect(canvas: Canvas, player: Player, paint: Paint, gameStateManager: GameStateManager) {
        if (gameStateManager.hasShield()) {
            val centerX = player.x + player.width / 2f
            val centerY = player.y + player.height / 2f
            val time = System.currentTimeMillis()
            val progress = gameStateManager.getInvulnerabilityProgress()

            // Shield bubble
            val shieldRadius = 40f
            val alpha = (150 * progress).toInt().coerceIn(50, 150)

            // Outer glow
            paint.color = Color.argb(alpha / 3, 0, 150, 255)
            canvas.drawCircle(centerX, centerY, shieldRadius + 8f, paint)

            // Main shield
            paint.color = Color.argb(alpha, 100, 200, 255)
            canvas.drawCircle(centerX, centerY, shieldRadius, paint)

            // Shield patterns
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = Color.argb(alpha + 50, 255, 255, 255)

            for (i in 0 until 6) {
                val angle = (i * 60f + time * 0.002f) % 360f
                val x1 = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * (shieldRadius - 10f)
                val y1 = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * (shieldRadius - 10f)
                val x2 = centerX + cos(Math.toRadians(angle.toDouble())).toFloat() * (shieldRadius + 5f)
                val y2 = centerY + sin(Math.toRadians(angle.toDouble())).toFloat() * (shieldRadius + 5f)
                canvas.drawLine(x1, y1, x2, y2, paint)
            }

            paint.style = Paint.Style.FILL
        }
    }

    private fun drawUpArrow(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint) {
        val path = Path()
        path.moveTo(x, y - size)
        path.lineTo(x - size/2, y)
        path.lineTo(x + size/2, y)         // Bottom right
        path.close()
        canvas.drawPath(path, paint)
    }
}
