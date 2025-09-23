package com.example.game2d

import android.graphics.Canvas

interface TileMapInterface {
    val worldWidth: Float
    val worldHeight: Float

    fun getGroundTopY(): Float
    fun draw(canvas: Canvas)
    fun update(deltaMs: Long)
    fun updateMonsters(deltaMs: Long, player: Player)
    fun checkBulletHitAndRespawnIfNeeded(player: Player)
    fun resolvePlayerCollision(player: Player): Boolean
    fun isCompleted(player: Player): Boolean
}
