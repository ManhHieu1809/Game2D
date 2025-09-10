package com.example.game2d

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Rect
import android.util.Log
import kotlin.math.max

class Player(ctx: Context, sx: Float, sy: Float) {
    var x = sx
    var y = sy
    var vx = 0f
    var vy = 0f
    var width = 48f
    var height = 64f

    private val moveSpeed = 220f
    private val jumpPower = -620f
    val gravity = 1600f

    private var bmpIdle: Bitmap? = null
    private var bmpRun: Bitmap? = null
    private var bmpJump: Bitmap? = null
    private var bmpFall: Bitmap? = null
    private var framesIdle = 1
    private var framesRun = 1
    private var curFrame = 0
    private var timer = 0L
    private val frameDt = 120L

    private var facing = 1
    private var movingState = 0

    // current animation frame count (kept in update to keep curFrame in range)
    private var animFrames = 1

    // paint for bitmap drawing: nearest neighbor (no filtering) - FIXED
    private val paintBitmap = Paint().apply {
        isFilterBitmap = false  // Tắt filtering để tránh blur
        isAntiAlias = false     // Tắt anti-alias cho pixel art
        isDither = false        // Tắt dithering
    }

    init {
        bmpIdle = loadBmp(ctx, "idle_32x32")
        bmpRun  = loadBmp(ctx, "run_32x32")
        bmpJump = loadBmp(ctx, "jump_32x32")
        bmpFall = loadBmp(ctx, "fall_32x32")

        bmpIdle?.let { b ->
            if (b.height > 0 && b.width >= b.height) framesIdle = b.width / b.height
            width = (b.width / framesIdle).toFloat()
            height = b.height.toFloat()
        } ?: run {
            bmpRun?.let { b ->
                if (b.height > 0 && b.width >= b.height) framesRun = b.width / b.height
                width = (b.width / max(1, framesRun)).toFloat()
                height = b.height.toFloat()
            }
        }

        // Đảm bảo kích thước hợp lý cho Mario-style game
        width = width.coerceIn(32f, 64f)
        height = height.coerceIn(32f, 64f)
        Log.d("ASSET_DBG", "Player init: idle=${bmpIdle != null}, run=${bmpRun != null}, jump=${bmpJump != null}, fall=${bmpFall != null}, size=${width}x$height")
    }

    private fun loadBmp(ctx: Context, name: String): Bitmap? {
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        Log.d("ASSET_DBG", "try load '$name' -> id=$id")
        if (id == 0) return null
        return try {
            val opts = BitmapFactory.Options().apply {
                inScaled = false // prevent automatic density scaling
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inDither = false // Tắt dithering
                inPreferQualityOverSpeed = true
            }
            BitmapFactory.decodeResource(ctx.resources, id, opts)
        } catch (e: Exception) {
            Log.e("ASSET_DBG", "decode error $name: ${e.message}")
            null
        }
    }

    fun setMoving(dir: Int) {
        movingState = dir
        vx = when (dir) { -1 -> -moveSpeed; 1 -> moveSpeed; else -> 0f }
        if (dir != 0) facing = if (dir > 0) 1 else -1
    }

    fun jump() {
        if (vy == 0f) vy = jumpPower
    }

    fun update(dtMs: Long, map: TileMap) {
        val dt = dtMs / 1000f
        vy += gravity * dt
        x += vx * dt
        y += vy * dt

        if (x < 0f) { x = 0f; vx = 0f }
        if (x + width > map.worldWidth) { x = map.worldWidth - width; vx = 0f }

        map.resolvePlayerCollision(this)

        timer += dtMs
        // determine number of frames for current animation and advance curFrame accordingly
        animFrames = when {
            vy != 0f && vy < 0f -> 1
            vy != 0f && vy > 0f -> 1
            movingState != 0 -> framesRun.coerceAtLeast(1)
            else -> framesIdle.coerceAtLeast(1)
        }
        if (timer >= frameDt) {
            curFrame = (curFrame + 1) % animFrames
            timer = 0
        }
    }

    fun getRect() = RectF(x, y, x + width, y + height)

    fun draw(canvas: Canvas, paint: Paint) {
        val bmp = when {
            vy != 0f && vy < 0f -> bmpJump
            vy != 0f && vy > 0f -> bmpFall
            movingState != 0 -> bmpRun
            else -> bmpIdle
        }

        // Làm tròn tọa độ để tránh sub-pixel rendering gây sọc
        val drawX = kotlin.math.round(x)
        val drawY = kotlin.math.round(y)
        val drawWidth = kotlin.math.round(width)
        val drawHeight = kotlin.math.round(height)

        val dst = RectF(drawX, drawY, drawX + drawWidth, drawY + drawHeight)

        if (bmp != null) {
            // number of frames for this bitmap
            val frames = when (bmp) {
                bmpRun -> framesRun
                bmpIdle -> framesIdle
                else -> 1
            }

            val fw = if (frames > 0) (bmp.width / frames) else bmp.width
            val fh = bmp.height
            val frameIndex = if (frames > 0) (curFrame % frames) else 0
            val srcLeft = frameIndex * fw

            // FIXED: Đảm bảo không có frame bleeding
            val srcRight = kotlin.math.min(srcLeft + fw, bmp.width)
            val src = Rect(srcLeft, 0, srcRight, fh)

            if (facing < 0) {
                canvas.save()
                canvas.scale(-1f, 1f, dst.centerX(), dst.centerY())
                canvas.drawBitmap(bmp, src, dst, paintBitmap)
                canvas.restore()
            } else {
                canvas.drawBitmap(bmp, src, dst, paintBitmap)
            }
        } else {
            // Fallback rectangle nếu không có sprite
            paint.style = Paint.Style.FILL
            paint.color = android.graphics.Color.RED
            canvas.drawRect(dst, paint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 12f
            canvas.drawText("MARIO", dst.left, dst.top - 6f, paint)
        }
    }
}