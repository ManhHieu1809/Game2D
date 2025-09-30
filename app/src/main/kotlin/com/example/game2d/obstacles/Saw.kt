package com.example.game2d.obstacles

import android.graphics.*
import com.example.game2d.Player
import com.example.game2d.resources.SpriteLoader
import kotlin.math.*

class Saw(var x: Float, var y: Float, var rotSpeed: Float = 180f) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var rotation = 0f
    private var sparkTimer = 0f
    private val sparks = mutableListOf<Spark>()

    // Spark effect class
    private data class Spark(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var life: Float,
        var maxLife: Float,
        var color: Int
    )

    fun update(dt: Float) {
        rotation += rotSpeed * dt
        if (rotation >= 360f) rotation -= 360f
        if (rotation < 0f) rotation += 360f

        // Update spark timer
        sparkTimer += dt
        if (sparkTimer >= 0.1f) { // Create spark every 0.1 seconds
            createSpark()
            sparkTimer = 0f
        }

        // Update existing sparks
        val iterator = sparks.iterator()
        while (iterator.hasNext()) {
            val spark = iterator.next()
            spark.x += spark.vx * dt
            spark.y += spark.vy * dt
            spark.vy += 200f * dt // Gravity effect
            spark.life -= dt

            if (spark.life <= 0f) {
                iterator.remove()
            }
        }
    }

    private fun createSpark() {
        val angle = Math.random() * 2 * PI
        val speed = 50f + Math.random() * 100f
        val sparkX = x + cos(angle) * 20f
        val sparkY = y + sin(angle) * 20f

        val colors = arrayOf(
            Color.rgb(255, 100, 0),  // Orange
            Color.rgb(255, 200, 0),  // Yellow
            Color.rgb(255, 50, 0),   // Red-orange
            Color.rgb(255, 255, 100) // Light yellow
        )

        sparks.add(Spark(
            x = sparkX.toFloat(),
            y = sparkY.toFloat(),
            vx = (cos(angle) * speed).toFloat(),
            vy = (sin(angle) * speed).toFloat(),
            life = 0.5f + Math.random().toFloat() * 0.5f,
            maxLife = 1f,
            color = colors[(Math.random() * colors.size).toInt()]
        ))
    }

    fun draw(canvas: Canvas) {
        val size = 48f
        val radius = size / 2f

        // Draw glow effect
        glowPaint.color = Color.argb(100, 255, 0, 0)
        canvas.drawCircle(x, y, radius + 8f, glowPaint)

        // Draw sparks
        for (spark in sparks) {
            val alpha = (spark.life / spark.maxLife * 255).toInt().coerceIn(0, 255)
            sparkPaint.color = Color.argb(alpha, Color.red(spark.color), Color.green(spark.color), Color.blue(spark.color))
            canvas.drawCircle(spark.x, spark.y, 2f, sparkPaint)
        }

        canvas.save()
        canvas.rotate(rotation, x, y)

        val sprite = SpriteLoader.get("saw")
        if (sprite != null) {
            val rect = RectF(x - radius, y - radius, x + radius, y + radius)
            paint.isFilterBitmap = true
            paint.isDither = true
            canvas.drawBitmap(sprite, null, rect, paint)
        } else {
            // Fallback: Draw detailed saw blade
            drawDetailedSaw(canvas, x, y, radius)
        }

        canvas.restore()

        // Draw danger indicator
        val dangerRadius = radius + 15f
        paint.color = Color.argb(100, 255, 0, 0)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(x, y, dangerRadius, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawDetailedSaw(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        // Draw saw blade body
        paint.color = Color.rgb(150, 150, 150)
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Draw saw teeth
        paint.color = Color.rgb(200, 200, 200)
        val numTeeth = 16
        val angleStep = 360f / numTeeth

        for (i in 0 until numTeeth) {
            val angle = Math.toRadians((i * angleStep).toDouble())
            val innerRadius = radius - 8f
            val outerRadius = radius + 4f

            val x1 = centerX + cos(angle) * innerRadius
            val y1 = centerY + sin(angle) * innerRadius
            val x2 = centerX + cos(angle) * outerRadius
            val y2 = centerY + sin(angle) * outerRadius

            canvas.drawLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), paint)
        }

        // Draw center hub
        paint.color = Color.rgb(100, 100, 100)
        canvas.drawCircle(centerX, centerY, radius * 0.3f, paint)

        // Draw metallic shine
        paint.color = Color.argb(100, 255, 255, 255)
        canvas.drawCircle(centerX - radius * 0.2f, centerY - radius * 0.2f, radius * 0.4f, paint)
    }

    fun isHit(player: Player): Boolean {
        val px = player.x + player.width/2f
        val py = player.y + player.height/2f
        val dx = px - x
        val dy = py - y
        val distance = sqrt(dx*dx + dy*dy)
        return distance < 30f
    }
}
