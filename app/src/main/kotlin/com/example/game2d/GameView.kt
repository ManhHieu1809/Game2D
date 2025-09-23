package com.example.game2d

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.util.Log

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private var thread: GameThread = GameThread(holder, this)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 28f }

    private val tileMap: TileMap
    private val player: Player

    // camera (world coords)
    private var cameraX = 0f
    private var cameraY = 0f

    // Sound & boundary flags
    private var touchedLeft = false
    private var touchedRight = false
    private var halfCrossed = false

    // scale/center
    private var worldScale = 1f
    private var screenOffsetX = 0f
    private var screenOffsetY = 0f

    // HUD buttons (screen coords)
    private val btnLeft = RectF()
    private val btnRight = RectF()
    private val btnJump = RectF()
    private val btnShot = RectF()
    private val activePointers = HashMap<Int, String>()

    // --- audio UI bitmaps & rects ---
    private var musicOnBmp: Bitmap? = null    // music_turn_on.png (shows when music is OFF)
    private var musicOffBmp: Bitmap? = null   // music_turn_off.png (shows when music is ON)
    private var soundOnBmp: Bitmap? = null    // sound_on.png (shows when sound effects ON)
    private var soundOffBmp: Bitmap? = null   // sound_off.png (shows when sound effects OFF)

    private val musicRect = RectF()
    private val soundRect = RectF()

    // icon size in px (will scale based on screen density or width)
    private var iconSizePx = 96f   // you can tune this: px not dp. adjust if too big/small

    // state flags
    private var isMusicOn = false
    private var areEffectsOn = true

    private var screenW = 1f
    private var screenH = 1f

    // paints for buttons
    private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 36f; textAlign = Paint.Align.CENTER
    }

    init {
        com.example.game2d.AppCtx.ctx = context

        // now safe to construct TileMap and Player (they may load sprites)
        tileMap = TileMap(context)
        player = Player(context, 200f, 0f)

        holder.addCallback(this)
        isFocusable = true
    }

    private fun initAudioUI() {
        // load bitmaps from resources (ensure resource names are correct)
        try {
            var tmp1 = BitmapFactory.decodeResource(resources, R.drawable.music_turn_on)
            var tmp2 = BitmapFactory.decodeResource(resources, R.drawable.music_turn_off)
            var tmp3 = BitmapFactory.decodeResource(resources, R.drawable.sound_on)
            var tmp4 = BitmapFactory.decodeResource(resources, R.drawable.sound_off)

            // pre-scale icons once using display density (48dp default)
            val density = resources.displayMetrics.density
            val sizePx = (48f * density).toInt().coerceAtLeast(32)
            musicOnBmp = tmp1?.let { Bitmap.createScaledBitmap(it, sizePx, sizePx, true) }
            musicOffBmp = tmp2?.let { Bitmap.createScaledBitmap(it, sizePx, sizePx, true) }
            soundOnBmp = tmp3?.let { Bitmap.createScaledBitmap(it, sizePx, sizePx, true) }
            soundOffBmp = tmp4?.let { Bitmap.createScaledBitmap(it, sizePx, sizePx, true) }
            iconSizePx = sizePx.toFloat()
        } catch (e: Exception) {
            Log.e("GameView", "Failed to load audio UI bitmaps: ${e.message}")
        }

        // init sound effects using SoundManager
        try { SoundManager.init(context) } catch (e: Exception) { Log.e("GameView","SoundManager.init err: ${e.message}") }

        // init background music player (ensure bg_music exists in res/raw)
        try { BackgroundMusic.init(context, R.raw.bg_music) } catch (e: Exception) { Log.e("GameView","BackgroundMusic.init err: ${e.message}") }

        // default initial states
        isMusicOn = BackgroundMusic.musicEnabled // usually false initially
        areEffectsOn = SoundManager.effectsEnabled
    }



    override fun surfaceCreated(holder: SurfaceHolder) {
        // initial compute
        screenW = width.toFloat()
        screenH = height.toFloat()

        recomputeLayout()
        // place player on ground after we know player.height
        player.x = 200f
        player.y = tileMap.getGroundTopY() - player.height

        initAudioUI()

        thread = GameThread(holder, this)
        thread.running = true
        thread.start()
        try { SoundManager.init(context) } catch (e: Exception) { /* ignore */ }
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
            try { thread.join(); retry = false } catch (e: InterruptedException) {}
        }
    }

    private fun recomputeLayout() {
        // Button sizing (screen coords)
        val btnSize = min(screenW, screenH) * 0.16f
        val margin = btnSize * 0.18f
        btnLeft.set(margin, screenH - margin - btnSize, margin + btnSize, screenH - margin)
        btnRight.set(btnLeft.right + margin * 0.6f, btnLeft.top, btnLeft.right + margin * 0.6f + btnSize, btnLeft.bottom)
        btnJump.set(screenW - margin - btnSize, screenH - margin - btnSize, screenW - margin, screenH - margin)
        btnShot.set(screenW - margin - btnSize, screenH - margin - btnSize * 2.2f, screenW - margin, screenH - margin - btnSize * 1.2f)

        // Scale the world so it fits the SCREEN HEIGHT properly
        worldScale = screenH / tileMap.worldHeight

        // Optional caps to avoid super large/small scale
        worldScale = worldScale.coerceIn(0.5f, 2.0f)

        // Center vertically
        screenOffsetY = (screenH - tileMap.worldHeight * worldScale) / 2f
        // For X we typically want 0 (camera will scroll horizontally)
        screenOffsetX = 0f
    }

    fun update(deltaMs: Long) {
        var mv = 0
        if (activePointers.containsValue("left")) mv = -1
        if (activePointers.containsValue("right")) mv = 1
        player.setMoving(mv)

        player.update(deltaMs, tileMap)
        tileMap.update(deltaMs)
        tileMap.updateMonsters(deltaMs, player)
        tileMap.checkBulletHitAndRespawnIfNeeded(player)

        // Compute viewport in world units
        val viewportWorldW = screenW / worldScale
        val viewportWorldH = screenH / worldScale

        // Camera follows player (centered on player)
        val targetCameraX = player.x + player.width / 2f - viewportWorldW / 2f
        val targetCameraY = player.y + player.height / 2f - viewportWorldH / 2f

        // Round camera to avoid sub-pixel rendering issues
        cameraX = round(targetCameraX)
        cameraY = round(targetCameraY)

        // FIXED: Properly clamp camera so we never show black areas
        // Left boundary
        cameraX = cameraX.coerceAtLeast(0f)
        // Right boundary - make sure we don't go past the world
        cameraX = cameraX.coerceAtMost(max(0f, tileMap.worldWidth - viewportWorldW))

        // Top boundary
        cameraY = cameraY.coerceAtLeast(0f)
        // Bottom boundary
        cameraY = cameraY.coerceAtMost(max(0f, tileMap.worldHeight - viewportWorldH))

        // ----- Sound triggers for gameplay events -----
        val playerScreenX = player.x - cameraX
        val playerCenterScreenX = player.x + player.width / 2f - cameraX
        val playerRightScreenX = playerScreenX + player.width

// Left edge contact
        if (playerScreenX <= 0f && !touchedLeft) {
            touchedLeft = true
            try { SoundManager.playWallHit() } catch (e: Exception) {}
        } else if (playerScreenX > 0f) {
            touchedLeft = false
        }

// Right edge contact
        if (playerRightScreenX >= viewportWorldW && !touchedRight) {
            touchedRight = true
            try { SoundManager.playWallHit() } catch (e: Exception) {}
        } else if (playerRightScreenX < viewportWorldW) {
            touchedRight = false
        }

// Crossing center from left to right: trigger warning 3..6 times
        if (playerCenterScreenX > viewportWorldW / 2f && !halfCrossed) {
            halfCrossed = true
            val repeats = (3..6).random() // nếu muốn random 3->6, hoặc dùng số cố định
            try { SoundManager.playWarning(repeats, 300L) } catch (e: Exception) {}
        } else if (playerCenterScreenX <= viewportWorldW / 2f) {
            halfCrossed = false
        }
// ----- end sound triggers -----

    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        // Clear canvas
        canvas.drawColor(Color.BLACK)

        // World transforms
        canvas.save()
        canvas.translate(screenOffsetX, screenOffsetY)
        canvas.scale(worldScale, worldScale)

        val camX = kotlin.math.round(cameraX)
        val camY = kotlin.math.round(cameraY)
        canvas.translate(-camX, -camY)

        // draw world
        tileMap.draw(canvas)
        player.draw(canvas, paint)

        // restore to screen coords
        canvas.restore()

        // ---------------------------
        // draw audio icons in SCREEN coordinates (pre-scaled bitmaps)
        // ---------------------------
        val size = iconSizePx
        val density = resources.displayMetrics.density
        val margin = 12f * density

        // SOUND icons -> top-left
        val soundCenterX = size / 2f + margin
        val soundCenterY = margin + size / 2f
        soundRect.set(
            soundCenterX - size / 2f,
            soundCenterY - size / 2f,
            soundCenterX + size / 2f,
            soundCenterY + size / 2f
        )
        val soundBmp = if (areEffectsOn) soundOnBmp else soundOffBmp
        soundBmp?.let { canvas.drawBitmap(it, soundRect.left, soundRect.top, paint) }

        // MUSIC icons -> top-right
        val musicCenterX = width - size / 2f - margin
        val musicCenterY = margin + size / 2f
        musicRect.set(
            musicCenterX - size / 2f,
            musicCenterY - size / 2f,
            musicCenterX + size / 2f,
            musicCenterY + size / 2f
        )
        val musicBmp = if (isMusicOn) musicOffBmp else musicOnBmp
        musicBmp?.let { canvas.drawBitmap(it, musicRect.left, musicRect.top, paint) }

        // Draw HUD/buttons in screen coordinates (after restore)
        drawControlButtonVisible(canvas, btnLeft, "◀", activePointers.containsValue("left"))
        drawControlButtonVisible(canvas, btnRight, "▶", activePointers.containsValue("right"))
        drawControlButtonVisible(canvas, btnJump, "▲", activePointers.containsValue("jump"))
        drawControlButtonVisible(canvas, btnShot, "●", activePointers.containsValue("shot"))

        // Debug info
        hudPaint.color = Color.WHITE
        canvas.drawText("Use buttons: ← → ▲ ●", 12f, 34f, hudPaint)
        canvas.drawText("Camera: (${cameraX.toInt()}, ${cameraY.toInt()})", 12f, 64f, hudPaint)
    }


    private fun drawControlButtonVisible(canvas: Canvas, r: RectF, label: String, pressed: Boolean) {
        btnPaint.style = Paint.Style.FILL
        btnPaint.color = if (pressed) android.graphics.Color.argb(220, 20, 160, 90) else android.graphics.Color.argb(200, 60, 60, 60)
        canvas.drawRoundRect(r, 18f, 18f, btnPaint)
        btnPaint.style = Paint.Style.STROKE
        btnPaint.color = android.graphics.Color.argb(220, 0, 0, 0)
        btnPaint.strokeWidth = 4f
        canvas.drawRoundRect(r, 18f, 18f, btnPaint)

        val ty = r.centerY() - (btnTextPaint.descent() + btnTextPaint.ascent()) / 2f
        canvas.drawText(label, r.centerX(), ty, btnTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val idx = event.actionIndex
        val pid = event.getPointerId(idx)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(idx)
                val y = event.getY(idx)

                // 1) Check audio toggles first (screen coords)
                if (soundRect.contains(x, y)) {
                    areEffectsOn = !areEffectsOn
                    SoundManager.effectsEnabled = areEffectsOn
                    if (areEffectsOn) {
                        try { SoundManager.playShot() } catch (_: Exception) {}
                    }
                    return true
                }

                if (musicRect.contains(x, y)) {
                    isMusicOn = !isMusicOn
                    try {
                        if (isMusicOn) BackgroundMusic.play() else BackgroundMusic.pause()
                    } catch (_: Exception) {}
                    return true
                }

                // 2) Not hitting audio icons -> existing control detection
                val which = whichControl(x, y)
                if (which != null) {
                    activePointers[pid] = which
                    if (which == "jump") player.jump()
                    if (which == "shot") player.shoot()
                } else {
                    activePointers[pid] = if (x < width / 2f) "left" else "right"
                }
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val p = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)
                    val which = whichControl(x, y)
                    if (which != null) activePointers[p] = which
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                activePointers.remove(pid)
            }
        }
        return true
    }


    private fun whichControl(x: Float, y: Float): String? {
        if (btnLeft.contains(x, y)) return "left"
        if (btnRight.contains(x, y)) return "right"
        if (btnJump.contains(x, y)) return "jump"
        if (btnShot.contains(x, y)) return "shot"
        return null
    }

    fun pause() {
        // stop game thread
        thread.running = false
        try { thread.join() } catch (e: InterruptedException) {}
        // pause music (keep soundPool loaded for faster resume)
        try { BackgroundMusic.pause() } catch (_: Exception) {}
        // do NOT release SoundManager here if you want quick resume; optional:
        // SoundManager.release()
    }

    fun resume() {
        if (!thread.running) {
            thread = GameThread(holder, this)
            thread.running = true
            thread.start()
        }
        // if previously music was on, resume play
        try {
            if (isMusicOn) BackgroundMusic.play()
        } catch (_: Exception) {}
        // ensure sound pool is initialized
        try { SoundManager.init(context) } catch (_: Exception) {}
    }
}