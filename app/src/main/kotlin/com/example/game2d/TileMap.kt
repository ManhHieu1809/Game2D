package com.example.game2d

import android.content.Context
import android.graphics.*
import com.example.game2d.entities.EntityManager
import com.example.game2d.entities.Monster1
import com.example.game2d.entities.Monster2
import com.example.game2d.entities.Pickup
import com.example.game2d.obstacles.Checkpoint
import com.example.game2d.obstacles.MovingPlatform
import com.example.game2d.obstacles.Saw
import com.example.game2d.obstacles.Spike
import com.example.game2d.resources.SpriteLoader
import kotlin.math.min

class TileMap(ctx: Context) : TileMapInterface {

    // World size - giống Mario 1-1 length
    override val worldWidth = 6400f  // Tương đương ~200 tiles (32px/tile)
    override val worldHeight = 720f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val groundTopY = worldHeight * 0.8f  // Higher ground như Mario

    // Hình học tĩnh
    private val platforms = ArrayList<RectF>()
    private val pipes = ArrayList<RectF>()
    private val bricks = ArrayList<RectF>()

    // Vật cản/hazard
    private val spikes = ArrayList<Spike>()
    private val saws = ArrayList<Saw>()
    private val movingPlatforms = ArrayList<MovingPlatform>()
    private val checkpoints = ArrayList<Checkpoint>()

    // Cloud - Mario style
    data class Cloud(var x: Float, var baseY: Float, var vx: Float, val type: Int = 0)
    private val clouds = ArrayList<Cloud>()

    // Entity động
    private val entities = EntityManager()
    private val monsters = mutableListOf<com.example.game2d.entities.Entity>()

    // Checkpoint - FIX: không đặt player đứng im
    private var lastCheckpointX = 100f
    private var lastCheckpointY = 0f

    init {
        SpriteLoader.preloadDefaults(ctx)
        setupMarioStyleLevel()
    }

    private fun setupMarioStyleLevel() {
        setupGroundBasedLevel()
        setupClouds()
        setupPickups()
        setupMonsters()
    }

