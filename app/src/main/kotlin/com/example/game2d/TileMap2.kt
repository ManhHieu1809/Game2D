package com.example.game2d

import android.content.Context
import android.graphics.*
import com.example.game2d.entities.EntityManager
import com.example.game2d.entities.HealthPickup
import com.example.game2d.entities.Monster1
import com.example.game2d.entities.Monster2
import com.example.game2d.entities.Pickup
import com.example.game2d.obstacles.Checkpoint
import com.example.game2d.obstacles.MovingPlatform
import com.example.game2d.obstacles.Saw
import com.example.game2d.obstacles.Spike
import com.example.game2d.resources.SpriteLoader
import kotlin.math.min

class TileMap2(ctx: Context) : TileMapInterface {

    override val worldWidth = 8000f
    override val worldHeight = 720f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val groundTopY = worldHeight * 0.8f

    private val platforms = ArrayList<RectF>()
    private val pipes = ArrayList<RectF>()
    private val bricks = ArrayList<RectF>()

    private val spikes = ArrayList<Spike>()
    private val saws = ArrayList<Saw>()
    private val movingPlatforms = ArrayList<MovingPlatform>()
    private val checkpoints = ArrayList<Checkpoint>()

    data class Cloud(var x: Float, var baseY: Float, var vx: Float, val type: Int = 0)
    private val clouds = ArrayList<Cloud>()

    private val entities = EntityManager()
    private val monsters = mutableListOf<com.example.game2d.entities.Entity>()

    private var lastCheckpointX = 100f
    private var lastCheckpointY = 0f

    init {
        SpriteLoader.preloadDefaults(ctx)
        setupLevel2()
    }

    private fun setupLevel2() {
        setupAdvancedLevel()
        setupClouds()
        setupPickups()
        setupMonsters()
    }

