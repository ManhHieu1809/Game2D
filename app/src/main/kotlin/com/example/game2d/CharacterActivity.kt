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
    private lateinit var view: CharacterSelectView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = CharacterSelectView(this)
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

class CharacterSelectView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var thread: CharacterThread

    // Character data - based on your sprites
    data class CharacterData(
        val name: String,
        val spriteName: String, // base name for idle_32x32, run_32x32, etc.
        val description: String
    )

    private val characters = listOf(
        CharacterData("Mask Dude", "idle", "Agile ninja with jumping abilities"),
        CharacterData("Ninja Frog", "pink_idle", "Balanced character with good speed"),
        CharacterData("Virtual Guy", "virtual_idle", "Tech warrior with special moves"),
        CharacterData("Pink Man", "mask_idle", "Mysterious fighter with stealth")
    )

    private var selectedIndex = 0
    private var animTimer = 0L
    private var animFrame = 0

    // UI elements
    private val characterCards = ArrayList<RectF>()
    private val backButton = RectF()
    private val selectButton = RectF()

    // Character preview bitmaps
    private val characterPreviews = HashMap<String, Bitmap?>()

    private val prefs: SharedPreferences

    init {
        holder.addCallback(this)
        isFocusable = true

        prefs = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
        selectedIndex = prefs.getInt("selected_character", 0)

        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)

        // Load character preview sprites
        loadCharacterPreviews()

        thread = CharacterThread(holder, this)
    }

    private fun loadCharacterPreviews() {
        for (char in characters) {
            characterPreviews[char.spriteName] = loadBitmap(char.spriteName + "_32x32")
        }
    }

    private fun loadBitmap(name: String): Bitmap? {
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        if (id == 0) return null
        return try {
            BitmapFactory.decodeResource(context.resources, id)
        } catch (e: Exception) {
            null
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        setupUI()
        thread = CharacterThread(holder, this)
        thread.running = true
        thread.start()
    }

    private fun setupUI() {
        val w = width.toFloat()
        val h = height.toFloat()

        // Character selection cards
        characterCards.clear()
        val cardW = w * 0.8f
        val cardH = h * 0.15f
        val startY = h * 0.25f
        val gap = h * 0.02f

        for (i in characters.indices) {
            val y = startY + i * (cardH + gap)
            characterCards.add(RectF((w - cardW) / 2f, y, (w + cardW) / 2f, y + cardH))
        }

        // Buttons
        val btnW = w * 0.3f
        val btnH = h * 0.08f
        val btnY = h * 0.85f

        backButton.set(w * 0.1f, btnY, w * 0.1f + btnW, btnY + btnH)
        selectButton.set(w * 0.6f, btnY, w * 0.6f + btnW, btnY + btnH)

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
            } catch (e: InterruptedException) {}
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
                Color.rgb(40, 44, 52),
                Color.rgb(20, 22, 26),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Title
        textPaint.textSize = h * 0.06f
        textPaint.color = Color.WHITE
        canvas.drawText("SELECT CHARACTER", w / 2f, h * 0.15f, textPaint)

        // Character cards
        textPaint.textSize = h * 0.035f
        for (i in characters.indices) {
            val card = characterCards[i]
            val char = characters[i]
            val isSelected = i == selectedIndex

            // Card background
            paint.color = if (isSelected) Color.rgb(0, 150, 100) else Color.rgb(60, 65, 75)
            canvas.drawRoundRect(card, 12f, 12f, paint)

            // Character sprite preview
            val preview = characterPreviews[char.spriteName]
            if (preview != null) {
                val spriteSize = card.height() * 0.6f
                val spriteX = card.left + card.height() * 0.2f
                val spriteY = card.centerY() - spriteSize / 2f

                // Animate the sprite
                val bounce = sin((System.currentTimeMillis() + i * 500L) * 0.003f) * 3f
                val spriteDest = RectF(spriteX, spriteY + bounce, spriteX + spriteSize, spriteY + spriteSize + bounce)

                // Extract frame from sprite sheet
                val frameW = if (preview.width >= preview.height && preview.width % preview.height == 0) {
                    preview.height
                } else preview.width
                val frameH = preview.height
                val frameCount = preview.width / frameW
                val currentFrame = if (frameCount > 1) animFrame % frameCount else 0

                val srcRect = Rect(currentFrame * frameW, 0, (currentFrame + 1) * frameW, frameH)
                canvas.drawBitmap(preview, srcRect, spriteDest, paint)
            }

            // Character info
            val textX = card.left + card.height() + 20f
            textPaint.color = if (isSelected) Color.WHITE else Color.LTGRAY
            textPaint.textAlign = Paint.Align.LEFT

            // Name
            textPaint.textSize = h * 0.04f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)
            canvas.drawText(char.name, textX, card.centerY() - 10f, textPaint)

            // Description
            textPaint.textSize = h * 0.025f
            textPaint.typeface = Typeface.DEFAULT
            canvas.drawText(char.description, textX, card.centerY() + 20f, textPaint)

            // Selection indicator
            if (isSelected) {
                paint.color = Color.YELLOW
                paint.strokeWidth = 4f
                paint.style = Paint.Style.STROKE
                canvas.drawRoundRect(card, 12f, 12f, paint)
                paint.style = Paint.Style.FILL
            }
        }

        // Buttons
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.textSize = h * 0.035f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)

        // Back button
        paint.color = Color.rgb(80, 80, 80)
        canvas.drawRoundRect(backButton, 8f, 8f, paint)
        textPaint.color = Color.WHITE
        canvas.drawText("BACK", backButton.centerX(), backButton.centerY() + 8f, textPaint)

        // Select button
        paint.color = Color.rgb(0, 180, 120)
        canvas.drawRoundRect(selectButton, 8f, 8f, paint)
        textPaint.color = Color.WHITE
        canvas.drawText("SELECT", selectButton.centerX(), selectButton.centerY() + 8f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check character cards
                for (i in characterCards.indices) {
                    if (characterCards[i].contains(x, y)) {
                        selectedIndex = i
                        return true
                    }
                }

                // Check buttons
                if (backButton.contains(x, y)) {
                    (context as Activity).finish()
                    return true
                }

                if (selectButton.contains(x, y)) {
                    // Save selection
                    prefs.edit().putInt("selected_character", selectedIndex).apply()
                    (context as Activity).finish()
                    return true
                }
            }
        }
        return true
    }

    fun pause() {
        thread.running = false
        try { thread.join() } catch (e: InterruptedException) {}
    }

    fun resume() {
        if (!thread.running) {
            thread = CharacterThread(holder, this)
            thread.running = true
            thread.start()
        }
    }
}

class CharacterThread(
    private val surfaceHolder: SurfaceHolder,
    private val view: CharacterSelectView
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
                    } catch (e: Exception) {}
                }
            }
            val timeMillis = (System.nanoTime() - start) / 1_000_000
            val wait = targetTime - timeMillis
            if (wait > 0) {
                try { sleep(wait) } catch (e: InterruptedException) {}
            }
        }
    }
}