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

    private val moveSpeed = 220f
    private val jumpPower = -620f
    val gravity = 1600f

    // Character sprite sets - based on your images
    private val characterSprites = mapOf(
        0 to "idle",        // Ninja Frog (default green character)
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

    // Bullet system
    private val bullets = mutableListOf<Bullet>()
    private var lastShotTime = 0L
    private val shotCooldown = 300L // 300ms between shots

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

    fun shoot() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastShotTime >= shotCooldown) {
            // Create bullet at player center, slightly in front
            val bulletX = if (facing > 0) x + width else x - 8f
            val bulletY = y + height / 2f - 2f

            bullets.add(Bullet(bulletX, bulletY, facing))
            lastShotTime = currentTime

            try { SoundManager.playShot() } catch (e: Exception) { /* ignore */ }
        }
    }

    fun update(dtMs: Long, map: TileMap) {
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

        // Update bullets
        bullets.forEach { it.update(dtMs, 6400f) } // worldWidth from TileMap
        bullets.removeAll { !it.isActive() }
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

        // Draw bullets
        bullets.forEach { it.draw(canvas) }
    }

    fun getBullets(): List<Bullet> = bullets

    private data class Quad(val bmp: Bitmap?, val frames: Int, val frameW: Int, val frameH: Int)
}