package com.example.game2d

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast

class MenuView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {
    private val thread: MenuThread
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val btnRects = ArrayList<RectF>()
    // Bạn có thể đổi label "CHARACTER" thành "OPTIONS" nếu opt = options
    private val btnLabels = arrayListOf("CHARACTER", "START", "EXIT")
    private var pressedIndex = -1

    // pairs of (normalBitmap, pressedBitmap) for each button
    private val btnBitmaps = arrayListOf<Pair<Bitmap?, Bitmap?>>()

    // optional background
    private var bgBitmap: Bitmap? = null

    init {
        holder.addCallback(this)
        isFocusable = true

        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD)

        // try load background candidates if any
        bgBitmap = safeLoadDrawableByNames(context, listOf("logo_menu", "background", "menu_bg"))

        // load button image pairs using exact names you provided first
        btnBitmaps.clear()
        // BUTTON 0: use optbnt / optclick (user-provided)
        btnBitmaps.add(loadButtonPair(context,
            normalCandidates = listOf("optbnt", "optbtn", "opt_bnt", "opt_but", "optbnt"), // try close variants
            pressedCandidates = listOf("optclick", "opt_click", "optbnt_click", "optbtn_click")
        ))
        // BUTTON 1: use playbtn / playclick
        btnBitmaps.add(loadButtonPair(context,
            normalCandidates = listOf("playbtn", "btn_play", "play_btn", "play"),
            pressedCandidates = listOf("playclick", "play_click", "btn_play_click", "play_pressed")
        ))
        // BUTTON 2: exit - try exitbtn/exitclick as fallback
        btnBitmaps.add(loadButtonPair(context,
            normalCandidates = listOf("exitbtn", "btn_exit", "exit"),
            pressedCandidates = listOf("exitclick", "btn_exit_click", "exit_pressed")
        ))

        thread = MenuThread(holder, this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val w = width.toFloat()
        val h = height.toFloat()


        val btnW = w * 0.22f
        val btnH = h * 0.14f
        val gap = w * 0.06f
        val totalWidth = btnW * btnLabels.size + gap * (btnLabels.size - 1)
        var startX = (w - totalWidth) / 2f
        val y = h * 0.70f

        btnRects.clear()
        for (i in btnLabels.indices) {
            btnRects.add(RectF(startX, y, startX + btnW, y + btnH))
            startX += btnW + gap
        }

        // text size relative to button height
        textPaint.textSize = btnH * 0.28f

        // start thread
        thread.running = true
        if (!thread.isAlive) thread.start()
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

    fun update(deltaMs: Long) { /* no animation now */ }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        // background
        if (bgBitmap != null) {
            try {
                val scaled = Bitmap.createScaledBitmap(bgBitmap!!, width, height, true)
                canvas.drawBitmap(scaled, 0f, 0f, paint)
            } catch (e: Exception) { canvas.drawColor(Color.rgb(70,160,110)) }
        } else canvas.drawColor(Color.rgb(70,160,110))

        // logo/top panel
        val logoH = h * 0.28f
        val logoW = w * 0.45f
        val logoLeft = (w - logoW) / 2f
        val logoRect = RectF(logoLeft, h * 0.10f, logoLeft + logoW, h * 0.10f + logoH)
        paint.color = Color.argb(200, 255, 255, 255)
        canvas.drawRoundRect(logoRect, 16f, 16f, paint)
        textPaint.color = Color.DKGRAY
        textPaint.textSize = logoH * 0.12f
        canvas.drawText("GAME MENU", logoRect.centerX(), logoRect.centerY(), textPaint)

        // draw buttons
        for (i in btnRects.indices) {
            val r = btnRects[i]
            val bmpPair = btnBitmaps.getOrNull(i)
            val normalBmp = bmpPair?.first
            val pressedBmp = bmpPair?.second
            val usingPressedImg = (i == pressedIndex) && (pressedBmp != null)
            if (usingPressedImg) {
                canvas.drawBitmap(pressedBmp!!, null, r, paint)
            } else if (normalBmp != null) {
                canvas.drawBitmap(normalBmp, null, r, paint)
                if (i == pressedIndex) {
                    paint.color = Color.argb(80, 0, 0, 0)
                    canvas.drawRoundRect(r, 12f, 12f, paint)
                }
            } else {
                drawFallbackButton(canvas, r, btnLabels[i], pressed = (i == pressedIndex))
            }
        }
    }

