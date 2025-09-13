package com.example.game2d.entities

import android.graphics.*
import com.example.game2d.AppCtx
import com.example.game2d.Player
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

class Monster2(
    startX: Float,
    startY: Float,
    private val patrolWidth: Float = 160f,
    scaleOverride: Float? = null
) : Entity() {

    companion object {
        private const val FRAME_W = 52
        private const val FRAME_H = 34

        private const val RUN_FRAMES = 6
        private const val IDLE_FRAMES = 11
        private const val HITWALL_FRAMES = 4
        private const val HIT_FRAMES = 5

        // Defaults (tweak to taste)
        private const val DEFAULT_SCALE = 1.6f
        private const val PATROL_SPEED_BASE = 40f    // px/s (will be scaled)
        private const val ATTACK_SPEED_BASE = 140f   // px/s (dash speed, scaled)
        private const val DETECT_RANGE_BASE = 320f   // px horizontal detection
        private const val ATTACK_RANGE_BASE = 240f   // px maximum dash travel
        private const val ATTACK_DURATION_MS = 700L // max ms for dash
        private const val ATTACK_COOLDOWN_MS = 900L // ms between attacks
        private const val VERTICAL_THRESHOLD = 48f  // allowed vertical diff to attack
        private const val IGNORE_ON_TOP_OFFSET = 6f // if player is above (on top), ignore attack
    }

    private enum class State { IDLE, RUN, ATTACK, HIT_WALL, HIT, DEAD }
    private var state = State.IDLE
    var facingLeft = true

    private val scale = scaleOverride ?: DEFAULT_SCALE
    private val displayW get() = FRAME_W * scale
    private val displayH get() = FRAME_H * scale

    init {
        // caller should pass startY as top-left of scaled frame (groundTopY - FRAME_H * scale)
        x = startX
        y = startY
        vx = -PATROL_SPEED_BASE * scale
    }

    // patrol bounds
    private val leftBound = startX - patrolWidth / 2f
    private val rightBound = startX + patrolWidth / 2f

    // movement params (scaled)
    private val patrolSpeed = PATROL_SPEED_BASE * scale
    private val attackSpeed = ATTACK_SPEED_BASE * scale
    private val detectRange = DETECT_RANGE_BASE * scale
    private val attackRange = ATTACK_RANGE_BASE * scale

    // attack timers
    private var attackTimer = 0L
    private var attackCooldownTimer = 0L
    private var attackTravelled = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isFilterBitmap = false
        isDither = false
    }

    // frames
    private val runFrames = mutableListOf<Bitmap>()
    private val idleFrames = mutableListOf<Bitmap>()
    private val hitWallFrames = mutableListOf<Bitmap>()
    private val hitFrames = mutableListOf<Bitmap>()

    // anim state
    private var animTimer = 0L
    private var animFrame = 0

    // HP / alive
    private var hp = 2
    private var aliveFlag = true

    init {
        loadStrip("monster2_run", runFrames, FRAME_W, FRAME_H, RUN_FRAMES)
        loadStrip("monster2_idle", idleFrames, FRAME_W, FRAME_H, IDLE_FRAMES)
        loadStrip("monster2_hitwall", hitWallFrames, FRAME_W, FRAME_H, HITWALL_FRAMES)
        loadStrip("monster2_hit", hitFrames, FRAME_W, FRAME_H, HIT_FRAMES)
    }

    override fun getBounds(): RectF = RectF(x, y, x + displayW, y + displayH)

    /**
     * Update with player reference so monster can decide to attack.
     * This implements patrol within [leftBound, rightBound] and attack dash when conditions meet.
     */
    fun update(dtMs: Long, player: Player?) {
        if (!aliveFlag) return

        val dt = dtMs / 1000f

        // decide if want to attack
        var wantAttack = false
        var playerIsAbove = false
        if (player != null) {
            val pxCenter = player.x + player.width / 2f
            val mxCenter = x + displayW / 2f
            val dx = pxCenter - mxCenter
            val dy = player.y + player.height / 2f - (y + displayH / 2f)
            playerIsAbove = (player.y + player.height) <= (y + IGNORE_ON_TOP_OFFSET)
            val withinDetect = abs(dx) <= detectRange
            val nearSameRow = abs(dy) <= VERTICAL_THRESHOLD * scale
            // player must be in front: same sign as facing or near to decide facing
            wantAttack = withinDetect && nearSameRow && !playerIsAbove
            // set facing toward player early (but don't run to follow if not attacking)
            if (abs(dx) < detectRange) facingLeft = dx < 0
        }

        // update cooldown timer
        if (attackCooldownTimer > 0L) {
            attackCooldownTimer = max(0L, attackCooldownTimer - dtMs)
        }

        when (state) {
            State.ATTACK -> {
                // in dash: move faster; accumulate travelled distance and timer
                val move = (if (facingLeft) -attackSpeed else attackSpeed) * dt
                x += move
                attackTravelled += abs(move)
                attackTimer += dtMs

                // if exceeded travel or duration or out of bounds -> stop
                val stopBecauseRange = attackTravelled >= attackRange
                val stopBecauseTime = attackTimer >= ATTACK_DURATION_MS
                if (stopBecauseRange || stopBecauseTime) {
                    // stop dash, go to idle and start cooldown
                    state = State.IDLE
                    attackCooldownTimer = ATTACK_COOLDOWN_MS
                    attackTravelled = 0f
                    attackTimer = 0L
                    animFrame = 0
                    animTimer = 0L
                }

                // if hit bounds while attacking -> hit wall logic
                if (x <= leftBound) {
                    x = leftBound
                    state = State.HIT_WALL
                    animFrame = 0; animTimer = 0L
                } else if (x >= rightBound) {
                    x = rightBound
                    state = State.HIT_WALL
                    animFrame = 0; animTimer = 0L
                }

                // animate run frames faster while attacking
                val frameMs = 50L
                animTimer += dtMs
                if (runFrames.isNotEmpty() && animTimer >= frameMs) {
                    val steps = (animTimer / frameMs).toInt()
                    animTimer -= steps * frameMs
                    animFrame = (animFrame + steps) % runFrames.size
                }
            }

            State.HIT_WALL -> {
                // play hitWall animation then reverse to RUN
                stepAnim(dtMs, hitWallFrames, 80L) { idx: Int ->
                    if (idx == hitWallFrames.size - 1) {
                        facingLeft = !facingLeft
                        state = State.RUN
                        animFrame = 0; animTimer = 0L
                    }
                }
            }

            State.HIT -> {
                // play hit then resume patrol or die
                stepAnim(dtMs, hitFrames, 90L) { idx: Int ->
                    if (idx == hitFrames.size - 1) {
                        if (hp <= 0) {
                            aliveFlag = false
                            state = State.DEAD
                        } else {
                            state = State.IDLE
                        }
                        animFrame = 0; animTimer = 0L
                    }
                }
            }

            State.RUN, State.IDLE -> {
                // If player is in front and attack conditions met and cooldown ready -> start dash
                if (wantAttack && attackCooldownTimer <= 0L && state != State.HIT && state != State.DEAD) {
                    // Only attack if player is in front of monster (not behind)
                    val pxCenter = player!!.x + player.width / 2f
                    val mxCenter = x + displayW / 2f
                    val dx = pxCenter - mxCenter
                    val inFront = (dx * (if (facingLeft) -1f else 1f)) < 0f  // negative if player is actually behind; we want same sign
                    // simpler: ensure facing is toward player already (we set facing earlier)
                    if (!playerIsAbove && abs(dx) <= detectRange) {
                        // start attack dash
                        state = State.ATTACK
                        attackTimer = 0L
                        attackTravelled = 0f
                        animFrame = 0; animTimer = 0L
                        // set vx for possible use elsewhere
                        vx = if (facingLeft) -attackSpeed else attackSpeed
                        // do not move extra this frame (we handle movement above)
                    }
                }

                // Patrol movement if not attacking
                if (state != State.ATTACK) {
                    vx = if (facingLeft) -patrolSpeed else patrolSpeed
                    x += vx * dt
                    // clamp and trigger hit wall when reaching edges
                    if (x < leftBound) {
                        x = leftBound
                        state = State.HIT_WALL
                        animFrame = 0; animTimer = 0L
                    } else if (x > rightBound) {
                        x = rightBound
                        state = State.HIT_WALL
                        animFrame = 0; animTimer = 0L
                    } else {
                        state = State.RUN
                    }

                    // animate run/idle
                    val frames = if (state == State.RUN) runFrames else idleFrames
                    val frameMs = if (state == State.RUN) 80L else 120L
                    animTimer += dtMs
                    if (frames.isNotEmpty() && animTimer >= frameMs) {
                        val steps = (animTimer / frameMs).toInt()
                        animTimer -= steps * frameMs
                        animFrame = (animFrame + steps) % frames.size
                    }
                }
            }
            else -> {}
        }

    }

    override fun update(dtMs: Long) {
        update(dtMs, null)
    }

    override fun draw(canvas: Canvas) {
        if (!aliveFlag) return

        val frames = when (state) {
            State.HIT_WALL -> hitWallFrames
            State.HIT -> hitFrames
            State.ATTACK -> runFrames // show run frames while attacking
            State.RUN -> runFrames
            else -> idleFrames
        }
        val bmp = if (frames.isNotEmpty()) frames[animFrame % frames.size] else null

        canvas.save()
        val cx = x + displayW / 2f
        val cy = y + displayH / 2f
        if (!facingLeft) canvas.scale(-1f, 1f, cx, cy)
        if (bmp != null) {
            val dst = RectF(x, y, x + displayW, y + displayH)
            canvas.drawBitmap(bmp, null, dst, paint)
        } else {
            // fallback rect
            paint.color = Color.GRAY
            canvas.drawRect(x, y, x + displayW, y + displayH, paint)
        }
        canvas.restore()
    }

    fun tryStompBy(player: Player): Boolean {
        if (!aliveFlag) return false
        val playerBottomNow = player.y + player.height
        val playerBottomPrev = player.prevY + player.height
        val monsterTop = y
        val falling = player.vy > 0f
        val wasAbove = playerBottomPrev <= monsterTop + 4f
        if (falling && wasAbove && playerBottomNow > monsterTop && player.x + player.width > x && player.x < x + displayW) {
            hp = 0
            onHit()
            return true
        }
        return false
    }

    fun onHit(damage: Int = 1) {
        hp -= damage
        animFrame = 0; animTimer = 0L
        state = State.HIT
    }

    // small helper to advance a full animation with callback when index changes
    private fun stepAnim(dtMs: Long, frames: List<Bitmap>, frameMs: Long, onIndex: ((Int) -> Unit)? = null) {
        if (frames.isEmpty()) {
            onIndex?.invoke(0)
            return
        }
        animTimer += dtMs
        if (animTimer >= frameMs) {
            val steps = (animTimer / frameMs).toInt()
            animTimer -= steps * frameMs
            animFrame = (animFrame + steps) % frames.size
            onIndex?.invoke(animFrame)
        }
    }

    // image helpers (same style as Monster1)
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
