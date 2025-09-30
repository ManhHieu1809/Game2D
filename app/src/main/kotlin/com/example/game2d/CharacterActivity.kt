package com.example.game2d

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.sin

class CharacterActivity : Activity() {
    private lateinit var view: CharacterShopView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = CharacterShopView(this)
        setContentView(view)
    }

    override fun onResume() {
        super.onResume()
        view.resume()
    }

    override fun onPause() {
        super.onPause()
        view.pause()
    }
}

class CharacterShopView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var thread: CharacterShopThread

    // Shop manager for handling purchases and unlocks
    private val shopManager = ShopManager(context)

    // Current tab: 0 = Characters, 1 = Shop Items
    private var currentTab = 0

    // Character data
    data class CharacterData(
        val name: String,
        val spriteName: String,
        val description: String,
        val price: Int
    )

    // Shop item data
    data class ShopItemData(
        val name: String,
        val icon: String,
        val description: String,
        val price: Int,
        val buyAction: () -> Boolean
    )

    private val characters = listOf(
        CharacterData("Ninja Frog", "idle", "Agile ninja with jumping abilities", 0),
        CharacterData("Pink Man", "pink_idle", "Balanced character with good speed", 50),
        CharacterData("Virtual Guy", "virtual_idle", "Tech warrior with special moves", 100),
        CharacterData("Mask Dude", "mask_idle", "Mysterious fighter with stealth", 150)
    )

    private val shopItems = listOf(
        ShopItemData("Health Potion", "❤️", "Restore 1 life instantly", ShopManager.HEALTH_POTION_PRICE) {
            shopManager.buyHealthPotion()
        },
        ShopItemData("Jump Potion", "🦘", "Jump 50% higher for 30s", ShopManager.JUMP_POTION_PRICE) {
            shopManager.buyJumpPotion()
        },
        ShopItemData("Speed Potion", "💨", "Run 40% faster for 25s", ShopManager.SPEED_POTION_PRICE) {
            shopManager.buySpeedPotion()
        },
        ShopItemData("Shield Potion", "🛡️", "Immune to damage for 20s", ShopManager.SHIELD_POTION_PRICE) {
            shopManager.buyShieldPotion()
        },
        ShopItemData("Magnet Potion", "🧲", "Auto-collect coins for 15s", ShopManager.MAGNET_POTION_PRICE) {
            shopManager.buyMagnetPotion()
        }
    )

    private var selectedCharacterIndex = 0
    private var selectedShopIndex = 0
    private var animTimer = 0L
    private var animFrame = 0

    // UI elements
    private val tabButtons = ArrayList<RectF>() // Tab buttons
    private val itemCards = ArrayList<RectF>() // Character/shop item cards
    private val backButton = RectF()
    private val actionButton = RectF() // Select/Buy button

    // Character preview bitmaps
    private val characterPreviews = HashMap<String, Bitmap?>()

    private val prefs: SharedPreferences

    init {
        holder.addCallback(this)
        isFocusable = true

        prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        selectedCharacterIndex = prefs.getInt("selected_character", 0)

        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)

        // Load character preview sprites
        loadCharacterPreviews()

        thread = CharacterShopThread(holder, this)
    }

    private fun loadCharacterPreviews() {
        for (char in characters) {
            characterPreviews[char.spriteName] = loadBitmap(char.spriteName + "_32x32")
        }
    }

    private fun loadBitmap(name: String): Bitmap? {
        @Suppress("DiscouragedApi")
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (id == 0) return null
        return try {
            BitmapFactory.decodeResource(context.resources, id)
        } catch (_: Exception) {
            null
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        setupUI()
        thread = CharacterShopThread(holder, this)
        thread.running = true
        thread.start()
    }

    private fun setupUI() {
        val w = width.toFloat()
        val h = height.toFloat()

        // Tab buttons
        tabButtons.clear()
        val tabW = w * 0.4f
        val tabH = h * 0.08f
        val tabY = h * 0.1f

        tabButtons.add(RectF(w * 0.1f, tabY, w * 0.1f + tabW, tabY + tabH)) // Characters tab
        tabButtons.add(RectF(w * 0.5f, tabY, w * 0.5f + tabW, tabY + tabH)) // Shop tab

        // Item cards
        itemCards.clear()
        val cardW = w * 0.85f
        val cardH = h * 0.12f
        val startY = h * 0.22f
        val gap = h * 0.02f

        val itemCount = if (currentTab == 0) characters.size else shopItems.size
        for (i in 0 until itemCount) {
            val y = startY + i * (cardH + gap)
            itemCards.add(RectF((w - cardW) / 2f, y, (w + cardW) / 2f, y + cardH))
        }

        // Buttons
        val btnW = w * 0.25f
        val btnH = h * 0.08f
        val btnY = h * 0.88f

        backButton.set(w * 0.1f, btnY, w * 0.1f + btnW, btnY + btnH)
        actionButton.set(w * 0.65f, btnY, w * 0.65f + btnW, btnY + btnH)

        textPaint.textSize = h * 0.04f
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        setupUI()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        thread.running = false
        var retry = true
        while (retry) {
            try {
                thread.join()
                retry = false
            } catch (_: InterruptedException) {
                // Thread interrupted, continue trying
            }
        }
    }

    fun update(deltaMs: Long) {
        animTimer += deltaMs
        if (animTimer >= 150L) {
            animFrame = (animFrame + 1) % 4
            animTimer = 0
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // Background gradient
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, 0f, h,
                Color.rgb(25, 30, 40),
                Color.rgb(15, 20, 30),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Title
        textPaint.textSize = h * 0.055f
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("CHARACTER & SHOP", w / 2f, h * 0.06f, textPaint)

        // Coins display
        textPaint.textSize = h * 0.03f
        textPaint.color = Color.YELLOW
        val coins = shopManager.getCoins()
        if (currentTab == 1) {
            // Show inventory in shop tab
            val healthPots = shopManager.getHealthPotions()
            val jumpPots = shopManager.getJumpPotions()
            val speedPots = shopManager.getSpeedPotions()
            val shieldPots = shopManager.getShieldPotions()
            val magnetPots = shopManager.getMagnetPotions()
            canvas.drawText("💰 $coins | Inventory: ❤️×$healthPots 🦘×$jumpPots 💨×$speedPots 🛡️×$shieldPots 🧲×$magnetPots", w / 2f, h * 0.95f, textPaint)
        } else {
            canvas.drawText("💰 $coins Coins", w / 2f, h * 0.95f, textPaint)
        }

        // Draw tabs
        drawTabs(canvas, w, h)

        // Draw content based on current tab
        if (currentTab == 0) {
            drawCharacters(canvas, w, h)
        } else {
            drawShopItems(canvas, w, h)
        }

        // Draw buttons
        drawButtons(canvas, w, h)
    }

    private fun drawTabs(canvas: Canvas, w: Float, h: Float) {
        textPaint.textSize = h * 0.035f
        textPaint.textAlign = Paint.Align.CENTER

        for (i in tabButtons.indices) {
            val tab = tabButtons[i]
            val isActive = i == currentTab

            val tabPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    tab.left, tab.top, tab.right, tab.bottom,
                    if (isActive) Color.rgb(0, 120, 200) else Color.rgb(60, 60, 60),
                    if (isActive) Color.rgb(0, 100, 180) else Color.rgb(40, 40, 40),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(tab, 12f, 12f, tabPaint)

            textPaint.color = if (isActive) Color.WHITE else Color.LTGRAY
            val tabText = if (i == 0) "👤 CHARACTERS" else "🛒 SHOP"
            canvas.drawText(tabText, tab.centerX(), tab.centerY() + 6f, textPaint)
        }
    }

    private fun drawCharacters(canvas: Canvas, w: Float, h: Float) {
        for (i in characters.indices) {
            val card = itemCards[i]
            val char = characters[i]
            val isSelected = i == selectedCharacterIndex
            val isUnlocked = shopManager.isCharacterUnlocked(i)

            // Card background
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            if (!isUnlocked) {
                cardPaint.shader = LinearGradient(
                    card.left, card.top, card.right, card.bottom,
                    Color.rgb(30, 30, 30), Color.rgb(20, 20, 20),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(card, 15f, 15f, cardPaint)
                cardPaint.shader = null
                cardPaint.color = Color.argb(120, 0, 0, 0)
                canvas.drawRoundRect(card, 15f, 15f, cardPaint)
            } else {
                cardPaint.shader = LinearGradient(
                    card.left, card.top, card.right, card.bottom,
                    if (isSelected) Color.rgb(0, 120, 80) else Color.rgb(50, 55, 65),
                    if (isSelected) Color.rgb(0, 100, 60) else Color.rgb(35, 40, 50),
                    Shader.TileMode.CLAMP
                )
                canvas.drawRoundRect(card, 15f, 15f, cardPaint)
            }

            // Character sprite
            val preview = characterPreviews[char.spriteName]
            if (preview != null) {
                val spriteSize = card.height() * 0.8f
                val spriteX = card.left + spriteSize * 0.1f
                val spriteY = card.centerY() - spriteSize / 2f

                val bounce = if (isUnlocked) sin((System.currentTimeMillis() + i * 500L) * 0.003f) * 3f else 0f
                val spriteDest = RectF(spriteX, spriteY + bounce, spriteX + spriteSize, spriteY + spriteSize + bounce)

                val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    isFilterBitmap = true  // Enable bitmap filtering for smooth scaling
                    isDither = false       // Disable dithering to prevent patterns
                }

                if (!isUnlocked) {
                    val matrix = ColorMatrix()
                    matrix.setSaturation(0f)
                    spritePaint.colorFilter = ColorMatrixColorFilter(matrix)
                    spritePaint.alpha = 180
                }
                val srcRect = if (preview.width >= preview.height && preview.width % preview.height == 0) {
                    val frameW = preview.height
                    Rect(0, 0, frameW, preview.height)
                } else {
                    Rect(0, 0, preview.width, preview.height)
                }
                canvas.drawBitmap(preview, srcRect, spriteDest, spritePaint)

                if (!isUnlocked) {
                    val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.RED
                        textSize = h * 0.04f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("🔒", spriteDest.centerX(), spriteDest.centerY() + lockPaint.textSize/3, lockPaint)
                }
            }

            // Character info
            val textX = card.left + card.height() * 0.9f
            textPaint.textAlign = Paint.Align.LEFT

            textPaint.textSize = h * 0.03f
            textPaint.color = if (isUnlocked) (if (isSelected) Color.WHITE else Color.LTGRAY) else Color.GRAY
            canvas.drawText(char.name, textX, card.centerY() - 15f, textPaint)

            textPaint.textSize = h * 0.022f
            textPaint.color = if (isUnlocked) Color.LTGRAY else Color.GRAY
            canvas.drawText(char.description, textX, card.centerY() + 5f, textPaint)

            if (!isUnlocked && char.price > 0) {
                textPaint.textSize = h * 0.025f
                textPaint.color = Color.YELLOW
                canvas.drawText("💰 ${char.price} coins", textX, card.centerY() + 25f, textPaint)
            } else if (isUnlocked) {
                textPaint.color = Color.GREEN
                canvas.drawText("✓ UNLOCKED", textX, card.centerY() + 25f, textPaint)
            }

            // Selection indicator
            if (isSelected) {
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.YELLOW
                    strokeWidth = 4f
                    style = Paint.Style.STROKE
                }
                canvas.drawRoundRect(card, 15f, 15f, glowPaint)
            }
        }
    }

    private fun drawShopItems(canvas: Canvas, w: Float, h: Float) {
        for (i in shopItems.indices) {
            val card = itemCards[i]
            val item = shopItems[i]
            val isSelected = i == selectedShopIndex
            val canAfford = shopManager.getCoins() >= item.price

            // Card background
            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    card.left, card.top, card.right, card.bottom,
                    if (isSelected) Color.rgb(120, 80, 0) else Color.rgb(50, 55, 65),
                    if (isSelected) Color.rgb(100, 60, 0) else Color.rgb(35, 40, 50),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(card, 15f, 15f, cardPaint)

            // Item icon
            val iconSize = card.height() * 0.6f
            val iconX = card.left + iconSize * 0.2f
            val iconY = card.centerY() - iconSize / 2f

            val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = iconSize
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(item.icon, iconX + iconSize/2, iconY + iconSize*0.7f, iconPaint)

            // Item info
            val textX = card.left + card.height() * 0.9f
            textPaint.textAlign = Paint.Align.LEFT

            textPaint.textSize = h * 0.03f
            textPaint.color = if (isSelected) Color.WHITE else Color.LTGRAY
            canvas.drawText(item.name, textX, card.centerY() - 15f, textPaint)

            textPaint.textSize = h * 0.022f
            textPaint.color = Color.LTGRAY
            canvas.drawText(item.description, textX, card.centerY() + 5f, textPaint)

            textPaint.textSize = h * 0.025f
            textPaint.color = if (canAfford) Color.YELLOW else Color.RED
            canvas.drawText("💰 ${item.price} coins", textX, card.centerY() + 25f, textPaint)

            // Selection indicator
            if (isSelected) {
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(255, 165, 0) // Orange color
                    strokeWidth = 4f
                    style = Paint.Style.STROKE
                }
                canvas.drawRoundRect(card, 15f, 15f, glowPaint)
            }
        }
    }

    private fun drawButtons(canvas: Canvas, w: Float, h: Float) {
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = h * 0.032f

        // Back button
        val backPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                backButton.left, backButton.top, backButton.right, backButton.bottom,
                Color.rgb(100, 100, 100), Color.rgb(60, 60, 60),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(backButton, 12f, 12f, backPaint)
        textPaint.color = Color.WHITE
        canvas.drawText("🔙 BACK", backButton.centerX(), backButton.centerY() + 6f, textPaint)

        // Action button (Select/Buy)
        if (currentTab == 0) {
            // Character tab - Select button
            val isUnlocked = shopManager.isCharacterUnlocked(selectedCharacterIndex)
            if (isUnlocked) {
                val selectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        actionButton.left, actionButton.top, actionButton.right, actionButton.bottom,
                        Color.rgb(0, 200, 120), Color.rgb(0, 160, 100),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(actionButton, 12f, 12f, selectPaint)
                textPaint.color = Color.WHITE
                canvas.drawText("✓ SELECT", actionButton.centerX(), actionButton.centerY() + 6f, textPaint)
            } else {
                val char = characters[selectedCharacterIndex]
                val canAfford = shopManager.getCoins() >= char.price
                val buyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = LinearGradient(
                        actionButton.left, actionButton.top, actionButton.right, actionButton.bottom,
                        if (canAfford) Color.rgb(255, 180, 0) else Color.rgb(120, 120, 120),
                        if (canAfford) Color.rgb(220, 150, 0) else Color.rgb(80, 80, 80),
                        Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRoundRect(actionButton, 12f, 12f, buyPaint)
                textPaint.color = if (canAfford) Color.BLACK else Color.GRAY
                canvas.drawText("💰 BUY", actionButton.centerX(), actionButton.centerY() + 6f, textPaint)
            }
        } else {
            // Shop tab - buy item
            val item = shopItems[selectedShopIndex]
            val canAfford = shopManager.getCoins() >= item.price
            val buyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    actionButton.left, actionButton.top, actionButton.right, actionButton.bottom,
                    if (canAfford) Color.rgb(255, 180, 0) else Color.rgb(120, 120, 120),
                    if (canAfford) Color.rgb(220, 150, 0) else Color.rgb(80, 80, 80),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(actionButton, 12f, 12f, buyPaint)
            textPaint.color = if (canAfford) Color.BLACK else Color.GRAY
            canvas.drawText("💰 BUY", actionButton.centerX(), actionButton.centerY() + 6f, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check tab buttons
                for (i in tabButtons.indices) {
                    if (tabButtons[i].contains(x, y)) {
                        currentTab = i
                        setupUI() // Refresh UI for new tab
                        performClick()
                        return true
                    }
                }

                // Check item cards
                for (i in itemCards.indices) {
                    if (itemCards[i].contains(x, y)) {
                        if (currentTab == 0) {
                            selectedCharacterIndex = i
                        } else {
                            selectedShopIndex = i
                        }
                        performClick()
                        return true
                    }
                }

                // Check back button
                if (backButton.contains(x, y)) {
                    (context as Activity).finish()
                    performClick()
                    return true
                }

                // Check action button
                if (actionButton.contains(x, y)) {
                    if (currentTab == 0) {
                        // Character tab
                        val isUnlocked = shopManager.isCharacterUnlocked(selectedCharacterIndex)
                        if (isUnlocked) {
                            // Select character
                            prefs.edit().putInt("selected_character", selectedCharacterIndex).apply()
                            (context as Activity).finish()
                        } else {
                            // Buy character
                            shopManager.unlockCharacter(selectedCharacterIndex)
                        }
                    } else {
                        // Shop tab - buy item
                        val item = shopItems[selectedShopIndex]
                        item.buyAction()
                    }
                    performClick()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun pause() {
        thread.running = false
        try {
            thread.join()
        } catch (_: InterruptedException) {
            // Thread interrupted
        }
    }

    fun resume() {
        if (!thread.running) {
            thread = CharacterShopThread(holder, this)
            thread.running = true
            thread.start()
        }
    }
}

class CharacterShopThread(
    private val surfaceHolder: SurfaceHolder,
    private val view: CharacterShopView
) : Thread() {
    @Volatile var running = false
    private val targetFps = 60
    private val targetTime = (1000L / targetFps)

    override fun run() {
        while (running) {
            val start = System.nanoTime()
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                synchronized(surfaceHolder) {
                    view.update(targetTime)
                    if (canvas != null) view.draw(canvas)
                }
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {
                        // Handle canvas exception
                    }
                }
            }
            val timeMillis = (System.nanoTime() - start) / 1_000_000
            val wait = targetTime - timeMillis
            if (wait > 0) {
                try {
                    sleep(wait)
                } catch (_: InterruptedException) {
                    // Thread interrupted during sleep
                }
            }
        }
    }
}
