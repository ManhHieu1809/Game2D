package com.example.game2d.entities

import android.graphics.*
import com.example.game2d.AppCtx
import com.example.game2d.Player
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Monster1 (fixed)
 * - Đã bổ sung stepAnim(...) để tránh Unresolved reference
 * - DISPLAY_SCALE = 1.6f (kích thước hiển thị)
 * - BULLET_SPEED = 120f
 * - Patrol chỉ khi player không thỏa điều kiện attack
 * - Khi kết thúc death animation -> alive = false (EntityManager sẽ remove)
 */

class Monster1(
    startX: Float,
    startY: Float,
    patrolWidth: Float = 160f,
    private val detectRange: Float = 420f
) : Entity() {

    // resource names
    private val RES_IDLE   = "monster1_idle"
    private val RES_RUN    = "monster1_run"
    private val RES_ATTACK = "monster1_attack"
    private val RES_HIT    = "monster1_hit"
    private val RES_BULLET = "bullet"
    private val RES_BHIT   = "bullet_hit"

    companion object {
        private const val FRAME_W = 64
        private const val FRAME_H = 32
        private const val RUN_FRAMES = 14
        private const val IDLE_FRAMES = 18
        private const val HIT_FRAMES = 5
        private const val ATTACK_FRAMES = 11

        // scale to 1.6x
        private const val DISPLAY_SCALE = 1.6f

        // attack tuning
        private const val ATTACK_RANGE = 220f
        private const val VERTICAL_THRESHOLD = 80f
        private const val IGNORE_ON_TOP_OFFSET = 6f

        // bullet tuning
        private const val BULLET_SPEED = 120f
        private const val BULLET_RANGE = 600f
    }

    private enum class State { IDLE, RUN, ATTACK, DEAD }
    private var state = State.IDLE
    var facingLeft = true

    // display sizes
    private val displayW get() = FRAME_W * DISPLAY_SCALE
    private val displayH get() = FRAME_H * DISPLAY_SCALE

    init {
        // adjust y so feet align with original ground y (TileMap used startY = groundTopY - FRAME_H)
        x = startX
        y = startY - (displayH - FRAME_H)
        vx = -42f
    }

    private val patrolSpeed = 42f
    private val leftBound = startX - patrolWidth / 2f
    private val rightBound = startX + patrolWidth / 2f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = false
        isDither = false
    }

    // frames
    private val idleFrames = mutableListOf<Bitmap>()
    private val runFrames = mutableListOf<Bitmap>()
    private val attackFrames = mutableListOf<Bitmap>()
    private val hitFrames = mutableListOf<Bitmap>()
    private val bulletHitFrames = mutableListOf<Bitmap>()
    private var bulletBmp: Bitmap? = null

    private val bullets = ArrayList<Bullet>()

    private var animTimer = 0L
    private var animFrame = 0
    private var fireTimer = 0L
    private var attackedThisCycle = false
    private var deathDone = false

    init {
        // load frames (loadStrip sẽ setHasAlpha + prepareToDraw cho từng frame)
        loadStrip(RES_IDLE, idleFrames, FRAME_W, FRAME_H, IDLE_FRAMES)
        loadStrip(RES_RUN, runFrames, FRAME_W, FRAME_H, RUN_FRAMES)
        loadStrip(RES_ATTACK, attackFrames, FRAME_W, FRAME_H, ATTACK_FRAMES)
        loadStrip(RES_HIT, hitFrames, FRAME_W, FRAME_H, HIT_FRAMES)
        loadStrip(RES_BHIT, bulletHitFrames, 16, 16, 2)
        bulletBmp = loadSingle(RES_BULLET)
    }

    override fun getBounds(): RectF = RectF(x, y, x + displayW, y + displayH)

    override fun update(dtMs: Long) {
        update(dtMs, null)
    }

    fun update(dtMs: Long, player: Player?) {
        if (!alive) return

        val dt = dtMs / 1000f

        // detect / attack flags
        var wantAttack = false
        var playerIsAbove = false
        var playerInDetect = false
        if (player != null) {
            val playerCenterX = player.x + player.width / 2f
            val playerCenterY = player.y + player.height / 2f
            val monsterCenterX = x + displayW / 2f
            val monsterCenterY = y + displayH / 2f
            val dx = playerCenterX - monsterCenterX
            val dy = playerCenterY - monsterCenterY

            playerIsAbove = (player.y + player.height) <= (y + IGNORE_ON_TOP_OFFSET)
            playerInDetect = abs(dx) <= detectRange
            val withinAttackRange = abs(dx) <= ATTACK_RANGE
            val nearSameRow = abs(dy) <= VERTICAL_THRESHOLD

            wantAttack = playerInDetect && withinAttackRange && nearSameRow && !playerIsAbove

            if (playerInDetect) facingLeft = dx < 0
        }

        when (state) {
            State.DEAD -> {
                vx = 0f
                // advance death animation; stepAnim sẽ set alive=false khi animation kết thúc
                stepAnim(dtMs, hitFrames, 80L)
            }
            State.ATTACK -> {
                vx = 0f
                stepAnim(dtMs, attackFrames, 60L) { idx: Int ->
                    if (!attackedThisCycle && idx >= 6) {
                        shoot()
                        attackedThisCycle = true
                    }
                    if (idx == 0) {
                        attackedThisCycle = false
                        state = if (wantAttack) State.ATTACK else State.IDLE
                        fireTimer = 0L
                    }
                }
            }
            else -> {
                // PATROL when no attack intent
                if (player == null || !wantAttack) {
                    vx = if (facingLeft) -patrolSpeed else patrolSpeed
                    x += vx * dt
                    if (x < leftBound) { x = leftBound; facingLeft = false }
                    if (x > rightBound) { x = rightBound; facingLeft = true }
                    state = if (abs(vx) > 1f) State.RUN else State.IDLE
                    fireTimer = max(0, (fireTimer + dtMs).toInt()).toLong()
                } else {
                    // player in attack area: build cooldown then attack
                    fireTimer += dtMs
                    if (fireTimer >= 900L) {
                        state = State.ATTACK
                    } else {
                        state = State.IDLE
                    }
                }

                // step run/idle anim
                val frames = if (state == State.RUN) runFrames else idleFrames
                val frameMs = if (state == State.RUN) 60L else 80L
                animTimer += dtMs
                if (frames.isNotEmpty() && animTimer >= frameMs) {
                    val steps = (animTimer / frameMs).toInt()
                    animTimer -= steps * frameMs
                    animFrame = (animFrame + steps) % frames.size
                }
            }
        }

        // bullets
        val it = bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.update(dtMs)
            if (!b.alive()) it.remove()
        }
    }

    override fun draw(canvas: Canvas) {
        if (!alive) return

        val frames = when (state) {
            State.DEAD -> hitFrames
            State.ATTACK -> attackFrames
            State.RUN -> runFrames
            else -> idleFrames
        }
        val bmp = if (frames.isNotEmpty()) frames[animFrame % frames.size] else null

        val centerX = x + displayW / 2f
        val centerY = y + displayH / 2f

        canvas.save()
        if (!facingLeft) canvas.scale(-1f, 1f, centerX, centerY)
        if (bmp != null) {
            val dst = RectF(x, y, x + displayW, y + displayH)
            paint.isFilterBitmap = false
            paint.isDither = false
            canvas.drawBitmap(bmp, Rect(0, 0, bmp.width, bmp.height), dst, paint)
        }
        canvas.restore()

        bullets.forEach { it.draw(canvas) }
    }

    fun tryStompBy(player: Player): Boolean {
        if (state == State.DEAD || !alive) return false
        val playerBottomNow = player.y + player.height
        val playerBottomPrev = player.prevY + player.height
        val monsterTop = y
        val falling = player.vy > 0f
        val wasAbove = playerBottomPrev <= monsterTop + 4f
        if (falling && wasAbove && playerBottomNow > monsterTop && player.x + player.width > x && player.x < x + displayW) {
            state = State.DEAD
            animFrame = 0
            animTimer = 0
            deathDone = false
            return true
        }
        return false
    }

    fun bulletHitPlayer(player: Player): Boolean {
        var hit = false
        bullets.forEach { b ->
            if (!b.exploded && RectF.intersects(b.bounds(), RectF(player.x, player.y, player.x + player.width, player.y + player.height))) {
                b.explode(); hit = true
            }
        }
        return hit
    }

    private fun shoot() {
        val sx = x + if (facingLeft) 6f else displayW - 6f
        val sy = y + displayH * 0.45f
        bullets += Bullet(sx, sy, facingLeft, BULLET_SPEED, BULLET_RANGE, bulletBmp, bulletHitFrames)
    }

    // ===== stepAnim implementation (fixed, typed) =====
    private fun stepAnim(dtMs: Long, frames: List<Bitmap>, frameMs: Long, onIndex: ((Int) -> Unit)? = null) {
        if (frames.isEmpty()) {
            if (state == State.DEAD && !deathDone) {
                deathDone = true
                alive = false
            }
            return
        }
        animTimer += dtMs
        if (animTimer >= frameMs) {
            val steps = (animTimer / frameMs).toInt()
            animTimer -= steps * frameMs
            animFrame = (animFrame + steps) % frames.size
            onIndex?.invoke(animFrame)
            if (state == State.DEAD && animFrame == frames.size - 1 && !deathDone) {
                deathDone = true
                alive = false
            }
        }
    }

    private fun frameFrom(frames: List<Bitmap>) = if (frames.isEmpty()) null else frames[min(animFrame, frames.size - 1)]

    // Bullet inner class
    private class Bullet(
        startX: Float, startY: Float,
        private val dirLeft: Boolean,
        private val speed: Float,
        private val maxRange: Float,
        private val sprite: Bitmap?,
        private val hitFrames: List<Bitmap>
    ) {
        private val paint = Paint()
        var x = startX; var y = startY
        private val startX0 = startX
        var exploded = false; private var aliveInternal = true
        private var f = 0; private var t = 0L

        fun update(dtMs: Long) {
            if (!exploded) {
                x += (if (dirLeft) -speed else speed) * (dtMs / 1000f)
                if (abs(x - startX0) > maxRange) aliveInternal = false
            } else {
                t += dtMs
                if (t >= 70L) { t -= 70L; f++; if (f >= max(1, hitFrames.size)) aliveInternal = false }
            }
        }

        fun draw(canvas: Canvas) {
            paint.isFilterBitmap = false; paint.isDither = false
            if (!exploded) {
                if (sprite != null) {
                    canvas.drawBitmap(sprite, x, y, paint)
                } else {
                    paint.color = Color.YELLOW
                    canvas.drawCircle(x, y, 6f, paint)
                }
            } else {
                val bmp = if (hitFrames.isNotEmpty()) hitFrames[min(f, hitFrames.size - 1)] else null
                if (bmp != null) canvas.drawBitmap(bmp, x - 8f, y - 8f, paint)
                else { paint.color = Color.argb(160, 255, 220, 0); canvas.drawCircle(x, y, 10f, paint) }
            }
        }

        fun explode() { if (!exploded) { exploded = true } }
        fun alive() = aliveInternal
        fun bounds() = RectF(x, y, x + 16f, y + 16f)
    }

    // image helpers
    private fun loadSingle(name: String): Bitmap? {
        val bmp = loadBitmap(name) ?: return null
        return bmp.copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun loadStrip(name: String, out: MutableList<Bitmap>, frameW: Int, frameH: Int, expectedFrames: Int = -1) {
        val sheet = loadBitmap(name) ?: return
        val src = if (sheet.config != Bitmap.Config.ARGB_8888) sheet.copy(Bitmap.Config.ARGB_8888, false) else sheet
        val count = if (expectedFrames > 0) expectedFrames else (src.width / frameW)
        for (i in 0 until count) {
            val srcRect = Rect(i * frameW, 0, (i + 1) * frameW, frameH)
            val frame = Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
            frame.setHasAlpha(true)
            val c = Canvas(frame)
            val p = Paint().apply { isFilterBitmap = false; isDither = false }
            c.drawBitmap(src, srcRect, Rect(0, 0, frameW, frameH), p)
            try { frame.prepareToDraw() } catch (_: Throwable) {}
            out.add(frame)
        }
    }

    private fun loadBitmap(name: String): Bitmap? {
        try {
            val res = AppCtx.res
            val pkg = AppCtx.pkg
            val id = res.getIdentifier(name, "drawable", pkg)
            val opts = BitmapFactory.Options().apply { inScaled = false; inPreferredConfig = Bitmap.Config.ARGB_8888 }
            if (id != 0) return BitmapFactory.decodeResource(res, id, opts)?.copy(Bitmap.Config.ARGB_8888, false)
        } catch (_: Exception) {}
        return try {
            AppCtx.assets.open("$name.png").use { s ->
                val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888; inScaled = false }
                BitmapFactory.decodeStream(s, null, opts)?.copy(Bitmap.Config.ARGB_8888, false)
            }
        } catch (_: Exception) { null }
    }
}