    // Thiết kế level dựa trên ground, giống Mario thật
    private fun setupGroundBasedLevel() {

        // ===== AREA 1: Opening (0-800px) =====
        // Checkpoint đầu - đặt trên ground, không bay
        checkpoints += Checkpoint(150f, groundTopY - 40f)

        // Question blocks và bricks trên đầu - Mario style
        for (i in 0..2) {
            bricks.add(RectF(280f + i * 32f, groundTopY - 160f, 312f + i * 32f, groundTopY - 128f))
        }

        // First pipe - nhỏ, đảm bảo có đường đi qua
        pipes.add(RectF(450f, groundTopY - 64f, 482f, groundTopY))

        // Gap đầu tiên với platform dễ nhảy
        platforms.add(RectF(580f, groundTopY - 32f, 680f, groundTopY - 16f))

        // Platform cao hơn nhưng vẫn nhảy được
        platforms.add(RectF(720f, groundTopY - 64f, 780f, groundTopY - 48f))

        // Spike ở vị trí an toàn hơn
        spikes += Spike(RectF(650f, groundTopY - 16f, 682f, groundTopY))

        // Checkpoint
        checkpoints += Checkpoint(850f, groundTopY - 40f)

        // ===== AREA 2: Pipe Section (800-1600px) =====
        // Series pipes tăng dần nhưng vẫn qua được
        pipes.add(RectF(900f, groundTopY - 80f, 932f, groundTopY))   // Medium pipe
        pipes.add(RectF(1200f, groundTopY - 112f, 1232f, groundTopY)) // Large pipe
        pipes.add(RectF(1400f, groundTopY - 144f, 1432f, groundTopY)) // Extra large - giảm chiều cao

        // Brick blocks trên pipes - tạo đường nhảy
        bricks.add(RectF(1150f, groundTopY - 80f, 1182f, groundTopY - 48f))
        bricks.add(RectF(1320f, groundTopY - 112f, 1352f, groundTopY - 80f))

        // Brick pattern cho easy path
        for (i in 0..1) {
            bricks.add(RectF(1000f + i * 32f, groundTopY - 128f, 1032f + i * 32f, groundTopY - 96f))
        }

        // Checkpoint sau pipe section
        checkpoints += Checkpoint(1500f, groundTopY - 40f)

        // ===== AREA 3: Platform Jumps (1600-2400px) =====
        // Platforms với khoảng cách hợp lý để nhảy
        platforms.add(RectF(1700f, groundTopY - 48f, 1800f, groundTopY - 32f))
        platforms.add(RectF(1900f, groundTopY - 80f, 2000f, groundTopY - 64f))
        platforms.add(RectF(2100f, groundTopY - 48f, 2200f, groundTopY - 32f))

        // Platform nhỏ cho advanced path
        platforms.add(RectF(1820f, groundTopY - 96f, 1880f, groundTopY - 80f))
        platforms.add(RectF(2020f, groundTopY - 112f, 2080f, groundTopY - 96f))

        // Floating bricks ở độ cao vừa phải
        for (i in 0..3) {
            bricks.add(RectF(2050f + i * 32f, groundTopY - 144f, 2082f + i * 32f, groundTopY - 112f))
        }

        // Moving platform với range ngắn hơn
        movingPlatforms += MovingPlatform(2250f, groundTopY - 64f, 2250f, 2320f, 25f, 1)
        movingPlatforms += MovingPlatform(2380f, groundTopY - 96f, 2380f, 2420f, 20f, -1)

        // ===== AREA 4: Castle Approach (2400-3200px) =====
        // Checkpoint
        checkpoints += Checkpoint(2480f, groundTopY - 40f)

        // Stair pattern như Mario - dễ leo
        for (i in 0..4) {
            for (j in 0..i) {
                bricks.add(RectF(
                    2500f + j * 32f,
                    groundTopY - 32f - i * 32f,
                    2532f + j * 32f,
                    groundTopY - i * 32f
                ))
            }
        }

        // Bridge với gaps vừa phải - có thể nhảy qua
        platforms.add(RectF(2750f, groundTopY - 16f, 2850f, groundTopY))
        platforms.add(RectF(2920f, groundTopY - 16f, 3020f, groundTopY)) // Gap 70px thay vì 50px
        platforms.add(RectF(3090f, groundTopY - 16f, 3190f, groundTopY)) // Gap 70px

        // Hazards dưới bridge - chỉ giữa gaps
        spikes += Spike(RectF(2880f, groundTopY - 16f, 2900f, groundTopY))
        spikes += Spike(RectF(3050f, groundTopY - 16f, 3070f, groundTopY))



        // ===== AREA 5: Underground Feel (3200-4000px) =====
        // Checkpoint
        checkpoints += Checkpoint(3250f, groundTopY - 40f)

        // Low ceiling nhưng đủ cao để nhảy - 160px thay vì 200px
        for (i in 0..8) {
            bricks.add(RectF(3300f + i * 32f, groundTopY - 160f, 3332f + i * 32f, groundTopY - 128f))
        }

        // Tạo gaps trong ceiling để thoát
        // Gap 1 tại 3460-3492
        // Gap 2 tại 3620-3652

        // Platform trong underground
        platforms.add(RectF(3450f, groundTopY - 48f, 3520f, groundTopY - 32f))
        platforms.add(RectF(3600f, groundTopY - 80f, 3670f, groundTopY - 64f))

        // Pipes trong underground - thấp hơn
        pipes.add(RectF(3400f, groundTopY - 80f, 3432f, groundTopY))
        pipes.add(RectF(3700f, groundTopY - 96f, 3732f, groundTopY))

        // Saw obstacle ở vị trí cao hơn
        saws += Saw(3550f, groundTopY - 120f, 80f)

        // Moving platform với range ngắn
        movingPlatforms += MovingPlatform(3800f, groundTopY - 64f, 3780f, 3820f, 30f, 1)

        // ===== AREA 6: Final Challenge (4000-5200px) =====
        // Series gaps với timing challenge nhưng không quá khó
        platforms.add(RectF(4100f, groundTopY - 32f, 4180f, groundTopY - 16f))
        platforms.add(RectF(4250f, groundTopY - 64f, 4330f, groundTopY - 48f))
        platforms.add(RectF(4400f, groundTopY - 32f, 4480f, groundTopY - 16f))

        // Platform cao cho advanced path
        platforms.add(RectF(4150f, groundTopY - 96f, 4200f, groundTopY - 80f))
        platforms.add(RectF(4350f, groundTopY - 112f, 4400f, groundTopY - 96f))

        // Moving platforms với range hợp lý
        movingPlatforms += MovingPlatform(4550f, groundTopY - 80f, 4550f, 4600f, 35f, 1)
        movingPlatforms += MovingPlatform(4750f, groundTopY - 48f, 4720f, 4780f, 30f, -1)
        movingPlatforms += MovingPlatform(4900f, groundTopY - 96f, 4880f, 4920f, 25f, 1)

        // Saw ở vị trí cao, không cản đường
        saws += Saw(4600f, groundTopY - 120f, 100f)

        // Final checkpoint
        checkpoints += Checkpoint(5000f, groundTopY - 40f)

        // ===== AREA 7: Castle/Flag (5200-6400px) =====
        // Flag pole area - victory stairs dễ leo
        for (i in 0..6) { // Giảm từ 7 xuống 6 để không quá cao
            for (j in 0..i) {
                bricks.add(RectF(
                    5400f + j * 32f,
                    groundTopY - 32f - i * 32f,
                    5432f + j * 32f,
                    groundTopY - i * 32f
                ))
            }
        }

        // Platform trước castle
        platforms.add(RectF(5200f, groundTopY - 48f, 5300f, groundTopY - 32f))

        // Castle entrance - không quá cao
        for (i in 0..2) { // Giảm từ 3 xuống 2
            bricks.add(RectF(5800f + i * 32f, groundTopY - 96f, 5832f + i * 32f, groundTopY - 64f))
            bricks.add(RectF(5800f + i * 32f, groundTopY - 64f, 5832f + i * 32f, groundTopY - 32f))
        }

        // Final checkpoint
        checkpoints += Checkpoint(6000f, groundTopY - 40f)
    }

