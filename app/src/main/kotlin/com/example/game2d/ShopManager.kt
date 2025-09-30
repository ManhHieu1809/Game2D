package com.example.game2d

import android.content.Context
import android.content.SharedPreferences

class ShopManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("shop_prefs", Context.MODE_PRIVATE)

    // Default coins amount
    private val defaultCoins = 100

    companion object {
        // Potion prices
        const val HEALTH_POTION_PRICE = 30
        const val JUMP_POTION_PRICE = 25
        const val SPEED_POTION_PRICE = 20
        const val SHIELD_POTION_PRICE = 40
        const val MAGNET_POTION_PRICE = 35

        // Effect durations (in milliseconds)
        const val JUMP_EFFECT_DURATION = 30000L    // 30 seconds
        const val SPEED_EFFECT_DURATION = 25000L   // 25 seconds
        const val SHIELD_EFFECT_DURATION = 20000L  // 20 seconds
        const val MAGNET_EFFECT_DURATION = 15000L  // 15 seconds
    }

    init {
        // First character (index 0) is unlocked by default
        if (!prefs.contains("character_0")) {
            prefs.edit().putBoolean("character_0", true).apply()
        }

        // Set default coins if first time
        if (!prefs.contains("coins")) {
            prefs.edit().putInt("coins", defaultCoins).apply()
        }
    }

    // ===== COIN MANAGEMENT =====
    fun getCoins(): Int {
        return prefs.getInt("coins", defaultCoins)
    }

    fun addCoins(amount: Int) {
        val currentCoins = getCoins()
        prefs.edit().putInt("coins", currentCoins + amount).apply()
    }

    fun spendCoins(amount: Int): Boolean {
        val currentCoins = getCoins()
        if (currentCoins >= amount) {
            prefs.edit().putInt("coins", currentCoins - amount).apply()
            return true
        }
        return false
    }

    // ===== CHARACTER UNLOCKING =====
    fun isCharacterUnlocked(characterIndex: Int): Boolean {
        return prefs.getBoolean("character_$characterIndex", characterIndex == 0)
    }

    fun unlockCharacter(characterIndex: Int): Boolean {
        if (isCharacterUnlocked(characterIndex)) {
            return true // Already unlocked
        }

        // Get character price based on index
        val prices = listOf(0, 50, 100, 150) // Matching the prices in CharacterActivity
        if (characterIndex < prices.size) {
            val price = prices[characterIndex]
            if (spendCoins(price)) {
                prefs.edit().putBoolean("character_$characterIndex", true).apply()
                return true
            }
        }
        return false
    }

    // ===== POTION MANAGEMENT =====
    fun getHealthPotions(): Int = prefs.getInt("health_potions", 0)
    fun getJumpPotions(): Int = prefs.getInt("jump_potions", 0)
    fun getSpeedPotions(): Int = prefs.getInt("speed_potions", 0)
    fun getShieldPotions(): Int = prefs.getInt("shield_potions", 0)
    fun getMagnetPotions(): Int = prefs.getInt("magnet_potions", 0)

    fun buyHealthPotion(): Boolean {
        if (spendCoins(HEALTH_POTION_PRICE)) {
            val current = getHealthPotions()
            prefs.edit().putInt("health_potions", current + 1).apply()
            return true
        }
        return false
    }

    fun buyJumpPotion(): Boolean {
        if (spendCoins(JUMP_POTION_PRICE)) {
            val current = getJumpPotions()
            prefs.edit().putInt("jump_potions", current + 1).apply()
            return true
        }
        return false
    }

    fun buySpeedPotion(): Boolean {
        if (spendCoins(SPEED_POTION_PRICE)) {
            val current = getSpeedPotions()
            prefs.edit().putInt("speed_potions", current + 1).apply()
            return true
        }
        return false
    }

    fun buyShieldPotion(): Boolean {
        if (spendCoins(SHIELD_POTION_PRICE)) {
            val current = getShieldPotions()
            prefs.edit().putInt("shield_potions", current + 1).apply()
            return true
        }
        return false
    }

    fun buyMagnetPotion(): Boolean {
        if (spendCoins(MAGNET_POTION_PRICE)) {
            val current = getMagnetPotions()
            prefs.edit().putInt("magnet_potions", current + 1).apply()
            return true
        }
        return false
    }

    // Use potions
    fun useHealthPotion(): Boolean {
        val current = getHealthPotions()
        if (current > 0) {
            prefs.edit().putInt("health_potions", current - 1).apply()
            return true
        }
        return false
    }

    fun useJumpPotion(): Boolean {
        val current = getJumpPotions()
        if (current > 0) {
            prefs.edit().putInt("jump_potions", current - 1).apply()
            return true
        }
        return false
    }

    fun useSpeedPotion(): Boolean {
        val current = getSpeedPotions()
        if (current > 0) {
            prefs.edit().putInt("speed_potions", current - 1).apply()
            return true
        }
        return false
    }

    fun useShieldPotion(): Boolean {
        val current = getShieldPotions()
        if (current > 0) {
            prefs.edit().putInt("shield_potions", current - 1).apply()
            return true
        }
        return false
    }

    fun useMagnetPotion(): Boolean {
        val current = getMagnetPotions()
        if (current > 0) {
            prefs.edit().putInt("magnet_potions", current - 1).apply()
            return true
        }
        return false
    }

    fun unlockAllCharacters() {
        val editor = prefs.edit()
        for (i in 0..3) {
            editor.putBoolean("character_$i", true)
        }
        editor.apply()
    }

    // Reset shop data
    fun resetShop() {
        val editor = prefs.edit()
        editor.clear()
        editor.putBoolean("character_0", true) // Keep first character unlocked
        editor.putInt("coins", defaultCoins)
        editor.apply()
    }
}
