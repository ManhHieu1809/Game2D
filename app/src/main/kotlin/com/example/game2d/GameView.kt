package com.example.game2d

import android.content.Context
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

    private var screenW = 1f
    private var screenH = 1f

    // paints for buttons
    private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; textSize = 36f; textAlign = Paint.Align.CENTER
    }

    // ====== NEW: Toggle bitmaps & rects ======
    private val togglePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rectMusic = RectF() // giữa cạnh trên
    private val rectSfx = RectF()   // giữa cạnh dưới

    private var bmpMusicOn: Bitmap? = null   // X  (music_turnon)
    private var bmpMusicOff: Bitmap? = null  // Y  (music_turnoff)
    private var bmpSfxOn: Bitmap? = null     // O  (sound_on)
    private var bmpSfxOff: Bitmap? = null    // Z  (sound_off)

    private var musicEnabled = false
    // SFX state lấy trực tiếp từ SoundManager.isSfxEnabled()

    init {
        com.example.game2d.AppCtx.ctx = context

        // now safe to construct TileMap and Player (they may load sprites)
        tileMap = TileMap(context)
        player = Player(context, 200f, 0f)

        holder.addCallback(this)
        isFocusable = true

        // ====== NEW: decode toggle icons once ======
        bmpMusicOn = BitmapFactory.decodeResource(resources, R.drawable.music_turnon)
        bmpMusicOff = BitmapFactory.decodeResource(resources, R.drawable.music_turnoff)
        bmpSfxOn = BitmapFactory.decodeResource(resources, R.drawable.sound_on)
        bmpSfxOff = BitmapFactory.decodeResource(resources, R.drawable.sound_off)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // initial compute
        screenW = width.toFloat()
        screenH = height.toFloat()

        recomputeLayout()
        // place player on ground after we know player.height
        player.x = 200f
        player.y = tileMap.getGroundTopY() - player.height

        thread = GameThread(holder, this)
        thread.running = true
        thread.start()

        // Init audio
        try { SoundManager.init(context) } catch (_: Exception) {}
        try {
            MusicManager.init(context, R.raw.bgm) // cần res/raw/bgm.mp3
            MusicManager.setEnabled(false)        // mặc định tắt, nhấn X để bật
            musicEnabled = MusicManager.isEnabled()
        } catch (_: Exception) {}
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

        // ====== NEW: place toggle icons at middle of edges ======
        val density = resources.displayMetrics.density
        val sizeDp = 48f
        val marginDp = 8f
        val size = sizeDp * density
        val marginPx = marginDp * density

        // Music (X/Y) giữa cạnh TRÊN
        val cxTop = screenW / 2f
        val cyTop = marginPx + size / 2f
        rectMusic.set(cxTop - size / 2f, cyTop - size / 2f, cxTop + size / 2f, cyTop + size / 2f)

        // SFX (Z/O) giữa cạnh DƯỚI (đối diện)
        val cxBot = screenW / 2f
        val cyBot = screenH - marginPx - size / 2f
        rectSfx.set(cxBot - size / 2f, cyBot - size / 2f, cxBot + size / 2f, cyBot + size / 2f)
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

        // Clamp camera
        cameraX = cameraX.coerceAtLeast(0f)
        cameraX = cameraX.coerceAtMost(max(0f, tileMap.worldWidth - viewportWorldW))
        cameraY = cameraY.coerceAtLeast(0f)
        cameraY = cameraY.coerceAtMost(max(0f, tileMap.worldHeight - viewportWorldH))

        // ----- Sound triggers for gameplay events -----
        val playerScreenX = player.x - cameraX
        val playerCenterScreenX = player.x + player.width / 2f - cameraX
        val playerRightScreenX = playerScreenX + player.width

        // Left edge contact
        if (playerScreenX <= 0f && !touchedLeft) {
            touchedLeft = true
            try { SoundManager.playWallHit() } catch (_: Exception) {}
        } else if (playerScreenX > 0f) {
            touchedLeft = false
        }

        // Right edge contact
        if (playerRightScreenX >= viewportWorldW && !touchedRight) {
            touchedRight = true
            try { SoundManager.playWallHit() } catch (_: Exception) {}
        } else if (playerRightScreenX < viewportWorldW) {
            touchedRight = false
        }

        // Crossing center from left to right: trigger warning 3..6 times
        if (playerCenterScreenX > viewportWorldW / 2f && !halfCrossed) {
            halfCrossed = true
            val repeats = (3..6).random()
            try { SoundManager.playWarning(repeats, 300L) } catch (_: Exception) {}
        } else if (playerCenterScreenX <= viewportWorldW / 2f) {
            halfCrossed = false
        }
        // ----- end sound triggers -----
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        // Clear canvas to prevent frame overlap
        canvas.drawColor(Color.BLACK)

        // Apply transforms: translate offset -> scale -> translate camera
        canvas.save()
        canvas.translate(screenOffsetX, screenOffsetY)
        canvas.scale(worldScale, worldScale)

        val camX = kotlin.math.round(cameraX)
        val camY = kotlin.math.round(cameraY)
        canvas.translate(-camX, -camY)

        tileMap.draw(canvas)
        player.draw(canvas, paint)
        canvas.restore()

        // Draw HUD/buttons in screen coordinates (after restore)
        drawControlButtonVisible(canvas, btnLeft, "◀", activePointers.containsValue("left"))
        drawControlButtonVisible(canvas, btnRight, "▶", activePointers.containsValue("right"))
        drawControlButtonVisible(canvas, btnJump, "▲", activePointers.containsValue("jump"))
        drawControlButtonVisible(canvas, btnShot, "●", activePointers.containsValue("shot"))
        // --- vẽ coin counter (góc trên trái) ---
        try {
            // lấy giá trị coin từ tileMap (an toàn)
            val coinCount = try { tileMap.getCoinCount() } catch (e: Exception) { 0 }

            val coinBmp = com.example.game2d.resources.SpriteLoader.get("cherry")

            val coinSize = 48f
            val cx = 12f
            val cy = 12f

            if (coinBmp != null) {
                // vẽ bitmap icon
                canvas.drawBitmap(coinBmp, null, android.graphics.RectF(cx, cy, cx + coinSize, cy + coinSize), paint)
            } else {
                // fallback: vẽ hình tròn
                val tmpP = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                tmpP.color = android.graphics.Color.YELLOW
                canvas.drawCircle(cx + coinSize/2f, cy + coinSize/2f, coinSize/2f, tmpP)
            }

            // vẽ số lượng (dùng nối chuỗi, tránh lỗi hiển thị ${...})
            val countText = "x " + coinCount.toString()
            hudPaint.color = android.graphics.Color.WHITE
            hudPaint.textSize = 36f
            hudPaint.isAntiAlias = true
            // Y vị trí baseline: điều chỉnh nếu cần (số nằm giữa icon)
            canvas.drawText(countText, cx + coinSize + 10f, cy + coinSize*0.75f, hudPaint)

            // debug (tạm): in log mỗi lần draw để kiểm tra coin value
            android.util.Log.d("GAME_HUD", "coinCount = $coinCount")
        } catch (e: Exception) {
            android.util.Log.e("GAME_HUD", "Error drawing coin HUD: ${e.message}")
        }
// --- end coin HUD ---



        // ====== NEW: draw toggles on top ======
        val mBmp = if (musicEnabled) bmpMusicOn else bmpMusicOff
        mBmp?.let { canvas.drawBitmap(it, null, rectMusic, togglePaint) }

        val sfxOn = SoundManager.isSfxEnabled()
        val sBmp = if (sfxOn) bmpSfxOn else bmpSfxOff
        sBmp?.let { canvas.drawBitmap(it, null, rectSfx, togglePaint) }

        // Debug info
        hudPaint.color = Color.WHITE
    }

    private fun drawControlButtonVisible(canvas: Canvas, r: RectF, label: String, pressed: Boolean) {
        btnPaint.style = Paint.Style.FILL
        btnPaint.color = if (pressed) Color.argb(220, 20, 160, 90) else Color.argb(200, 60, 60, 60)
        canvas.drawRoundRect(r, 18f, 18f, btnPaint)
        btnPaint.style = Paint.Style.STROKE
        btnPaint.color = Color.argb(220, 0, 0, 0)
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

                // ====== NEW: handle toggle taps first ======
                if (rectMusic.contains(x, y)) {
                    musicEnabled = MusicManager.toggle() // đổi nhạc + lưu trạng thái
                    invalidate()
                    return true
                }
                if (rectSfx.contains(x, y)) {
                    val on = SoundManager.toggleSfx()
                    // Không cần lưu local; draw() đọc trực tiếp từ SoundManager
                    invalidate()
                    return true
                }

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

                    // chặn kéo qua icon toggle: không chuyển sang điều khiển
                    if (rectMusic.contains(x, y) || rectSfx.contains(x, y)) continue

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
        thread.running = false
        try { thread.join() } catch (_: InterruptedException) {}
    }

    fun resume() {
        if (!thread.running) {
            thread = GameThread(holder, this)
            thread.running = true
            thread.start()
        }
    }
}
