package com.example.game2d

import android.content.Context
import android.graphics.*
import com.example.game2d.entities.Boss
import com.example.game2d.obstacles.Saw
import com.example.game2d.obstacles.Spike
import kotlin.math.min

class TileMap3(ctx: Context) : TileMapInterface {

    override val worldWidth = 3000f
    override val worldHeight = 720f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val groundTopY = worldHeight * 0.8f

    // Boss
    private var boss: Boss? = null

    // Simple platforms for movement
    private val platforms = ArrayList<RectF>()

    // Boss defeated callback
    private var bossDefeatedCallback: (() -> Unit)? = null

    // Hit effect particles - optimized limit
    private val playerHitEffects = mutableListOf<HitEffect>()
    private val maxHitEffects = 20

    data class HitEffect(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, var alpha: Int)

    // Cached gradients for performance
    private var skyGradient: LinearGradient? = null
    private var gradientInitialized = false

    init {
        setupBossArena()

        // Spawn boss in the middle of arena
        boss = Boss(ctx, worldWidth / 2 - 150f, groundTopY - 200f)
    }

    private fun setupBossArena() {
        // Create a simple flat arena for boss fight
        // Main ground is already at groundTopY

        // Add some side platforms for dodging
        platforms.add(RectF(200f, groundTopY - 100f, 400f, groundTopY - 84f))
        platforms.add(RectF(worldWidth - 400f, groundTopY - 100f, worldWidth - 200f, groundTopY - 84f))

        // Higher platforms for advanced dodging
        platforms.add(RectF(400f, groundTopY - 200f, 600f, groundTopY - 184f))
        platforms.add(RectF(worldWidth - 600f, groundTopY - 200f, worldWidth - 400f, groundTopY - 184f))
    }

    fun setBossDefeatedCallback(callback: () -> Unit) {
        bossDefeatedCallback = callback
    }

    override fun update(deltaMs: Long) {
        // Update hit effects
        val iterator = playerHitEffects.iterator()
        while (iterator.hasNext()) {
            val effect = iterator.next()
            effect.x += effect.vx * (deltaMs / 1000f)
            effect.y += effect.vy * (deltaMs / 1000f)
            effect.vy += 200f * (deltaMs / 1000f)  // Gravity
            effect.life -= deltaMs.toFloat()
            effect.alpha = ((effect.life / 500f) * 255).toInt().coerceIn(0, 255)

            if (effect.life <= 0) {
                iterator.remove()
            }
        }
    }

    override fun updateMonsters(deltaMs: Long, player: Player) {
        // Update boss AI and check for player hits
        boss?.let { b ->
            b.update(deltaMs, player, groundTopY)

            // Check if boss attacks hit player
            if (b.checkPlayerHit(player)) {
                // Handle player damage in GameView through collision detection
            }

            // Check if boss defeated
            if (b.isDefeated()) {
                bossDefeatedCallback?.invoke()
            }
        }
    }

    override fun draw(canvas: Canvas) {
        // Draw optimized background
        drawOptimizedBackground(canvas)

        // Draw platforms
        drawPlatforms(canvas)

        // Draw boss - MUST be drawn last to appear on top
        boss?.draw(canvas)

        // Draw hit effects
        drawHitEffects(canvas)
    }

