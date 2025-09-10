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

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private var thread: GameThread = GameThread(holder, this)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 28f
        setShadowLayer(2f, 2f, 2f, Color.BLACK)  // Text shadow
    }

    private val tileMap = TileMap(context)
    private val player = Player(context, 200f, 0f)

    // Camera với smooth movement
    private var cameraX = 0f
    private var cameraY = 0f
    private var targetCameraX = 0f
    private var targetCameraY = 0f
    private val cameraSpeed = 5f  // Camera lerp speed

    // Scale/center
    private var worldScale = 1f
    private var screenOffsetX = 0f
    private var screenOffsetY = 0f

    // HUD buttons (screen coords)
    private val btnLeft = RectF()
    private val btnRight = RectF()
    private val btnJump = RectF()
    private val activePointers = HashMap<Int, String>()

    private var screenW = 1f
    private var screenH = 1f

    // Paints for buttons với shadow effects
    private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        setShadowLayer(2f, 2f, 2f, Color.BLACK)
    }

    // Game stats
    private var score = 0
    private var lives = 3

    init {
        holder.addCallback(this)
        isFocusable = true

        // Tắt hardware acceleration để tránh rendering issues với pixel art
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenW = width.toFloat()
        screenH = height.toFloat()

        // Responsive button sizing
        val btnSize = min(screenW, screenH) * 0.14f
        val margin = btnSize * 0.2f

        // Position buttons better for Mario-style game
        btnLeft.set(margin, screenH - margin - btnSize, margin + btnSize, screenH - margin)
        btnRight.set(btnLeft.right + margin * 0.4f, btnLeft.top, btnLeft.right + margin * 0.4f + btnSize, btnLeft.bottom)
        btnJump.set(screenW - margin - btnSize, screenH - margin - btnSize, screenW - margin, screenH - margin)

        // Compute worldScale so world fits screen (keep aspect ratio)
        worldScale = min(screenW / tileMap.worldWidth, screenH / tileMap.worldHeight)
        screenOffsetX = (screenW - tileMap.worldWidth * worldScale) / 2f
        screenOffsetY = (screenH - tileMap.worldHeight * worldScale) / 2f

        // Place player on ground after we know player.height
        player.x = 200f
        player.y = tileMap.getGroundTopY() - player.height

        // Initialize camera
        targetCameraX = player.x + player.width / 2f - (screenW / worldScale) / 2f
        targetCameraY = player.y + player.height / 2f - (screenH / worldScale) / 2f
        cameraX = targetCameraX
        cameraY = targetCameraY

        thread = GameThread(holder, this)
        thread.running = true
        thread.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

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
        // Handle input
        var mv = 0
        if (activePointers.containsValue("left")) mv = -1
        if (activePointers.containsValue("right")) mv = 1
        player.setMoving(mv)

        // Update game objects
        player.update(deltaMs, tileMap)
        tileMap.update(deltaMs)

        // Smooth camera following với viewport clamping
        val viewportWorldW = screenW / worldScale
        val viewportWorldH = screenH / worldScale

        // Target camera position (follow player)
        targetCameraX = player.x + player.width / 2f - viewportWorldW / 2f
        targetCameraY = player.y + player.height / 2f - viewportWorldH / 2f

        // Clamp target camera to world bounds
        targetCameraX = targetCameraX.coerceIn(0f, tileMap.worldWidth - viewportWorldW)
        targetCameraY = targetCameraY.coerceIn(0f, tileMap.worldHeight - viewportWorldH)

        // Smooth camera movement (lerp)
        val dt = deltaMs / 1000f
        val lerpFactor = 1f - Math.pow(0.1, (dt * cameraSpeed).toDouble()).toFloat()
        cameraX += (targetCameraX - cameraX) * lerpFactor
        cameraY += (targetCameraY - cameraY) * lerpFactor

        // Pixel-perfect camera positioning
        cameraX = kotlin.math.round(cameraX * worldScale) / worldScale
        cameraY = kotlin.math.round(cameraY * worldScale) / worldScale
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        canvas.drawColor(Color.rgb(135, 206, 250))  // Sky blue background

        // Apply world transforms
        canvas.save()
        canvas.translate(screenOffsetX, screenOffsetY)
        canvas.scale(worldScale, worldScale)
        canvas.translate(-cameraX, -cameraY)

        // Draw world objects in world coordinates
        tileMap.draw(canvas)
        player.draw(canvas, paint)

        canvas.restore()

        // Draw HUD in screen coordinates (after restore)
        drawHUD(canvas)
        drawControls(canvas)
    }

    private fun drawHUD(canvas: Canvas) {
        // HUD Background
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(180, 0, 0, 0)
        canvas.drawRoundRect(12f, 12f, 300f, 80f, 8f, 8f, paint)

        // Score
        hudPaint.color = Color.YELLOW
        hudPaint.textSize = 24f
        canvas.drawText("SCORE", 24f, 35f, hudPaint)
        hudPaint.color = Color.WHITE
        canvas.drawText("$score", 24f, 60f, hudPaint)

        // Lives
        hudPaint.color = Color.RED
        canvas.drawText("LIVES", 120f, 35f, hudPaint)
        hudPaint.color = Color.WHITE
        canvas.drawText("$lives", 120f, 60f, hudPaint)

        // World info
        hudPaint.color = Color.CYAN
        canvas.drawText("WORLD 1-1", 200f, 35f, hudPaint)

        // Instructions (only show first few seconds)
        hudPaint.color = Color.WHITE
        hudPaint.textSize = 20f
        canvas.drawText("Use buttons: ← → ↑ to move and jump", 12f, screenH - 20f, hudPaint)
    }

    private fun drawControls(canvas: Canvas) {
        drawControlButton(canvas, btnLeft, "←", activePointers.containsValue("left"))
        drawControlButton(canvas, btnRight, "→", activePointers.containsValue("right"))
        drawControlButton(canvas, btnJump, "↑", activePointers.containsValue("jump"))
    }

    private fun drawControlButton(canvas: Canvas, rect: RectF, label: String, pressed: Boolean) {
        val cornerRadius = 16f

        // Button shadow
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(100, 0, 0, 0)
        canvas.drawRoundRect(
            rect.left + 4f, rect.top + 4f,
            rect.right + 4f, rect.bottom + 4f,
            cornerRadius, cornerRadius, paint
        )

        // Button background với gradient effect
        val topColor = if (pressed) Color.rgb(50, 150, 250) else Color.rgb(80, 180, 250)
        val bottomColor = if (pressed) Color.rgb(30, 100, 200) else Color.rgb(40, 120, 220)

        val shader = android.graphics.LinearGradient(
            rect.left, rect.top, rect.left, rect.bottom,
            topColor, bottomColor,
            android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = shader
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        paint.shader = null

        // Button border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = if (pressed) Color.rgb(20, 80, 160) else Color.rgb(60, 140, 200)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        // Button text
        val textY = rect.centerY() - (btnTextPaint.descent() + btnTextPaint.ascent()) / 2f
        btnTextPaint.color = Color.WHITE
        canvas.drawText(label, rect.centerX(), textY, btnTextPaint)

        paint.style = Paint.Style.FILL  // Reset
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
                    if (which == "jump") {
                        player.jump()
                        // TODO: Add jump sound effect
                    }
                } else {
                    // Fallback touch controls
                    activePointers[pid] = if (x < screenW / 2f) "left" else "right"
                }
            }

            MotionEvent.ACTION_MOVE -> {
                // Handle dragging between buttons
                for (i in 0 until event.pointerCount) {
                    val p = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)
                    val which = whichControl(x, y)
                    if (which != null) {
                        val oldWhich = activePointers[p]
                        if (oldWhich != which) {
                            activePointers[p] = which
                            if (which == "jump" && oldWhich != "jump") {
                                player.jump()
                            }
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                activePointers.remove(pid)
            }
        }
        return true
    }

    private fun whichControl(x: Float, y: Float): String? {
        return when {
            btnLeft.contains(x, y) -> "left"
            btnRight.contains(x, y) -> "right"
            btnJump.contains(x, y) -> "jump"
            else -> null
        }
    }

    fun pause() {
        thread.running = false
        try {
            thread.join()
        } catch (e: InterruptedException) {}
    }

    fun resume() {
        if (!thread.running) {
            thread = GameThread(holder, this)
            thread.running = true
            thread.start()
        }
    }
}