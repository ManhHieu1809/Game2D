package com.example.game2d

import android.content.Context
import android.graphics.*
import kotlin.math.sin

class TileMap(private val ctx: Context) {

    val worldWidth = 3200f  // Extended world
    val worldHeight = 720f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val groundTopY = worldHeight * 0.75f

    // Platforms and structures
    private val platforms = ArrayList<RectF>()
    private val pipes = ArrayList<RectF>()
    private val bricks = ArrayList<RectF>()

    // New obstacles from your sprites
    private val spikes = ArrayList<RectF>()
    private val saws = ArrayList<Saw>()
    private val movingPlatforms = ArrayList<MovingPlatform>()
    private val checkpoints = ArrayList<Checkpoint>()

    // Cloud system
    data class Cloud(var x: Float, var baseY: Float, var vx: Float, val bobOffset: Float)
    private val clouds = ArrayList<Cloud>()

    // Enhanced pickups
    data class Pickup(var x: Float, var y: Float, var collected: Boolean = false, var type: String = "coin", var animOffset: Float = 0f)
    private val pickups = ArrayList<Pickup>()

    // Enhanced enemies with different types
    data class Enemy(
        var x: Float,
        var y: Float,
        var vx: Float = -50f,
        var alive: Boolean = true,
        val type: String = "goomba",
        var animFrame: Int = 0,
        var animTimer: Float = 0f
    )
    private val enemies = ArrayList<Enemy>()

    // New obstacle types
    data class Saw(var x: Float, var y: Float, var rotation: Float = 0f, var rotSpeed: Float = 180f)
    data class MovingPlatform(var x: Float, var y: Float, var startX: Float, var endX: Float, var speed: Float = 50f, var direction: Int = 1)
    data class Checkpoint(var x: Float, var y: Float, var activated: Boolean = false)

    // Sprite loading
    private val sprites = HashMap<String, Bitmap?>()

    init {
        loadSprites()
        setupLevel()
    }

    private fun loadSprites() {
        // Load obstacle and enemy sprites based on your images
        sprites["spike"] = loadBitmap("spike")
        sprites["saw"] = loadBitmap("saw")
        sprites["goomba"] = loadBitmap("goomba")
        sprites["chicken"] = loadBitmap("chicken")
        sprites["checkpoint"] = loadBitmap("checkpoint")
        sprites["coin"] = loadBitmap("coin")
        sprites["gem"] = loadBitmap("gem")
    }

    private fun loadBitmap(name: String): Bitmap? {
        val id = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (id == 0) return null
        return try {
            BitmapFactory.decodeResource(ctx.resources, id)
        } catch (e: Exception) {
            null
        }
    }

