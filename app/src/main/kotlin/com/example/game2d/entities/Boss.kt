package com.example.game2d.entities

import android.content.Context
import android.graphics.*
import com.example.game2d.Player
import kotlin.math.abs

class Boss(ctx: Context, startX: Float, startY: Float) {
    var x = startX
    var y = startY
    var width = 400f  // Increased from 288f
    var height = 250f  // Increased from 160f

    private var health = 100
    private val maxHealth = 100

    // Movement
    private var vx = 0f
    private var moveSpeed = 100f
    private var facing = -1 // -1 = left, 1 = right

    // AI behavior
    private var aiState = AIState.IDLE
    private var stateTimer = 0L
    private var attackCooldown = 0L
    private val attackCooldownTime = 2000L // 2 seconds between attacks

    // Animation
    private val idleFrames = mutableListOf<Bitmap>()
    private val walkFrames = mutableListOf<Bitmap>()
    private val attackFrames = mutableListOf<Bitmap>()

    // Healthbar images
    private var healthBarUnder: Bitmap? = null
    private var healthBarProgress: Bitmap? = null
    private var healthBarOver: Bitmap? = null

    private var currentFrame = 0
    private var animTimer = 0L
    private val frameDuration = 80L

    // Attack hitbox
    private var attackActive = false
    private var attackHitbox = RectF()
    private val attackRange = 250f  // Increased attack range
    private val attackDamage = 1 // 1 heart per hit

    // Ground stomp attack
    private var stompActive = false
    private var stompShockwaves = mutableListOf<Shockwave>()

    // Invulnerability after being hit
    private var invulnerableUntil = 0L

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    enum class AIState {
        IDLE, CHASE, ATTACK, STOMP, DEAD
    }

    data class Shockwave(var x: Float, val y: Float, var radius: Float, val maxRadius: Float, var alpha: Int)

    init {
        loadAnimations(ctx)
        loadHealthBarImages(ctx)
    }

    private fun loadAnimations(ctx: Context) {
        try {
            // Load idle frames from assets
            for (i in 1..16) {
                val bitmap = loadBitmapFromAssets(ctx, "Boss/animations/idle/idle_$i.png")
                if (bitmap != null) {
                    idleFrames.add(bitmap)
                }
            }

            // Load walk frames from assets
            for (i in 1..12) {
                val bitmap = loadBitmapFromAssets(ctx, "Boss/animations/walk/walk_$i.png")
                if (bitmap != null) {
                    walkFrames.add(bitmap)
                }
            }

            // Load attack frames from assets
            for (i in 1..16) {
                val bitmap = loadBitmapFromAssets(ctx, "Boss/animations/atk_1/atk_1_$i.png")
                if (bitmap != null) {
                    attackFrames.add(bitmap)
                }
            }

            android.util.Log.d("Boss", "Loaded animations - idle:${idleFrames.size}, walk:${walkFrames.size}, attack:${attackFrames.size}")
        } catch (e: Exception) {
            android.util.Log.e("Boss", "Error loading animations: ${e.message}")
        }
    }

    private fun loadHealthBarImages(ctx: Context) {
        try {
            healthBarUnder = loadBitmapFromAssets(ctx, "Boss/bonus_mino_healthbar_UI/mino_health_under.png")
            healthBarProgress = loadBitmapFromAssets(ctx, "Boss/bonus_mino_healthbar_UI/mino_health_progress.png")
            healthBarOver = loadBitmapFromAssets(ctx, "Boss/bonus_mino_healthbar_UI/mino_health_over.png")
            android.util.Log.d("Boss", "Loaded healthbar images")
        } catch (e: Exception) {
            android.util.Log.e("Boss", "Error loading healthbar: ${e.message}")
        }
    }

    private fun loadBitmapFromAssets(ctx: Context, path: String): Bitmap? {
        return try {
            val inputStream = ctx.assets.open(path)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            android.util.Log.e("Boss", "Failed to load $path: ${e.message}")
            null
        }
    }

    fun update(deltaMs: Long, player: Player, groundY: Float) {
        if (aiState == AIState.DEAD) return

        animTimer += deltaMs
        stateTimer += deltaMs

        if (attackCooldown > 0) {
            attackCooldown -= deltaMs
        }

        // Update AI behavior
        updateAI(player, deltaMs)

        // Update position
        x += vx * (deltaMs / 1000f)

        // Keep on ground
        y = groundY - height

        // Update animation
        updateAnimation()

        // Update shockwaves
        updateShockwaves(deltaMs)
    }

