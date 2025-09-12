package com.example.game2d

import android.content.Context
import android.graphics.*
import com.example.game2d.entities.EntityManager
import com.example.game2d.entities.Monster1
import com.example.game2d.entities.Pickup
import com.example.game2d.obstacles.Checkpoint
import com.example.game2d.obstacles.MovingPlatform
import com.example.game2d.obstacles.Saw
import com.example.game2d.obstacles.Spike
import com.example.game2d.resources.SpriteLoader
import kotlin.math.min
import kotlin.math.sin

class TileMap(private val ctx: Context) {

    // Kích thước world (pixel thế giới, không phải màn hình)
    val worldWidth = 3200f
    val worldHeight = 720f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val groundTopY = worldHeight * 0.75f

    // Hình học tĩnh
    private val platforms = ArrayList<RectF>()
    private val pipes = ArrayList<RectF>()
    private val bricks = ArrayList<RectF>()

    // Vật cản/hazard
    private val spikes = ArrayList<Spike>()
    private val saws = ArrayList<Saw>()
    private val movingPlatforms = ArrayList<MovingPlatform>()
    private val checkpoints = ArrayList<Checkpoint>()

    // Cloud
    data class Cloud(var x: Float, var baseY: Float, var vx: Float, val bobOffset: Float)
    private val clouds = ArrayList<Cloud>()

    // Entity động
    private val entities = EntityManager()              // dùng cho pickups (hoa quả/coin)
    private val monsters = mutableListOf<Monster1>()    // quái có bắn đạn

    // Checkpoint hiện tại (respawn)
    private var lastCheckpointX = 200f
    private var lastCheckpointY = 0f

    init {
        // Nạp/cached sprite mặc định (bullet, pig, fruit, spike, saw, ...)
        SpriteLoader.preloadDefaults(ctx)
        setupLevel()
    }

