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

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private var thread: GameThread = GameThread(holder, this)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 28f }

    private val tileMap: TileMap
    private val player: Player

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
    private val btnShot = RectF()
    private val activePointers = HashMap<Int, String>()

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
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        // Clear canvas to prevent frame overlap
        canvas.drawColor(Color.BLACK)

        // Apply transforms: translate offset -> scale -> translate camera
        canvas.save()
        canvas.translate(screenOffsetX, screenOffsetY)
        canvas.scale(worldScale, worldScale)

// round camera to integer pixels (avoid sub-pixel sampling)
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

        // Debug info
        hudPaint.color = Color.WHITE
        canvas.drawText("Use buttons: ← → ▲ ●", 12f, 34f, hudPaint)
        // Show camera position for debugging
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
        thread.running = false
        try { thread.join() } catch (e: InterruptedException) {}
    }

    fun resume() {
        if (!thread.running) {
            thread = GameThread(holder, this)
            thread.running = true
            thread.start()
        }
    }
}