    private fun setupLevel() {
        // === ENHANCED LEVEL DESIGN ===

        // Grass platforms with more variety
        platforms.add(RectF(300f, groundTopY - 120f, 450f, groundTopY - 100f))
        platforms.add(RectF(600f, groundTopY - 80f, 720f, groundTopY - 60f))
        platforms.add(RectF(900f, groundTopY - 160f, 1050f, groundTopY - 140f))
        platforms.add(RectF(1200f, groundTopY - 100f, 1350f, groundTopY - 80f))
        platforms.add(RectF(1500f, groundTopY - 200f, 1620f, groundTopY - 180f))
        platforms.add(RectF(1800f, groundTopY - 140f, 1950f, groundTopY - 120f))
        platforms.add(RectF(2200f, groundTopY - 180f, 2350f, groundTopY - 160f))
        platforms.add(RectF(2600f, groundTopY - 220f, 2750f, groundTopY - 200f))

        // Pipes with more strategic placement
        pipes.add(RectF(800f, groundTopY - 80f, 850f, groundTopY))
        pipes.add(RectF(1400f, groundTopY - 120f, 1450f, groundTopY))
        pipes.add(RectF(2000f, groundTopY - 100f, 2050f, groundTopY))
        pipes.add(RectF(2800f, groundTopY - 140f, 2850f, groundTopY))

        // Brick patterns
        // Staircase pattern
        for (i in 0..4) {
            for (j in 0..i) {
                bricks.add(RectF(1600f + j * 32f, groundTopY - 32f - i * 32f, 1632f + j * 32f, groundTopY - i * 32f))
            }
        }
        // Floating brick platforms
        for (i in 0..3) {
            bricks.add(RectF(2400f + i * 32f, groundTopY - 180f, 2432f + i * 32f, groundTopY - 148f))
        }

        // === NEW OBSTACLES ===

        // Spikes - dangerous ground hazards
        spikes.add(RectF(520f, groundTopY - 16f, 552f, groundTopY))
        spikes.add(RectF(750f, groundTopY - 16f, 782f, groundTopY))
        spikes.add(RectF(1100f, groundTopY - 16f, 1148f, groundTopY))
        spikes.add(RectF(1700f, groundTopY - 16f, 1732f, groundTopY))
        spikes.add(RectF(2100f, groundTopY - 16f, 2148f, groundTopY))

        // Rotating saws - moving hazards
        saws.add(Saw(1250f, groundTopY - 50f, 0f, 120f))
        saws.add(Saw(1850f, groundTopY - 90f, 0f, -90f))
        saws.add(Saw(2300f, groundTopY - 120f, 0f, 150f))

        // Moving platforms
        movingPlatforms.add(MovingPlatform(1050f, groundTopY - 220f, 1050f, 1180f, 40f, 1))
        movingPlatforms.add(MovingPlatform(2500f, groundTopY - 160f, 2500f, 2650f, 60f, 1))

        // Checkpoints
        checkpoints.add(Checkpoint(1000f, groundTopY - 40f))
        checkpoints.add(Checkpoint(2000f, groundTopY - 40f))
        checkpoints.add(Checkpoint(3000f, groundTopY - 40f))

        // Enhanced cloud system
        clouds.add(Cloud(200f, 100f, -12f, 0.2f))
        clouds.add(Cloud(500f, 80f, -8f, 1.1f))
        clouds.add(Cloud(800f, 120f, -16f, 2.3f))
        clouds.add(Cloud(1100f, 90f, -10f, 0.5f))
        clouds.add(Cloud(1400f, 110f, -6f, 3.7f))
        clouds.add(Cloud(1700f, 85f, -14f, 4.2f))
        clouds.add(Cloud(2000f, 100f, -9f, 5.9f))
        clouds.add(Cloud(2400f, 95f, -11f, 1.8f))
        clouds.add(Cloud(2800f, 105f, -7f, 4.6f))

        // === ENHANCED COLLECTIBLES ===

        // Regular coins
        pickups.add(Pickup(375f, groundTopY - 150f, type = "coin", animOffset = 0f))
        pickups.add(Pickup(660f, groundTopY - 110f, type = "coin", animOffset = 0.5f))
        pickups.add(Pickup(975f, groundTopY - 190f, type = "coin", animOffset = 1.0f))
        pickups.add(Pickup(1275f, groundTopY - 130f, type = "coin", animOffset = 1.5f))
        pickups.add(Pickup(1560f, groundTopY - 230f, type = "coin", animOffset = 2.0f))
        pickups.add(Pickup(1875f, groundTopY - 170f, type = "coin", animOffset = 2.5f))

        // Bonus coin trails
        for (i in 0..4) {
            pickups.add(Pickup(2200f + i * 40f, groundTopY - 220f - i * 10f, type = "coin", animOffset = i * 0.3f))
        }

        // Gems (higher value collectibles)
        pickups.add(Pickup(1025f, groundTopY - 200f, type = "gem", animOffset = 0f))
        pickups.add(Pickup(2375f, groundTopY - 220f, type = "gem", animOffset = 1.5f))
        pickups.add(Pickup(2900f, groundTopY - 50f, type = "gem", animOffset = 3.0f))

        // Power-ups
        pickups.add(Pickup(1875f, groundTopY - 170f, type = "mushroom", animOffset = 0f))
        pickups.add(Pickup(2700f, groundTopY - 260f, type = "star", animOffset = 2.0f))

        // === DIVERSE ENEMY TYPES ===

        // Goombas (brown enemies from your sprites)
        enemies.add(Enemy(400f, groundTopY - 32f, -50f, true, "goomba"))
        enemies.add(Enemy(700f, groundTopY - 32f, -60f, true, "goomba"))
        enemies.add(Enemy(1300f, groundTopY - 32f, -40f, true, "goomba"))
        enemies.add(Enemy(2100f, groundTopY - 32f, -55f, true, "goomba"))

        // Chickens (white enemies from your sprites)
        enemies.add(Enemy(950f, groundTopY - 40f, -70f, true, "chicken"))
        enemies.add(Enemy(1750f, groundTopY - 40f, -45f, true, "chicken"))
        enemies.add(Enemy(2400f, groundTopY - 40f, -65f, true, "chicken"))

        // Platform patrol enemies
        enemies.add(Enemy(320f, groundTopY - 152f, -30f, true, "goomba")) // On first platform
        enemies.add(Enemy(920f, groundTopY - 192f, -25f, true, "chicken")) // On high platform
    }