    private fun setupClouds() {
        // Mario-style clouds - regular pattern
        for (i in 0..12) {
            val x = 200f + i * 450f
            val y = 100f + (i % 3) * 20f
            clouds += Cloud(x, y, -8f - (i % 3) * 2f, i % 2)
        }
    }

    private fun setupPickups() {
        // Coins in logical Mario positions - cập nhật theo tilemap mới

        // Area 1: Opening area
        entities.addPickup(Pickup("cherry", 312f, groundTopY - 190f))
        entities.addPickup(Pickup("banana", 750f, groundTopY - 95f)) // Trên platform cao (adjusted)

        // Area 2: Pipe section
        entities.addPickup(Pickup("apple", 916f, groundTopY - 110f)) // Trên pipe (adjusted)
        entities.addPickup(Pickup("orange", 1016f, groundTopY - 158f)) // Trên brick pattern (adjusted)
        entities.addPickup(Pickup("cherry", 1216f, groundTopY - 142f)) // Trên pipe (adjusted)
        entities.addPickup(Pickup("strawberry", 1336f, groundTopY - 142f)) // Trên brick (adjusted)
        entities.addPickup(Pickup("banana", 1416f, groundTopY - 174f)) // Trên extra large pipe (adjusted)

        // Area 3: Platform jumps
        entities.addPickup(Pickup("apple", 1750f, groundTopY - 78f)) // Platform (adjusted)
        entities.addPickup(Pickup("orange", 1850f, groundTopY - 126f)) // Platform cao (adjusted)
        entities.addPickup(Pickup("cherry", 1950f, groundTopY - 110f)) // Platform (adjusted)
        entities.addPickup(Pickup("strawberry", 2050f, groundTopY - 142f)) // Platform cao (adjusted)
        entities.addPickup(Pickup("banana", 2150f, groundTopY - 78f)) // Platform (adjusted)

        // Above floating bricks
        val fruits = arrayOf("cherry", "apple", "strawberry", "orange")
        for (i in 0..3) {
            entities.addPickup(Pickup(fruits[i], 2066f + i * 32f, groundTopY - 174f)) // Adjusted height
        }

        // Moving platform rewards - adjusted positions
        entities.addPickup(Pickup("apple", 2285f, groundTopY - 94f)) // Platform di động 1 (adjusted)
        entities.addPickup(Pickup("orange", 2400f, groundTopY - 126f)) // Platform di động 2 (adjusted)

        // Area 4: Castle approach
        entities.addPickup(Pickup("banana", 2550f, groundTopY - 62f)) // Stair (adjusted)
        entities.addPickup(Pickup("cherry", 2600f, groundTopY - 94f)) // Stair cao hơn (adjusted)
        entities.addPickup(Pickup("strawberry", 2650f, groundTopY - 126f)) // Stair cao nhất (adjusted)

        // Bridge section - rủi ro cao
        entities.addPickup(Pickup("apple", 2800f, groundTopY - 46f)) // Bridge (adjusted)
        entities.addPickup(Pickup("orange", 2970f, groundTopY - 46f)) // Bridge center (adjusted)
        entities.addPickup(Pickup("banana", 3140f, groundTopY - 46f)) // Bridge (adjusted)

        // Area 5: Underground
        entities.addPickup(Pickup("cherry", 3485f, groundTopY - 78f)) // Platform underground (adjusted)
        entities.addPickup(Pickup("strawberry", 3635f, groundTopY - 110f)) // Platform cao (adjusted)
        entities.addPickup(Pickup("apple", 3716f, groundTopY - 126f)) // Gần pipe (adjusted)
        entities.addPickup(Pickup("orange", 3800f, groundTopY - 94f)) // Moving platform (adjusted)

        // Area 6: Final challenge - vật phẩm quý
        entities.addPickup(Pickup("banana", 4140f, groundTopY - 62f)) // Platform (adjusted)
        entities.addPickup(Pickup("apple", 4175f, groundTopY - 126f)) // Platform cao (adjusted)
        entities.addPickup(Pickup("cherry", 4290f, groundTopY - 94f)) // Platform (adjusted)
        entities.addPickup(Pickup("orange", 4375f, groundTopY - 142f)) // Platform cao (adjusted)
        entities.addPickup(Pickup("strawberry", 4440f, groundTopY - 62f)) // Platform (adjusted)

        // Moving platform rewards trong final challenge
        entities.addPickup(Pickup("apple", 4575f, groundTopY - 110f)) // Moving platform 1 (adjusted)
        entities.addPickup(Pickup("orange", 4750f, groundTopY - 78f)) // Moving platform 2 (adjusted)
        entities.addPickup(Pickup("banana", 4900f, groundTopY - 126f)) // Moving platform 3 (adjusted)

        // Area 7: Victory area
        entities.addPickup(Pickup("cherry", 5250f, groundTopY - 78f)) // Platform trước castle (adjusted)

        // Victory fruits on stairs - thưởng lớn
        for (i in 0..4) {
            entities.addPickup(Pickup(fruits[i % 4], 5450f + i * 40f, groundTopY - 60f - i * 32f))
        }

        // Final reward
        entities.addPickup(Pickup("strawberry", 5950f, groundTopY - 60f)) // Gần flag
    }