    private fun updateAI(player: Player, deltaMs: Long) {
        val distanceToPlayer = abs(player.x - x)
        val playerDirection = if (player.x > x) 1 else -1

        when (aiState) {
            AIState.IDLE -> {
                vx = 0f
                if (stateTimer > 1000L) {
                    // Check if player is in range
                    if (distanceToPlayer < 600f) {
                        aiState = AIState.CHASE
                        stateTimer = 0L
                    }
                }
            }

            AIState.CHASE -> {
                facing = playerDirection

                if (distanceToPlayer < attackRange && attackCooldown <= 0) {
                    // Close enough to attack
                    aiState = AIState.ATTACK
                    stateTimer = 0L
                    currentFrame = 0
                    attackCooldown = attackCooldownTime
                } else if (distanceToPlayer < 600f) {
                    // Chase player
                    vx = moveSpeed * facing
                } else {
                    // Player too far, go idle
                    aiState = AIState.IDLE
                    stateTimer = 0L
                    vx = 0f
                }

                // Random stomp attack when health is low
                if (health < maxHealth / 2 && attackCooldown <= 0 && Math.random() < 0.01) {
                    aiState = AIState.STOMP
                    stateTimer = 0L
                    currentFrame = 0
                    attackCooldown = (attackCooldownTime * 1.5).toLong()
                }
            }

            AIState.ATTACK -> {
                vx = 0f

                // Activate hitbox in middle of attack animation
                if (currentFrame >= 8 && currentFrame <= 12) {
                    attackActive = true
                    updateAttackHitbox()
                } else {
                    attackActive = false
                }

                // Return to chase after attack animation
                if (stateTimer > frameDuration * attackFrames.size) {
                    aiState = AIState.CHASE
                    stateTimer = 0L
                    attackActive = false
                }
            }

            AIState.STOMP -> {
                vx = 0f

                // Create shockwaves at specific frame
                if (currentFrame == 12 && !stompActive) {
                    stompActive = true
                    createStompShockwaves()
                }

                // Return to chase after stomp
                if (stateTimer > frameDuration * attackFrames.size) {
                    aiState = AIState.CHASE
                    stateTimer = 0L
                    stompActive = false
                }
            }

            AIState.DEAD -> {
                vx = 0f
            }
        }
    }

    private fun updateAnimation() {
        if (animTimer >= frameDuration) {
            animTimer = 0L

            val maxFrames = when (aiState) {
                AIState.IDLE -> idleFrames.size
                AIState.CHASE -> walkFrames.size
                AIState.ATTACK, AIState.STOMP -> attackFrames.size
                AIState.DEAD -> 1
            }

            currentFrame = (currentFrame + 1) % maxFrames
        }
    }

    private fun updateAttackHitbox() {
        val hitboxX = if (facing == 1) x + width else x - attackRange
        attackHitbox.set(hitboxX, y, hitboxX + attackRange, y + height)
    }

    private fun createStompShockwaves() {
        // Create multiple shockwaves spreading outward
        for (i in 0..2) {
            val offset = i * 150f
            stompShockwaves.add(Shockwave(x - offset, y + height, 0f, 200f + offset, 255))
            stompShockwaves.add(Shockwave(x + offset, y + height, 0f, 200f + offset, 255))
        }
    }

    private fun updateShockwaves(deltaMs: Long) {
        val iterator = stompShockwaves.iterator()
        while (iterator.hasNext()) {
            val wave = iterator.next()
            wave.radius += 300f * (deltaMs / 1000f)
            wave.alpha = ((1f - wave.radius / wave.maxRadius) * 255).toInt().coerceIn(0, 255)

            if (wave.radius >= wave.maxRadius) {
                iterator.remove()
            }
        }
    }

    fun takeDamage(damage: Int): Boolean {
        val now = System.currentTimeMillis()
        if (now < invulnerableUntil) {
            return false // Still invulnerable
        }

        health -= damage
        invulnerableUntil = now + 500L // 0.5 second invulnerability

        if (health <= 0) {
            health = 0
            aiState = AIState.DEAD
            return true // Boss defeated
        }
        return false
    }