    private fun setupLevel() {
        // --- Nền tảng/địa hình mẫu ---
        platforms.add(RectF(300f, groundTopY - 120f, 450f, groundTopY - 100f))
        platforms.add(RectF(600f, groundTopY - 80f, 720f, groundTopY - 60f))
        platforms.add(RectF(900f, groundTopY - 160f, 1050f, groundTopY - 140f))
        platforms.add(RectF(1200f, groundTopY - 100f, 1350f, groundTopY - 80f))
        platforms.add(RectF(1500f, groundTopY - 200f, 1620f, groundTopY - 180f))
        platforms.add(RectF(1800f, groundTopY - 140f, 1950f, groundTopY - 120f))
        platforms.add(RectF(2200f, groundTopY - 180f, 2350f, groundTopY - 160f))
        platforms.add(RectF(2600f, groundTopY - 220f, 2750f, groundTopY - 200f))

        pipes.add(RectF(800f, groundTopY - 80f, 850f, groundTopY))
        pipes.add(RectF(1400f, groundTopY - 120f, 1450f, groundTopY))
        pipes.add(RectF(2000f, groundTopY - 100f, 2050f, groundTopY))
        pipes.add(RectF(2800f, groundTopY - 140f, 2850f, groundTopY))

        for (i in 0..4) for (j in 0..i) {
            bricks.add(RectF(1600f + j * 32f, groundTopY - 32f - i * 32f, 1632f + j * 32f, groundTopY - i * 32f))
        }
        for (i in 0..3) {
            bricks.add(RectF(2400f + i * 32f, groundTopY - 180f, 2432f + i * 32f, groundTopY - 148f))
        }

        // --- Hazards/đối tượng chuyển động ---
        listOf(
            RectF(520f, groundTopY - 16f, 552f, groundTopY),
            RectF(750f, groundTopY - 16f, 782f, groundTopY),
            RectF(1100f, groundTopY - 16f, 1148f, groundTopY),
            RectF(1700f, groundTopY - 16f, 1732f, groundTopY),
            RectF(2100f, groundTopY - 16f, 2148f, groundTopY)
        ).forEach { spikes += Spike(it) }

        saws += Saw(1250f, groundTopY - 50f, 120f)
        saws += Saw(1850f, groundTopY - 90f, -90f)
        saws += Saw(2300f, groundTopY - 120f, 150f)

        movingPlatforms += MovingPlatform(1050f, groundTopY - 220f, 1050f, 1180f, 40f, 1)
        movingPlatforms += MovingPlatform(2500f, groundTopY - 160f, 2500f, 2650f, 60f, 1)

        checkpoints += Checkpoint(1000f, groundTopY - 40f)
        checkpoints += Checkpoint(2000f, groundTopY - 40f)
        checkpoints += Checkpoint(3000f, groundTopY - 40f)

        // --- Clouds ---
        clouds += Cloud(200f, 100f, -12f, 0.2f)
        clouds += Cloud(500f, 80f, -8f, 1.1f)
        clouds += Cloud(800f, 120f, -16f, 2.3f)
        clouds += Cloud(1100f, 90f, -10f, 0.5f)
        clouds += Cloud(1400f, 110f, -6f, 3.7f)
        clouds += Cloud(1700f, 85f, -14f, 4.2f)
        clouds += Cloud(2000f, 100f, -9f, 5.9f)
        clouds += Cloud(2400f, 95f, -11f, 1.8f)
        clouds += Cloud(2800f, 105f, -7f, 4.6f)

        // --- Pickups (coin/fruit) ---
        entities.addPickup(Pickup("strawberry", 375f, groundTopY - 150f))
        entities.addPickup(Pickup("strawberry", 660f, groundTopY - 110f))
        entities.addPickup(Pickup("strawberry", 975f, groundTopY - 190f))
        entities.addPickup(Pickup("apple", 1275f, groundTopY - 130f))
        entities.addPickup(Pickup("apple", 1875f, groundTopY - 170f))
        entities.addPickup(Pickup("cherry", 1560f, groundTopY - 230f))
        entities.addPickup(Pickup("cherry", 2375f, groundTopY - 220f))
        val fruits = arrayOf("strawberry", "apple", "cherry", "orange", "banana")
        for (i in 0..6) {
            entities.addPickup(Pickup(fruits[i % fruits.size], 2200f + i * 40f, groundTopY - 220f - i * 8f))
        }
        entities.addPickup(Pickup("banana", 2900f, groundTopY - 50f))

        // --- Monsters (Monster1: tuần tra + bắn khi lại gần) ---
        monsters += Monster1(700f, groundTopY - 32f, patrolWidth = 180f, detectRange = 420f)
        monsters += Monster1(1300f, groundTopY - 32f, patrolWidth = 160f, detectRange = 420f)
        monsters += Monster1(2100f, groundTopY - 32f, patrolWidth = 160f, detectRange = 420f)
    }

    fun getGroundTopY(): Float = groundTopY

    // ===================== DRAW =====================
    fun draw(canvas: Canvas) {
        // Sky
        val skyPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, groundTopY,
                Color.rgb(135, 206, 250),
                Color.rgb(176, 224, 230),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, worldWidth, groundTopY, skyPaint)

        // Clouds
        paint.shader = null
        paint.color = Color.WHITE
        for (c in clouds) {
            val bob = sin((System.currentTimeMillis() / 1000f) + c.bobOffset) * 6f
            drawCloud(canvas, c.x, c.baseY + bob)
        }

        // Ground
        paint.color = Color.rgb(101, 67, 33)
        canvas.drawRect(0f, groundTopY, worldWidth, worldHeight, paint)
        paint.color = Color.rgb(34, 139, 34)
        canvas.drawRect(0f, groundTopY, worldWidth, groundTopY + 24f, paint)

        drawPlatforms(canvas)
        drawPipes(canvas)
        bricks.forEach { drawBrick(canvas, it) }

        // Obstacles
        spikes.forEach { it.draw(canvas) }
        saws.forEach { it.draw(canvas) }
        movingPlatforms.forEach { it.draw(canvas) }
        checkpoints.forEach { it.draw(canvas) }

        // Entities
        entities.drawAll(canvas)
        monsters.forEach { it.draw(canvas) }// fruit/coin
        drawMonsters(canvas)       // quái + đạn của chúng