    private fun setupMonsters() {
        // Strategic monster placement - cân bằng độ khó

        // Area 1: Opening - tutorial enemy
        monsters += Monster1(650f, groundTopY - 32f, patrolWidth = 80f, detectRange = 250f)

        // Area 2: Pipe section - guards
        monsters += Monster2(1050f, groundTopY - 34f * 1.6f, patrolWidth = 120f, scaleOverride = 1.6f)
        monsters += Monster1(1300f, groundTopY - 32f, patrolWidth = 100f, detectRange = 280f)

        // Area 3: Platform section - timing challenge
        monsters += Monster1(1750f, groundTopY - 32f, patrolWidth = 120f, detectRange = 300f) // Guard platform area
        monsters += Monster2(2050f, groundTopY - 34f * 1.6f, patrolWidth = 100f, scaleOverride = 1.6f) // Near moving platforms

        // Area 4: Castle approach - bridge guards
        monsters += Monster1(2800f, groundTopY - 32f, patrolWidth = 80f, detectRange = 250f) // Bridge guard 1
        monsters += Monster2(3000f, groundTopY - 34f * 1.6f, patrolWidth = 60f, scaleOverride = 1.6f) // Bridge guard 2

//        // Area 5: Underground - patrol monsters
//        monsters += Monster1(3500f, groundTopY - 32f, patrolWidth = 150f, detectRange = 320f) // Underground patrol
        monsters += Monster2(3750f, groundTopY - 34f * 1.6f, patrolWidth = 140f, scaleOverride = 1.6f) // Near moving platform

        // Area 6: Final challenge - elite guards
        monsters += Monster1(4200f, groundTopY - 32f, patrolWidth = 100f, detectRange = 350f) // Platform guardian
        monsters += Monster2(4500f, groundTopY - 34f * 1.6f, patrolWidth = 120f, scaleOverride = 1.6f) // Moving platform area
        monsters += Monster1(4800f, groundTopY - 32f, patrolWidth = 150f, detectRange = 400f) // Final area guard

        // Area 7: Castle - final boss approach
        monsters += Monster2(5300f, groundTopY - 34f * 1.6f, patrolWidth = 180f, scaleOverride = 1.6f) // Castle guardian
    }

