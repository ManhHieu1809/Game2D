package com.example.game2d

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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

    // paint for bitmap drawing: nearest neighbor (no filtering)
    private val paintBitmap = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        isDither = false
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

        width = width.coerceIn(16f, 128f)
        height = height.coerceIn(16f, 128f)
        Log.d("ASSET_DBG", "Player init: idle=${bmpIdle != null}...pJump != null}, fall=${bmpFall != null}, size=${width}x$height")
    }

    private fun loadBmp(ctx: Context, name: String): Bitmap? {
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        Log.d("ASSET_DBG", "try load '$name' -> id=$id")
        if (id == 0) return null
        return try {
            val opts = BitmapFactory.Options().apply {
                inScaled = false // prevent automatic density scaling
                inPreferredConfig = Bitmap.Config.ARGB_8888
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

        val dst = RectF(x, y, x + width, y + height)

        if (bmp != null) {
            // number of frames for this bitmap
            val frames = if (bmp === bmpRun) framesRun else framesIdle
            val fw = if (frames > 0) (bmp.width / frames) else bmp.width
            val fh = bmp.height
            val frameIndex = if (frames > 0) (curFrame % frames) else 0
            val srcLeft = frameIndex * fw
            // avoid bleeding from adjacent frames: ensure last frame uses bitmap.width as right edge
            val srcRight = if (frames > 0 && frameIndex == frames - 1) bmp.width else (srcLeft + fw)
            val src = android.graphics.Rect(srcLeft, 0, srcRight, fh)

            if (facing < 0) {
                canvas.save()
                canvas.scale(-1f, 1f, dst.left + dst.width()/2f, dst.top + dst.height()/2f)
                canvas.drawBitmap(bmp, src, dst, paintBitmap)
                canvas.restore()
            } else {
                canvas.drawBitmap(bmp, src, dst, paintBitmap)
            }
        } else {
            paint.style = Paint.Style.FILL
            paint.color = android.graphics.Color.MAGENTA
            canvas.drawRect(dst, paint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 12f
            canvas.drawText("NO SPRITE", dst.left, dst.top - 6f, paint)
        }
    }
}