    private fun drawOptimizedBackground(canvas: Canvas) {
        // Initialize gradient once for performance
        if (!gradientInitialized) {
            skyGradient = LinearGradient(
                0f, 0f, 0f, worldHeight,
                intArrayOf(
                    Color.rgb(135, 206, 250),  // Light sky blue at top
                    Color.rgb(100, 149, 237),  // Cornflower blue
                    Color.rgb(70, 130, 180)    // Steel blue at bottom
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
            gradientInitialized = true
        }

        // Draw sky with cached gradient
        paint.shader = skyGradient
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, worldWidth, worldHeight, paint)
        paint.shader = null

        // Draw simple mountains - reduced complexity
        paint.color = Color.rgb(80, 100, 120)
        paint.alpha = 150
        drawMountain(canvas, 100f, groundTopY - 50f, 700f, 350f)
        drawMountain(canvas, 1200f, groundTopY - 30f, 800f, 380f)
        drawMountain(canvas, 2100f, groundTopY - 60f, 750f, 400f)
        paint.alpha = 255

        // Draw simple clouds - reduced count
        paint.color = Color.rgb(240, 248, 255)
        paint.alpha = 180
        drawCloud(canvas, 400f, 100f, 130f)
        drawCloud(canvas, 1400f, 90f, 140f)
        drawCloud(canvas, 2300f, 110f, 135f)
        paint.alpha = 255

        // Draw ground decoration - grass layer
        paint.color = Color.rgb(34, 139, 34)
        canvas.drawRect(0f, groundTopY - 10f, worldWidth, groundTopY, paint)
    }

    private fun drawMountain(canvas: Canvas, x: Float, baseY: Float, width: Float, height: Float) {
        val path = Path()
        path.moveTo(x, baseY)
        path.lineTo(x + width / 2, baseY - height)
        path.lineTo(x + width, baseY)
        path.close()
        paint.style = Paint.Style.FILL
        canvas.drawPath(path, paint)
    }

    private fun drawCloud(canvas: Canvas, x: Float, y: Float, size: Float) {
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, size * 0.6f, paint)
        canvas.drawCircle(x + size * 0.5f, y, size * 0.7f, paint)
        canvas.drawCircle(x + size, y, size * 0.6f, paint)
        canvas.drawCircle(x + size * 0.3f, y + size * 0.3f, size * 0.5f, paint)
        canvas.drawCircle(x + size * 0.7f, y + size * 0.3f, size * 0.5f, paint)
    }

    private fun drawPlatforms(canvas: Canvas) {
        // Draw platforms with stone texture effect
        paint.style = Paint.Style.FILL
        for (platform in platforms) {
            // Main platform color - dark gray stone
            paint.color = Color.rgb(90, 90, 90)
            canvas.drawRect(platform, paint)

            // Top highlight
            paint.color = Color.rgb(120, 120, 120)
            canvas.drawRect(platform.left, platform.top, platform.right, platform.top + 3f, paint)

            // Border
            paint.color = Color.rgb(60, 60, 60)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRect(platform, paint)
        }

        // Draw ground - simplified
        paint.style = Paint.Style.FILL

        // Dark brown earth
        paint.color = Color.rgb(60, 40, 20)
        canvas.drawRect(0f, groundTopY, worldWidth, worldHeight, paint)

        // Reduced texture lines for performance
        paint.color = Color.rgb(50, 30, 15)
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        var lineY = groundTopY + 40f
        while (lineY < worldHeight) {
            canvas.drawLine(0f, lineY, worldWidth, lineY, paint)
            lineY += 60f  // Increased spacing
        }

        paint.style = Paint.Style.FILL
    }

    override fun resolvePlayerCollision(player: Player): Boolean {
        resolvePlayerCollisionSafe(player)
        return false
    }

    override fun resolvePlayerCollisionSafe(player: Player) {
        player.prevX = player.x
        player.prevY = player.y

        val playerRect = RectF(player.x, player.y, player.x + player.width, player.y + player.height)

        // Check platform collisions
        for (plat in platforms) {
            if (RectF.intersects(playerRect, plat)) {
                val overlapLeft = (player.x + player.width) - plat.left
                val overlapRight = plat.right - player.x
                val overlapTop = (player.y + player.height) - plat.top
                val overlapBottom = plat.bottom - player.y

                val minOverlap = min(min(overlapLeft, overlapRight), min(overlapTop, overlapBottom))

                when (minOverlap) {
                    overlapTop -> {
                        player.y = plat.top - player.height
                        if (player.vy > 0) player.vy = 0f
                    }
                    overlapBottom -> {
                        player.y = plat.bottom
                        if (player.vy < 0) player.vy = 0f
                    }
                    overlapLeft -> player.x = plat.left - player.width
                    overlapRight -> player.x = plat.right
                }
            }
        }

        // Ground collision
        if (player.y + player.height >= groundTopY) {
            player.y = groundTopY - player.height
            if (player.vy > 0) player.vy = 0f
        }

        // Wall boundaries
        if (player.x < 0f) player.x = 0f
        if (player.x + player.width > worldWidth) player.x = worldWidth - player.width
    }