        // Viền world
        paint.color = Color.rgb(20, 20, 20)
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(0f, 0f, worldWidth, worldHeight, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawPlatforms(canvas: Canvas) {
        paint.color = Color.rgb(34, 139, 34)
        for (p in platforms) {
            canvas.drawRect(p, paint)
            paint.color = Color.rgb(50, 205, 50)
            canvas.drawRect(p.left, p.top, p.right, p.top + 8f, paint)
            paint.color = Color.rgb(101, 67, 33)
            canvas.drawRect(p.left, p.top + 8f, p.right, p.bottom + 6f, paint)
            paint.color = Color.rgb(34, 139, 34)
        }
    }

    private fun drawPipes(canvas: Canvas) {
        paint.color = Color.rgb(0, 128, 0)
        for (pipe in pipes) {
            canvas.drawRect(pipe, paint)
            paint.color = Color.rgb(50, 205, 50)
            canvas.drawRect(pipe.left + 4f, pipe.top, pipe.left + 8f, pipe.bottom, paint)
            canvas.drawRect(pipe.right - 8f, pipe.top, pipe.right - 4f, pipe.bottom, paint)
            paint.color = Color.rgb(0, 100, 0)
            canvas.drawRect(pipe.left - 6f, pipe.top, pipe.right + 6f, pipe.top + 12f, paint)
            paint.color = Color.rgb(0, 128, 0)
        }
    }

    private fun drawBrick(canvas: Canvas, brick: RectF) {
        paint.color = Color.rgb(139, 69, 19)
        canvas.drawRect(brick, paint)
        paint.color = Color.rgb(160, 82, 45)
        canvas.drawRect(brick.left + 2f, brick.top + 2f, brick.right - 2f, brick.top + 8f, paint)
        canvas.drawRect(brick.left + 2f, brick.top + 12f, brick.right - 2f, brick.top + 18f, paint)
        canvas.drawRect(brick.left + 2f, brick.top + 22f, brick.right - 2f, brick.bottom - 2f, paint)
        paint.color = Color.rgb(101, 67, 33)
        paint.strokeWidth = 1f
        canvas.drawLine(brick.left, brick.top + 8f, brick.right, brick.top + 8f, paint)
        canvas.drawLine(brick.left, brick.top + 18f, brick.right, brick.top + 18f, paint)
        canvas.drawLine(brick.left + 16f, brick.top, brick.left + 16f, brick.top + 8f, paint)
        canvas.drawLine(brick.left + 16f, brick.top + 18f, brick.left + 16f, brick.bottom, paint)
    }

    private fun drawCloud(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.WHITE
        canvas.drawCircle(x, y, 30f, paint)
        canvas.drawCircle(x + 25f, y, 35f, paint)
        canvas.drawCircle(x + 50f, y, 30f, paint)
        canvas.drawCircle(x + 70f, y, 25f, paint)
        canvas.drawCircle(x + 15f, y + 15f, 25f, paint)
        canvas.drawCircle(x + 40f, y + 15f, 30f, paint)
        canvas.drawCircle(x + 60f, y + 15f, 25f, paint)
    }

    private fun drawMonsters(canvas: Canvas) {
        monsters.forEach { it.draw(canvas) }
    }

    // ===================== UPDATE =====================
    fun update(deltaMs: Long) {
        val dt = deltaMs / 1000f

        movingPlatforms.forEach { it.update(dt) }
        saws.forEach { it.update(dt) }

        clouds.forEach {
            it.x += it.vx * dt
            val wrap = 120f
            if (it.x < -wrap) it.x = worldWidth + wrap
            if (it.x > worldWidth + wrap) it.x = -wrap
        }

        entities.updateAll(deltaMs) // pickups
        // monsters cần biết Player -> gọi updateMonsters(...) ở GameView (xem dưới)
    }


    fun updateMonsters(deltaMs: Long, player: Player) {
        monsters.forEach { it.update(deltaMs, player) }
    }

    fun checkBulletHitAndRespawnIfNeeded(player: Player) {
        if (monsters.any { it.bulletHitPlayer(player) }) respawnPlayer(player)
    }

    // ===================== COLLISION =====================
    fun resolvePlayerCollision(player: Player): Boolean {
        var collided = false

        // world bounds
        if (player.x < 0f) { player.x = 0f; player.vx = 0f; collided = true }
        if (player.x + player.width > worldWidth) { player.x = worldWidth - player.width; player.vx = 0f; collided = true }
        if (player.y < 0f) { player.y = 0f; if (player.vy < 0f) player.vy = 0f; collided = true }

        // ground
        if (player.y + player.height > groundTopY && player.vy >= 0f) {
            player.y = groundTopY - player.height
            player.vy = 0f
            collided = true
        }

        // static platforms (đỡ từ trên)
        for (p in platforms) {
            val pr = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
            if (pr.right > p.left && pr.left < p.right) {
                if (player.vy > 0f && pr.bottom > p.top && pr.top < p.top) {
                    val prevBottom = player.prevY + player.height
                    if (prevBottom <= p.top + 5f) {
                        player.y = p.top - player.height
                        player.vy = 0f
                        collided = true
                    }
                }
            }
        }

        // moving platforms (đỡ từ trên)
        for (mp in movingPlatforms) {
            val mpr = mp.rect()
            val pr = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
            if (pr.right > mpr.left && pr.left < mpr.right) {
                if (player.vy > 0f && pr.bottom > mpr.top && pr.top < mpr.top) {
                    val prevBottom = player.prevY + player.height
                    if (prevBottom <= mpr.top + 5f) {
                        player.y = mpr.top - player.height
                        player.vy = 0f
                        // đứng trên platform di chuyển kéo theo một chút
                        player.x += mp.speed * mp.direction * (1f / 60f)
                        collided = true
                    }
                }
            }
        }

        // solid (pipes/bricks)
        for (pipe in pipes)  collided = handleSolidCollision(player, pipe) || collided
        for (brick in bricks) collided = handleSolidCollision(player, brick) || collided

        // hazards
        for (sp in spikes) if (sp.isHit(player)) { respawnPlayer(player); return true }
        for (sw in saws)   if (sw.isHit(player)) { respawnPlayer(player); return true }

        // world bottom
        if (player.y + player.height > worldHeight) { respawnPlayer(player); collided = true }

        // pickups collect (bán kính mềm)
        for (pk in entities.getPickups()) {
            if (!pk.collected) {
                val dx = (player.x + player.width/2f) - pk.x
                val dy = (player.y + player.height/2f) - pk.y
                val r = when (pk.type) {
                    "cherry" -> 20f; "strawberry" -> 25f; "apple","orange" -> 28f; "banana" -> 30f
                    else -> 25f
                }
                if (dx*dx + dy*dy < r*r) pk.onCollide(pk)
            }
        }

        // checkpoint
        for (cp in checkpoints) {
            if (!cp.activated && cp.tryActivate(player.x + player.width/2f, player.y + player.height/2f)) {
                lastCheckpointX = cp.x
                lastCheckpointY = cp.y - player.height
            }
        }

        if (handleMonsterBodyAndStomp(player)) return true

        return collided
    }

    private fun handleSolidCollision(player: Player, obstacle: RectF): Boolean {
        val pRect = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
        if (!pRect.intersect(obstacle)) return false

        val overlapLeft = pRect.right - obstacle.left
        val overlapRight = obstacle.right - pRect.left
        val overlapTop = pRect.bottom - obstacle.top
        val overlapBottom = obstacle.bottom - pRect.top

        val minX = min(overlapLeft, overlapRight)
        val minY = min(overlapTop, overlapBottom)

        if (minX < minY) {
            if (overlapLeft < overlapRight) player.x = obstacle.left - player.width
            else player.x = obstacle.right
            player.vx = 0f
        } else {
            if (overlapTop < overlapBottom) { player.y = obstacle.top - player.height; if (player.vy > 0f) player.vy = 0f }
            else { player.y = obstacle.bottom; if (player.vy < 0f) player.vy = 0f }
        }
        return true
    }

    // ADD helper trong TileMap:
    private fun handleMonsterBodyAndStomp(player: Player): Boolean {
        val pr = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
        for (m in monsters) {
            if (m.tryStompBy(player)) {           // dẫm từ trên xuống
                player.vy = -300f
                return true
            }
            if (RectF.intersects(m.getBounds(), pr)) { // đụng thân
                respawnPlayer(player)
                return true
            }
        }
        return false
    }


    private fun respawnPlayer(player: Player) {
        player.x = lastCheckpointX
        player.y = if (lastCheckpointY != 0f) lastCheckpointY else groundTopY - player.height
        player.vx = 0f
        player.vy = 0f
    }
}
