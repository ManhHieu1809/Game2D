package com.example.game2d

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.sin

class TileMap(private val ctx: Context) {

    val worldWidth = 2400f  // Mở rộng thế giới
    val worldHeight = 720f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val groundTopY = worldHeight * 0.75f  // Ground thấp hơn một chút

    // Các platform với thiết kế Mario-style
    private val platforms = ArrayList<RectF>()
    private val pipes = ArrayList<RectF>()
    private val bricks = ArrayList<RectF>()

    // Cloud model (vị trí + vận tốc + offset cho bob)
    data class Cloud(var x: Float, var baseY: Float, var vx: Float, val bobOffset: Float)
    private val clouds = ArrayList<Cloud>()

    data class Pickup(var x: Float, var y: Float, var collected: Boolean = false, var type: String = "coin")
    private val pickups = ArrayList<Pickup>()

    // Enemies (Goomba-style)
    data class Enemy(var x: Float, var y: Float, var vx: Float = -50f, var alive: Boolean = true)
    private val enemies = ArrayList<Enemy>()

    init {
        setupLevel()
    }

    private fun setupLevel() {
        // === PLATFORMS (grass platforms) ===
        platforms.add(RectF(300f, groundTopY - 120f, 450f, groundTopY - 100f))
        platforms.add(RectF(600f, groundTopY - 80f, 720f, groundTopY - 60f))
        platforms.add(RectF(900f, groundTopY - 160f, 1050f, groundTopY - 140f))
        platforms.add(RectF(1200f, groundTopY - 100f, 1350f, groundTopY - 80f))
        platforms.add(RectF(1500f, groundTopY - 200f, 1620f, groundTopY - 180f))
        platforms.add(RectF(1800f, groundTopY - 140f, 1950f, groundTopY - 120f))

        // === PIPES (Mario-style green pipes) ===
        pipes.add(RectF(800f, groundTopY - 80f, 850f, groundTopY))
        pipes.add(RectF(1400f, groundTopY - 120f, 1450f, groundTopY))
        pipes.add(RectF(2000f, groundTopY - 100f, 2050f, groundTopY))

        // === BRICK BLOCKS ===
        // Brick pattern 1
        for (i in 0..3) {
            bricks.add(RectF(500f + i * 32f, groundTopY - 200f, 532f + i * 32f, groundTopY - 168f))
        }
        // Brick pattern 2 (stairs)
        for (i in 0..4) {
            for (j in 0..i) {
                bricks.add(RectF(1600f + j * 32f, groundTopY - 32f - i * 32f, 1632f + j * 32f, groundTopY - i * 32f))
            }
        }


        // vx negative -> mây trôi sang trái; thay đổi vx để thay đổi tốc độ (parallax)
        clouds.add(Cloud(200f, 100f, -12f, 0.2f))
        clouds.add(Cloud(500f, 80f, -8f, 1.1f))
        clouds.add(Cloud(800f, 120f, -16f, 2.3f))
        clouds.add(Cloud(1100f, 90f, -10f, 0.5f))
        clouds.add(Cloud(1400f, 110f, -6f, 3.7f))
        clouds.add(Cloud(1700f, 85f, -14f, 4.2f))
        clouds.add(Cloud(2000f, 100f, -9f, 5.9f))

        // === COINS ===
        pickups.add(Pickup(375f, groundTopY - 150f, type = "coin"))
        pickups.add(Pickup(660f, groundTopY - 110f, type = "coin"))
        pickups.add(Pickup(975f, groundTopY - 190f, type = "coin"))
        pickups.add(Pickup(1275f, groundTopY - 130f, type = "coin"))
        pickups.add(Pickup(1560f, groundTopY - 230f, type = "coin"))

        // Hidden coin blocks
        pickups.add(Pickup(520f, groundTopY - 230f, type = "coin"))
        pickups.add(Pickup(552f, groundTopY - 230f, type = "coin"))
        pickups.add(Pickup(584f, groundTopY - 230f, type = "coin"))

        // Power-ups
        pickups.add(Pickup(1875f, groundTopY - 170f, type = "mushroom"))

        // === ENEMIES ===
        enemies.add(Enemy(400f, groundTopY - 32f))
        enemies.add(Enemy(700f, groundTopY - 32f))
        enemies.add(Enemy(1100f, groundTopY - 32f))
        enemies.add(Enemy(1300f, groundTopY - 32f))
    }

    fun getGroundTopY(): Float = groundTopY

