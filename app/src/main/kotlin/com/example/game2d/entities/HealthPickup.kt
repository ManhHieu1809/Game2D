package com.example.game2d.entities

import android.graphics.*
import com.example.game2d.Player
import kotlin.math.*

class HealthPickup(
    var x: Float,
    var y: Float,
    val healAmount: Int = 1,
    val type: String = "health_potion"
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    var collected = false
    private var animTimer = 0f
    private var pulseScale = 1f
    private val particles = mutableListOf<HealParticle>()

    companion object {
        const val SIZE = 24f
        const val COLLECTION_RADIUS = 30f
    }

    // Particle effect for healing items
    private data class HealParticle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float,
        var maxLife: Float,
        var color: Int
    )

    fun update(deltaMs: Long) {
        if (collected) return

        val dt = deltaMs / 1000f
        animTimer += dt

        // Pulsing animation
        pulseScale = 1f + sin(animTimer * 4f) * 0.2f

        // Create healing particles
        if ((animTimer * 10f).toInt() % 3 == 0) {
            createHealParticle()
        }

        // Update particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.x += particle.vx * dt
            particle.y += particle.vy * dt
            particle.life -= dt

            if (particle.life <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun createHealParticle() {
        val angle = Math.random() * 2 * PI
        val speed = 20f + Math.random() * 30f
        val distance = 10f + Math.random() * 15f

        particles.add(HealParticle(
            x = (x + cos(angle) * distance).toFloat(),
            y = (y + sin(angle) * distance).toFloat(),
            vx = (cos(angle) * speed * 0.3f).toFloat(),
            vy = (sin(angle) * speed * 0.3f - 20f).toFloat(), // Float upward
            life = 1f + Math.random().toFloat(),
            maxLife = 2f,
            color = when (type) {
                "health_potion" -> Color.rgb(255, 100, 150) // Pink
                "big_health" -> Color.rgb(100, 255, 100)    // Green
                "super_health" -> Color.rgb(255, 215, 0)    // Gold
                else -> Color.rgb(255, 255, 255)            // White
            }
        ))
    }

    fun draw(canvas: Canvas) {
        if (collected) return

        val centerX = x + SIZE / 2f
        val centerY = y + SIZE / 2f
        val currentSize = SIZE * pulseScale

        // Draw glow effect
        val glowRadius = currentSize + 10f
        glowPaint.shader = RadialGradient(
            centerX, centerY, glowRadius,
            intArrayOf(
                Color.argb(100, 255, 255, 255),
                Color.argb(50, 255, 100, 150),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, glowRadius, glowPaint)

        // Draw particles
        for (particle in particles) {
            val alpha = (particle.life / particle.maxLife * 255).toInt().coerceIn(0, 255)
            paint.color = Color.argb(alpha, Color.red(particle.color), Color.green(particle.color), Color.blue(particle.color))
            canvas.drawCircle(particle.x, particle.y, 2f, paint)
        }

        // Draw health pickup based on type
        when (type) {
            "health_potion" -> drawHealthPotion(canvas, centerX, centerY, currentSize)
            "big_health" -> drawBigHealth(canvas, centerX, centerY, currentSize)
            "super_health" -> drawSuperHealth(canvas, centerX, centerY, currentSize)
            else -> drawHealthPotion(canvas, centerX, centerY, currentSize)
        }

        // Draw cross symbol
        paint.color = Color.WHITE
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        val crossSize = currentSize * 0.3f
        // Horizontal line
        canvas.drawLine(
            centerX - crossSize, centerY,
            centerX + crossSize, centerY,
            paint
        )
        // Vertical line
        canvas.drawLine(
            centerX, centerY - crossSize,
            centerX, centerY + crossSize,
            paint
        )
        paint.style = Paint.Style.FILL
    }

    private fun drawHealthPotion(canvas: Canvas, centerX: Float, centerY: Float, size: Float) {
        // Draw bottle
        paint.color = Color.rgb(150, 150, 200) // Glass color
        val bottleRect = RectF(
            centerX - size * 0.3f,
            centerY - size * 0.4f,
            centerX + size * 0.3f,
            centerY + size * 0.4f
        )
        canvas.drawRoundRect(bottleRect, size * 0.1f, size * 0.1f, paint)

        // Draw liquid
        paint.color = Color.rgb(255, 100, 150) // Pink healing liquid
        val liquidRect = RectF(
            bottleRect.left + size * 0.05f,
            bottleRect.top + size * 0.1f,
            bottleRect.right - size * 0.05f,
            bottleRect.bottom - size * 0.05f
        )
        canvas.drawRoundRect(liquidRect, size * 0.08f, size * 0.08f, paint)

        // Draw bottle neck
        paint.color = Color.rgb(120, 120, 160)
        canvas.drawRect(
            centerX - size * 0.1f,
            centerY - size * 0.5f,
            centerX + size * 0.1f,
            centerY - size * 0.3f,
            paint
        )
    }

    private fun drawBigHealth(canvas: Canvas, centerX: Float, centerY: Float, size: Float) {
        // Draw heart shape
        paint.color = Color.rgb(255, 100, 100) // Red heart

        // Left curve
        canvas.drawCircle(centerX - size * 0.15f, centerY - size * 0.1f, size * 0.2f, paint)
        // Right curve
        canvas.drawCircle(centerX + size * 0.15f, centerY - size * 0.1f, size * 0.2f, paint)

        // Bottom triangle
        val path = Path()
        path.moveTo(centerX - size * 0.3f, centerY)
        path.lineTo(centerX + size * 0.3f, centerY)
        path.lineTo(centerX, centerY + size * 0.3f)
        path.close()
        canvas.drawPath(path, paint)

        // Heart shine
        paint.color = Color.argb(150, 255, 200, 200)
        canvas.drawCircle(centerX - size * 0.1f, centerY - size * 0.15f, size * 0.1f, paint)
    }

    private fun drawSuperHealth(canvas: Canvas, centerX: Float, centerY: Float, size: Float) {
        // Draw golden orb
        paint.shader = RadialGradient(
            centerX, centerY, size * 0.4f,
            intArrayOf(
                Color.rgb(255, 255, 200), // Light gold
                Color.rgb(255, 215, 0),   // Gold
                Color.rgb(184, 134, 11)   // Dark gold
            ),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(centerX, centerY, size * 0.4f, paint)
        paint.shader = null

        // Draw sparkles around orb
        paint.color = Color.WHITE
        val sparkleCount = 8
        for (i in 0 until sparkleCount) {
            val angle = (i * 45f + animTimer * 50f) * PI / 180f
            val sparkleX = centerX + cos(angle).toFloat() * size * 0.6f
            val sparkleY = centerY + sin(angle).toFloat() * size * 0.6f
            canvas.drawCircle(sparkleX, sparkleY, 2f, paint)
        }
    }

    fun checkCollision(player: Player): Boolean {
        if (collected) return false

        val playerCenterX = player.x + player.width / 2f
        val playerCenterY = player.y + player.height / 2f
        val itemCenterX = x + SIZE / 2f
        val itemCenterY = y + SIZE / 2f

        val dx = playerCenterX - itemCenterX
        val dy = playerCenterY - itemCenterY
        val distance = sqrt(dx * dx + dy * dy)

        return distance < COLLECTION_RADIUS
    }

    fun collect(): Int {
        if (!collected) {
            collected = true
            return healAmount
        }
        return 0
    }
}