    override fun getGroundTopY(): Float = groundTopY

    // ===================== DRAW =====================
    override fun draw(canvas: Canvas) {
        // Mario-style sky
        val skyPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, groundTopY,
                Color.rgb(107, 140, 255),  // Mario blue
                Color.rgb(132, 168, 255),  // Lighter blue
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, worldWidth, groundTopY, skyPaint)

        // Mario-style clouds
        paint.shader = null
        paint.color = Color.WHITE
        for (c in clouds) {
            drawMarioCloud(canvas, c.x, c.baseY, c.type)
        }

        // Ground - simple Mario style
        paint.color = Color.rgb(34, 139, 34)  // Mario green
        canvas.drawRect(0f, groundTopY, worldWidth, worldHeight, paint)

        // Underground brown
        paint.color = Color.rgb(101, 67, 33)
        canvas.drawRect(0f, groundTopY + 16f, worldWidth, worldHeight, paint)

        drawPlatforms(canvas)
        drawPipes(canvas)
        bricks.forEach { drawMarioBrick(canvas, it) }

        // Obstacles
        spikes.forEach { it.draw(canvas) }
        saws.forEach { it.draw(canvas) }
        movingPlatforms.forEach { it.draw(canvas) }
        checkpoints.forEach { it.draw(canvas) }

        // Entities
        entities.drawAll(canvas)
        drawMonsters(canvas)