    fun draw(canvas: Canvas) {
        // === SKY GRADIENT ===
        val skyPaint = Paint().apply {
            shader = android.graphics.LinearGradient(
                0f, 0f, 0f, groundTopY,
                Color.rgb(135, 206, 250),  // Light sky blue
                Color.rgb(176, 224, 230),  // Light blue
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, worldWidth, groundTopY, skyPaint)

        // === CLOUDS ===
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

        // === GRASS PLATFORMS ===
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

        // === PIPES ===
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

        // === BRICK BLOCKS ===
        for (brick in bricks) {
            drawBrick(canvas, brick)
        }

        // === PICKUPS ===
        val time = System.currentTimeMillis()
        for (pk in pickups) {
            if (!pk.collected) {
                when (pk.type) {
                    "coin" -> {
                        // Animated spinning coin
                        val bounce = sin((time + pk.x * 10) * 0.005f) * 3f
                        paint.color = Color.YELLOW
                        canvas.drawCircle(pk.x, pk.y + bounce, 12f, paint)
                        paint.color = Color.rgb(255, 215, 0)  // Gold
                        canvas.drawCircle(pk.x, pk.y + bounce, 8f, paint)
                    }
                    "mushroom" -> {
                        // Super Mushroom
                        paint.color = Color.RED
                        canvas.drawCircle(pk.x, pk.y, 16f, paint)
                        paint.color = Color.WHITE
                        canvas.drawCircle(pk.x - 6f, pk.y - 4f, 4f, paint)
                        canvas.drawCircle(pk.x + 6f, pk.y - 4f, 4f, paint)
                    }
                }
            }
        }

        // === ENEMIES ===
        paint.color = Color.rgb(139, 69, 19)  // Brown Goomba
        for (enemy in enemies) {
            if (enemy.alive) {
                canvas.drawCircle(enemy.x, enemy.y, 16f, paint)
                // Eyes
                paint.color = Color.WHITE
                canvas.drawCircle(enemy.x - 6f, enemy.y - 4f, 3f, paint)
                canvas.drawCircle(enemy.x + 6f, enemy.y - 4f, 3f, paint)
                paint.color = Color.BLACK
                canvas.drawCircle(enemy.x - 6f, enemy.y - 4f, 2f, paint)
                canvas.drawCircle(enemy.x + 6f, enemy.y - 4f, 2f, paint)
                paint.color = Color.rgb(139, 69, 19)
            }
        }

        // === WORLD BORDERS ===
        paint.color = Color.rgb(20, 20, 20)
        canvas.drawRect(-20f, -20f, worldWidth + 20f, 0f, paint)
        canvas.drawRect(-20f, worldHeight, worldWidth + 20f, worldHeight + 20f, paint)
        canvas.drawRect(-20f, -20f, 0f, worldHeight + 20f, paint)
        canvas.drawRect(worldWidth, -20f, worldWidth + 20f, worldHeight + 20f, paint)
    }

    private fun drawCloud(canvas: Canvas, x: Float, y: Float) {
        paint.color = Color.WHITE
        // Main cloud body
        canvas.drawCircle(x, y, 30f, paint)
        canvas.drawCircle(x + 25f, y, 35f, paint)
        canvas.drawCircle(x + 50f, y, 30f, paint)
        canvas.drawCircle(x + 70f, y, 25f, paint)
        // Cloud base
        canvas.drawCircle(x + 15f, y + 15f, 25f, paint)
        canvas.drawCircle(x + 40f, y + 15f, 30f, paint)
        canvas.drawCircle(x + 60f, y + 15f, 25f, paint)
    }

    private fun drawBrick(canvas: Canvas, brick: RectF) {
        // Brick base
        paint.color = Color.rgb(139, 69, 19)  // Saddle brown
        canvas.drawRect(brick, paint)

        // Brick pattern
        paint.color = Color.rgb(160, 82, 45)  // Sandy brown
        canvas.drawRect(brick.left + 2f, brick.top + 2f, brick.right - 2f, brick.top + 8f, paint)
        canvas.drawRect(brick.left + 2f, brick.top + 12f, brick.right - 2f, brick.top + 18f, paint)
        canvas.drawRect(brick.left + 2f, brick.top + 22f, brick.right - 2f, brick.bottom - 2f, paint)

        // Brick lines
        paint.color = Color.rgb(101, 67, 33)  // Dark brown
        paint.strokeWidth = 1f
        canvas.drawLine(brick.left, brick.top + 8f, brick.right, brick.top + 8f, paint)
        canvas.drawLine(brick.left, brick.top + 18f, brick.right, brick.top + 18f, paint)
        canvas.drawLine(brick.left + 16f, brick.top, brick.left + 16f, brick.top + 8f, paint)
        canvas.drawLine(brick.left + 16f, brick.top + 18f, brick.left + 16f, brick.bottom, paint)
    }

    fun resolvePlayerCollision(player: Player): Boolean {
        val rect = player.getRect()
        var collided = false

        // Top boundary
        if (rect.top < 0f) {
            player.y = 0f
            if (player.vy < 0f) player.vy = 0f
            collided = true
        }

        // Horizontal world bounds
        if (player.x < 0f) { player.x = 0f; player.vx = 0f }
        if (player.x + player.width > worldWidth) { player.x = worldWidth - player.width; player.vx = 0f }

        // Ground collision
        if (rect.bottom > groundTopY) {
            if (player.vy >= 0f) {
                player.y = groundTopY - player.height
                player.vy = 0f
                collided = true
            }
        }

        // Platform collisions
        for (p in platforms) {
            if (rect.right > p.left && rect.left < p.right) {
                if (player.vy > 0f && rect.bottom > p.top && rect.top < p.top) {
                    player.y = p.top - player.height
                    player.vy = 0f
                    collided = true
                }
            }
        }

        // Pipe collisions (solid blocks)
        for (pipe in pipes) {
            if (rect.intersect(pipe)) {
                // Simple push out collision
                val overlapX = minOf(rect.right - pipe.left, pipe.right - rect.left)
                val overlapY = minOf(rect.bottom - pipe.top, pipe.bottom - rect.top)

                if (overlapX < overlapY) {
                    // Horizontal collision
                    if (rect.centerX() < pipe.centerX()) {
                        player.x = pipe.left - player.width
                    } else {
                        player.x = pipe.right
                    }
                    player.vx = 0f
                } else {
                    // Vertical collision
                    if (rect.centerY() < pipe.centerY()) {
                        player.y = pipe.top - player.height
                        if (player.vy > 0f) player.vy = 0f
                    } else {
                        player.y = pipe.bottom
                        if (player.vy < 0f) player.vy = 0f
                    }
                }
                collided = true
            }
        }

        // Brick collisions
        for (brick in bricks) {
            if (rect.intersect(brick)) {
                val overlapX = minOf(rect.right - brick.left, brick.right - rect.left)
                val overlapY = minOf(rect.bottom - brick.top, brick.bottom - rect.top)

                if (overlapX < overlapY) {
                    if (rect.centerX() < brick.centerX()) {
                        player.x = brick.left - player.width
                    } else {
                        player.x = brick.right
                    }
                    player.vx = 0f
                } else {
                    if (rect.centerY() < brick.centerY()) {
                        player.y = brick.top - player.height
                        if (player.vy > 0f) player.vy = 0f
                    } else {
                        player.y = brick.bottom
                        if (player.vy < 0f) player.vy = 0f
                    }
                }
                collided = true
            }
        }

        // World bottom
        if (rect.bottom > worldHeight) {
            player.y = worldHeight - player.height
            player.vy = 0f
            collided = true
        }

        // Pickup collection
        for (pk in pickups) {
            if (!pk.collected) {
                val dx = (player.x + player.width / 2f) - pk.x
                val dy = (player.y + player.height / 2f) - pk.y
                if (dx * dx + dy * dy < 25f * 25f) {
                    pk.collected = true
                    // TODO: Add sound effects and score
                }
            }
        }

        // Enemy collision (simple)
        for (enemy in enemies) {
            if (enemy.alive) {
                val dx = (player.x + player.width / 2f) - enemy.x
                val dy = (player.y + player.height / 2f) - enemy.y
                if (dx * dx + dy * dy < 30f * 30f) {
                    if (player.vy > 0 && player.y < enemy.y - 10f) {
                        // Jump on enemy
                        enemy.alive = false
                        player.vy = -300f  // Bounce
                    } else {
                        // TODO: Take damage
                    }
                }
            }
        }

        return collided
    }

    fun update(deltaMs: Long) {
        // Update enemies
        for (enemy in enemies) {
            if (enemy.alive) {
                enemy.x += enemy.vx * (deltaMs / 1000f)

                // Simple AI: turn around at edges or pipes
                if (enemy.x < 50f || enemy.x > worldWidth - 50f) {
                    enemy.vx = -enemy.vx
                }

                // Turn around at pipes
                for (pipe in pipes) {
                    if (enemy.x > pipe.left - 50f && enemy.x < pipe.right + 50f &&
                        enemy.y > pipe.top - 50f && enemy.y < pipe.bottom + 50f) {
                        enemy.vx = -enemy.vx
                    }
                }
            }
        }

        // === Update clouds: move and wrap ===
        val dt = deltaMs / 1000f
        val wrapMargin = 120f
        for (cloud in clouds) {
            cloud.x += cloud.vx * dt

            // wrap horizontally for continuous movement
            if (cloud.x < -wrapMargin) {
                cloud.x = worldWidth + wrapMargin
            } else if (cloud.x > worldWidth + wrapMargin) {
                cloud.x = -wrapMargin
            }
            // (vertical bob handled in draw using sine)
        }
    }
}
