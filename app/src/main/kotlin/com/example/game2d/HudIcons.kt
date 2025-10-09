package com.example.game2d

import android.graphics.*

object HudIcons {

    // Vẽ icon tim (heart) cho lives
    fun drawHeart(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint, filled: Boolean = true) {
        val path = Path()

        // Tạo hình tim
        val heartSize = size
        val centerX = x + heartSize / 2f
        val centerY = y + heartSize / 2f

        // Left curve
        path.moveTo(centerX, centerY + heartSize * 0.3f)
        path.cubicTo(
            centerX - heartSize * 0.5f, centerY - heartSize * 0.2f,
            centerX - heartSize * 0.5f, centerY - heartSize * 0.5f,
            centerX - heartSize * 0.25f, centerY - heartSize * 0.5f
        )

        // Right curve
        path.cubicTo(
            centerX, centerY - heartSize * 0.7f,
            centerX + heartSize * 0.25f, centerY - heartSize * 0.5f,
            centerX + heartSize * 0.25f, centerY - heartSize * 0.5f
        )

        path.cubicTo(
            centerX + heartSize * 0.5f, centerY - heartSize * 0.5f,
            centerX + heartSize * 0.5f, centerY - heartSize * 0.2f,
            centerX, centerY + heartSize * 0.3f
        )

        path.close()

        if (filled) {
            paint.style = Paint.Style.FILL
            paint.color = Color.RED
            canvas.drawPath(path, paint)

            // Add shine effect
            paint.color = Color.rgb(255, 150, 150)
            paint.style = Paint.Style.FILL
            val shinePath = Path()
            shinePath.moveTo(centerX - heartSize * 0.15f, centerY - heartSize * 0.3f)
            shinePath.cubicTo(
                centerX - heartSize * 0.05f, centerY - heartSize * 0.4f,
                centerX + heartSize * 0.05f, centerY - heartSize * 0.35f,
                centerX + heartSize * 0.1f, centerY - heartSize * 0.25f
            )
            shinePath.cubicTo(
                centerX, centerY - heartSize * 0.15f,
                centerX - heartSize * 0.1f, centerY - heartSize * 0.2f,
                centerX - heartSize * 0.15f, centerY - heartSize * 0.3f
            )
            canvas.drawPath(shinePath, paint)
        } else {
            // Empty heart (grey)
            paint.style = Paint.Style.FILL
            paint.color = Color.GRAY
            canvas.drawPath(path, paint)

            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            paint.color = Color.DKGRAY
            canvas.drawPath(path, paint)
        }
    }

    // Vẽ icon đồng xu (coin) cho coins
    fun drawCoin(canvas: Canvas, x: Float, y: Float, size: Float, paint: Paint, animationTime: Long = 0L) {
        val centerX = x + size / 2f
        val centerY = y + size / 2f
        val radius = size / 2f

        // Animation effect - coin rotation (slower)
        val rotationPhase = (animationTime * 0.002f) % (2 * Math.PI.toFloat())
        val scaleX = kotlin.math.abs(kotlin.math.cos(rotationPhase.toDouble())).toFloat().coerceAtLeast(0.3f)

        canvas.save()
        canvas.scale(scaleX, 1f, centerX, centerY)

        // Outer ring - bright gold with gradient effect
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = RadialGradient(
                centerX, centerY, radius,
                intArrayOf(
                    Color.rgb(255, 235, 59),  // Bright yellow center
                    Color.rgb(255, 193, 7),   // Gold middle
                    Color.rgb(255, 152, 0)    // Orange edge
                ),
                floatArrayOf(0f, 0.7f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(centerX, centerY, radius, goldPaint)

        // Inner ring - darker gold for depth
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(218, 165, 32)
        paint.shader = null
        canvas.drawCircle(centerX, centerY, radius * 0.75f, paint)

        // Center circle - bright highlight
        paint.color = Color.rgb(255, 235, 59)
        canvas.drawCircle(centerX, centerY, radius * 0.5f, paint)

        // Dollar sign in center
        paint.color = Color.rgb(184, 134, 11)
        paint.textSize = size * 0.5f
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.setShadowLayer(2f, 1f, 1f, Color.BLACK)

        val textY = centerY - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText("$", centerX, textY, paint)
        paint.clearShadowLayer()

        // Outer highlight ring
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.rgb(255, 245, 157)
        canvas.drawCircle(centerX, centerY, radius * 0.9f, paint)

        // Inner highlight ring
        paint.strokeWidth = 2f
        paint.color = Color.rgb(255, 235, 59)
        canvas.drawCircle(centerX, centerY, radius * 0.6f, paint)

        canvas.restore()
    }

    // Vẽ progress bar cho bất tử
    fun drawInvulnerabilityBar(canvas: Canvas, x: Float, y: Float, width: Float, height: Float,
                               progress: Float, paint: Paint) {
        // Background
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(100, 255, 255, 255)
        canvas.drawRoundRect(x, y, x + width, y + height, height/2f, height/2f, paint)

        // Progress bar
        val progressWidth = width * progress.coerceIn(0f, 1f)
        paint.color = Color.rgb(0, 150, 255) // Blue
        canvas.drawRoundRect(x, y, x + progressWidth, y + height, height/2f, height/2f, paint)

        // Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = Color.WHITE
        canvas.drawRoundRect(x, y, x + width, y + height, height/2f, height/2f, paint)

        // Text
        paint.style = Paint.Style.FILL
        paint.textSize = height * 0.6f
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.WHITE
        paint.setShadowLayer(2f, 1f, 1f, Color.BLACK)

        val textY = y + height/2f - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText("INVULNERABLE", x + width/2f, textY, paint)
        paint.clearShadowLayer()
    }
}