    private fun drawFallbackButton(canvas: Canvas, r: RectF, label: String, pressed: Boolean) {
        val radius = 14f
        val topColor = if (pressed) Color.rgb(30, 120, 80) else Color.rgb(40, 200, 120)
        val bottomColor = if (pressed) Color.rgb(10, 90, 50) else Color.rgb(10, 150, 70)
        val shader = LinearGradient(r.left, r.top, r.left, r.bottom, topColor, bottomColor, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(r, radius, radius, paint)
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.argb(120, 0, 0, 0)
        canvas.drawRoundRect(r, radius, radius, paint)
        paint.style = Paint.Style.FILL
        textPaint.color = Color.WHITE
        val tx = r.centerX()
        val ty = r.centerY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(label, tx, ty, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x; val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> pressedIndex = findButtonIndex(x, y)
            MotionEvent.ACTION_MOVE -> { if (findButtonIndex(x,y) != pressedIndex) pressedIndex = -1 }
            MotionEvent.ACTION_UP -> {
                val idx = findButtonIndex(x, y)
                if (idx >= 0 && idx == pressedIndex) {
                    when (idx) {
                        0 -> openCharacter()
                        1 -> startGame()
                        2 -> exitGame()
                    }
                }
                pressedIndex = -1
            }
            MotionEvent.ACTION_CANCEL -> pressedIndex = -1
        }
        return true
    }

    private fun findButtonIndex(x: Float, y: Float): Int {
        for (i in btnRects.indices) if (btnRects[i].contains(x, y)) return i
        return -1
    }

    private fun openCharacter() {
        val act = context as? Activity
        act?.let {
            try {
                val intent = Intent()
                intent.setClassName(context, "com.example.game2d.CharacterActivity")
                it.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Character screen chưa có", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startGame() {
        val act = context as? android.app.Activity
        if (act == null) {
            android.widget.Toast.makeText(context, "Không thể mở game (context không phải Activity)", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        // Try compile-time Intent if GameActivity exists in project
        try {

            val intent = android.content.Intent()
            intent.setClassName(context, "com.example.game2d.GameActivity")
            act.startActivity(intent)

            android.util.Log.d("MENU", "startGame: started GameActivity via setClassName")
        } catch (e: Exception) {
            // show and log error so you can inspect logcat
            android.util.Log.e("MENU", "startGame: failed to start GameActivity", e)
            android.widget.Toast.makeText(context, "Không thể mở GameActivity: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
        }
    }




    private fun exitGame() {
        val act = context as? Activity
        act?.finish()
    }

    private fun safeLoadDrawableByNames(ctx: Context, names: List<String>): Bitmap? {
        for (n in names) {
            val id = ctx.resources.getIdentifier(n, "drawable", ctx.packageName)
            if (id != 0) {
                try { return BitmapFactory.decodeResource(ctx.resources, id) } catch (e: Exception) { }
            }
        }
        return null
    }

    private fun loadButtonPair(ctx: Context, normalCandidates: List<String>, pressedCandidates: List<String>): Pair<Bitmap?, Bitmap?> {
        val normal = safeLoadDrawableByNames(ctx, normalCandidates)
        val pressed = safeLoadDrawableByNames(ctx, pressedCandidates)
        return Pair(normal, pressed)
    }

    fun pause() {
        thread.running = false
        try { thread.join() } catch (e: InterruptedException) {}
    }

    fun resume() {
        if (!thread.running) {
            val t = MenuThread(holder, this)
            t.running = true
            t.start()
        }
    }
}
