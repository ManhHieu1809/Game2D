package com.example.game2d.entities

import android.graphics.*
import com.example.game2d.Player
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Monster1(
    startX: Float,
    startY: Float,
    patrolWidth: Float = 160f,
    private val detectRange: Float = 420f,
) : Entity() {

    // ==== RESOURCE NAMES (đổi nếu bạn đặt tên khác) ====
    private val RES_IDLE   = "monster1_idle"    // 28f @32x32
    private val RES_RUN    = "monster1_run"     // 36f @32x32
    private val RES_ATTACK = "monster1_attack"  // 22f @32x32
    private val RES_HIT    = "monster1_hit"     // 10f @32x32
    private val RES_BULLET = "bullet"           // 1f  @16x16
    private val RES_BHIT   = "bullet_hit"       // 2f  @16x16 (từ sheet 32x16)

    // ==== CONFIG ====
    private val size = 48f
    private val patrolSpeed = 42f
    private val bulletSpeed = 280f
    private val bulletRange = 600f
    private val fireCooldownMs = 900L
    private val attackShootFrame = 10          // bắn ở frame này của animation attack

    // ==== STATE ====
    private enum class State { IDLE, RUN, ATTACK, DEAD }
    private var state = State.IDLE
    var facingLeft = true

    private var leftBound = startX - patrolWidth/2f
    private var rightBound = startX + patrolWidth/2f

    private var animTimer = 0L
    private var animFrame = 0
    private var fireTimer = 0L
    private var attackedThisCycle = false
    private var deathDone = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // animations (đã cắt sẵn từng frame)
    private val idleFrames   = mutableListOf<Bitmap>()
    private val runFrames    = mutableListOf<Bitmap>()
    private val attackFrames = mutableListOf<Bitmap>()
    private val hitFrames    = mutableListOf<Bitmap>()
    private val bulletHitFrames = mutableListOf<Bitmap>()
    private var bulletBmp: Bitmap? = null

    private val bullets = ArrayList<Bullet>()

    init {
        x = startX; y = startY; vx = -patrolSpeed; facingLeft = true
        // nạp/cắt tất cả spritesheet
        loadStrip(RES_IDLE,   idleFrames)
        loadStrip(RES_RUN,    runFrames)
        loadStrip(RES_ATTACK, attackFrames)
        loadStrip(RES_HIT,    hitFrames)
        loadStrip(RES_BHIT,   bulletHitFrames, frameSizeOverride = 16)
        bulletBmp = loadSingle(RES_BULLET)
    }

    // ---------- API ----------
    override fun getBounds(): RectF = RectF(x - size/2, y - size/2, x + size/2, y + size/2)

    override fun update(dtMs: Long) {
        // fallback nếu engine cũ không truyền player
        update(dtMs, null)
    }

    fun update(dtMs: Long, player: Player?) {
        val dt = dtMs/1000f

        // === chọn state ===
        var wantAttack = false
        if (player != null) {
            val px = player.x + player.width/2f
            val py = player.y + player.height/2f
            val dx = px - x
            val inSameRow = abs(py - y) < 80f
            wantAttack = inSameRow && abs(dx) <= detectRange
            facingLeft = dx < 0
        }

        when (state) {
            State.DEAD -> {
                vx = 0f
                // chơi hết hit animation rồi đứng im (deathDone để ngừng update animation)
            }
            State.ATTACK -> {
                vx = 0f
                fireTimer += dtMs
                // phát animation attack và bắn 1 lần trong chu kỳ
                stepAnim(dtMs, attackFrames, 60L) { i ->
                    if (!attackedThisCycle && i >= attackShootFrame) {
                        shoot()
                        attackedThisCycle = true
                    }
                    // khi kết thúc 1 vòng attack -> về patrol/idle
                    if (i == 0) {
                        attackedThisCycle = false
                        state = State.IDLE
                        fireTimer = 0
                    }
                }
            }
            else -> {
                // tuần tra
                vx = if (facingLeft) -patrolSpeed else patrolSpeed
                x += vx * dt
                if (x < leftBound) { x = leftBound; facingLeft = false }
                if (x > rightBound){ x = rightBound; facingLeft = true }

                state = if (wantAttack && fireTimer >= fireCooldownMs) State.ATTACK
                else if (abs(vx) > 1f) State.RUN else State.IDLE

                val frames = if (state==State.RUN) runFrames else idleFrames
                stepAnim(dtMs, frames, if (state==State.RUN) 60L else 80L)
                if (!wantAttack) fireTimer = max(0, (fireCooldownMs/2).toInt()).toLong()
                else fireTimer += dtMs
            }
        }

        // === bullets ===
        val it = bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.update(dtMs)
            if (!b.alive()) it.remove()
        }
    }

    override fun draw(canvas: Canvas) {
        // chọn frame theo state
        val bmp = when (state) {
            State.DEAD -> frameFrom(hitFrames)
            State.ATTACK -> frameFrom(attackFrames)
            State.RUN -> frameFrom(runFrames)
            else -> frameFrom(idleFrames)
        }

        val dest = snapRect(x - size/2, y - size/2, x + size/2, y + size/2)

        canvas.save()
        if (!facingLeft) canvas.scale(-1f, 1f, x.roundTo(), y.roundTo())
        paint.isFilterBitmap = true; paint.isDither = true

        if (bmp != null) canvas.drawBitmap(bmp, Rect(0,0,bmp.width,bmp.height), dest, paint)
        else { paint.color = Color.MAGENTA; canvas.drawCircle(x.roundTo(), y.roundTo(), size/2, paint) }
        canvas.restore()

        // bullets
        bullets.forEach { it.draw(canvas) }
    }

    /** người chơi dẫm từ trên -> chết */
    fun tryStompBy(player: Player): Boolean {
        if (state == State.DEAD) return false
        val pr = RectF(player.x, player.y, player.x+player.width, player.y+player.height)
        val mr = getBounds()
        val onTop = player.vy > 0 && pr.bottom > mr.top && (player.prevY + player.height) <= mr.top + 5f
        if (onTop) {
            state = State.DEAD
            animFrame = 0; animTimer = 0; deathDone = false
            return true
        }
        return false
    }

    /** true nếu frame này có viên đạn vừa trúng player (bạn xử lý respawn ở ngoài) */
    fun bulletHitPlayer(player: Player): Boolean {
        var hit = false
        bullets.forEach { b ->
            if (!b.exploded && RectF.intersects(b.bounds(), RectF(player.x, player.y, player.x+player.width, player.y+player.height))) {
                b.explode(); hit = true
            }
        }
        return hit
    }

    // ===== INTERNAL =====
    private fun stepAnim(dtMs: Long, frames: List<Bitmap>, frameMs: Long, onIndex: ((Int)->Unit)? = null) {
        if (frames.isEmpty()) return
        animTimer += dtMs
        if (animTimer >= frameMs) {
            val steps = (animTimer / frameMs).toInt()
            animTimer -= steps * frameMs
            animFrame = (animFrame + steps) % frames.size
            onIndex?.invoke(animFrame)
            if (state == State.DEAD && animFrame == frames.size-1 && !deathDone) deathDone = true
        }
    }
    private fun frameFrom(frames: List<Bitmap>) = if (frames.isEmpty()) null else frames[min(animFrame, frames.size-1)]

    private fun shoot() {
        val dirLeft = facingLeft
        val sx = x + if (dirLeft) -size*0.35f else size*0.35f
        val sy = y - size*0.12f
        bullets += Bullet(sx, sy, dirLeft, bulletSpeed, bulletRange, bulletBmp, bulletHitFrames)
    }

    // ===== image helpers (tự cắt từng frame, fix sọc) =====
    private fun loadSingle(name: String): Bitmap? =
        loadBitmap(name)?.copy(Bitmap.Config.ARGB_8888, false)

    private fun loadStrip(name: String, out: MutableList<Bitmap>, frameSizeOverride: Int? = null) {
        val sheet = loadBitmap(name) ?: return
        val h = sheet.height
        val s = frameSizeOverride ?: h               // frame vuông = theo chiều cao
        val count = sheet.width / s
        for (i in 0 until count) {
            out += Bitmap.createBitmap(sheet, i*s, 0, s, min(s, sheet.height))
        }
    }

    private fun loadBitmap(name: String): Bitmap? {
        // 1) drawable
        val res = com.example.game2d.AppCtx.res // tiện: AppCtx là object giữ context toàn cục (nếu bạn chưa có, thay bằng context truyền vào)
        val pkg = com.example.game2d.AppCtx.pkg
        val id = res.getIdentifier(name, "drawable", pkg)
        val opts = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inDither = true
        }
        if (id != 0) {
            return BitmapFactory.decodeResource(res, id, opts)?.copy(Bitmap.Config.ARGB_8888, false)
        }
        // 2) assets/name.png
        return try {
            com.example.game2d.AppCtx.assets.open("$name.png").use { s ->
                BitmapFactory.decodeStream(s, null, opts)?.copy(Bitmap.Config.ARGB_8888, false)
            }
        } catch (_: Exception) { null }
    }

    private fun Float.roundTo() = kotlin.math.round(this)
    private fun snapRect(l: Float, t: Float, r: Float, b: Float): RectF =
        RectF(l.roundTo(), t.roundTo(), r.roundTo(), b.roundTo())

    // ===== inner Bullet =====
    private class Bullet(
        startX: Float,
        startY: Float,
        private val dirLeft: Boolean,
        private val speed: Float,
        private val maxRange: Float,
        private val sprite: Bitmap?,
        private val hitFrames: List<Bitmap>
    ) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var x = startX; var y = startY; var vx = if (dirLeft) -speed else speed
        private val x0 = startX
        var exploded = false; private var aliveInternal = true
        private var f = 0; private var t = 0L

        fun update(dtMs: Long) {
            if (!exploded) {
                x += vx * (dtMs/1000f)
                if (abs(x - x0) > maxRange) aliveInternal = false
            } else {
                t += dtMs
                if (t >= 70L) { t -= 70L; f++ ; if (f >= max(1, hitFrames.size)) aliveInternal = false }
            }
        }

        fun draw(canvas: Canvas) {
            paint.isFilterBitmap = true; paint.isDither = true
            if (!exploded) {
                val s = 18f
                val dst = RectF(round(x - s/2), round(y - s/2), round(x + s/2), round(y + s/2))
                if (sprite != null) {
                    canvas.save()
                    if (!dirLeft) canvas.scale(-1f, 1f, round(x), round(y))
                    canvas.drawBitmap(sprite, Rect(0,0,sprite.width,sprite.height), dst, paint)
                    canvas.restore()
                } else {
                    paint.color = Color.YELLOW
                    canvas.drawCircle(round(x), round(y), s/2, paint)
                }
            } else {
                val s = 24f
                val bmp = if (hitFrames.isNotEmpty()) hitFrames[min(f, hitFrames.size-1)] else null
                val dst = RectF(round(x - s/2), round(y - s/2), round(x + s/2), round(y + s/2))
                if (bmp != null) canvas.drawBitmap(bmp, Rect(0,0,bmp.width,bmp.height), dst, paint)
                else { paint.color = Color.argb(160,255,220,0); canvas.drawCircle(round(x), round(y), s/2, paint) }
            }
        }

        fun explode() { if (!exploded) { exploded = true; vx = 0f } }
        fun alive() = aliveInternal
        fun bounds(): RectF {
            val s = if (!exploded) 18f else 24f
            return RectF(round(x - s/2), round(y - s/2), round(x + s/2), round(y + s/2))
        }
        private fun round(v: Float) = kotlin.math.round(v)
    }
}