        // World border
        paint.color = Color.DKGRAY
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(0f, 0f, worldWidth, worldHeight, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawMarioCloud(canvas: Canvas, x: Float, y: Float, type: Int) {
        paint.color = Color.WHITE

        when (type) {
            0 -> {
                // Small cloud
                canvas.drawCircle(x, y, 24f, paint)
                canvas.drawCircle(x + 20f, y, 28f, paint)
                canvas.drawCircle(x + 40f, y, 24f, paint)
                canvas.drawCircle(x + 12f, y + 12f, 20f, paint)
                canvas.drawCircle(x + 28f, y + 12f, 24f, paint)
            }
            1 -> {
                // Large cloud
                canvas.drawCircle(x, y, 28f, paint)
                canvas.drawCircle(x + 24f, y, 32f, paint)
                canvas.drawCircle(x + 48f, y, 28f, paint)
                canvas.drawCircle(x + 68f, y, 24f, paint)
                canvas.drawCircle(x + 16f, y + 16f, 24f, paint)
                canvas.drawCircle(x + 36f, y + 16f, 28f, paint)
                canvas.drawCircle(x + 56f, y + 16f, 24f, paint)
            }
        }
    }

    private fun drawPlatforms(canvas: Canvas) {
        // Simple Mario-style platforms
        for (p in platforms) {
            // Main platform - brown
            paint.color = Color.rgb(101, 67, 33)
            canvas.drawRect(p, paint)

            // Top surface - green if thin, brown if thick
            if (p.height() <= 20f) {
                paint.color = Color.rgb(34, 139, 34)
                canvas.drawRect(p.left, p.top, p.right, p.top + 4f, paint)
            }
        }
    }

    private fun drawPipes(canvas: Canvas) {
        // Mario-style pipes
        for (pipe in pipes) {
            // Main body - green
            paint.color = Color.rgb(0, 128, 0)
            canvas.drawRect(pipe, paint)

            // Highlights
            paint.color = Color.rgb(50, 205, 50)
            canvas.drawRect(pipe.left + 2f, pipe.top, pipe.left + 6f, pipe.bottom, paint)
            canvas.drawRect(pipe.right - 6f, pipe.top, pipe.right - 2f, pipe.bottom, paint)

            // Cap
            paint.color = Color.rgb(0, 100, 0)
            canvas.drawRect(pipe.left - 4f, pipe.top - 2f, pipe.right + 4f, pipe.top + 12f, paint)

            // Cap highlight
            paint.color = Color.rgb(30, 150, 30)
            canvas.drawRect(pipe.left - 2f, pipe.top, pipe.right + 2f, pipe.top + 6f, paint)
        }
    }

    private fun drawMarioBrick(canvas: Canvas, brick: RectF) {
        // Mario-style brick
        paint.color = Color.rgb(139, 69, 19)
        canvas.drawRect(brick, paint)

        // Brick pattern - simple
        paint.color = Color.rgb(160, 82, 45)
        canvas.drawRect(brick.left + 1f, brick.top + 1f, brick.right - 1f, brick.bottom - 1f, paint)

        // Inner lines
        paint.color = Color.rgb(101, 67, 33)
        paint.strokeWidth = 0.5f
        canvas.drawLine(brick.left, brick.centerY(), brick.right, brick.centerY(), paint)
        canvas.drawLine(brick.centerX(), brick.top, brick.centerX(), brick.bottom, paint)
    }

    private fun drawMonsters(canvas: Canvas) {
        for (m in monsters) m.draw(canvas)
    }

    // ===================== UPDATE =====================
    override fun update(deltaMs: Long) {
        val dt = deltaMs / 1000f

        movingPlatforms.forEach { it.update(dt) }
        saws.forEach { it.update(dt) }
        checkpoints.forEach { it.update(deltaMs) }

        // Simple cloud movement
        clouds.forEach {
            it.x += it.vx * dt
            if (it.x < -100f) it.x = worldWidth + 100f
            if (it.x > worldWidth + 100f) it.x = -100f
        }

        entities.updateAll(deltaMs)
    }


    override fun updateMonsters(deltaMs: Long, player: Player) {
        for (m in monsters) {
            when (m) {
                is Monster1 -> m.update(deltaMs, player)
                is Monster2 -> m.update(deltaMs, player)
                else -> {
                    try {
                        val method = m.javaClass.getMethod("update", Long::class.java)
                        method.invoke(m, java.lang.Long.valueOf(deltaMs))
                    } catch (_: Exception) {}
                }
            }
        }
        monsters.removeAll { it is Monster1 && !it.isAlive() }
        monsters.removeAll { it is Monster2 && !it.isAlive() }
    }

    override fun checkBulletHitAndRespawnIfNeeded(player: Player) {
        val hit = monsters.any {
            it is Monster1 && it.bulletHitPlayer(player)
        }
        if (hit) {
            respawnPlayer(player)
        }
    }

    // ===================== COLLISION =====================
    override fun resolvePlayerCollision(player: Player): Boolean {
        var collided = false

        // World bounds
        if (player.x < 0f) { player.x = 0f; player.vx = 0f; collided = true }
        if (player.x + player.width > worldWidth) {
            player.x = worldWidth - player.width; player.vx = 0f; collided = true
        }
        if (player.y < 0f) { player.y = 0f; if (player.vy < 0f) player.vy = 0f; collided = true }

        // Ground collision
        if (player.y + player.height > groundTopY && player.vy >= 0f) {
            player.y = groundTopY - player.height
            player.vy = 0f
            collided = true
        }

        // Platform collisions - only from above
        for (p in platforms) {
            val pr = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
            if (pr.right > p.left && pr.left < p.right && player.vy > 0f) {
                if (pr.bottom > p.top && pr.top < p.top) {
                    val prevBottom = player.prevY + player.height
                    if (prevBottom <= p.top + 3f) {
                        player.y = p.top - player.height
                        player.vy = 0f
                        collided = true
                    }
                }
            }
        }

        // Moving platform collisions
        for (mp in movingPlatforms) {
            val mpr = mp.rect()
            val pr = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
            if (pr.right > mpr.left && pr.left < mpr.right && player.vy > 0f) {
                if (pr.bottom > mpr.top && pr.top < mpr.top) {
                    val prevBottom = player.prevY + player.height
                    if (prevBottom <= mpr.top + 3f) {
                        player.y = mpr.top - player.height
                        player.vy = 0f
                        // Move with platform
                        player.x += mp.speed * mp.direction * (1f / 60f)
                        collided = true
                    }
                }
            }
        }

        // Solid collisions
        for (pipe in pipes) collided = handleSolidCollision(player, pipe) || collided
        for (brick in bricks) collided = handleSolidCollision(player, brick) || collided

        // Hazards
        for (sp in spikes) if (sp.isHit(player)) { respawnPlayer(player); return true }
        for (sw in saws) if (sw.isHit(player)) { respawnPlayer(player); return true }

        // World death
        if (player.y + player.height > worldHeight) {
            respawnPlayer(player)
            collided = true
        }

        for (pk in entities.getPickups()) {
            if (!pk.collected) {
                val dx = (player.x + player.width/2f) - pk.x
                val dy = (player.y + player.height/2f) - pk.y
                val r = 30f
                if (dx*dx + dy*dy < r*r) {
                    pk.onCollide(pk)
                }
            }
        }

        for (cp in checkpoints) {
            if (!cp.activated && cp.tryActivate(player, 40f)) {
                lastCheckpointX = cp.x + 60f
                lastCheckpointY = groundTopY - player.height
            }
        }

        // Monster interactions
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
            if (overlapLeft < overlapRight) {
                player.x = obstacle.left - player.width
            } else {
                player.x = obstacle.right
            }
            player.vx = 0f
        } else {
            if (overlapTop < overlapBottom) {
                player.y = obstacle.top - player.height
                if (player.vy > 0f) player.vy = 0f
            } else {
                player.y = obstacle.bottom
                if (player.vy < 0f) player.vy = 0f
            }
        }
        return true
    }

