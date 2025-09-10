package com.example.game2d

import android.graphics.Canvas
import android.view.SurfaceHolder

class MenuThread(private val surfaceHolder: SurfaceHolder, private val menuView: MenuView) : Thread() {
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
                    menuView.update(targetTime)
                    if (canvas != null) menuView.draw(canvas)
                }
            } finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (e: Exception) {}
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
