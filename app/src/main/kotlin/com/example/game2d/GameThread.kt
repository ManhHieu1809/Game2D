package com.example.game2d

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameThread(private val surfaceHolder: SurfaceHolder, private val view: GameView) : Thread() {
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
                    val dtMs = targetTime
                    view.update(dtMs)
                    if (canvas != null) view.draw(canvas)
                }
            } finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (e: Exception) {}
                }
            }
            val elapsed = (System.nanoTime() - start) / 1_000_000
            val wait = targetTime - elapsed
            if (wait > 0) {
                try { sleep(wait) } catch (e: InterruptedException) {}
            }
        }
    }
}