    fun getGroundTopY(): Float = groundTopY

    fun draw(canvas: Canvas) {
        // === SKY GRADIENT ===
        val skyPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, groundTopY,
                Color.rgb(135, 206, 250),  // Light sky blue
                Color.rgb(176, 224, 230),  // Light blue
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, worldWidth, groundTopY, skyPaint)

        // === ANIMATED CLOUDS ===
        paint.shader = null
        paint.color = Color.WHITE
        for (cloud in clouds) {
            val time = System.currentTimeMillis()
            val bob = sin((time / 1000.0f) + cloud.bobOffset) * 6f
            drawCloud(canvas, cloud.x, cloud.baseY + bob)
        }

        // === GROUND ===
        paint.color = Color.rgb(101, 67, 33)  // Brown dirt
        canvas.drawRect(0f, groundTopY, worldWidth, worldHeight, paint)

        // === GRASS TOP ===
        paint.color = Color.rgb(34, 139, 34)  // Forest green
        canvas.drawRect(0f, groundTopY, worldWidth, groundTopY + 24f, paint)

        // === PLATFORMS ===
        drawPlatforms(canvas)

        // === PIPES ===
        drawPipes(canvas)

        // === BRICKS ===
        for (brick in bricks) {
            drawBrick(canvas, brick)
        }

        // === NEW OBSTACLES ===
        drawSpikes(canvas)
        drawSaws(canvas)
        drawMovingPlatforms(canvas)

        // === CHECKPOINTS ===
        drawCheckpoints(canvas)

        // === COLLECTIBLES ===
        drawPickups(canvas)

        // === ENEMIES ===
        drawEnemies(canvas)

