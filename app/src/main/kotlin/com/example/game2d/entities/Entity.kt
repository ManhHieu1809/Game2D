package com.example.game2d.entities

import android.graphics.Canvas
import android.graphics.RectF

abstract class Entity {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var alive = true

    abstract fun getBounds(): RectF
    abstract fun update(dtMs: Long)
    abstract fun draw(canvas: Canvas)
    open fun onCollide(other: Entity) {}
}
