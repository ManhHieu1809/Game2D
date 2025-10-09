package com.example.game2d

import android.graphics.Canvas

interface TileMapInterface {
    val worldWidth: Float
    val worldHeight: Float

    fun getGroundTopY(): Float
    fun draw(canvas: Canvas)
    fun update(deltaMs: Long)
    fun updateMonsters(deltaMs: Long, player: Player)
    fun checkBulletHitAndRespawnIfNeeded(player: Player): Boolean
    fun resolvePlayerCollision(player: Player): Boolean
    fun resolvePlayerCollisionSafe(player: Player) // Safe collision that ignores hazards during invulnerability
    fun isCompleted(player: Player): Boolean
    fun checkCoinCollection(player: Player, onCoinCollected: (String) -> Unit)
    fun checkHealthCollection(player: Player, onHealthCollected: (Int) -> Unit) // New method for health pickups
    fun resetLevel() // Reset all entities, monsters, and pickups to initial state

    // Hazard access methods for collision detection
    fun getSpikes(): List<com.example.game2d.obstacles.Spike>
    fun getSaws(): List<com.example.game2d.obstacles.Saw>

    fun getLastCheckpoint(): Triple<Float, Float, Int>

    // Methods for Monster1 collision detection
    fun getTileSize(): Float = 32f  // Default tile size
    fun isTileSolid(col: Int, row: Int): Boolean = false  // Default implementation
    fun getTileRect(col: Int, row: Int): android.graphics.RectF {
        val tileSize = getTileSize()
        return android.graphics.RectF(
            col * tileSize,
            row * tileSize,
            (col + 1) * tileSize,
            (row + 1) * tileSize
        )
    }
}
