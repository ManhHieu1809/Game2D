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

    private var spriteScale = 1.6f

    var width = 48f
    var height = 64f

    private var moveSpeed = 220f
    private var jumpPower = -620f
    val gravity = 1600f

    // Potion effects system
    private var jumpBoostEndTime = 0L
    private var speedBoostEndTime = 0L
    private var magnetEndTime = 0L
    private var jumpBoostActive = false
    private var speedBoostActive = false
    private var magnetActive = false

    // Original values for restoration
    private val originalMoveSpeed = 220f
    private val originalJumpPower = -620f

    // Character sprite sets - based on your images
    private val characterSprites = mapOf(
        0 to "idle",
        1 to "pink_idle",   // Pink Man
        2 to "virtual_idle", // Virtual Guy
        3 to "mask_idle"    // Mask Dude
    )

    private var currentCharacter = 0
    private var spritePrefix = "idle"

    // Animation bitmaps for current character
    private var bmpIdle: Bitmap? = null
    private var bmpRun: Bitmap? = null
    private var bmpJump: Bitmap? = null
    private var bmpFall: Bitmap? = null

    private var idleFrames = 1
    private var runFrames = 1
    private var jumpFrames = 1
    private var fallFrames = 1

    private var idleFrameW = 0
    private var idleFrameH = 0
    private var runFrameW = 0
    private var runFrameH = 0
    private var jumpFrameW = 0
    private var jumpFrameH = 0
    private var fallFrameW = 0
    private var fallFrameH = 0

    private var curFrame = 0
    private var timer = 0L
    private val frameDt = 100L

    private val paintBitmap = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        isDither = false
    }

    private var movingState = 0
    private var facing = 1

    init {
        // Get selected character from preferences
        val prefs = ctx.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        currentCharacter = prefs.getInt("selected_character", 0)

        loadCharacterSprites(ctx)
    }

    private fun loadCharacterSprites(ctx: Context) {
        // Get the sprite prefix for current character
        spritePrefix = characterSprites[currentCharacter] ?: "idle"

        // Load all animation states for the selected character
        bmpIdle = loadBmp(ctx, "${spritePrefix}_32x32")
        bmpRun = loadBmp(ctx, "${spritePrefix.replace("idle", "run")}_32x32")
        bmpJump = loadBmp(ctx, "${spritePrefix.replace("idle", "jump")}_32x32")
        bmpFall = loadBmp(ctx, "${spritePrefix.replace("idle", "fall")}_32x32")

        // Fallback: if specialized animations don't exist, use idle
        if (bmpRun == null) bmpRun = bmpIdle
        if (bmpJump == null) bmpJump = bmpIdle
        if (bmpFall == null) bmpFall = bmpIdle

        // Calculate frame dimensions for each animation
        calculateFrameDimensions()

        // Set player world size based on sprite dimensions
        if (idleFrameW > 0 && idleFrameH > 0) {
            width = idleFrameW * spriteScale
            height = idleFrameH * spriteScale
        } else {
            width = 48f
            height = 64f
        }

        Log.d("PLAYER_CHAR", "Loaded character $currentCharacter ($spritePrefix) - size: ${width}x${height}")
    }

    private fun calculateFrameDimensions() {
        // Idle animation
        bmpIdle?.let { b ->
            idleFrameH = b.height
            idleFrameW = if (b.width >= b.height && b.width % b.height == 0) b.height else b.width
            idleFrames = if (idleFrameH > 0) (b.width / idleFrameW) else 1
        }

        // Run animation
        bmpRun?.let { b ->
            runFrameH = b.height
            runFrameW = if (b.width >= b.height && b.width % b.height == 0) b.height else b.width
            runFrames = if (runFrameH > 0) (b.width / runFrameW) else 1
        }

        // Jump animation
        bmpJump?.let { b ->
            jumpFrameH = b.height
            jumpFrameW = if (b.width >= b.height && b.width % b.height == 0) b.height else b.width
            jumpFrames = if (jumpFrameH > 0) (b.width / jumpFrameW) else 1
        }

        // Fall animation
        bmpFall?.let { b ->
            fallFrameH = b.height
            fallFrameW = if (b.width >= b.height && b.width % b.height == 0) b.height else b.width
            fallFrames = if (fallFrameH > 0) (b.width / fallFrameW) else 1
        }
    }

    private fun loadBmp(ctx: Context, name: String): Bitmap? {
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (id == 0) {
            Log.w("PLAYER_ASSET", "Sprite not found: $name")
            return null
        }
        return try {
            val opts = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeResource(ctx.resources, id, opts)
        } catch (e: Exception) {
            Log.e("PLAYER_ASSET", "Failed to load $name: $e")
            null
        }
    }

    fun setMoving(dir: Int) {
        movingState = dir
        vx = when (dir) {
            -1 -> -moveSpeed
            1 -> moveSpeed
            else -> 0f
        }
        if (dir != 0) facing = if (dir > 0) 1 else -1
    }

    fun jump() {
        if (vy == 0f) vy = jumpPower
    }

    fun update(dtMs: Long, map: TileMapInterface) {
        val dt = dtMs / 1000f

        prevX = x
        prevY = y

        vy += gravity * dt

        x += vx * dt
        y += vy * dt

        val maxSpeed = 420f
        vx = vx.coerceIn(-maxSpeed, maxSpeed)

        map.resolvePlayerCollision(this)

        // Animation logic
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
        curFrame = curFrame % targetFrames

        updatePotionEffects()
    }

    fun getRect() = RectF(x, y, x + width, y + height)

    fun draw(canvas: Canvas, paint: Paint) {
        val drawX = round(x)
        val drawY = round(y)
        val drawW = round(width)
        val drawH = round(height)
        val dst = RectF(drawX, drawY, drawX + drawW, drawY + drawH)

        // Choose current animation based on state
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
            // Fallback colored rectangle for each character
            paint.style = Paint.Style.FILL
            paint.color = when (currentCharacter) {
                1 -> android.graphics.Color.MAGENTA  // Pink Man
                2 -> android.graphics.Color.CYAN     // Virtual Guy
                3 -> android.graphics.Color.YELLOW   // Mask Dude
                else -> android.graphics.Color.GREEN // Ninja Frog
            }
            canvas.drawRect(dst, paint)
        }
    }

    // Potion effect methods
    fun applyJumpBoost(duration: Long) {
        jumpBoostActive = true
        jumpBoostEndTime = System.currentTimeMillis() + duration
        jumpPower = originalJumpPower * 1.5f // 50% increase
    }

    fun applySpeedBoost(duration: Long) {
        speedBoostActive = true
        speedBoostEndTime = System.currentTimeMillis() + duration
        moveSpeed = originalMoveSpeed * 1.4f // 40% increase
    }

    fun applyCoinMagnet(duration: Long) {
        magnetActive = true
        magnetEndTime = System.currentTimeMillis() + duration
    }

    private fun updatePotionEffects() {
        val currentTime = System.currentTimeMillis()

        // Update jump boost
        if (jumpBoostActive && currentTime >= jumpBoostEndTime) {
            jumpBoostActive = false
            jumpPower = originalJumpPower
        }

        // Update speed boost
        if (speedBoostActive && currentTime >= speedBoostEndTime) {
            speedBoostActive = false
            moveSpeed = originalMoveSpeed
        }

        // Update magnet
        if (magnetActive && currentTime >= magnetEndTime) {
            magnetActive = false
        }
    }

    // Check if effects are active
    fun hasJumpBoost(): Boolean = jumpBoostActive

    fun hasSpeedBoost(): Boolean = speedBoostActive

    fun hasCoinMagnet(): Boolean = magnetActive

    // Get remaining time for countdown timers
    fun getJumpBoostRemainingTime(): Long {
        return if (jumpBoostActive) {
            (jumpBoostEndTime - System.currentTimeMillis()).coerceAtLeast(0L)
        } else 0L
    }

    fun getSpeedBoostRemainingTime(): Long {
        return if (speedBoostActive) {
            (speedBoostEndTime - System.currentTimeMillis()).coerceAtLeast(0L)
        } else 0L
    }

    fun getMagnetRemainingTime(): Long {
        return if (magnetActive) {
            (magnetEndTime - System.currentTimeMillis()).coerceAtLeast(0L)
        } else 0L
    }

    fun getMagnetRange(): Float = 220f

    private data class Quad(val bmp: Bitmap?, val frames: Int, val frameW: Int, val frameH: Int)
}