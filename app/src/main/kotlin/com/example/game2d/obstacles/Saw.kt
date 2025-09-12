package com.example.game2d.obstacles

import android.graphics.*
import com.example.game2d.Player
import com.example.game2d.resources.SpriteLoader
import kotlin.math.max
import kotlin.math.min

class Saw(var x: Float, var y: Float, var rotSpeed: Float = 180f) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    var rotation = 0f

    fun update(dt: Float) {
        rotation += rotSpeed * dt
        if (rotation >= 360f) rotation -= 360f
        if (rotation < 0f) rotation += 360f
    }

    fun draw(canvas: Canvas) {
        val sprite = SpriteLoader.get("saw")
        canvas.save()
        canvas.rotate(rotation, x, y)
        val size = 48f
        val rect = RectF(x - size/2, y - size/2, x + size/2, y + size/2)
        if (sprite != null) {
            paint.isFilterBitmap = true
            paint.isDither = true
            canvas.drawBitmap(sprite, null, rect, paint)
        } else {
            paint.color = Color.LTGRAY
            canvas.drawCircle(x, y, size/2, paint)
        }
        canvas.restore()
    }

    fun isHit(player: Player): Boolean {
        val px = player.x + player.width/2f
        val py = player.y + player.height/2f
        val dx = px - x; val dy = py - y
        return dx*dx + dy*dy < 30f*30f
    }
}
