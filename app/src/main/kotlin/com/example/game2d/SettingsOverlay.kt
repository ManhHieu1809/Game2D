package com.example.game2d

import android.content.Context
import android.graphics.*


class SettingsOverlay(private val context: Context, private val soundManager: SoundManager) {
    private var isVisible = false
    private var isPaused = false

    private var textLeft = 0f
    // UI elements
    private val overlayRect = RectF()
    private val settingsButton = RectF()
    private val musicVolumeBar = RectF()
    private val sfxVolumeBar = RectF()
    private val previousButton = RectF()
    private val playButton = RectF()
    private val restartButton = RectF()
    private val closeButton = RectF()

    // Bitmaps
    private var settingsBitmap: Bitmap? = null
    private var volumeBitmap: Bitmap? = null
    private var previousBitmap: Bitmap? = null
    private var playBitmap: Bitmap? = null
    private var restartBitmap: Bitmap? = null

    // Paint objects
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.LEFT
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }

    // Volume control
    private var isDraggingMusic = false
    private var isDraggingSfx = false

    init {
        loadBitmaps()
    }

    private fun loadBitmaps() {
        try {
            settingsBitmap = safeLoadDrawableByNames(context, listOf("setting", "settings"))
            volumeBitmap = safeLoadDrawableByNames(context, listOf("volume", "volum"))
            previousBitmap = safeLoadDrawableByNames(context, listOf("previous", "back"))
            playBitmap = safeLoadDrawableByNames(context, listOf("play", "resume"))
            restartBitmap = safeLoadDrawableByNames(context, listOf("restart", "refresh"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun safeLoadDrawableByNames(ctx: Context, names: List<String>): Bitmap? {
        for (n in names) {
            @Suppress("DiscouragedApi")
            val id = ctx.resources.getIdentifier(n, "drawable", ctx.packageName)
            if (id != 0) {
                try {
                    return BitmapFactory.decodeResource(ctx.resources, id)
                } catch (_: Exception) {
                    // Continue to next candidate
                }
            }
        }
        return null
    }

    // *** PHIÊN BẢN SỬA LỖI HOÀN CHỈNH ***
    fun setupLayout(screenWidth: Float, screenHeight: Float) {
        val buttonSize = 100f
        settingsButton.set(screenWidth - buttonSize - 20f, 20f, screenWidth - 20f, 20f + buttonSize)

        val overlayWidth = screenWidth * 0.85f
        val overlayHeight = screenHeight * 0.75f
        val overlayLeft = (screenWidth - overlayWidth) / 2f
        val overlayTop = (screenHeight - overlayHeight) / 2f
        overlayRect.set(overlayLeft, overlayTop, overlayLeft + overlayWidth, overlayTop + overlayHeight)

        closeButton.set(overlayRect.right - 60f, overlayRect.top + 10f,
            overlayRect.right - 10f, overlayRect.top + 60f)

        // 1. Tạo một Paint tạm để đo chiều rộng văn bản
        val measurePaint = Paint().apply {
            textSize = 30f // Phải giống với textSize dùng để vẽ nhãn
            typeface = Typeface.DEFAULT_BOLD
        }

        // 2. Đo chiều rộng của nhãn dài nhất
        val musicLabel = "Music Volume:"
        val sfxLabel = "SFX Volume:"
        val maxLabelWidth = maxOf(measurePaint.measureText(musicLabel), measurePaint.measureText(sfxLabel))

        // 3. Tính toán vị trí dựa trên phép đo
        val padding = 30f
        val spacing = 20f // Khoảng cách giữa chữ và thanh volume
        textLeft = overlayRect.left + padding // Lưu lại để dùng khi vẽ
        val barLeft = textLeft + maxLabelWidth + spacing

        // 4. Đặt vị trí cho các thanh volume
        val barHeight = 40f
        // Trừ đi padding và khoảng trống cho icon loa
        val barWidth = overlayRect.right - barLeft - padding - 50f

        musicVolumeBar.set(barLeft, overlayTop + 150f, barLeft + barWidth, overlayTop + 150f + barHeight)
        sfxVolumeBar.set(barLeft, overlayTop + 220f, barLeft + barWidth, overlayTop + 220f + barHeight)

        // Control buttons at bottom
        val btnSize = 80f
        val btnSpacing = 100f
        val totalBtnWidth = btnSize * 3 + btnSpacing * 2
        val btnStartX = overlayLeft + (overlayWidth - totalBtnWidth) / 2f
        val btnY = overlayRect.bottom - 120f

        previousButton.set(btnStartX, btnY, btnStartX + btnSize, btnY + btnSize)
        playButton.set(btnStartX + btnSize + btnSpacing, btnY,
            btnStartX + btnSize + btnSpacing + btnSize, btnY + btnSize)
        restartButton.set(btnStartX + (btnSize + btnSpacing) * 2, btnY,
            btnStartX + (btnSize + btnSpacing) * 2 + btnSize, btnY + btnSize)
    }

    fun draw(canvas: Canvas) {
        // Draw settings button
        if (settingsBitmap != null) {
            canvas.drawBitmap(settingsBitmap!!, null, settingsButton, paint)
        } else {
            drawFallbackButton(canvas, settingsButton, "⚙", Color.GRAY)
        }

        // Draw settings overlay if visible
        if (isVisible) {
            drawSettingsOverlay(canvas)
        }
    }

    // *** PHIÊN BẢN SỬA LỖI HOÀN CHỈNH ***
    private fun drawSettingsOverlay(canvas: Canvas) {
        // Semi-transparent background
        paint.color = Color.argb(180, 0, 0, 0)
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)

        // Settings panel background
        paint.color = Color.argb(240, 50, 50, 50)
        canvas.drawRoundRect(overlayRect, 20f, 20f, paint)

        // Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = Color.WHITE
        canvas.drawRoundRect(overlayRect, 20f, 20f, paint)
        paint.style = Paint.Style.FILL

        // Close button
        paint.color = Color.RED
        canvas.drawRoundRect(closeButton, 10f, 10f, paint)
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER // Đặt căn giữa
        canvas.drawText("✕", closeButton.centerX(), closeButton.centerY() + 10f, textPaint)

        // Title
        textPaint.textAlign = Paint.Align.CENTER // Đặt căn giữa
        textPaint.textSize = 50f
        textPaint.color = Color.WHITE
        canvas.drawText("SETTINGS", overlayRect.centerX(), overlayRect.top + 60f, textPaint)

        // Volume controls
        textPaint.textSize = 30f
        val textOffsetY = (textPaint.descent() + textPaint.ascent()) / 2

        // Music volume
        val musicTextY = musicVolumeBar.centerY() - textOffsetY
        textPaint.textAlign = Paint.Align.LEFT // *** SỬA LỖI: Luôn đặt căn trái trước khi vẽ nhãn
        canvas.drawText("Music Volume:", textLeft, musicTextY, textPaint)
        drawVolumeBar(canvas, musicVolumeBar, soundManager.musicVolume)

        // SFX volume
        val sfxTextY = sfxVolumeBar.centerY() - textOffsetY
        textPaint.textAlign = Paint.Align.LEFT // *** SỬA LỖI: Luôn đặt căn trái trước khi vẽ nhãn
        canvas.drawText("SFX Volume:", textLeft, sfxTextY, textPaint)
        drawVolumeBar(canvas, sfxVolumeBar, soundManager.sfxVolume)

        // Control buttons
        drawControlButton(canvas, previousButton, previousBitmap, "◀")
        drawControlButton(canvas, playButton, playBitmap, if (isPaused) "▶" else "⏸")
        drawControlButton(canvas, restartButton, restartBitmap, "↻")
    }

    private fun drawVolumeBar(canvas: Canvas, rect: RectF, volume: Float) {
        // Background
        paint.color = Color.GRAY
        canvas.drawRoundRect(rect, 15f, 15f, paint)

        // Fill
        paint.color = Color.GREEN
        val fillWidth = rect.width() * volume
        val fillRect = RectF(rect.left, rect.top, rect.left + fillWidth, rect.bottom)
        canvas.drawRoundRect(fillRect, 15f, 15f, paint)

        // Volume icon
        if (volumeBitmap != null) {
            val iconSize = rect.height()
            val iconRect = RectF(rect.right + 10f, rect.top,
                rect.right + 10f + iconSize, rect.bottom)
            canvas.drawBitmap(volumeBitmap!!, null, iconRect, paint)
        }

        // Volume percentage
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER // *** Đặt căn giữa cho %
        canvas.drawText("${(volume * 100).toInt()}%",
            rect.centerX(), rect.centerY() + 8f, textPaint)
    }

    private fun drawControlButton(canvas: Canvas, rect: RectF, bitmap: Bitmap?, fallbackText: String) {
        // Button background
        paint.color = Color.argb(200, 100, 100, 100)
        canvas.drawRoundRect(rect, 10f, 10f, paint)

        if (bitmap != null) {
            canvas.drawBitmap(bitmap, null, rect, paint)
        } else {
            textPaint.color = Color.WHITE
            textPaint.textAlign = Paint.Align.CENTER // Đặt căn giữa
            textPaint.textSize = 30f
            canvas.drawText(fallbackText, rect.centerX(), rect.centerY() + 10f, textPaint)
        }
    }

    private fun drawFallbackButton(canvas: Canvas, rect: RectF, text: String, color: Int) {
        paint.color = color
        canvas.drawRoundRect(rect, 10f, 10f, paint)
        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER // Đặt căn giữa
        textPaint.textSize = 40f
        canvas.drawText(text, rect.centerX(), rect.centerY() + 10f, textPaint)
    }

    // Touch handling
    fun onTouchDown(x: Float, y: Float): Boolean {
        if (!isVisible && settingsButton.contains(x, y)) {
            show()
            return true
        }

        if (isVisible) {
            when {
                closeButton.contains(x, y) -> {
                    hide()
                    return true
                }
                musicVolumeBar.contains(x, y) -> {
                    isDraggingMusic = true
                    updateMusicVolume(x)
                    return true
                }
                sfxVolumeBar.contains(x, y) -> {
                    isDraggingSfx = true
                    updateSfxVolume(x)
                    return true
                }
                previousButton.contains(x, y) -> {
                    // Return to menu logic will be handled by caller
                    return true
                }
                playButton.contains(x, y) -> {
                    togglePause()
                    return true
                }
                restartButton.contains(x, y) -> {
                    // Restart logic will be handled by caller
                    return true
                }
            }
        }

        return false
    }

    fun onTouchMove(x: Float, y: Float): Boolean {
        if (isDraggingMusic && musicVolumeBar.height() > 0 && musicVolumeBar.width() > 0) {
            updateMusicVolume(x)
            return true
        }
        if (isDraggingSfx && sfxVolumeBar.height() > 0 && sfxVolumeBar.width() > 0) {
            updateSfxVolume(x)
            return true
        }
        return false
    }

    fun onTouchUp(): Boolean {
        val wasDragging = isDraggingMusic || isDraggingSfx
        isDraggingMusic = false
        isDraggingSfx = false
        return wasDragging
    }

    private fun updateMusicVolume(x: Float) {
        val progress = ((x - musicVolumeBar.left) / musicVolumeBar.width()).coerceIn(0f, 1f)
        soundManager.musicVolume = progress
    }

    private fun updateSfxVolume(x: Float) {
        val progress = ((x - sfxVolumeBar.left) / sfxVolumeBar.width()).coerceIn(0f, 1f)
        soundManager.sfxVolume = progress
    }

    fun show() {
        isVisible = true
        isPaused = true
    }

    fun hide() {
        isVisible = false
        isPaused = false
    }

    fun togglePause() {
        isPaused = !isPaused
        if (isPaused) {
            soundManager.pauseBackgroundMusic()
        } else {
            soundManager.resumeBackgroundMusic()
        }
    }

    fun isVisible(): Boolean = isVisible
    fun isPaused(): Boolean = isPaused

    // Check if specific buttons were clicked
    fun wasPreviousClicked(x: Float, y: Float): Boolean {
        return isVisible && previousButton.contains(x, y)
    }

    fun wasRestartClicked(x: Float, y: Float): Boolean {
        return isVisible && restartButton.contains(x, y)
    }
}