    private fun setupAdvancedLevel() {

        checkpoints += Checkpoint(150f, groundTopY - 40f)

        pipes.add(RectF(300f, groundTopY - 64f, 332f, groundTopY))
        pipes.add(RectF(450f, groundTopY - 80f, 482f, groundTopY))
        pipes.add(RectF(600f, groundTopY - 96f, 632f, groundTopY))

        platforms.add(RectF(350f, groundTopY - 48f, 420f, groundTopY - 32f))
        platforms.add(RectF(500f, groundTopY - 32f, 580f, groundTopY - 16f))
        platforms.add(RectF(650f, groundTopY - 48f, 750f, groundTopY - 32f))

        for (i in 0..2) {
            bricks.add(RectF(380f + i * 32f, groundTopY - 200f, 412f + i * 32f, groundTopY - 168f))  // Cao hơn
        }
        for (i in 0..3) {
            bricks.add(RectF(520f + i * 32f, groundTopY - 220f, 552f + i * 32f, groundTopY - 188f))  // Cao hơn
        }

        platforms.add(RectF(780f, groundTopY - 32f, 850f, groundTopY - 16f))  // Platform thay spike

        movingPlatforms += MovingPlatform(870f, groundTopY - 48f, 850f, 900f, 20f, 1)  // Chậm hơn, range nhỏ

        checkpoints += Checkpoint(950f, groundTopY - 40f)

        for (i in 0..3) {
            for (j in 0..(3-i)) {
                bricks.add(RectF(
                    1100f + j * 32f,
                    groundTopY - 32f - i * 24f,
                    1132f + j * 32f,
                    groundTopY - 8f - i * 24f
                ))
            }
        }


        platforms.add(RectF(1250f, groundTopY - 80f, 1400f, groundTopY - 64f))
        platforms.add(RectF(1450f, groundTopY - 64f, 1550f, groundTopY - 48f))
        platforms.add(RectF(1600f, groundTopY - 48f, 1700f, groundTopY - 32f))

        platforms.add(RectF(1350f, groundTopY - 112f, 1400f, groundTopY - 96f))
        platforms.add(RectF(1500f, groundTopY - 96f, 1550f, groundTopY - 80f))

        platforms.add(RectF(1720f, groundTopY - 32f, 1800f, groundTopY - 16f))
        platforms.add(RectF(1820f, groundTopY - 48f, 1900f, groundTopY - 32f))

        movingPlatforms += MovingPlatform(1950f, groundTopY - 64f, 1930f, 1980f, 20f, 1)  // Chậm, range nhỏ
        movingPlatforms += MovingPlatform(2050f, groundTopY - 48f, 2030f, 2080f, 15f, -1) // Rất chậm

        checkpoints += Checkpoint(2150f, groundTopY - 40f)

        for (i in 0..15) {
            bricks.add(RectF(2200f + i * 32f, groundTopY - 240f, 2232f + i * 32f, groundTopY - 208f))  // Cao hơn nhiều
        }

        platforms.add(RectF(2300f, groundTopY - 48f, 2450f, groundTopY - 32f))
        platforms.add(RectF(2500f, groundTopY - 32f, 2650f, groundTopY - 16f))
        platforms.add(RectF(2700f, groundTopY - 48f, 2850f, groundTopY - 32f))

        platforms.add(RectF(2350f, groundTopY - 120f, 2400f, groundTopY - 104f))
        platforms.add(RectF(2550f, groundTopY - 100f, 2600f, groundTopY - 84f))

        movingPlatforms += MovingPlatform(2900f, groundTopY - 64f, 2880f, 2920f, 15f, 1)
        movingPlatforms += MovingPlatform(3000f, groundTopY - 48f, 2980f, 3020f, 12f, -1)

        pipes.add(RectF(2750f, groundTopY - 64f, 2782f, groundTopY - 32f))
        pipes.add(RectF(3100f, groundTopY - 80f, 3132f, groundTopY))


        platforms.add(RectF(3150f, groundTopY - 64f, 3250f, groundTopY - 48f))


        for (i in 0..3) {
            platforms.add(RectF(3300f + i * 60f, groundTopY - 24f - i * 16f, 3350f + i * 60f, groundTopY - 8f - i * 16f))  // Thấp hơn, gần hơn
        }

        // Checkpoint cave exit
        checkpoints += Checkpoint(3480f, groundTopY - 40f)

        platforms.add(RectF(3600f, groundTopY - 64f, 3720f, groundTopY - 48f))   // Thấp hơn, dài hơn
        platforms.add(RectF(3760f, groundTopY - 80f, 3860f, groundTopY - 64f))   // Gap nhỏ hơn
        platforms.add(RectF(3900f, groundTopY - 64f, 4000f, groundTopY - 48f))   // Thấp hơn
        platforms.add(RectF(4040f, groundTopY - 96f, 4140f, groundTopY - 80f))   // Thấp hơn
        platforms.add(RectF(4180f, groundTopY - 64f, 4280f, groundTopY - 48f))   // Thấp hơn

        // Advanced route platforms - không quá cao
        platforms.add(RectF(3680f, groundTopY - 120f, 3720f, groundTopY - 104f)) // Thấp hơn
        platforms.add(RectF(3820f, groundTopY - 140f, 3860f, groundTopY - 124f)) // Thấp hơn
        platforms.add(RectF(3960f, groundTopY - 120f, 4000f, groundTopY - 104f)) // Thấp hơn

        movingPlatforms += MovingPlatform(4320f, groundTopY - 80f, 4300f, 4350f, 25f, 1)  // Thấp hơn
        movingPlatforms += MovingPlatform(4450f, groundTopY - 64f, 4430f, 4480f, 20f, -1) // Thấp hơn
        movingPlatforms += MovingPlatform(4580f, groundTopY - 96f, 4560f, 4610f, 18f, 1)  // Thấp hơn

        platforms.add(RectF(4650f, groundTopY - 48f, 4750f, groundTopY - 32f))

        // Floating brick formations thấp hơn
        for (i in 0..2) {
            bricks.add(RectF(4050f + i * 32f, groundTopY - 160f, 4082f + i * 32f, groundTopY - 128f))  // Thấp hơn nhiều
        }

        // Island checkpoint
        checkpoints += Checkpoint(4800f, groundTopY - 40f)

        for (i in 0..3) {
            bricks.add(RectF(5100f + i * 32f, groundTopY - 96f, 5132f + i * 32f, groundTopY - 64f))   // Thấp hơn
            bricks.add(RectF(5100f + i * 32f, groundTopY - 64f, 5132f + i * 32f, groundTopY - 32f))
        }


        for (i in 0..2) {
            for (j in 0..2) {
                bricks.add(RectF(
                    5300f + i * 120f + j * 32f,
                    groundTopY - 96f + j * 16f,
                    5332f + i * 120f + j * 32f,
                    groundTopY - 80f + j * 16f
                ))
            }
        }


        platforms.add(RectF(5200f, groundTopY - 16f, 5280f, groundTopY))
        platforms.add(RectF(5580f, groundTopY - 16f, 5660f, groundTopY))


        platforms.add(RectF(5280f, groundTopY - 16f, 5580f, groundTopY))

        platforms.add(RectF(5700f, groundTopY - 32f, 5850f, groundTopY - 16f))
        platforms.add(RectF(5720f, groundTopY - 80f, 5830f, groundTopY - 64f))
        platforms.add(RectF(5750f, groundTopY - 128f, 5800f, groundTopY - 112f))

        // Throne room dễ hơn
        platforms.add(RectF(6000f, groundTopY - 48f, 6200f, groundTopY - 32f))


        for (i in 0..2) {
            bricks.add(RectF(
                6050f + i * 40f,
                groundTopY - 48f - i * 12f,
                6090f + i * 40f,
                groundTopY - 32f - i * 12f
            ))
        }

        checkpoints += Checkpoint(6250f, groundTopY - 40f)

        platforms.add(RectF(6500f, groundTopY - 32f, 6650f, groundTopY - 16f))  // Sớm hơn
        platforms.add(RectF(6700f, groundTopY - 48f, 6800f, groundTopY - 32f))
        platforms.add(RectF(6850f, groundTopY - 32f, 6950f, groundTopY - 16f))   // Thấp hơn
        platforms.add(RectF(7000f, groundTopY - 48f, 7100f, groundTopY - 32f))

        // Garden decorations (pipes as trees) - thấp hơn
        pipes.add(RectF(6600f, groundTopY - 48f, 6620f, groundTopY))  // Thấp hơn
        pipes.add(RectF(6750f, groundTopY - 64f, 6770f, groundTopY))
        pipes.add(RectF(6900f, groundTopY - 48f, 6920f, groundTopY))  // Thấp hơn

        // Final victory platform
        platforms.add(RectF(7200f, groundTopY - 64f, 7400f, groundTopY - 48f))  // Dài hơn

        // Victory stairs dễ leo
        for (i in 0..4) {  // Giảm từ 5 xuống 4
            for (j in 0..i) {
                bricks.add(RectF(
                    7450f + j * 32f,
                    groundTopY - 16f - i * 16f,   // Step nhỏ hơn
                    7482f + j * 32f,
                    groundTopY - i * 16f
                ))
            }
        }

        // Final checkpoint - end of tilemap 2
        checkpoints += Checkpoint(7700f, groundTopY - 40f)  // Sớm hơn
    }