    fun checkPlayerHit(player: Player): Boolean {
        if (attackActive) {
            val playerRect = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
            if (RectF.intersects(attackHitbox, playerRect)) {
                return true
            }
        }

        // Check shockwave damage
        for (wave in stompShockwaves) {
            val playerCenterX = player.x + player.width / 2
            val playerBottom = player.y + player.height
            val distance = abs(playerCenterX - wave.x)

            if (distance < wave.radius && abs(playerBottom - wave.y) < 50f) {
                return true
            }
        }

        return false
    }

    fun getBounds(): RectF {
        return RectF(x, y, x + width, y + height)
    }

    fun isDefeated(): Boolean {
        return aiState == AIState.DEAD
    }

    fun getHealth(): Int = health
    fun getMaxHealth(): Int = maxHealth

    fun draw(canvas: Canvas) {
        // Get current frame bitmap
        val frameBitmap = when (aiState) {
            AIState.IDLE -> if (idleFrames.isNotEmpty()) idleFrames[currentFrame % idleFrames.size] else null
            AIState.CHASE -> if (walkFrames.isNotEmpty()) walkFrames[currentFrame % walkFrames.size] else null
            AIState.ATTACK, AIState.STOMP -> if (attackFrames.isNotEmpty()) attackFrames[currentFrame % attackFrames.size] else null
            AIState.DEAD -> if (attackFrames.isNotEmpty()) attackFrames.last() else null
        }

        frameBitmap?.let { bitmap ->
            val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
            val destRect = RectF(x, y, x + width, y + height)

            // Flip horizontally based on facing direction
            canvas.save()
            if (facing == 1) {
                canvas.scale(-1f, 1f, x + width / 2, y + height / 2)
            }

            // Blink when invulnerable
            val now = System.currentTimeMillis()
            if (now < invulnerableUntil && (now / 100) % 2 == 0L) {
                paint.alpha = 128
            } else {
                paint.alpha = 255
            }

            canvas.drawBitmap(bitmap, srcRect, destRect, paint)
            canvas.restore()
        }

        // Draw health bar
        drawHealthBar(canvas)

        // Draw shockwaves
        drawShockwaves(canvas)

    }

    private fun drawHealthBar(canvas: Canvas) {
        val barWidth = width * 1.5f  // Increased width significantly
        val barHeight = 50f  // Increased height from 30f to 50f
        val barX = x - (barWidth - width) / 2
        val barY = y - 70f  // Move up more to avoid overlap with boss

        val healthPercent = health.toFloat() / maxHealth.toFloat()

        // Use custom healthbar images if available
        if (healthBarUnder != null && healthBarProgress != null && healthBarOver != null) {
            paint.alpha = 255  // Ensure full opacity

            // Draw under layer (background)
            healthBarUnder?.let {
                val destRect = RectF(barX, barY, barX + barWidth, barY + barHeight)
                canvas.drawBitmap(it, null, destRect, paint)
            }

            // Draw progress (health bar fill) - clip to health percentage
            healthBarProgress?.let {
                val progressWidth = barWidth * healthPercent
                val srcRect = Rect(0, 0, (it.width * healthPercent).toInt(), it.height)
                val destRect = RectF(barX, barY, barX + progressWidth, barY + barHeight)
                canvas.drawBitmap(it, srcRect, destRect, paint)
            }

            // Draw over layer (border/frame)
            healthBarOver?.let {
                val destRect = RectF(barX, barY, barX + barWidth, barY + barHeight)
                canvas.drawBitmap(it, null, destRect, paint)
            }
        } else {
            // Fallback to simple colored bars
            paint.style = Paint.Style.FILL

            // Background
            paint.color = Color.RED
            canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint)

            // Health
            paint.color = Color.GREEN
            canvas.drawRect(barX, barY, barX + barWidth * healthPercent, barY + barHeight, paint)

            // Border
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRect(barX, barY, barX + barWidth, barY + barHeight, paint)
            paint.style = Paint.Style.FILL
        }
    }

    private fun drawShockwaves(canvas: Canvas) {
        for (wave in stompShockwaves) {
            paint.color = Color.argb(wave.alpha, 255, 100, 0)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 8f
            canvas.drawCircle(wave.x, wave.y, wave.radius, paint)
        }
    }
}

