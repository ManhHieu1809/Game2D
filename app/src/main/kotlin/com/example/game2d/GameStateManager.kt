package com.example.game2d

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class GameStateManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)

    companion object {
        const val LIVES_KEY = "lives"
        const val CHECKPOINT_X_KEY = "checkpoint_x"
        const val CHECKPOINT_Y_KEY = "checkpoint_y"
        const val CHECKPOINT_MAP_KEY = "checkpoint_map"
        const val MAX_LIVES = 3
        const val DEFAULT_SPAWN_X = 200f
        const val DEFAULT_SPAWN_Y = 0f
        const val INVULNERABILITY_DURATION = 2000L // 2 seconds in milliseconds
    }

    // Invulnerability system
    private var invulnerabilityStartTime = 0L
    private var isInvulnerable = false

    // Shield system
    private var shieldEndTime = 0L
    private var shieldActive = false

    // Lives management
    fun getLives(): Int {
        val lives = prefs.getInt(LIVES_KEY, MAX_LIVES)
        return lives
    }

    fun setLives(lives: Int) {
        val targetLives = lives.coerceIn(0, MAX_LIVES)
        val editor = prefs.edit()
        editor.putInt(LIVES_KEY, targetLives)
        val success = editor.commit()
        // Verify the value was actually saved
        val verifyLives = prefs.getInt(LIVES_KEY, -1)
    }

    fun loseLife(): Int {
        val currentLives = getLives()
        val newLives = (currentLives - 1).coerceAtLeast(0)
        setLives(newLives)

        startInvulnerability()

        return newLives
    }

    fun resetLives() {
        setLives(MAX_LIVES)
    }

    fun isGameOver(): Boolean {
        return getLives() <= 0
    }

    // Initialize fresh game session
    fun initializeNewSession() {
        resetLives()
        clearCheckpoint()
        isInvulnerable = false
        shieldActive = false
        shieldEndTime = 0L
    }

    // Invulnerability system
    fun startInvulnerability() {
        isInvulnerable = true
        invulnerabilityStartTime = System.currentTimeMillis()
    }

    fun updateInvulnerability() {
        if (isInvulnerable) {
            val elapsed = System.currentTimeMillis() - invulnerabilityStartTime
            if (elapsed >= INVULNERABILITY_DURATION) {
                isInvulnerable = false
            }
        }

        // Update shield
        if (shieldActive) {
            if (System.currentTimeMillis() >= shieldEndTime) {
                shieldActive = false
            }
        }
    }

    fun isInvulnerable(): Boolean {
        updateInvulnerability()
        return isInvulnerable || shieldActive
    }

    fun getInvulnerabilityProgress(): Float {
        if (shieldActive) {
            val remaining = shieldEndTime - System.currentTimeMillis()
            return (remaining.toFloat() / ShopManager.SHIELD_EFFECT_DURATION.toFloat()).coerceIn(0f, 1f)
        }

        if (!isInvulnerable) return 0f
        val elapsed = System.currentTimeMillis() - invulnerabilityStartTime
        return 1f - (elapsed.toFloat() / INVULNERABILITY_DURATION.toFloat())
    }

    // Shield system methods
    fun applyShield(duration: Long) {
        shieldActive = true
        shieldEndTime = System.currentTimeMillis() + duration
    }

    fun hasShield(): Boolean = shieldActive

    fun getShieldRemainingTime(): Long {
        return if (shieldActive) {
            (shieldEndTime - System.currentTimeMillis()).coerceAtLeast(0L)
        } else 0L
    }

    // Checkpoint management
    fun setCheckpoint(x: Float, y: Float, mapId: Int) {
        prefs.edit {
            putFloat(CHECKPOINT_X_KEY, x)
            putFloat(CHECKPOINT_Y_KEY, y)
            putInt(CHECKPOINT_MAP_KEY, mapId)
        }
    }

    fun getCheckpoint(): Triple<Float, Float, Int> {
        // If no checkpoint has been set, return default spawn
        val hasCheckpoint = prefs.contains(CHECKPOINT_X_KEY)
        if (!hasCheckpoint) {
            return Triple(DEFAULT_SPAWN_X, DEFAULT_SPAWN_Y, 0)
        }

        val x = prefs.getFloat(CHECKPOINT_X_KEY, DEFAULT_SPAWN_X)
        val y = prefs.getFloat(CHECKPOINT_Y_KEY, DEFAULT_SPAWN_Y)
        val mapId = prefs.getInt(CHECKPOINT_MAP_KEY, 0)

        android.util.Log.d("GameStateManager", "getCheckpoint: x=$x, y=$y, mapId=$mapId")
        return Triple(x, y, mapId)
    }

    fun clearCheckpoint() {
        prefs.edit {
            remove(CHECKPOINT_X_KEY)
            remove(CHECKPOINT_Y_KEY)
            remove(CHECKPOINT_MAP_KEY)
        }
    }

    // Game over - reset to beginning
    fun handleGameOver() {
        resetLives()
        clearCheckpoint()
        isInvulnerable = false
        shieldActive = false
        shieldEndTime = 0L
    }

}