    private fun setupClouds() {
        // More clouds for tilemap 2
        for (i in 0..15) {
            val x = 300f + i * 500f
            val y = 80f + (i % 4) * 25f
            clouds += Cloud(x, y, -10f - (i % 3) * 3f, i % 2)
        }
    }

    private fun setupPickups() {
        // Strategic pickups throughout tilemap 2 - CẬP NHẬT THEO THIẾT KẾ MỚI

        // Area 1: Forest - cập nhật vị trí theo platforms mới
        entities.addPickup(Pickup("apple", 385f, groundTopY - 78f))     // Trên platform đầu
        entities.addPickup(Pickup("banana", 540f, groundTopY - 62f))    // Trên platform thứ 2
        entities.addPickup(Pickup("cherry", 700f, groundTopY - 78f))    // Trên platform thứ 3
        entities.addPickup(Pickup("orange", 825f, groundTopY - 62f))    // Trên platform thay spike
        entities.addPickup(Pickup("strawberry", 885f, groundTopY - 78f)) // Trên moving platform

        // Thêm health pickups trong area 1
        entities.addHealthPickup(HealthPickup(450f, groundTopY - 50f, 1, "health_potion"))

        // Area 2: Gentle Hills - vị trí dễ lấy hơn
        entities.addPickup(Pickup("banana", 1125f, groundTopY - 62f))   // Trên stair
        entities.addPickup(Pickup("apple", 1325f, groundTopY - 110f))   // Trên platform
        entities.addPickup(Pickup("cherry", 1500f, groundTopY - 94f))   // Trên platform
        entities.addPickup(Pickup("orange", 1650f, groundTopY - 78f))   // Trên platform
        entities.addPickup(Pickup("strawberry", 1750f, groundTopY - 62f)) // Trên platform mới
        entities.addPickup(Pickup("banana", 1860f, groundTopY - 78f))   // Trên platform mới
        entities.addPickup(Pickup("apple", 1975f, groundTopY - 94f))    // Trên moving platform

        // Thêm health pickups trong area 2
        entities.addHealthPickup(HealthPickup(1375f, groundTopY - 142f, 1, "health_potion"))
        entities.addHealthPickup(HealthPickup(1875f, groundTopY - 78f, 2, "big_health"))

        // Area 3: Easy Cave - vị trí đơn giản
        entities.addPickup(Pickup("apple", 2375f, groundTopY - 78f))    // Trên platform dài
        entities.addPickup(Pickup("banana", 2375f, groundTopY - 150f))  // Trên platform cao
        entities.addPickup(Pickup("cherry", 2575f, groundTopY - 62f))   // Trên platform ground level
        entities.addPickup(Pickup("orange", 2575f, groundTopY - 130f))  // Trên platform cao
        entities.addPickup(Pickup("strawberry", 2775f, groundTopY - 78f)) // Trên platform
        entities.addPickup(Pickup("apple", 2925f, groundTopY - 94f))    // Trên moving platform
        entities.addPickup(Pickup("cherry", 3200f, groundTopY - 94f))   // Trên platform mới

        // Thêm health pickups trong cave (nơi khó khăn)
        entities.addHealthPickup(HealthPickup(2375f, groundTopY - 180f, 1, "health_potion")) // Trên platform cao
        entities.addHealthPickup(HealthPickup(2925f, groundTopY - 124f, 1, "health_potion"))
        entities.addHealthPickup(HealthPickup(3200f, groundTopY - 124f, 2, "big_health"))

        // Area 4: Sky Platforms - vị trí hợp lý hơn
        entities.addPickup(Pickup("strawberry", 3660f, groundTopY - 94f)) // Thấp hơn
        entities.addPickup(Pickup("banana", 3810f, groundTopY - 110f))    // Thấp hơn
        entities.addPickup(Pickup("apple", 3950f, groundTopY - 94f))      // Thấp hơn
        entities.addPickup(Pickup("cherry", 4090f, groundTopY - 126f))    // Thấp hơn
        entities.addPickup(Pickup("orange", 4230f, groundTopY - 94f))     // Thấp hơn

        // Moving platform rewards - dễ lấy hơn
        entities.addPickup(Pickup("strawberry", 4345f, groundTopY - 110f)) // Thấp hơn
        entities.addPickup(Pickup("banana", 4465f, groundTopY - 94f))      // Thấp hơn
        entities.addPickup(Pickup("apple", 4595f, groundTopY - 126f))      // Thấp hơn
        entities.addPickup(Pickup("cherry", 4700f, groundTopY - 78f))      // Trên platform mới

        // Thêm health pickups trong sky platforms (area khó)
        entities.addHealthPickup(HealthPickup(3700f, groundTopY - 150f, 1, "health_potion"))
        entities.addHealthPickup(HealthPickup(4120f, groundTopY - 156f, 2, "big_health"))
        entities.addHealthPickup(HealthPickup(4450f, groundTopY - 124f, 1, "health_potion"))

        // Area 5: Simple Castle - vị trí dễ lấy
        entities.addPickup(Pickup("cherry", 5775f, groundTopY - 62f))      // Thấp hơn
        entities.addPickup(Pickup("orange", 5775f, groundTopY - 110f))     // Thấp hơn
        entities.addPickup(Pickup("apple", 6100f, groundTopY - 78f))       // Thấp hơn
        entities.addPickup(Pickup("strawberry", 6075f, groundTopY - 78f))  // Trên stair

        // Thêm health pickups trong castle (boss area)
        entities.addHealthPickup(HealthPickup(5400f, groundTopY - 50f, 2, "big_health"))
        entities.addHealthPickup(HealthPickup(5775f, groundTopY - 140f, 1, "health_potion"))
        entities.addHealthPickup(HealthPickup(6075f, groundTopY - 108f, 3, "super_health")) // Super health trước boss

        // Area 6: Victory Garden - giữ nguyên vì đã dễ
        entities.addPickup(Pickup("banana", 6575f, groundTopY - 62f))
        entities.addPickup(Pickup("apple", 6750f, groundTopY - 78f))
        entities.addPickup(Pickup("cherry", 6900f, groundTopY - 62f))     // Thấp hơn
        entities.addPickup(Pickup("orange", 7050f, groundTopY - 78f))
        entities.addPickup(Pickup("strawberry", 7300f, groundTopY - 94f))

        // Victory health rewards
        entities.addHealthPickup(HealthPickup(6750f, groundTopY - 108f, 2, "big_health"))
        entities.addHealthPickup(HealthPickup(7300f, groundTopY - 124f, 3, "super_health"))

        // Victory rewards - dễ lấy hơn
        val victoryFruits = arrayOf("strawberry", "banana", "apple", "cherry", "orange")
        for (i in 0..4) {
            entities.addPickup(Pickup(victoryFruits[i], 7500f + i * 40f, groundTopY - 50f - i * 16f)) // Thấp hơn, gần hơn
        }

        // Final super health reward
        entities.addHealthPickup(HealthPickup(7600f, groundTopY - 130f, 3, "super_health"))
    }

