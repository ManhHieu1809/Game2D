package com.example.game2d.obstacles

import android.graphics.*
import com.example.game2d.Player
import com.example.game2d.resources.SpriteLoader

class Spike(val rect: RectF) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(canvas: Canvas) {
        val sprite = SpriteLoader.get("spike")
        if (sprite != null) {
            paint.isFilterBitmap = true
            paint.isDither = true
            canvas.drawBitmap(sprite, null, rect, paint)
        } else {
            val path = Path().apply {
                moveTo(rect.left, rect.bottom)
                lineTo(rect.centerX(), rect.top)
                lineTo(rect.right, rect.bottom)
                close()
            }
            paint.color = Color.LTGRAY
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.DKGRAY
            canvas.drawPath(path, paint)
            paint.style = Paint.Style.FILL
        }
    }

    fun isHit(player: Player): Boolean {
        val p = RectF(player.x, player.y, player.x + player.width, player.y + player.height)
        return RectF.intersects(p, rect)
    }
}
