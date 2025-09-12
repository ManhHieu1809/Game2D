package com.example.game2d.entities

import android.graphics.Canvas

class EntityManager {
    private val pickups = mutableListOf<Pickup>()
    private val enemies = mutableListOf<Enemy>()

    fun addPickup(p: Pickup) { pickups += p }
    fun addEnemy(e: Enemy) { enemies += e }

    fun updateAll(dtMs: Long) {
        pickups.forEach { it.update(dtMs) }
        enemies.forEach { it.update(dtMs) }

        // gc
        pickups.removeAll { !it.alive }
        enemies.removeAll { !it.alive }
    }

    fun drawAll(canvas: Canvas) {
        // vẽ pickup trước, enemy sau (tuỳ ý)
        pickups.forEach { it.draw(canvas) }
        enemies.forEach { it.draw(canvas) }
    }

    fun getPickups(): List<Pickup> = pickups
    fun getEnemies(): List<Enemy> = enemies
}
