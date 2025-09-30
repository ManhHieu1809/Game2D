package com.example.game2d.entities

import android.graphics.Canvas

class EntityManager {
    private val pickups = mutableListOf<Pickup>()
    private val enemies = mutableListOf<Enemy>()
    private val healthPickups = mutableListOf<HealthPickup>()

    fun addPickup(p: Pickup) { pickups += p }
    fun addEnemy(e: Enemy) { enemies += e }
    fun addHealthPickup(h: HealthPickup) { healthPickups += h }

    // Add clear method to reset all entities
    fun clear() {
        pickups.clear()
        enemies.clear()
        healthPickups.clear()
    }

    fun updateAll(dtMs: Long) {
        pickups.forEach { it.update(dtMs) }
        enemies.forEach { it.update(dtMs) }
        healthPickups.forEach { it.update(dtMs) }

        // gc
        pickups.removeAll { !it.alive }
        enemies.removeAll { !it.alive }
        healthPickups.removeAll { it.collected }
    }

    fun drawAll(canvas: Canvas) {
        // vẽ pickup trước, enemy sau (tuỳ ý)
        pickups.forEach { it.draw(canvas) }
        healthPickups.forEach { it.draw(canvas) }
        enemies.forEach { it.draw(canvas) }
    }

    fun getPickups(): List<Pickup> = pickups
    fun getEnemies(): List<Enemy> = enemies
    fun getHealthPickups(): List<HealthPickup> = healthPickups
}
