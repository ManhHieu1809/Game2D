package com.example.game2d

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.Log
import kotlin.math.round

class Player(ctx: Context, sx: Float, sy: Float) {
    var x = sx
    var y = sy
    var prevX = x
    var prevY = y
    var vx = 0f
    var vy = 0f

    // Tùy chỉnh hiển thị sprite: tăng/giảm để nhân vật to/nhỏ
    private var spriteScale = 1.6f

    // world size (được set sau khi đọc bitmap)
    var width = 48f
    var height = 64f

    private val moveSpeed = 220f
    private val jumpPower = -620f
    val gravity = 1600f

    // Bitmaps cho từng animation
    private var bmpIdle: Bitmap? = null
    private var idleFrames = 1
    private var idleFrameW = 0
    private var idleFrameH = 0

    private var bmpRun: Bitmap? = null
    private var runFrames = 1
    private var runFrameW = 0
    private var runFrameH = 0

    private var bmpJump: Bitmap? = null
    private var jumpFrames = 1
    private var jumpFrameW = 0
    private var jumpFrameH = 0

    private var bmpFall: Bitmap? = null
    private var fallFrames = 1
    private var fallFrameW = 0
    private var fallFrameH = 0

    private var curFrame = 0
    private var timer = 0L
    private val frameDt = 100L // ms giữa frame — tăng nếu animation quá nhanh

    private val paintBitmap = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        isDither = false
    }

    private var movingState = 0 // -1 left, 0 idle, 1 right
    private var facing = 1

    init {
        // Tên resource phải trùng với tên file trong res/drawable(-nodpi)
        bmpIdle = loadBmp(ctx, "idle_32x32")
        bmpRun  = loadBmp(ctx, "run_32x32")
        bmpJump = loadBmp(ctx, "jump_32x32")
        bmpFall = loadBmp(ctx, "fall_32x32")

        // detect frames & frame size cho từng sheet
        bmpIdle?.let { b ->
            idleFrameH = b.height
            idleFrameW = if (b.width >= b.height && b.width % b.height == 0) b.height else b.width
            idleFrames = if (idleFrameH>0) (b.width / idleFrameW) else 1
        }
        bmpRun?.let { b ->
            runFrameH = b.height
            runFrameW = if (b.width >= b.height && b.width % b.height == 0) b.height else b.width
            runFrames = if (runFrameH>0) (b.width / runFrameW) else 1
        }
        bmpJump?.let { b ->
            jumpFrameH = b.height
            jumpFrameW = b.width
            jumpFrames = 1
        }
        bmpFall?.let { b ->
            fallFrameH = b.height
            fallFrameW = b.width
            fallFrames = 1
        }


        if (idleFrameW > 0 && idleFrameH > 0) {
            width = idleFrameW * spriteScale
            height = idleFrameH * spriteScale
        } else if (runFrameW > 0 && runFrameH > 0) {
            width = runFrameW * spriteScale
            height = runFrameH * spriteScale
        } else {
            width = 48f
            height = 64f
        }
        Log.d("PLAYER_INIT","player world size ${width}x${height} - idleFrames=$idleFrames runFrames=$runFrames")
    }

    private fun loadBmp(ctx: Context, name: String): Bitmap? {
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (id == 0) {
            return null
        }
        return try {
            val opts = BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 }
            BitmapFactory.decodeResource(ctx.resources, id, opts)
        } catch (e: Exception) {
            Log.e("PLAYER_ASSET", "decodeResource failed: $e")
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

        // FIXED: Store previous position BEFORE updating
        prevX = x
        prevY = y

        // Apply gravity
        vy += gravity * dt

        // Apply movement
        x += vx * dt
        y += vy * dt

        // Cap horizontal speed to reduce tunneling
        val maxSpeed = 420f
        vx = vx.coerceIn(-maxSpeed, maxSpeed)

        // Collision resolution - this will adjust x, y if needed
        map.resolvePlayerCollision(this)

        // Animation timer & frame index (choose frames by current state)
        timer += dtMs
        val targetFrames = when {
            vy < 0f -> jumpFrames
            vy > 0f -> fallFrames
            movingState != 0 -> runFrames
            else -> idleFrames
        }.coerceAtLeast(1)

        if (timer >= frameDt) {
            curFrame = (curFrame + 1) % targetFrames
            timer = 0
        }
        // ensure curFrame within targetFrames
        curFrame = curFrame % targetFrames
    }

    fun getRect() = RectF(x, y, x + width, y + height)

    fun draw(canvas: Canvas, paint: Paint) {
        val drawX = round(x)
        val drawY = round(y)
        val drawW = round(width)
        val drawH = round(height)
        val dst = RectF(drawX, drawY, drawX + drawW, drawY + drawH)

        // choose current sheet & frame info
        val (sheet, fCount, fW, fH) = when {
            vy < 0f -> Quad(bmpJump, jumpFrames, jumpFrameW, jumpFrameH)
            vy > 0f -> Quad(bmpFall, fallFrames, fallFrameW, fallFrameH)
            movingState != 0 -> Quad(bmpRun, runFrames, runFrameW, runFrameH)
            else -> Quad(bmpIdle, idleFrames, idleFrameW, idleFrameH)
        }

        sheet?.let { b ->
            val framesCount = fCount.coerceAtLeast(1)
            val fw = if (fW > 0) fW else b.width
            val fh = if (fH > 0) fH else b.height
            val frameIndex = (curFrame % framesCount)
            val srcLeft = frameIndex * fw
            val src = Rect(srcLeft, 0, srcLeft + fw, fh)

            canvas.save()
            if (facing < 0) {
                canvas.scale(-1f, 1f, dst.centerX(), dst.centerY())
            }
            canvas.drawBitmap(b, src, dst, paintBitmap)
            canvas.restore()
        } ?: run {
            // fallback rectangle
            paint.style = Paint.Style.FILL
            paint.color = android.graphics.Color.RED
            canvas.drawRect(dst, paint)
        }
    }

    // small data class-like holder
    private data class Quad(val bmp: Bitmap?, val frames: Int, val frameW: Int, val frameH: Int)
}