        // === WORLD BORDERS ===
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
            // Grass texture on top
            paint.color = Color.rgb(50, 205, 50)  // Lime green
            canvas.drawRect(p.left, p.top, p.right, p.top + 8f, paint)
            // Dirt underneath
            paint.color = Color.rgb(101, 67, 33)
            canvas.drawRect(p.left, p.top + 8f, p.right, p.bottom + 6f, paint)
            paint.color = Color.rgb(34, 139, 34)
        }
    }

    private fun drawPipes(canvas: Canvas) {
        paint.color = Color.rgb(0, 128, 0)  // Dark green
        for (pipe in pipes) {
            canvas.drawRect(pipe, paint)
            // Pipe highlights
            paint.color = Color.rgb(50, 205, 50)
            canvas.drawRect(pipe.left + 4f, pipe.top, pipe.left + 8f, pipe.bottom, paint)
            canvas.drawRect(pipe.right - 8f, pipe.top, pipe.right - 4f, pipe.bottom, paint)
            // Pipe top
            paint.color = Color.rgb(0, 100, 0)
            canvas.drawRect(pipe.left - 6f, pipe.top, pipe.right + 6f, pipe.top + 12f, paint)
            paint.color = Color.rgb(0, 128, 0)
        }
    }

    private fun drawSpikes(canvas: Canvas) {
        paint.color = Color.rgb(128, 128, 128)  // Gray spikes
        for (spike in spikes) {
            val sprite = sprites["spike"]
            if (sprite != null) {
                canvas.drawBitmap(sprite, null, spike, paint)
            } else {
                // Fallback: draw triangular spikes
                val path = Path()
                val centerX = spike.centerX()
                path.moveTo(spike.left, spike.bottom)
                path.lineTo(centerX, spike.top)
                path.lineTo(spike.right, spike.bottom)
                path.close()
                paint.color = Color.rgb(160, 160, 160)
                canvas.drawPath(path, paint)
                paint.color = Color.rgb(100, 100, 100)
                paint.strokeWidth = 2f
                paint.style = Paint.Style.STROKE
                canvas.drawPath(path, paint)
                paint.style = Paint.Style.FILL
            }
        }
    }

    private fun drawSaws(canvas: Canvas) {
        for (saw in saws) {
            val sprite = sprites["saw"]
            if (sprite != null) {
                canvas.save()
                canvas.rotate(saw.rotation, saw.x, saw.y)
                val size = 48f
                val rect = RectF(saw.x - size/2, saw.y - size/2, saw.x + size/2, saw.y + size/2)
                canvas.drawBitmap(sprite, null, rect, paint)
                canvas.restore()
            } else {
                // Fallback: draw circular saw
                paint.color = Color.rgb(150, 150, 150)
                canvas.drawCircle(saw.x, saw.y, 24f, paint)
                paint.color = Color.rgb(200, 50, 50)
                paint.strokeWidth = 3f
                paint.style = Paint.Style.STROKE
                canvas.drawCircle(saw.x, saw.y, 20f, paint)
                // Draw rotating teeth
                canvas.save()
                canvas.rotate(saw.rotation, saw.x, saw.y)
                for (i in 0..7) {
                    val angle = i * 45f
                    canvas.save()
                    canvas.rotate(angle, saw.x, saw.y)
                    canvas.drawLine(saw.x, saw.y - 24f, saw.x, saw.y - 28f, paint)
                    canvas.restore()
                }
                canvas.restore()
                paint.style = Paint.Style.FILL
            }
        }
    }

    private fun drawMovingPlatforms(canvas: Canvas) {
        paint.color = Color.rgb(139, 69, 19)  // Brown
        for (platform in movingPlatforms) {
            val rect = RectF(platform.x, platform.y, platform.x + 120f, platform.y + 20f)
            canvas.drawRoundRect(rect, 4f, 4f, paint)
            // Platform texture
            paint.color = Color.rgb(160, 82, 45)
            canvas.drawRect(platform.x + 4f, platform.y + 4f, platform.x + 116f, platform.y + 16f, paint)
            paint.color = Color.rgb(139, 69, 19)
        }
    }

    private fun drawCheckpoints(canvas: Canvas) {
        for (checkpoint in checkpoints) {
            val sprite = sprites["checkpoint"]
            if (sprite != null) {
                val rect = RectF(checkpoint.x, checkpoint.y, checkpoint.x + 32f, checkpoint.y + 40f)
                paint.colorFilter = if (checkpoint.activated) {
                    ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1.5f) })
                } else null
                canvas.drawBitmap(sprite, null, rect, paint)
                paint.colorFilter = null
            } else {
                // Fallback: draw flag
                paint.color = if (checkpoint.activated) Color.GREEN else Color.RED
                canvas.drawRect(checkpoint.x, checkpoint.y, checkpoint.x + 4f, checkpoint.y + 40f, paint)
                paint.color = if (checkpoint.activated) Color.YELLOW else Color.WHITE
                canvas.drawRect(checkpoint.x + 4f, checkpoint.y, checkpoint.x + 32f, checkpoint.y + 20f, paint)
            }
        }
    }

    private fun drawPickups(canvas: Canvas) {
        val time = System.currentTimeMillis()
        for (pk in pickups) {
            if (!pk.collected) {
                when (pk.type) {
                    "coin" -> {
                        val bounce = sin((time + pk.animOffset * 1000f) * 0.005f) * 3f
                        val sprite = sprites["coin"]
                        if (sprite != null) {
                            val rect = RectF(pk.x - 12f, pk.y + bounce - 12f, pk.x + 12f, pk.y + bounce + 12f)
                            canvas.drawBitmap(sprite, null, rect, paint)
                        } else {
                            paint.color = Color.YELLOW
                            canvas.drawCircle(pk.x, pk.y + bounce, 12f, paint)
                            paint.color = Color.rgb(255, 215, 0)  // Gold
                            canvas.drawCircle(pk.x, pk.y + bounce, 8f, paint)
                        }
                    }
                    "gem" -> {
                        val bounce = sin((time + pk.animOffset * 1000f) * 0.003f) * 4f
                        val sprite = sprites["gem"]
                        if (sprite != null) {
                            val rect = RectF(pk.x - 16f, pk.y + bounce - 16f, pk.x + 16f, pk.y + bounce + 16f)
                            canvas.drawBitmap(sprite, null, rect, paint)
                        } else {
                            paint.color = Color.MAGENTA
                            canvas.drawCircle(pk.x, pk.y + bounce, 16f, paint)
                            paint.color = Color.rgb(255, 0, 255)
                            canvas.drawCircle(pk.x, pk.y + bounce, 12f, paint)
                        }
                    }
                    "mushroom" -> {
                        paint.color = Color.RED
                        canvas.drawCircle(pk.x, pk.y, 16f, paint)
                        paint.color = Color.WHITE
                        canvas.drawCircle(pk.x - 6f, pk.y - 4f, 4f, paint)
                        canvas.drawCircle(pk.x + 6f, pk.y - 4f, 4f, paint)
                    }
                    "star" -> {
                        val rotation = (time * 0.1f) % 360f
                        canvas.save()
                        canvas.rotate(rotation, pk.x, pk.y)
                        paint.color = Color.YELLOW
                        drawStar(canvas, pk.x, pk.y, 18f, 8f)
                        canvas.restore()
                    }
                }
            }
        }
    }

    private fun drawEnemies(canvas: Canvas) {
        for (enemy in enemies) {
            if (enemy.alive) {
                when (enemy.type) {
                    "goomba" -> {
                        val sprite = sprites["goomba"]
                        if (sprite != null) {
                            val rect = RectF(enemy.x - 16f, enemy.y - 16f, enemy.x + 16f, enemy.y + 16f)
                            canvas.drawBitmap(sprite, null, rect, paint)
                        } else {
                            // Fallback brown goomba
                            paint.color = Color.rgb(139, 69, 19)
                            canvas.drawCircle(enemy.x, enemy.y, 16f, paint)
                            // Eyes
                            paint.color = Color.WHITE
                            canvas.drawCircle(enemy.x - 6f, enemy.y - 4f, 3f, paint)
                            canvas.drawCircle(enemy.x + 6f, enemy.y - 4f, 3f, paint)
                            paint.color = Color.BLACK
                            canvas.drawCircle(enemy.x - 6f, enemy.y - 4f, 2f, paint)
                            canvas.drawCircle(enemy.x + 6f, enemy.y - 4f, 2f, paint)
                        }
                    }
                    "chicken" -> {
                        val sprite = sprites["chicken"]
                        if (sprite != null) {
                            val rect = RectF(enemy.x - 20f, enemy.y - 20f, enemy.x + 20f, enemy.y + 20f)
                            // Flip sprite based on direction
                            if (enemy.vx > 0) {
                                canvas.save()
                                canvas.scale(-1f, 1f, enemy.x, enemy.y)
                                canvas.drawBitmap(sprite, null, rect, paint)
                                canvas.restore()
                            } else {
                                canvas.drawBitmap(sprite, null, rect, paint)
                            }
                        } else {
                            // Fallback white chicken
                            paint.color = Color.WHITE
                            canvas.drawOval(enemy.x - 18f, enemy.y - 12f, enemy.x + 18f, enemy.y + 12f, paint)
                            // Head
                            paint.color = Color.rgb(255, 240, 240)
                            canvas.drawCircle(enemy.x + 12f, enemy.y - 8f, 8f, paint)
                            // Beak
                            paint.color = Color.YELLOW
                            canvas.drawCircle(enemy.x + 18f, enemy.y - 6f, 3f, paint)
                        }
                    }
                }
            }
        }
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

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, outerRadius: Float, innerRadius: Float) {
        val path = Path()
        val points = 5
        var angle = -Math.PI / 2.0  // Start at top

        for (i in 0 until points * 2) {
            val radius = if (i % 2 == 0) outerRadius else innerRadius
            val x = cx + kotlin.math.cos(angle) * radius
            val y = cy + kotlin.math.sin(angle) * radius
            if (i == 0) path.moveTo(x.toFloat(), y.toFloat())
            else path.lineTo(x.toFloat(), y.toFloat())
            angle += Math.PI / points
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    fun resolvePlayerCollision(player: Player): Boolean {
        val rect = player.getRect()
        var collided = false

        // World boundaries
        if (player.x < 0f) {
            player.x = 0f
            player.vx = 0f
            collided = true
        }
        if (player.x + player.width > worldWidth) {
            player.x = worldWidth - player.width
            player.vx = 0f
            collided = true
        }
        if (player.y < 0f) {
            player.y = 0f
            if (player.vy < 0f) player.vy = 0f
            collided = true
        }

        // Ground collision
        if (player.y + player.height > groundTopY) {
            if (player.vy >= 0f) {
                player.y = groundTopY - player.height
                player.vy = 0f
                collided = true
            }
        }

        // Platform collisions
        for (p in platforms) {
            val pRect = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
            if (pRect.right > p.left && pRect.left < p.right) {
                if (player.vy > 0f && pRect.bottom > p.top && pRect.top < p.top) {
                    val prevBottom = player.prevY + player.height
                    if (prevBottom <= p.top + 5f) {
                        player.y = p.top - player.height
                        player.vy = 0f
                        collided = true
                    }
                }
            }
        }

        // Moving platform collisions
        for (mp in movingPlatforms) {
            val mpRect = RectF(mp.x, mp.y, mp.x + 120f, mp.y + 20f)
            val pRect = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
            if (pRect.right > mpRect.left && pRect.left < mpRect.right) {
                if (player.vy > 0f && pRect.bottom > mpRect.top && pRect.top < mpRect.top) {
                    val prevBottom = player.prevY + player.height
                    if (prevBottom <= mpRect.top + 5f) {
                        player.y = mpRect.top - player.height
                        player.vy = 0f
                        player.x += mp.speed * mp.direction * (1f/60f) // Move with platform
                        collided = true
                    }
                }
            }
        }

        // Pipe and brick collisions (same as before)
        for (pipe in pipes) {
            collided = handleSolidCollision(player, pipe) || collided
        }
        for (brick in bricks) {
            collided = handleSolidCollision(player, brick) || collided
        }

        // === NEW HAZARD COLLISIONS ===

        // Spike collisions - deadly
        for (spike in spikes) {
            val pRect = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
            if (pRect.intersect(spike)) {
                // Reset player position (or reduce health)
                respawnPlayer(player)
                return true
            }
        }

        // Saw collisions - deadly
        for (saw in saws) {
            val dx = (player.x + player.width / 2f) - saw.x
            val dy = (player.y + player.height / 2f) - saw.y
            if (dx * dx + dy * dy < 30f * 30f) {
                respawnPlayer(player)
                return true
            }
        }

        // World bottom boundary
        if (player.y + player.height > worldHeight) {
            respawnPlayer(player)
            collided = true
        }

        // Pickup collection
        for (pk in pickups) {
            if (!pk.collected) {
                val dx = (player.x + player.width / 2f) - pk.x
                val dy = (player.y + player.height / 2f) - pk.y
                val collectRadius = when (pk.type) {
                    "gem" -> 35f
                    "coin" -> 25f
                    else -> 30f
                }
                if (dx * dx + dy * dy < collectRadius * collectRadius) {
                    pk.collected = true
                }
            }
        }

        // Checkpoint activation
        for (checkpoint in checkpoints) {
            if (!checkpoint.activated) {
                val dx = (player.x + player.width / 2f) - checkpoint.x
                val dy = (player.y + player.height / 2f) - checkpoint.y
                if (dx * dx + dy * dy < 40f * 40f) {
                    checkpoint.activated = true
                    // Save checkpoint position for respawning
                    lastCheckpointX = checkpoint.x
                    lastCheckpointY = checkpoint.y - player.height
                }
            }
        }

        // Enemy collisions
        for (enemy in enemies) {
            if (enemy.alive) {
                val dx = (player.x + player.width / 2f) - enemy.x
                val dy = (player.y + player.height / 2f) - enemy.y
                val hitRadius = when (enemy.type) {
                    "chicken" -> 35f
                    else -> 30f
                }
                if (dx * dx + dy * dy < hitRadius * hitRadius) {
                    if (player.vy > 0 && player.y < enemy.y - 10f) {
                        // Jump on enemy
                        enemy.alive = false
                        player.vy = -300f  // Bounce
                    } else {
                        // Take damage or respawn
                        respawnPlayer(player)
                        return true
                    }
                }
            }
        }

        return collided
    }

    private fun handleSolidCollision(player: Player, obstacle: RectF): Boolean {
        val pRect = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
        if (pRect.intersect(obstacle)) {
            val overlapLeft = pRect.right - obstacle.left
            val overlapRight = obstacle.right - pRect.left
            val overlapTop = pRect.bottom - obstacle.top
            val overlapBottom = obstacle.bottom - pRect.top

            val minOverlapX = kotlin.math.min(overlapLeft, overlapRight)
            val minOverlapY = kotlin.math.min(overlapTop, overlapBottom)

            if (minOverlapX < minOverlapY) {
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
        return false
    }

    // Checkpoint system
    private var lastCheckpointX = 200f
    private var lastCheckpointY = 0f

    private fun respawnPlayer(player: Player) {
        player.x = lastCheckpointX
        player.y = if (lastCheckpointY != 0f) lastCheckpointY else groundTopY - player.height
        player.vx = 0f
        player.vy = 0f
    }

    fun update(deltaMs: Long) {
        val dt = deltaMs / 1000f

        // Update moving platforms
        for (mp in movingPlatforms) {
            mp.x += mp.speed * mp.direction * dt
            if (mp.x <= mp.startX || mp.x >= mp.endX) {
                mp.direction *= -1
            }
        }

        // Update rotating saws
        for (saw in saws) {
            saw.rotation += saw.rotSpeed * dt
            if (saw.rotation >= 360f) saw.rotation -= 360f
            if (saw.rotation < 0f) saw.rotation += 360f
        }

        // Update enemies with improved AI
        for (enemy in enemies) {
            if (enemy.alive) {
                enemy.animTimer += dt
                if (enemy.animTimer >= 0.2f) {
                    enemy.animFrame = (enemy.animFrame + 1) % 4
                    enemy.animTimer = 0f
                }

                enemy.x += enemy.vx * dt

                // Enhanced enemy AI
                when (enemy.type) {
                    "goomba" -> {
                        // Turn around at edges or obstacles
                        if (enemy.x < 50f || enemy.x > worldWidth - 50f) {
                            enemy.vx = -enemy.vx
                        }

                        // Check for platform edges
                        var onPlatform = false
                        for (platform in platforms) {
                            if (enemy.x > platform.left && enemy.x < platform.right &&
                                enemy.y >= platform.top - 40f && enemy.y <= platform.bottom) {
                                onPlatform = true
                                // Turn around at platform edges
                                if ((enemy.vx < 0 && enemy.x <= platform.left + 20f) ||
                                    (enemy.vx > 0 && enemy.x >= platform.right - 20f)) {
                                    enemy.vx = -enemy.vx
                                }
                                break
                            }
                        }
                    }
                    "chicken" -> {
                        // Chickens move faster and jump occasionally
                        if (enemy.x < 50f || enemy.x > worldWidth - 50f) {
                            enemy.vx = -enemy.vx
                        }

                        // Chickens can "hop" by briefly changing their y position
                        if (kotlin.math.abs(enemy.vx) > 50f && System.currentTimeMillis() % 3000 < 100) {
                            enemy.y -= 5f // Small hop animation
                        }
                    }
                }

                // Turn around at pipes and obstacles
                for (pipe in pipes) {
                    if (enemy.x > pipe.left - 50f && enemy.x < pipe.right + 50f &&
                        enemy.y > pipe.top - 50f && enemy.y < pipe.bottom + 50f) {
                        enemy.vx = -enemy.vx
                    }
                }
            }
        }

        // Update clouds with parallax scrolling
        for (cloud in clouds) {
            cloud.x += cloud.vx * dt
            val wrapMargin = 120f
            if (cloud.x < -wrapMargin) {
                cloud.x = worldWidth + wrapMargin
            } else if (cloud.x > worldWidth + wrapMargin) {
                cloud.x = -wrapMargin
            }
        }
    }
}