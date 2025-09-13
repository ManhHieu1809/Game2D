package com.example.game2d.obstacles

import android.graphics.*
import com.example.game2d.Player
import com.example.game2d.resources.SpriteLoader
import kotlin.math.min

/**
 * Checkpoint.kt
 *
 * - Uses 3 resources (names below). If frames are not present, it will fallback to a single image.
 * - Supports scaling (scaleOverride) so you can make checkpoint bigger without "sinking" into ground.
 * - API:
 *    constructor: Checkpoint(x, y, activationRadius = 40f, scaleOverride = null)
 *    fun tryActivate(player: Player, radius: Float = activationRadius): Boolean
 *    fun tryActivate(px: Float, py: Float, radius: Float = activationRadius): Boolean
 *    fun update(dtMs: Long)
 *    fun draw(canvas: Canvas)
 *    fun getBounds(): RectF
 *
 * Resource names expected (put in assets/ or res/drawable so SpriteLoader can find them):
 * - checkpoint_pole.png          (single image)
 * - checkpoint_flag_out.png      (sheet, frames 64x64 each)  OR single image fallback
 * - checkpoint_flag_idle.png     (sheet, frames 64x64 each)  OR single image fallback
 */
class Checkpoint(
    var x: Float,
    var y: Float,
    private val activationRadius: Float = 40f,
    private val scaleOverride: Float? = null
) {
    private enum class State { NOT_VISITED, ACTIVATING, VISITED }
    private var state = State.NOT_VISITED
    var activated = false
        private set

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    companion object {
        private const val FRAME_W = 64
        private const val FRAME_H = 64

        // Default scale if not overridden in constructor
        private const val DEFAULT_SCALE = 1.3f

        // per-frame times (ms)
        private const val OUT_FRAME_MS = 60L
        private const val IDLE_FRAME_MS = 120L
    }

    // resource keys (change names if your files differ)
    private val RES_POLE = "checkpoint_pole"
    private val RES_FLAG_OUT = "checkpoint_flag_out"
    private val RES_FLAG_IDLE = "checkpoint_flag_idle"

    private val displayScale = scaleOverride ?: DEFAULT_SCALE
    private val displayW get() = FRAME_W * displayScale
    private val displayH get() = FRAME_H * displayScale

    // images/frames
    private var poleBmp: Bitmap? = null
    private val flagOutFrames = mutableListOf<Bitmap>()
    private val flagIdleFrames = mutableListOf<Bitmap>()

    // animation
    private var animTimer = 0L
    private var animFrame = 0

    init {
        // Adjust Y so the bottom of the scaled sprite lines up with the original expected ground.
        // If your TileMap calls new Checkpoint(x, groundTopY - ORIGINAL_POLE_HEIGHT) use that same startY
        // originalPoleHeightApprox is an approximation used by older code; tweak if needed.
        val originalPoleHeightApprox = 40f
        y = y - (displayH - originalPoleHeightApprox)

        // load pole single bitmap
        poleBmp = SpriteLoader.get(RES_POLE)

        // load flag_out frames safely (SpriteLoader.getFrames returns Array<Bitmap?>)
        try {
            val outArr: Array<Bitmap?> = SpriteLoader.getFrames(RES_FLAG_OUT)
            val outList: List<Bitmap> = outArr.filterNotNull()
            if (outList.isNotEmpty()) {
                flagOutFrames.addAll(outList)
            } else {
                SpriteLoader.get(RES_FLAG_OUT)?.let { flagOutFrames.add(it) }
            }
        } catch (e: Exception) {
            // fallback if getFrames throws or resource missing
            SpriteLoader.get(RES_FLAG_OUT)?.let { flagOutFrames.add(it) }
        }

        // load flag_idle frames safely
        try {
            val idleArr: Array<Bitmap?> = SpriteLoader.getFrames(RES_FLAG_IDLE)
            val idleList: List<Bitmap> = idleArr.filterNotNull()
            if (idleList.isNotEmpty()) {
                flagIdleFrames.addAll(idleList)
            } else {
                SpriteLoader.get(RES_FLAG_IDLE)?.let { flagIdleFrames.add(it) }
            }
        } catch (e: Exception) {
            SpriteLoader.get(RES_FLAG_IDLE)?.let { flagIdleFrames.add(it) }
        }

        // pixel-art friendly
        paint.isFilterBitmap = false
        paint.isDither = false
    }

    fun getBounds(): RectF = RectF(x, y, x + displayW, y + displayH)

    /** Legacy: activate by coordinates (px,py) */
    fun tryActivate(px: Float, py: Float, radius: Float = activationRadius): Boolean {
        val dx = px - (x + displayW / 2f)
        val dy = py - (y + displayH / 2f)
        if (dx * dx + dy * dy < radius * radius) {
            activate(null)
            return true
        }
        return false
    }

    /** Activate by Player (preferred) - also attempts to set player's spawn via reflection (safe) */
    fun tryActivate(player: Player, radius: Float = activationRadius): Boolean {
        val px = player.x + player.width / 2f
        val py = player.y + player.height / 2f
        val dx = px - (x + displayW / 2f)
        val dy = py - (y + displayH / 2f)
        if (dx * dx + dy * dy < radius * radius) {
            activate(player)
            return true
        }
        return false
    }

    private fun activate(player: Player?) {
        if (activated) return
        activated = true
        state = State.ACTIVATING
        animFrame = 0
        animTimer = 0L

        // attempt to set player's spawn point (if Player exposes spawnX/spawnY fields)
        if (player != null) {
            try {
                val cls = player.javaClass
                val fx = try { cls.getDeclaredField("spawnX") } catch (_: NoSuchFieldException) { null }
                val fy = try { cls.getDeclaredField("spawnY") } catch (_: NoSuchFieldException) { null }
                fx?.let { it.isAccessible = true; it.set(player, x) }
                fy?.let { it.isAccessible = true; it.set(player, y + displayH) } // spawn on ground
            } catch (_: Throwable) {
                // ignore - optional feature
            }
        }
    }

    fun update(dtMs: Long) {
        when (state) {
            State.ACTIVATING -> {
                if (flagOutFrames.isEmpty()) {
                    state = State.VISITED
                    animFrame = 0
                    animTimer = 0L
                } else {
                    animTimer += dtMs
                    if (animTimer >= OUT_FRAME_MS) {
                        val steps = (animTimer / OUT_FRAME_MS).toInt()
                        animTimer -= steps * OUT_FRAME_MS
                        animFrame += steps
                        if (animFrame >= flagOutFrames.size) {
                            state = State.VISITED
                            animFrame = 0
                            animTimer = 0L
                        }
                    }
                }
            }
            State.VISITED -> {
                if (flagIdleFrames.isEmpty()) {
                    animFrame = 0
                } else {
                    animTimer += dtMs
                    if (animTimer >= IDLE_FRAME_MS) {
                        val steps = (animTimer / IDLE_FRAME_MS).toInt()
                        animTimer -= steps * IDLE_FRAME_MS
                        animFrame = (animFrame + steps) % flagIdleFrames.size
                    }
                }
            }
            else -> {
                // NOT_VISITED: nothing to animate
            }
        }
    }

    fun draw(canvas: Canvas) {
        // draw pole if present
        poleBmp?.let {
            val dstPole = RectF(x, y, x + it.width * displayScale, y + it.height * displayScale)
            canvas.drawBitmap(it, null, dstPole, paint)
        } ?: run {
            val p = Paint(paint)
            p.color = if (activated) Color.GREEN else Color.DKGRAY
            canvas.drawRect(x, y, x + 6f * displayScale, y + displayH, p)
        }

        // draw flag depending on state
        when (state) {
            State.NOT_VISITED -> { /* no flag */ }
            State.ACTIVATING -> {
                val idx = animFrame.coerceAtMost(flagOutFrames.size - 1)
                val bmp = flagOutFrames.getOrNull(idx)
                bmp?.let {
                    val dst = RectF(x, y, x + displayW, y + displayH)
                    canvas.drawBitmap(it, null, dst, paint)
                }
            }
            State.VISITED -> {
                val idx = if (flagIdleFrames.isNotEmpty()) animFrame % flagIdleFrames.size else 0
                val bmp = flagIdleFrames.getOrNull(idx)
                bmp?.let {
                    val dst = RectF(x, y, x + displayW, y + displayH)
                    canvas.drawBitmap(it, null, dst, paint)
                }
            }
        }
    }
}
