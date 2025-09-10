package com.example.game2d

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameThread(private val surfaceHolder: SurfaceHolder, private val view: GameView) : Thread() {
    @Volatile var running = false
    private val targetFps = 60
    private var updated = false // Flag để đồng bộ update và draw

    override fun run() {
        var lastTime = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val deltaNs = now - lastTime
            lastTime = now
            val deltaMs = (deltaNs / 1_000_000L).coerceAtLeast(1L)

            // Update game state
            try {
                view.update(deltaMs)
                updated = true // Đánh dấu update hoàn tất
            } catch (e: Exception) {
                android.util.Log.e("GameThread", "Update error", e)
            }

            // Draw with robust lock/unlock
            var canvas: Canvas? = null
            try {
                try {
                    canvas = surfaceHolder.lockCanvas()
                } catch (ise: IllegalStateException) {
                    // Surface might be released briefly — don't crash, log and continue
                    android.util.Log.w("GameThread", "lockCanvas failed (surface maybe released). msg=${ise.message}")
                    canvas = null
                }

                if (canvas != null && updated) { // Chỉ vẽ nếu update hoàn tất
                    synchronized(surfaceHolder) {
                        view.draw(canvas)
                        updated = false // Reset flag sau khi vẽ
                    }
                } else {
                    // small sleep so we don't busy-loop if surface not ready
                    try { sleep(10) } catch (ignored: InterruptedException) {}
                }
            } catch (e: Exception) {
                android.util.Log.e("GameThread", "Draw error", e)
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                        android.util.Log.d("GameThread", "Posted frame at ${System.currentTimeMillis()}")
                    } catch (e: Exception) {
                        android.util.Log.e("GameThread", "unlockCanvasAndPost error", e)
                    }
                }
            }

            // Cap FPS approx
            val frameTime = 1000L / targetFps
            val sleepMs = frameTime - deltaMs
            if (sleepMs > 0) {
                try { sleep(sleepMs) } catch (ignored: InterruptedException) {}
            }
        }
        android.util.Log.d("GameThread", "Thread exiting")
    }
}