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
    private var bmpIdleFlipped: Bitmap? = null
    private var bmpRunFlipped: Bitmap? = null
    private var bmpJumpFlipped: Bitmap? = null
    private var bmpFallFlipped: Bitmap? = null
    private var framesIdle = 1
    private var framesRun = 10 // Cập nhật cứng giá trị dựa trên sprite sheet mới
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
        bmpRun = loadBmp(ctx, "run_32x32")
        bmpJump = loadBmp(ctx, "jump_32x32")
        bmpFall = loadBmp(ctx, "fall_32x32")

        val flipMatrix = android.graphics.Matrix().apply { preScale(-1f, 1f) }

        bmpIdle?.let { bmpIdleFlipped = Bitmap.createBitmap(it, 0, 0, it.width, it.height, flipMatrix, false) }
        bmpRun?.let { bmpRunFlipped = Bitmap.createBitmap(it, 0, 0, it.width, it.height, flipMatrix, false) }
        bmpJump?.let { bmpJumpFlipped = Bitmap.createBitmap(it, 0, 0, it.width, it.height, flipMatrix, false) }
        bmpFall?.let { bmpFallFlipped = Bitmap.createBitmap(it, 0, 0, it.width, it.height, flipMatrix, false) }

        bmpIdle?.let { b ->
            if (b.height > 0 && b.width >= b.height) framesIdle = b.width / b.height
            width = (b.width / framesIdle).toFloat()
            height = b.height.toFloat()
        } ?: run {
            bmpRun?.let { b ->
                if (b.height > 0 && b.width >= b.height) framesRun = b.width / b.height // Cập nhật tự động
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
        var bmp = when {
            vy != 0f && vy < 0f -> if (facing >= 0) bmpJump else bmpJumpFlipped
            vy != 0f && vy > 0f -> if (facing >= 0) bmpFall else bmpFallFlipped
            movingState != 0 -> if (facing >= 0) bmpRun else bmpRunFlipped
            else -> if (facing >= 0) bmpIdle else bmpIdleFlipped
        }

        // Làm tròn tọa độ và kích thước để tránh sub-pixel artifacts
        val drawX = kotlin.math.round(x).toInt().toFloat()
        val drawY = kotlin.math.round(y).toInt().toFloat()
        val drawWidth = kotlin.math.round(width).toInt().toFloat()
        val drawHeight = kotlin.math.round(height).toInt().toFloat()

        val dst = RectF(drawX, drawY, drawX + drawWidth, drawY + drawHeight)

        if (bmp != null) {
            val frames = when (bmp) {
                bmpRun, bmpRunFlipped -> framesRun
                bmpIdle, bmpIdleFlipped -> framesIdle
                else -> 1
            }

            val fw = if (frames > 0) (bmp.width / frames) else bmp.width
            val fh = bmp.height
            var frameIndex = if (frames > 0) (curFrame % frames) else 0

            // Nếu facing < 0, đảo ngược frame index vì sheet flipped đảo order
            if (facing < 0 && frames > 1) {
                frameIndex = frames - 1 - frameIndex
            }

            val srcLeft = frameIndex * fw
            val srcRight = kotlin.math.min(srcLeft + fw, bmp.width)
            val src = Rect(srcLeft, 0, srcRight, fh)

            // Debug log (xóa sau khi test)
            Log.d("RENDER_DBG", "Drawing frame=$frameIndex, srcLeft=$srcLeft, srcRight=$srcRight, facing=$facing")

            canvas.drawBitmap(bmp, src, dst, paintBitmap)
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