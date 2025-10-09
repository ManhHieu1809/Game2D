package com.example.game2d

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private var thread: GameThread = GameThread(holder, this)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val tileMap: TileMap
    private val tileMap2: TileMap2
    private val tileMap3: TileMap3
    private var currentTileMap: TileMapInterface
    private var isOnTileMap2 = false
    private var isOnTileMap3 = false
    private val player: Player

    // New game systems
    private val shopManager = ShopManager(context)
    private val gameStateManager = GameStateManager(context)

    // Sound and Settings
    private val soundManager = SoundManager(context)
    private val settingsOverlay = SettingsOverlay(context, soundManager)

    // camera (world coords)
    private var cameraX = 0f
    private var cameraY = 0f

    // scale/center
    private var worldScale = 1f
    private var screenOffsetX = 0f
    private var screenOffsetY = 0f

    // HUD buttons (screen coords)
    private val btnLeft = RectF()
    private val btnRight = RectF()
    private val btnJump = RectF()
    private val btnAttack = RectF() // New attack button for boss fight
    private val activePointers = HashMap<Int, String>()

    // Game Over/Win UI elements
    private val yesButton = RectF()
    private val noButton = RectF()
    private val settingsButton = RectF()
    private val highScoresButton = RectF() // Add high scores button for win screen
    private var gameOverImage: Bitmap? = null
    private var congratulationsImage: Bitmap? = null // Add congratulations image

    private var screenW = 1f
    private var screenH = 1f

    // paints for buttons
    private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 36f; textAlign = Paint.Align.CENTER
    }

    // Game state tracking
    private var monstersKilled = 0
    private var coinsCollectedThisSession = 0
    private var isGameWon = false

    init {
        AppCtx.ctx = context

        // Load game over image
        loadGameOverImage()

        // now safe to construct TileMap and Player (they may load sprites)
        tileMap = TileMap(context)
        tileMap2 = TileMap2(context)
        tileMap3 = TileMap3(context)
        currentTileMap = tileMap // Bắt đầu với tilemap 1
        player = Player(context, 200f, 0f)

        // Set up monster kill callbacks
        tileMap.setMonsterKillCallback { onMonsterKilled() }
        tileMap2.setMonsterKillCallback { onMonsterKilled() }

        // Set up boss defeated callback
        tileMap3.setBossDefeatedCallback { onBossDefeated() }

        // Always start fresh game session with full lives
        gameStateManager.initializeNewSession()

        holder.addCallback(this)
        isFocusable = true
    }

    private fun loadGameOverImage() {
        try {
            @Suppress("DiscouragedApi")
            val gameOverId = context.resources.getIdentifier("game_over", "drawable", context.packageName)
            if (gameOverId != 0) {
                gameOverImage = BitmapFactory.decodeResource(context.resources, gameOverId)
            }

            // Load congratulations image - try different resource locations
            @Suppress("DiscouragedApi")
            val congratsId = context.resources.getIdentifier("congratulations", "drawable", context.packageName)
            if (congratsId != 0) {
                congratulationsImage = BitmapFactory.decodeResource(context.resources, congratsId)
                android.util.Log.d("GameView", "Congratulations image loaded successfully")
            } else {
                android.util.Log.e("GameView", "Failed to load congratulations image")
            }
        } catch (e: Exception) {
            android.util.Log.e("GameView", "Error loading images: ${e.message}")
            // Fallback if images don't exist
            gameOverImage = null
            congratulationsImage = null
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // initial compute
        screenW = width.toFloat()
        screenH = height.toFloat()

        recomputeLayout()
        // Setup settings overlay layout
        settingsOverlay.setupLayout(screenW, screenH)

        // place player on ground after we know player.height
        player.x = 200f
        player.y = tileMap.getGroundTopY() - player.height

        thread = GameThread(holder, this)
        thread.running = true
        thread.start()

        // Start background music when game starts
        soundManager.playBackgroundMusic()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        // when orientation or size changes -> recompute scale / buttons
        screenW = width.toFloat()
        screenH = height.toFloat()
        recomputeLayout()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        thread.running = false
        var retry = true
        while (retry) {
            try { thread.join(); retry = false } catch (_: InterruptedException) {}
        }
    }

    private fun recomputeLayout() {
        val btnSize = min(screenW, screenH) * 0.16f
        val margin = btnSize * 0.18f
        btnLeft.set(margin, screenH - margin - btnSize, margin + btnSize, screenH - margin)
        btnRight.set(btnLeft.right + margin * 0.6f, btnLeft.top, btnLeft.right + margin * 0.6f + btnSize, btnLeft.bottom)
        btnJump.set(screenW - margin - btnSize, screenH - margin - btnSize, screenW - margin, screenH - margin)

        // Attack button positioned next to jump button (for boss fight in map 3)
        btnAttack.set(screenW - margin - btnSize * 2 - margin * 0.6f, screenH - margin - btnSize, screenW - margin - btnSize - margin * 0.6f, screenH - margin)

        // Game Over/Win buttons layout - 3 buttons in a row
        val gameOverBtnW = screenW * 0.18f  // Button width
        val gameOverBtnH = screenH * 0.08f  // Button height
        val gameOverBtnY = screenH * 0.75f  // Y position
        val gameOverBtnSpacing = screenW * 0.06f  // Spacing between buttons

        val totalButtonsWidth = gameOverBtnW * 3 + gameOverBtnSpacing * 2
        val startX = (screenW - totalButtonsWidth) / 2f

        // YES button (position 1)
        yesButton.set(startX, gameOverBtnY, startX + gameOverBtnW, gameOverBtnY + gameOverBtnH)

        // NO button (position 2)
        noButton.set(startX + gameOverBtnW + gameOverBtnSpacing, gameOverBtnY, startX + gameOverBtnW + gameOverBtnSpacing + gameOverBtnW, gameOverBtnY + gameOverBtnH)

        // SETTINGS button (position 3) - used for game over screen
        settingsButton.set(startX + (gameOverBtnW + gameOverBtnSpacing) * 2, gameOverBtnY, startX + (gameOverBtnW + gameOverBtnSpacing) * 2 + gameOverBtnW, gameOverBtnY + gameOverBtnH)

        // HIGH SCORES button (position 3) - same position as settings button, used for win screen
        highScoresButton.set(startX + (gameOverBtnW + gameOverBtnSpacing) * 2, gameOverBtnY, startX + (gameOverBtnW + gameOverBtnSpacing) * 2 + gameOverBtnW, gameOverBtnY + gameOverBtnH)

        worldScale = screenH / tileMap.worldHeight
        worldScale = worldScale.coerceIn(0.5f, 2.0f)
        screenOffsetY = (screenH - tileMap.worldHeight * worldScale) / 2f
        screenOffsetX = 0f
    }

    fun update(deltaMs: Long) {
        if (gameStateManager.isGameOver()) {
            return
        }

        gameStateManager.updateInvulnerability()

        var mv = 0
        if (activePointers.containsValue("left")) mv = -1
        if (activePointers.containsValue("right")) mv = 1
        player.setMoving(mv)

        if (isOnTileMap2) {
            player.update(deltaMs, tileMap2)
            tileMap2.update(deltaMs)
            tileMap2.updateMonsters(deltaMs, player)

            tileMap2.checkCoinCollection(player) { coinType ->
                onCoinCollected(coinType)
            }

            tileMap2.checkHealthCollection(player) { healAmount ->
                onHealthCollected(healAmount)
            }

            if (!gameStateManager.isInvulnerable()) {
                tileMap2.resolvePlayerCollisionSafe(player)

                var playerDied = false
                var hazardHit = false

                if (tileMap2.checkBulletHitAndRespawnIfNeeded(player)) {
                    playerDied = true
                }

                if (!playerDied) {
                    for (spike in tileMap2.getSpikes()) {
                        if (spike.isHit(player)) {
                            hazardHit = true
                            break
                        }
                    }

                    if (!hazardHit) {
                        for (saw in tileMap2.getSaws()) {
                            if (saw.isHit(player)) {
                                hazardHit = true
                                break
                            }
                        }
                    }

                    if (!hazardHit && player.y + player.height > tileMap2.worldHeight) {
                        hazardHit = true
                    }
                }

                if (playerDied) {
                    handlePlayerDeath()
                } else if (hazardHit) {
                    handleHazardCollision()
                }
            } else {
                tileMap2.resolvePlayerCollisionSafe(player)
            }

            checkForCheckpoints(tileMap2, 1)

            // Check for transition to map 3 when reaching end of map 2
            if (player.x >= 7700f) {
                switchToTileMap3()
                // Save checkpoint at start of Map3
                val entryX = 200f
                val entryY = tileMap3.getGroundTopY() - player.height
                gameStateManager.setCheckpoint(entryX, entryY, 2)
                android.util.Log.d("GameView", "Switched to Map3 (Boss Fight), saved checkpoint: x=$entryX, y=$entryY, mapId=2")
            }
        } else if (isOnTileMap3) {
            player.update(deltaMs, tileMap3)
            tileMap3.update(deltaMs)
            tileMap3.updateMonsters(deltaMs, player)

            tileMap3.checkCoinCollection(player) { coinType ->
                onCoinCollected(coinType)
            }

            tileMap3.checkHealthCollection(player) { healAmount ->
                onHealthCollected(healAmount)
            }

            if (!gameStateManager.isInvulnerable()) {
                tileMap3.resolvePlayerCollisionSafe(player)

                // Check if boss hit player - boss attack should reduce hearts, not instant death
                if (tileMap3.checkBulletHitAndRespawnIfNeeded(player)) {
                    // Boss hit player - lose one heart and become invulnerable temporarily
                    handleHazardCollision() // This will reduce 1 heart
                }

                // Check if player fell off map
                if (player.y + player.height > tileMap3.worldHeight) {
                    handlePlayerDeath() // Instant death for falling
                }
            } else {
                tileMap3.resolvePlayerCollisionSafe(player)
            }

            checkForCheckpoints(tileMap3, 2)

            // Check for victory condition - when boss is defeated, trigger win
            if (tileMap3.isBossDefeated()) {
                if (!isGameWon) {
                    onBossDefeated()
                }
            }
        } else {
            player.update(deltaMs, tileMap)
            tileMap.update(deltaMs)
            tileMap.updateMonsters(deltaMs, player)

            tileMap.checkCoinCollection(player) { coinType ->
                onCoinCollected(coinType)
            }

            tileMap.checkHealthCollection(player) { healAmount ->
                onHealthCollected(healAmount)
            }

            if (!gameStateManager.isInvulnerable()) {
                tileMap.resolvePlayerCollisionSafe(player)

                val (lx, ly, lm) = if (isOnTileMap2) tileMap2.getLastCheckpoint() else tileMap.getLastCheckpoint()
                val (gx, gy, gm) = gameStateManager.getCheckpoint()
                if (lx != gx || ly != gy || lm != gm) {
                    gameStateManager.setCheckpoint(lx, ly, lm)
                }

                var playerDied = false
                var hazardHit = false

                if (tileMap.checkBulletHitAndRespawnIfNeeded(player)) {
                    playerDied = true
                }

                if (!playerDied) {
                    for (spike in tileMap.getSpikes()) {
                        if (spike.isHit(player)) {
                            hazardHit = true
                            break
                        }
                    }

                    if (!hazardHit) {
                        for (saw in tileMap.getSaws()) {
                            if (saw.isHit(player)) {
                                hazardHit = true
                                break
                            }
                        }
                    }

                    if (!hazardHit && player.y + player.height > tileMap.worldHeight) {
                        hazardHit = true
                    }
                }

                if (playerDied) {
                    handlePlayerDeath()
                } else if (hazardHit) {
                    handleHazardCollision()
                }
            } else {
                tileMap.resolvePlayerCollisionSafe(player)

                val (lx, ly, lm) = if (isOnTileMap2) tileMap2.getLastCheckpoint() else tileMap.getLastCheckpoint()
                val (gx, gy, gm) = gameStateManager.getCheckpoint()
                if (lx != gx || ly != gy || lm != gm) {
                    gameStateManager.setCheckpoint(lx, ly, lm)
                }

            }

            checkForCheckpoints(tileMap, 0)

            if (player.x >= 6000f) {
                switchToTileMap2()
                // Lưu checkpoint đầu Map2 ngay khi chuyển map
                val entryX = 200f
                val entryY = tileMap2.getGroundTopY() - player.height
                gameStateManager.setCheckpoint(entryX, entryY, 1)
                android.util.Log.d("GameView", "Switched to Map2, saved checkpoint: x=$entryX, y=$entryY, mapId=1")
            }
        }

        val currentWorldWidth = if (isOnTileMap3) tileMap3.worldWidth else if (isOnTileMap2) tileMap2.worldWidth else tileMap.worldWidth
        val currentWorldHeight = if (isOnTileMap3) tileMap3.worldHeight else if (isOnTileMap2) tileMap2.worldHeight else tileMap.worldHeight

        val viewportWorldW = screenW / worldScale
        val viewportWorldH = screenH / worldScale

        val targetCameraX = player.x + player.width / 2f - viewportWorldW / 2f
        val targetCameraY = player.y + player.height / 2f - viewportWorldH / 2f

        cameraX = round(targetCameraX)
        cameraY = round(targetCameraY)

        cameraX = cameraX.coerceAtLeast(0f)
        cameraX = cameraX.coerceAtMost(max(0f, currentWorldWidth - viewportWorldW))

        cameraY = cameraY.coerceAtLeast(0f)
        cameraY = cameraY.coerceAtMost(max(0f, currentWorldHeight - viewportWorldH))
    }

    private fun switchToTileMap2() {
        isOnTileMap2 = true
        isOnTileMap3 = false

        player.x = 200f
        player.y = tileMap2.getGroundTopY() - player.height
        player.vx = 0f
        player.vy = 0f

        cameraX = 0f
        cameraY = 0f

        // Set checkpoint at start of map 2
        gameStateManager.setCheckpoint(200f, tileMap2.getGroundTopY() - player.height, 1)
    }

    private fun switchToTileMap3() {
        isOnTileMap3 = true
        isOnTileMap2 = false

        player.x = 200f
        player.y = tileMap3.getGroundTopY() - player.height
        player.vx = 0f
        player.vy = 0f

        cameraX = 0f
        cameraY = 0f

        // Set checkpoint at start of map 3 (boss arena)
        gameStateManager.setCheckpoint(200f, tileMap3.getGroundTopY() - player.height, 2)
    }

    fun onCoinCollected(coinType: String) {
        val coinValue = when(coinType) {
            "cherry", "strawberry" -> 5
            "apple", "orange" -> 10
            "banana" -> 15
            else -> 1
        }
        shopManager.addCoins(coinValue)
        coinsCollectedThisSession += coinValue // Track coins collected in this session
        soundManager.playCoinSound()
    }

    // Add method to track monster kills
    fun onMonsterKilled() {
        monstersKilled++
    }

    // Add method to track boss defeat
    fun onBossDefeated() {
        if (!isGameWon) {
            isGameWon = true
            soundManager.playVictorySound()

            // Save the current session score
            android.util.Log.d("GameView", "Boss defeated - Saving score: coins=$coinsCollectedThisSession, monsters=$monstersKilled")
            val scoreManager = ScoreManager(context)
            scoreManager.saveScore(coinsCollectedThisSession, monstersKilled)
        }
    }

    private fun handlePlayerDeath() {
        // Play death sound when player loses a life
        soundManager.playDeathSound()

        gameStateManager.loseLife()

        if (gameStateManager.isGameOver()) {
            val scoreManager = ScoreManager(context)
            scoreManager.saveScore(coinsCollectedThisSession, monstersKilled)

            // Play game over sound when all lives are lost
            soundManager.playGameOverSound()
        } else {
            respawnAtCheckpoint()
        }
    }

    fun onHealthCollected(healAmount: Int) {
        val currentLives = gameStateManager.getLives()
        if (currentLives < GameStateManager.MAX_LIVES) {
            val newLives = (currentLives + healAmount).coerceAtMost(GameStateManager.MAX_LIVES)
            gameStateManager.setLives(newLives)
        }
    }

    private fun resetToStart() {
        isOnTileMap2 = false
        isOnTileMap3 = false
        currentTileMap = tileMap

        player.x = GameStateManager.DEFAULT_SPAWN_X
        player.y = tileMap.getGroundTopY() - player.height
        player.vx = 0f
        player.vy = 0f

        cameraX = 0f
        cameraY = 0f
    }

    private fun startNewGame() {
        // Reset game state manager first - ensure lives are set to 3
        gameStateManager.initializeNewSession()
        gameStateManager.startInvulnerability()
        // Reset maps
        tileMap.resetLevel()
        tileMap2.resetLevel()
        tileMap3.resetLevel()
        // Reset to start position
        resetToStart()
    }

    private fun goToMenu() {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
        (context as GameActivity).finish()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(Color.BLACK)

        canvas.save()
        canvas.translate(screenOffsetX, screenOffsetY)
        canvas.scale(worldScale, worldScale)

        val camX = round(cameraX)
        val camY = round(cameraY)
        canvas.translate(-camX, -camY)

        if (isOnTileMap3) {
            tileMap3.draw(canvas)
        } else if (isOnTileMap2) {
            tileMap2.draw(canvas)
        } else {
            tileMap.draw(canvas)
        }

        if (gameStateManager.isInvulnerable()) {
            val blinkRate = 150L
            val shouldShow = (System.currentTimeMillis() / blinkRate) % 2 == 0L
            if (shouldShow) {
                player.draw(canvas, paint)
            }
        } else {
            player.draw(canvas, paint)
        }

        PotionEffects.drawJumpEffect(canvas, player, paint)
        PotionEffects.drawSpeedEffect(canvas, player, paint)
        PotionEffects.drawShieldEffect(canvas, player, paint, gameStateManager)

        canvas.restore()

        // Show win screen first (highest priority)
        if (isGameWon) {
            drawWinScreen(canvas)
        } else if (gameStateManager.isGameOver()) {
            drawGameOverScreen(canvas)
        } else {
            // Normal gameplay UI
            drawControlButtonVisible(canvas, btnLeft, "◀", activePointers.containsValue("left"))
            drawControlButtonVisible(canvas, btnRight, "▶", activePointers.containsValue("right"))
            drawControlButtonVisible(canvas, btnJump, "▲", activePointers.containsValue("jump"))

            // Draw attack button only in tilemap 3 (boss fight)
            if (isOnTileMap3) {
                drawControlButtonVisible(canvas, btnAttack, "⚔", activePointers.containsValue("attack"))
            }

            drawHudWithIcons(canvas)
        }

        // Draw settings overlay on top of everything (always last)
        settingsOverlay.draw(canvas)
    }

    private fun drawHudWithIcons(canvas: Canvas) {
        val hudMargin = 30f
        val iconSize = 45f

        // Draw settings overlay on top of everything (always last)
        settingsOverlay.draw(canvas)
        val spacing = 10f

        val lives = gameStateManager.getLives()
        val maxLives = GameStateManager.MAX_LIVES

        for (i in 0 until maxLives) {
            val heartX = hudMargin + i * (iconSize + spacing)
            val heartY = hudMargin
            val filled = i < lives
            HudIcons.drawHeart(canvas, heartX, heartY, iconSize, paint, filled)
        }

        val coinX = hudMargin
        val coinY = hudMargin + iconSize + spacing * 2
        val currentTime = System.currentTimeMillis()
        HudIcons.drawCoin(canvas, coinX, coinY, iconSize, paint, currentTime)

        val coinTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 36f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(3f, 2f, 2f, Color.BLACK)
        }

        val coinCount = shopManager.getCoins()
        canvas.drawText("× $coinCount", coinX + iconSize + spacing, coinY + iconSize * 0.7f, coinTextPaint)

        if (gameStateManager.isInvulnerable()) {
            val progress = gameStateManager.getInvulnerabilityProgress()
            val barWidth = 250f
            val barHeight = 20f
            val barX = screenW - barWidth - hudMargin
            val barY = hudMargin

            HudIcons.drawInvulnerabilityBar(canvas, barX, barY, barWidth, barHeight, progress, paint)
        }

        drawInventoryItems(canvas)
    }

    private fun drawInventoryItems(canvas: Canvas) {
        val itemSize = 80f
        val spacing = 15f
        val startX = screenW - itemSize - 30f
        val startY = 120f

        var currentY = startY

        val healthPotions = shopManager.getHealthPotions()
        if (healthPotions > 0) {
            drawInventorySlot(canvas, startX, currentY, itemSize, "❤️", healthPotions, "HP")
            currentY += itemSize + spacing
        }

        val jumpPotions = shopManager.getJumpPotions()
        if (jumpPotions > 0) {
            drawInventorySlot(canvas, startX, currentY, itemSize, "🦘", jumpPotions, "JUMP")
            currentY += itemSize + spacing
        }

        val speedPotions = shopManager.getSpeedPotions()
        if (speedPotions > 0) {
            drawInventorySlot(canvas, startX, currentY, itemSize, "💨", speedPotions, "SPEED")
            currentY += itemSize + spacing
        }

        val shieldPotions = shopManager.getShieldPotions()
        if (shieldPotions > 0) {
            drawInventorySlot(canvas, startX, currentY, itemSize, "🛡️", shieldPotions, "SHIELD")
            currentY += itemSize + spacing
        }

        val magnetPotions = shopManager.getMagnetPotions()
        if (magnetPotions > 0) {
            drawInventorySlot(canvas, startX, currentY, itemSize, "🧲", magnetPotions, "MAGNET")
        }

        drawEffectCountdowns(canvas)
    }

    private fun drawInventorySlot(canvas: Canvas, x: Float, y: Float, size: Float, icon: String, count: Int, label: String) {
        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(200, 40, 40, 40)
        }
        canvas.drawRoundRect(x, y, x + size, y + size, 12f, 12f, backgroundPaint)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(x, y, x + size, y + size, 12f, 12f, borderPaint)

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size * 0.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(icon, x + size/2, y + size * 0.4f, iconPaint)

        val countPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.YELLOW
            textSize = size * 0.25f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        canvas.drawText(count.toString(), x + size/2, y + size * 0.8f, countPaint)
    }

    private fun drawEffectCountdowns(canvas: Canvas) {
        val timerX = screenW - 150f
        val timerStartY = 120f
        var currentTimerY = timerStartY

        if (player.hasJumpBoost()) {
            val remainingTime = player.getJumpBoostRemainingTime()
            drawCountdownTimer(canvas, timerX, currentTimerY, "🦘", remainingTime)
            currentTimerY += 40f
        }

        if (player.hasSpeedBoost()) {
            val remainingTime = player.getSpeedBoostRemainingTime()
            drawCountdownTimer(canvas, timerX, currentTimerY, "💨", remainingTime)
            currentTimerY += 40f
        }

        if (gameStateManager.hasShield()) {
            val remainingTime = gameStateManager.getShieldRemainingTime()
            drawCountdownTimer(canvas, timerX, currentTimerY, "🛡️", remainingTime)
            currentTimerY += 40f
        }

        if (player.hasCoinMagnet()) {
            val remainingTime = player.getMagnetRemainingTime()
            drawCountdownTimer(canvas, timerX, currentTimerY, "🧲", remainingTime)
        }
    }

    private fun drawCountdownTimer(canvas: Canvas, x: Float, y: Float, icon: String, remainingTimeMs: Long) {
        val seconds = (remainingTimeMs / 1000f).toInt() + 1

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 0, 0, 0)
        }
        canvas.drawRoundRect(x, y, x + 120f, y + 30f, 8f, 8f, bgPaint)

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(icon, x + 15f, y + 20f, iconPaint)

        val timerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 18f
            textAlign = Paint.Align.LEFT
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        canvas.drawText("${seconds}s", x + 35f, y + 20f, timerPaint)
    }

    private fun handleHazardCollision() {
        handlePlayerDeath()
    }

    private fun respawnAtCheckpoint() {
        val (checkpointX, checkpointY, mapId) = gameStateManager.getCheckpoint()

        android.util.Log.d("GameView", "=== RESPAWN DEBUG ===")
        android.util.Log.d("GameView", "Checkpoint: x=$checkpointX, y=$checkpointY, mapId=$mapId")
        android.util.Log.d("GameView", "Current map state: isOnTileMap2=$isOnTileMap2, isOnTileMap3=$isOnTileMap3")

        if (mapId == 0) {
            isOnTileMap2 = false
            isOnTileMap3 = false
            currentTileMap = tileMap
            player.x = checkpointX
            player.y = checkpointY
            android.util.Log.d("GameView", "Respawning in MAP 1 at x=$checkpointX, y=$checkpointY")
        } else if (mapId == 1) {
            // Respawn in tilemap 2
            isOnTileMap2 = true
            isOnTileMap3 = false
            currentTileMap = tileMap2
            player.x = checkpointX
            player.y = checkpointY
            android.util.Log.d("GameView", "Respawning in MAP 2 at x=$checkpointX, y=$checkpointY")
        } else {
            // Respawn in tilemap 3 (boss fight)
            isOnTileMap3 = true
            isOnTileMap2 = false
            currentTileMap = tileMap3
            player.x = checkpointX
            player.y = checkpointY
            android.util.Log.d("GameView", "Respawning in MAP 3 at x=$checkpointX, y=$checkpointY")
        }

        // Reset player velocity to prevent falling/moving after respawn
        player.vx = 0f
        player.vy = 0f

        // Reset camera to follow player at new position
        cameraX = player.x - (screenW / worldScale) / 2f
        cameraY = player.y - (screenH / worldScale) / 2f

        // Clamp camera bounds
        val currentWorldWidth = if (isOnTileMap3) tileMap3.worldWidth else if (isOnTileMap2) tileMap2.worldWidth else tileMap.worldWidth
        val currentWorldHeight = if (isOnTileMap3) tileMap3.worldHeight else if (isOnTileMap2) tileMap2.worldHeight else tileMap.worldHeight
        val viewportWorldW = screenW / worldScale
        val viewportWorldH = screenH / worldScale

        cameraX = cameraX.coerceIn(0f, max(0f, currentWorldWidth - viewportWorldW))
        cameraY = cameraY.coerceIn(0f, max(0f, currentWorldHeight - viewportWorldH))

        android.util.Log.d("GameView", "Camera reset to: cameraX=$cameraX, cameraY=$cameraY")
        android.util.Log.d("GameView", "===================")
    }

    private fun checkForCheckpoints(map: TileMapInterface, mapId: Int) {
        val checkpointPositions = if (mapId == 0) {
            listOf(1000f, 2000f, 3000f, 4000f, 5000f)
        } else if (mapId == 1) {
            // For map 2, include all checkpoint positions including final one at 7700f
            listOf(950f, 2150f, 3480f, 4800f, 6250f, 7700f)
        } else {
            // For map 3, only checkpoint at start - no victory checkpoint, win only by defeating boss
            listOf()
        }

        for (checkpointX in checkpointPositions) {
            if (player.x >= checkpointX - 50f && player.x <= checkpointX + 50f) {
                val groundY = map.getGroundTopY() - player.height
                val currentCheckpoint = gameStateManager.getCheckpoint()

                // Only play sound if this is a new checkpoint
                if (currentCheckpoint.first != checkpointX || currentCheckpoint.third != mapId) {
                    soundManager.playCheckpointSound()
                }

                gameStateManager.setCheckpoint(checkpointX, groundY, mapId)

                // REMOVED: No victory condition here - only win by defeating boss in map 3
                break
            }
        }
    }

    private fun handleVictory() {
        if (!isGameWon) {
            isGameWon = true
            soundManager.playVictorySound()

            // Save the current session score
            android.util.Log.d("GameView", "Victory - Saving score: coins=$coinsCollectedThisSession, monsters=$monstersKilled")
            val scoreManager = ScoreManager(context)
            scoreManager.saveScore(coinsCollectedThisSession, monstersKilled)
        }
    }

    private fun drawGameOverScreen(canvas: Canvas) {
        // Semi-transparent overlay
        val overlayPaint = Paint().apply {
            color = Color.argb(200, 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, screenW, screenH, overlayPaint)

        // Draw game over image centered on screen
        gameOverImage?.let { image ->
            val imageW = screenW * 0.5f  // Slightly smaller for better proportions
            val imageH = imageW * (image.height.toFloat() / image.width.toFloat())
            val imageX = (screenW - imageW) / 2f  // Centered horizontally
            val imageY = (screenH - imageH) / 2f - screenH * 0.1f  // Centered vertically with slight upward offset

            val imageRect = RectF(imageX, imageY, imageX + imageW, imageY + imageH)
            canvas.drawBitmap(image, null, imageRect, paint)

            // "Play Again?" text positioned below the image
            val questionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 45f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(3f, 2f, 2f, Color.BLACK)
            }
            canvas.drawText("Play Again?", screenW / 2f, imageY + imageH - 250f, questionPaint)

        } ?: run {
            // Fallback text if image doesn't exist - centered
            val gameOverPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.RED
                textSize = 80f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(5f, 3f, 3f, Color.BLACK)
            }
            canvas.drawText("GAME OVER", screenW / 2f, screenH / 2f - 30f, gameOverPaint)

            // "Play Again?" text
            val questionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 50f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(3f, 2f, 2f, Color.BLACK)
            }
            canvas.drawText("Play Again?", screenW / 2f, screenH / 2f - 30f, questionPaint)
        }

        // YES button
        val yesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 180, 0) // Green
        }
        canvas.drawRoundRect(yesButton, 15f, 15f, yesPaint)

        // YES text
        val yesTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        val yesTextY = yesButton.centerY() - (yesTextPaint.descent() + yesTextPaint.ascent()) / 2f
        canvas.drawText("YES", yesButton.centerX(), yesTextY, yesTextPaint)

        // NO button
        val noPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 0, 0) // Red
        }
        canvas.drawRoundRect(noButton, 15f, 15f, noPaint)

        // NO text
        val noTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        val noTextY = noButton.centerY() - (noTextPaint.descent() + noTextPaint.ascent()) / 2f
        canvas.drawText("NO", noButton.centerX(), noTextY, noTextPaint)

        // SETTINGS button
        val settingsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 0, 180) // Blue
        }
        canvas.drawRoundRect(settingsButton, 15f, 15f, settingsPaint)

        // SETTINGS text
        val settingsTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        val settingsTextY = settingsButton.centerY() - (settingsTextPaint.descent() + settingsTextPaint.ascent()) / 2f
        canvas.drawText("SETTINGS", settingsButton.centerX(), settingsTextY, settingsTextPaint)

        // HIGH SCORES button (for win screen)
        val highScoresPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 255, 0) // Bright Green
        }
        canvas.drawRoundRect(highScoresButton, 15f, 15f, highScoresPaint)

        // HIGH SCORES text
        val highScoresTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        val highScoresTextY = highScoresButton.centerY() - (highScoresTextPaint.descent() + highScoresTextPaint.ascent()) / 2f
        canvas.drawText("HIGH SCORES", highScoresButton.centerX(), highScoresTextY, highScoresTextPaint)
    }

    private fun drawWinScreen(canvas: Canvas) {
        // Semi-transparent overlay
        val overlayPaint = Paint().apply {
            color = Color.argb(200, 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, screenW, screenH, overlayPaint)

        // Draw congratulations image centered on screen
        congratulationsImage?.let { image ->
            val imageW = screenW * 0.5f  // Slightly smaller for better proportions
            val imageH = imageW * (image.height.toFloat() / image.width.toFloat())
            val imageX = (screenW - imageW) / 2f  // Centered horizontally
            val imageY = (screenH - imageH) / 2f - screenH * 0.1f  // Centered vertically with slight upward offset

            val imageRect = RectF(imageX, imageY, imageX + imageW, imageY + imageH)
            canvas.drawBitmap(image, null, imageRect, paint)

            // "Victory!" text positioned above the image
            val victoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.YELLOW
                textSize = 60f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(4f, 2f, 2f, Color.BLACK)
            }
            canvas.drawText("VICTORY!", screenW / 2f, imageY - 50f, victoryPaint)

            // "Play Again?" text positioned below the image
            val questionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 45f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(3f, 2f, 2f, Color.BLACK)
            }
            canvas.drawText("Play Again?", screenW / 2f, imageY + imageH + 10f, questionPaint)

        } ?: run {
            // Fallback text if image doesn't exist - centered
            val victoryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.GREEN
                textSize = 80f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(5f, 3f, 3f, Color.BLACK)
            }
            canvas.drawText("YOU WIN!", screenW / 2f, screenH / 2f - 30f, victoryPaint)

            // "Play Again?" text
            val questionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 50f
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                setShadowLayer(3f, 2f, 2f, Color.BLACK)
            }
            canvas.drawText("Play Again?", screenW / 2f, screenH / 2f + 40f, questionPaint)
        }

        // YES button (position 1)
        val yesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 180, 0) // Green
        }
        canvas.drawRoundRect(yesButton, 15f, 15f, yesPaint)

        // YES text
        val yesTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        val yesTextY = yesButton.centerY() - (yesTextPaint.descent() + yesTextPaint.ascent()) / 2f
        canvas.drawText("YES", yesButton.centerX(), yesTextY, yesTextPaint)

        // NO button (position 2)
        val noPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(180, 0, 0) // Red
        }
        canvas.drawRoundRect(noButton, 15f, 15f, noPaint)

        // NO text
        val noTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 40f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        val noTextY = noButton.centerY() - (noTextPaint.descent() + noTextPaint.ascent()) / 2f
        canvas.drawText("NO", noButton.centerX(), noTextY, noTextPaint)

        // HIGH SCORES button (position 3) - ONLY show this button, not settings button
        val highScoresPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 255, 0) // Bright Green
        }
        canvas.drawRoundRect(highScoresButton, 15f, 15f, highScoresPaint)

        // HIGH SCORES text
        val highScoresTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f // Slightly smaller font to fit "HIGH SCORES"
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.BLACK)
        }
        val highScoresTextY = highScoresButton.centerY() - (highScoresTextPaint.descent() + highScoresTextPaint.ascent()) / 2f
        canvas.drawText("HIGH SCORES", highScoresButton.centerX(), highScoresTextY, highScoresTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                // First check settings overlay touch events (highest priority)
                if (settingsOverlay.onTouchDown(x, y)) {
                    // Handle specific settings overlay actions
                    if (settingsOverlay.wasPreviousClicked(x, y)) {
                        goToMenu()
                    } else if (settingsOverlay.wasRestartClicked(x, y)) {
                        startNewGame()
                        settingsOverlay.hide()
                    }
                    return true
                }

                if (gameStateManager.isGameOver() || isGameWon) {
                    // Handle game over/win screen touches
                    if (yesButton.contains(x, y)) {
                        // Reset game state when starting new game
                        isGameWon = false
                        monstersKilled = 0
                        coinsCollectedThisSession = 0
                        startNewGame()
                        return true
                    } else if (noButton.contains(x, y)) {
                        goToMenu()
                        return true
                    } else if (isGameWon && highScoresButton.contains(x, y)) {
                        // Handle high scores button (only shown on win screen) - CHECK THIS FIRST!
                        handleHighScores()
                        return true
                    } else if (!isGameWon && settingsButton.contains(x, y)) {
                        // Show settings overlay (only on game over screen, not win screen)
                        settingsOverlay.show()
                        return true
                    }
                } else {
                    // Handle game control touches (only if settings is not visible)
                    if (!settingsOverlay.isVisible()) {
                        when {
                            btnLeft.contains(x, y) -> activePointers[pointerId] = "left"
                            btnRight.contains(x, y) -> activePointers[pointerId] = "right"
                            btnJump.contains(x, y) -> {
                                activePointers[pointerId] = "jump"
                                player.jump()
                            }
                            btnAttack.contains(x, y) -> {
                                activePointers[pointerId] = "attack"
                                // Attack boss if in map 3
                                if (isOnTileMap3) {
                                    val hit = tileMap3.playerAttackBoss(player)
                                    if (hit) {
                                        // Play attack sound or visual feedback
                                        android.util.Log.d("GameView", "Player hit boss!")
                                    }
                                }
                            }
                            else -> {
                                // Check inventory item touches for using potions
                                val itemSize = 80f
                                val spacing = 15f
                                val startX = screenW - itemSize - 30f
                                val startY = 120f
                                var currentY = startY

                                // Check health potion
                                if (shopManager.getHealthPotions() > 0) {
                                    val itemRect = RectF(startX, currentY, startX + itemSize, currentY + itemSize)
                                    if (itemRect.contains(x, y)) {
                                        useHealthPotion()
                                        return true
                                    }
                                    currentY += itemSize + spacing
                                }

                                // Check jump potion
                                if (shopManager.getJumpPotions() > 0) {
                                    val itemRect = RectF(startX, currentY, startX + itemSize, currentY + itemSize)
                                    if (itemRect.contains(x, y)) {
                                        useJumpPotion()
                                        return true
                                    }
                                    currentY += itemSize + spacing
                                }

                                // Check speed potion
                                if (shopManager.getSpeedPotions() > 0) {
                                    val itemRect = RectF(startX, currentY, startX + itemSize, currentY + itemSize)
                                    if (itemRect.contains(x, y)) {
                                        useSpeedPotion()
                                        return true
                                    }
                                    currentY += itemSize + spacing
                                }

                                // Check shield potion
                                if (shopManager.getShieldPotions() > 0) {
                                    val itemRect = RectF(startX, currentY, startX + itemSize, currentY + itemSize)
                                    if (itemRect.contains(x, y)) {
                                        useShieldPotion()
                                        return true
                                    }
                                    currentY += itemSize + spacing
                                }

                                if (shopManager.getMagnetPotions() > 0) {
                                    val itemRect = RectF(startX, currentY, startX + itemSize, currentY + itemSize)
                                    if (itemRect.contains(x, y)) {
                                        useMagnetPotion()
                                        return true
                                    }
                                }
                            }
                        }
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.actionIndex
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                // Handle settings overlay drag events
                if (settingsOverlay.onTouchMove(x, y)) {
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)

                // Handle settings overlay touch up
                settingsOverlay.onTouchUp()

                // Remove pointer from active list
                activePointers.remove(pointerId)
            }
        }
        return true
    }

    // Potion usage methods
    private fun useHealthPotion() {
        android.util.Log.d("GameView", "useHealthPotion: Called")
        if (shopManager.useHealthPotion()) {
            android.util.Log.d("GameView", "useHealthPotion: Health potion used successfully")
            val currentLives = gameStateManager.getLives()
            android.util.Log.d("GameView", "useHealthPotion: Current lives=$currentLives, MAX_LIVES=${GameStateManager.MAX_LIVES}")
            if (currentLives < GameStateManager.MAX_LIVES) {
                val newLives = currentLives + 1
                android.util.Log.d("GameView", "useHealthPotion: Setting lives to $newLives")
                gameStateManager.setLives(newLives)
            } else {
                android.util.Log.d("GameView", "useHealthPotion: Lives already at max, not setting")
            }
        } else {
            android.util.Log.d("GameView", "useHealthPotion: No health potions available or failed to use")
        }
    }

    private fun useJumpPotion() {
        if (shopManager.useJumpPotion()) {
            player.applyJumpBoost(ShopManager.JUMP_EFFECT_DURATION)
        }
    }

    private fun useSpeedPotion() {
        if (shopManager.useSpeedPotion()) {
            player.applySpeedBoost(ShopManager.SPEED_EFFECT_DURATION)
        }
    }

    private fun useShieldPotion() {
        if (shopManager.useShieldPotion()) {
            gameStateManager.applyShield(ShopManager.SHIELD_EFFECT_DURATION)
        }
    }

    private fun useMagnetPotion() {
        if (shopManager.useMagnetPotion()) {
            player.applyCoinMagnet(ShopManager.MAGNET_EFFECT_DURATION)
        }
    }

    // Activity lifecycle methods
    fun resume() {
        if (!thread.running) {
            thread = GameThread(holder, this)
            thread.running = true
            thread.start()
        }
    }

    fun pause() {
        thread.running = false
        var retry = true
        while (retry) {
            try {
                thread.join()
                retry = false
            } catch (_: InterruptedException) {
                // Thread interrupted during join
            }
        }
    }

    private fun handleHighScores() {
        // Navigate to high scores screen (implement this activity and its layout)
        val intent = Intent(context, HighScoresActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }

    private fun drawControlButtonVisible(canvas: Canvas, rect: RectF, text: String, pressed: Boolean) {
        // Button background
        btnPaint.color = if (pressed) Color.argb(180, 100, 100, 100) else Color.argb(120, 80, 80, 80)
        canvas.drawRoundRect(rect, 20f, 20f, btnPaint)

        // Button border
        btnPaint.style = Paint.Style.STROKE
        btnPaint.strokeWidth = 3f
        btnPaint.color = Color.WHITE
        canvas.drawRoundRect(rect, 20f, 20f, btnPaint)
        btnPaint.style = Paint.Style.FILL

        // Button text
        btnTextPaint.color = if (pressed) Color.YELLOW else Color.WHITE
        canvas.drawText(text, rect.centerX(), rect.centerY() + 10f, btnTextPaint)
    }
}
