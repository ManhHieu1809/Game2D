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
            // fallback: draw cherry fruit when no sprite found
            val bmp = SpriteLoader.get(type)
            if (bmp != null) {
                paint.isFilterBitmap = false
                paint.isDither = false
                canvas.drawBitmap(bmp, null, dest, paint)
            } else {
                // Draw cherry fruit instead of red circle
                drawCherryFruit(canvas, x, y + vy, s/2)
            }
        }
    }

    private fun drawCherryFruit(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        // Scale down to match other fruits size - use smaller multiplier
        val size = radius * 1.2f  // Giảm từ 2.0 xuống 1.2 để cherry nhỏ hơn

        // Draw stem (cuống)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.06f
        paint.color = Color.rgb(101, 67, 33) // Brown
        paint.strokeCap = Paint.Cap.ROUND

        val stemPath = Path()
        stemPath.moveTo(centerX, centerY - size * 0.15f)
        stemPath.cubicTo(
            centerX + size * 0.08f, centerY - size * 0.3f,
            centerX + size * 0.15f, centerY - size * 0.35f,
            centerX + size * 0.12f, centerY - size * 0.45f
        )
        canvas.drawPath(stemPath, paint)

        // Draw leaf (lá) - smaller
        val leafPath = Path()
        leafPath.moveTo(centerX + size * 0.12f, centerY - size * 0.35f)
        leafPath.cubicTo(
            centerX + size * 0.2f, centerY - size * 0.38f,
            centerX + size * 0.28f, centerY - size * 0.35f,
            centerX + size * 0.25f, centerY - size * 0.28f
        )
        leafPath.cubicTo(
            centerX + size * 0.2f, centerY - size * 0.32f,
            centerX + size * 0.16f, centerY - size * 0.34f,
            centerX + size * 0.12f, centerY - size * 0.35f
        )
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(76, 175, 80) // Green
        canvas.drawPath(leafPath, paint)

        // Draw left cherry (quả cherry trái) - smaller
        val leftCherryX = centerX - size * 0.12f
        val leftCherryY = centerY + size * 0.05f
        val cherryRadius = size * 0.22f

        // Cherry gradient
        val cherryGradient = RadialGradient(
            leftCherryX - cherryRadius * 0.3f, leftCherryY - cherryRadius * 0.3f, cherryRadius,
            intArrayOf(
                Color.rgb(255, 100, 100),  // Light red highlight
                Color.rgb(220, 20, 60),    // Crimson
                Color.rgb(200, 0, 0)       // Dark red
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        paint.shader = cherryGradient
        canvas.drawCircle(leftCherryX, leftCherryY, cherryRadius, paint)

        // Left cherry highlight
        paint.shader = null
        paint.color = Color.argb(200, 255, 180, 180)
        canvas.drawCircle(leftCherryX - cherryRadius * 0.35f, leftCherryY - cherryRadius * 0.35f, cherryRadius * 0.3f, paint)

        // Draw right cherry (quả cherry phải) - smaller
        val rightCherryX = centerX + size * 0.12f
        val rightCherryY = centerY + size * 0.05f

        val cherryGradient2 = RadialGradient(
            rightCherryX - cherryRadius * 0.3f, rightCherryY - cherryRadius * 0.3f, cherryRadius,
            intArrayOf(
                Color.rgb(255, 100, 100),
                Color.rgb(220, 20, 60),
                Color.rgb(200, 0, 0)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = cherryGradient2
        canvas.drawCircle(rightCherryX, rightCherryY, cherryRadius, paint)

        // Right cherry highlight
        paint.shader = null
        paint.color = Color.argb(200, 255, 180, 180)
        canvas.drawCircle(rightCherryX - cherryRadius * 0.35f, rightCherryY - cherryRadius * 0.35f, cherryRadius * 0.3f, paint)

        // Draw shadows under cherries for depth - smaller
        paint.color = Color.argb(60, 0, 0, 0)
        canvas.drawOval(
            leftCherryX - cherryRadius * 0.7f, leftCherryY + cherryRadius * 0.5f,
            leftCherryX + cherryRadius * 0.7f, leftCherryY + cherryRadius * 0.8f,
            paint
        )
        canvas.drawOval(
            rightCherryX - cherryRadius * 0.7f, rightCherryY + cherryRadius * 0.5f,
            rightCherryX + cherryRadius * 0.7f, rightCherryY + cherryRadius * 0.8f,
            paint
        )

        // Reset paint shader
        paint.shader = null
    }

    override fun onCollide(other: Entity) {
        collected = true
        alive = false
    }
}