    override fun checkBulletHitAndRespawnIfNeeded(player: Player): Boolean {
        // Check if boss hit player
        boss?.let { b ->
            if (b.checkPlayerHit(player)) {
                return true // Player was hit
            }
        }
        return false
    }

    override fun isCompleted(player: Player): Boolean {
        // Map is completed when boss is defeated
        return boss?.isDefeated() ?: false
    }

    override fun checkCoinCollection(player: Player, onCoinCollected: (String) -> Unit) {
        // No coins in boss arena
    }

    override fun checkHealthCollection(player: Player, onHealthCollected: (Int) -> Unit) {
        // No health pickups in boss arena
    }

    override fun getSpikes(): List<Spike> {
        // No spikes in boss arena
        return emptyList()
    }

    override fun getSaws(): List<Saw> {
        // No saws in boss arena
        return emptyList()
    }

    override fun getGroundTopY(): Float = groundTopY

    override fun getLastCheckpoint(): Triple<Float, Float, Int> {
        return Triple(200f, groundTopY - 64f, 2) // mapId = 2 for TileMap3
    }

    override fun resetLevel() {
        // Reset boss when restarting level - create new boss instance
        val ctx = AppCtx.ctx
        if (ctx != null) {
            boss = Boss(ctx, worldWidth / 2 - 150f, groundTopY - 200f)
        }

        // Clear hit effects
        playerHitEffects.clear()
    }

    fun getBoss(): Boss? = boss

    fun isBossDefeated(): Boolean {
        return boss?.isDefeated() ?: false
    }

    // Player attack boss method - called from GameView when player attacks
    fun playerAttackBoss(player: Player): Boolean {
        boss?.let { b ->
            // Check if player is close enough to hit boss
            val playerRect = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
            val bossRect = b.getBounds()

            // Extend player attack range a bit
            val attackRect = RectF(
                playerRect.left - 50f,
                playerRect.top,
                playerRect.right + 50f,
                playerRect.bottom
            )

            if (RectF.intersects(attackRect, bossRect)) {
                // Deal damage to boss
                val defeated = b.takeDamage(10) // 10 damage per hit, needs 10 hits to defeat

                // Create hit effect particles when player hits boss
                createPlayerHitEffect(b.x + b.width / 2, b.y + b.height / 2)

                if (defeated) {
                    bossDefeatedCallback?.invoke()
                }
                return true // Attack hit
            }
        }
        return false // Attack missed
    }

    private fun createPlayerHitEffect(centerX: Float, centerY: Float) {
        // Limit number of particles to prevent lag
        if (playerHitEffects.size >= maxHitEffects) {
            return
        }

        // Create fewer but more visible explosion particles
        for (i in 0..8) {
            val angle = (i * 360f / 8f) * Math.PI / 180f
            val speed = 150f + Math.random().toFloat() * 100f

            playerHitEffects.add(
                HitEffect(
                    x = centerX,
                    y = centerY,
                    vx = (Math.cos(angle) * speed).toFloat(),
                    vy = (Math.sin(angle) * speed).toFloat() - 100f,  // Upward bias
                    life = 500f,
                    alpha = 255
                )
            )
        }
    }

    private fun drawHitEffects(canvas: Canvas) {
        // Draw hit effects as explosion particles - optimized
        paint.style = Paint.Style.FILL
        for (effect in playerHitEffects) {
            paint.color = Color.argb(effect.alpha, 255, 215, 0) // Gold color
            canvas.drawCircle(effect.x, effect.y, 6f, paint)  // Smaller circles for performance
        }
    }
}