    private fun setupMonsters() {
        // More challenging monster placement for tilemap 2

        // Area 1: Forest guards
        monsters += Monster1(400f, groundTopY - 32f, patrolWidth = 100f, detectRange = 300f)
        monsters += Monster2(650f, groundTopY - 34f * 1.8f, patrolWidth = 120f, scaleOverride = 1.8f)

        // Area 2: Mountain climbers
        monsters += Monster1(1200f, groundTopY - 32f, patrolWidth = 80f, detectRange = 250f)
        monsters += Monster2(1500f, groundTopY - 34f * 1.8f, patrolWidth = 100f, scaleOverride = 1.8f)
        monsters += Monster1(1800f, groundTopY - 32f, patrolWidth = 120f, detectRange = 350f)

        // Area 3: Cave dwellers
        monsters += Monster2(2400f, groundTopY - 34f * 1.8f, patrolWidth = 150f, scaleOverride = 1.8f)
        monsters += Monster1(2800f, groundTopY - 32f, patrolWidth = 100f, detectRange = 300f)
        monsters += Monster2(3200f, groundTopY - 34f * 1.8f, patrolWidth = 140f, scaleOverride = 1.8f)

        // Area 4: Sky guardians
        monsters += Monster1(3700f, groundTopY - 32f, patrolWidth = 120f, detectRange = 400f)
        monsters += Monster2(4100f, groundTopY - 34f * 1.8f, patrolWidth = 100f, scaleOverride = 1.8f)
        monsters += Monster1(4400f, groundTopY - 32f, patrolWidth = 150f, detectRange = 450f)

        // Area 5: Castle guards - elite
        monsters += Monster2(5200f, groundTopY - 34f * 2.0f, patrolWidth = 80f, scaleOverride = 2.0f)
        monsters += Monster1(5800f, groundTopY - 32f, patrolWidth = 100f, detectRange = 400f)
        monsters += Monster2(6100f, groundTopY - 34f * 2.0f, patrolWidth = 120f, scaleOverride = 2.0f)

        // Area 6: Final guardian
        monsters += Monster1(7200f, groundTopY - 32f, patrolWidth = 200f, detectRange = 500f)
    }