    private fun handleMonsterBodyAndStomp(player: Player): Boolean {
        val pr = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
        val it = monsters.iterator()
        while (it.hasNext()) {
            val m = it.next()
            if (m is Monster1) {
                if (!m.isAlive()) { it.remove(); continue }
                if (m.tryStompBy(player)) {
                    it.remove()
                    player.y = m.y - player.height - 1f
                    player.vy = -280f
                    return true
                }
                if (RectF.intersects(m.getBounds(), pr)) {
                    respawnPlayer(player)
                    return true
                }
            } else if (m is Monster2) {
                if (!m.isAlive()) { it.remove(); continue }
                if (m.tryStompBy(player)) {
                    it.remove()
                    player.y = m.y - player.height - 1f
                    player.vy = -280f
                    return true
                }
                if (RectF.intersects(m.getBounds(), pr)) {
                    respawnPlayer(player)
                    return true
                }
            }
            // Các monster type khác có thể xử lý ở đây nếu cần
        }
        return false
    }


    private fun respawnPlayer(player: Player) {
        player.x = lastCheckpointX
        player.y = lastCheckpointY
        player.vx = 0f
        player.vy = 0f
    }

    // Method to check if player reached the final checkpoint (end of tilemap 1)
    override fun isCompleted(player: Player): Boolean {
        return player.x >= 6000f  // Near the final checkpoint of tilemap 1
    }
}