    override fun getGroundTopY(): Float = groundTopY

    // ===================== DRAW =====================
    override fun draw(canvas: Canvas) {
        // Different sky for tilemap 2 - darker, more dramatic
        val skyPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, groundTopY,
                Color.rgb(70, 100, 180),  // Darker blue
                Color.rgb(100, 130, 200), // Less bright
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, worldWidth, groundTopY, skyPaint)

        // Clouds
        paint.shader = null
        paint.color = Color.WHITE
        for (c in clouds) {
            drawMarioCloud(canvas, c.x, c.baseY, c.type)
        }

        // Ground - darker for tilemap 2
        paint.color = Color.rgb(20, 100, 20)  // Darker green
        canvas.drawRect(0f, groundTopY, worldWidth, worldHeight, paint)

        // Underground - darker brown
        paint.color = Color.rgb(80, 50, 20)
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
                canvas.drawCircle(x, y, 24f, paint)
                canvas.drawCircle(x + 20f, y, 28f, paint)
                canvas.drawCircle(x + 40f, y, 24f, paint)
                canvas.drawCircle(x + 12f, y + 12f, 20f, paint)
                canvas.drawCircle(x + 28f, y + 12f, 24f, paint)
            }
            1 -> {
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
        for (p in platforms) {
            // Darker platform for tilemap 2
            paint.color = Color.rgb(80, 50, 20)
            canvas.drawRect(p, paint)

            if (p.height() <= 20f) {
                paint.color = Color.rgb(20, 100, 20)
                canvas.drawRect(p.left, p.top, p.right, p.top + 4f, paint)
            }
        }
    }

    private fun drawPipes(canvas: Canvas) {
        for (pipe in pipes) {
            // Darker pipes for tilemap 2
            paint.color = Color.rgb(0, 100, 0)
            canvas.drawRect(pipe, paint)

            paint.color = Color.rgb(30, 150, 30)
            canvas.drawRect(pipe.left + 2f, pipe.top, pipe.left + 6f, pipe.bottom, paint)
            canvas.drawRect(pipe.right - 6f, pipe.top, pipe.right - 2f, pipe.bottom, paint)

            paint.color = Color.rgb(0, 80, 0)
            canvas.drawRect(pipe.left - 4f, pipe.top - 2f, pipe.right + 4f, pipe.top + 12f, paint)

            paint.color = Color.rgb(20, 120, 20)
            canvas.drawRect(pipe.left - 2f, pipe.top, pipe.right + 2f, pipe.top + 6f, paint)
        }
    }

    private fun drawMarioBrick(canvas: Canvas, brick: RectF) {
        // Darker bricks for tilemap 2
        paint.color = Color.rgb(120, 60, 15)
        canvas.drawRect(brick, paint)

        paint.color = Color.rgb(140, 70, 20)
        canvas.drawRect(brick.left + 1f, brick.top + 1f, brick.right - 1f, brick.bottom - 1f, paint)

        paint.color = Color.rgb(80, 50, 15)
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

        clouds.forEach {
            it.x += it.vx * dt
            if (it.x < -100f) it.x = worldWidth + 100f
            if (it.x > worldWidth + 100f) it.x = -100f
        }

        entities.updateAll(deltaMs)
    }

    override fun updateMonsters(deltaMs: Long, player: Player) {
        val monstersBeforeUpdate = monsters.size

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

        // Count killed monsters before removing them
        val deadMonsters = monsters.count {
            (it is Monster1 && !it.isAlive()) || (it is Monster2 && !it.isAlive())
        }

        // Remove dead monsters
        monsters.removeAll { it is Monster1 && !it.isAlive() }
        monsters.removeAll { it is Monster2 && !it.isAlive() }

        // Notify about monster kills through callback
        if (deadMonsters > 0 && this::onMonsterKilled.isInitialized) {
            repeat(deadMonsters) {
                onMonsterKilled()
            }
        }
    }

    // Add callback for monster kills
    private lateinit var onMonsterKilled: () -> Unit

    fun setMonsterKillCallback(callback: () -> Unit) {
        onMonsterKilled = callback
    }

    override fun checkBulletHitAndRespawnIfNeeded(player: Player): Boolean {
        // Check both Monster1 bullets and Monster2 body collision
        val bulletHit = monsters.any { it is Monster1 && it.bulletHitPlayer(player) }
        val monsterHit = handleMonsterBodyAndStomp(player)
        return bulletHit || monsterHit
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

        // Platform collisions
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
                        player.x += mp.speed * mp.direction * (1f / 60f)
                        collided = true
                    }
                }
            }
        }

        for (pipe in pipes) collided = handleSolidCollision(player, pipe) || collided
        for (brick in bricks) collided = handleSolidCollision(player, brick) || collided

        for (sp in spikes) if (sp.isHit(player)) { respawnPlayer(player); return true }
        for (sw in saws) if (sw.isHit(player)) { respawnPlayer(player); return true }

        if (player.y + player.height > worldHeight) {
            respawnPlayer(player)
            collided = true
        }

        for (cp in checkpoints) {
            if (!cp.activated && cp.tryActivate(player, 40f)) {
                lastCheckpointX = cp.x + 60f
                lastCheckpointY = groundTopY - player.height
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
                    // Call the monster kill callback when stomped
                    if (this::onMonsterKilled.isInitialized) {
                        onMonsterKilled()
                    }
                    player.y = m.y - player.height - 1f
                    player.vy = -280f
                    continue
                }
                if (RectF.intersects(m.getBounds(), pr)) {
                    return true
                }
            } else if (m is Monster2) {
                if (!m.isAlive()) { it.remove(); continue }
                if (m.tryStompBy(player)) {
                    it.remove()
                    // Call the monster kill callback when stomped
                    if (this::onMonsterKilled.isInitialized) {
                        onMonsterKilled()
                    }
                    player.y = m.y - player.height - 1f
                    player.vy = -280f
                    continue
                }
                if (RectF.intersects(m.getBounds(), pr)) {
                    return true
                }
            }
        }
        return false
    }

    private fun respawnPlayer(player: Player) {
        player.x = lastCheckpointX
        player.y = lastCheckpointY
        player.vx = 0f
        player.vy = 0f
    }

    override fun isCompleted(player: Player): Boolean {
        return player.x >= 7800f
    }

    override fun checkCoinCollection(player: Player, onCoinCollected: (String) -> Unit) {
        val magnetRange = player.getMagnetRange()
        val hasMagnet = player.hasCoinMagnet()

        for (pk in entities.getPickups()) {
            if (!pk.collected) {
                val playerCenterX = player.x + player.width/2f
                val playerCenterY = player.y + player.height/2f
                val dx = playerCenterX - pk.x
                val dy = playerCenterY - pk.y
                val distance = kotlin.math.sqrt(dx*dx + dy*dy)

                val collectionRange = 30f

                if (distance < collectionRange) {
                    pk.onCollide(pk)
                    onCoinCollected(pk.type)
                }
                else if (hasMagnet && distance < magnetRange) {
                    val magnetStrength = 5f
                    val normalizedDx = dx / distance
                    val normalizedDy = dy / distance

                    pk.x += normalizedDx * magnetStrength
                    pk.y += normalizedDy * magnetStrength

                    val newDistance = kotlin.math.sqrt(
                        (playerCenterX - pk.x) * (playerCenterX - pk.x) +
                        (playerCenterY - pk.y) * (playerCenterY - pk.y)
                    )
                    if (newDistance < collectionRange) {
                        pk.onCollide(pk)
                        onCoinCollected(pk.type)
                    }
                }
            }
        }
    }

    override fun checkHealthCollection(player: Player, onHealthCollected: (Int) -> Unit) {
        for (healthPickup in entities.getHealthPickups()) {
            if (healthPickup.checkCollision(player)) {
                val healAmount = healthPickup.collect()
                if (healAmount > 0) {
                    onHealthCollected(healAmount)
                }
            }
        }
    }

    override fun resetLevel() {
        monsters.clear()
        entities.clear()

        lastCheckpointX = 100f
        lastCheckpointY = groundTopY - 100f

        setupPickups()
        setupMonsters()

        checkpoints.forEach { it.reset() }
    }

    override fun getLastCheckpoint(): Triple<Float, Float, Int> {
        return Triple(lastCheckpointX, lastCheckpointY, 0)
    }

    override fun resolvePlayerCollisionSafe(player: Player) {
        // World bounds
        if (player.x < 0f) { player.x = 0f; player.vx = 0f }
        if (player.x + player.width > worldWidth) {
            player.x = worldWidth - player.width; player.vx = 0f
        }
        if (player.y < 0f) { player.y = 0f; if (player.vy < 0f) player.vy = 0f }

        if (player.y + player.height > groundTopY && player.vy >= 0f) {
            player.y = groundTopY - player.height
            player.vy = 0f
        }

        for (p in platforms) {
            val pr = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
            if (pr.right > p.left && pr.left < p.right && player.vy > 0f) {
                if (pr.bottom > p.top && pr.top < p.top) {
                    val prevBottom = player.prevY + player.height
                    if (prevBottom <= p.top + 3f) {
                        player.y = p.top - player.height
                        player.vy = 0f
                    }
                }
            }
        }

        for (mp in movingPlatforms) {
            val mpr = mp.rect()
            val pr = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
            if (pr.right > mpr.left && pr.left < mpr.right && player.vy > 0f) {
                if (pr.bottom > mpr.top && pr.top < mpr.top) {
                    val prevBottom = player.prevY + player.height
                    if (prevBottom <= mpr.top + 3f) {
                        player.y = mpr.top - player.height
                        player.vy = 0f
                        player.x += mp.speed * mp.direction * (1f / 60f)
                    }
                }
            }
        }

        for (pipe in pipes) handleSolidCollision(player, pipe)
        for (brick in bricks) handleSolidCollision(player, brick)

        for (cp in checkpoints) {
            if (!cp.activated && cp.tryActivate(player, 40f)) {
                lastCheckpointX = cp.x + 60f
                lastCheckpointY = groundTopY - player.height
            }
        }
    }

    override fun getSpikes(): List<Spike> = spikes
    override fun getSaws(): List<Saw> = saws
}
