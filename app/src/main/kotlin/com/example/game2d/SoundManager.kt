package com.example.game2d

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper

object SoundManager {
    private var soundPool: SoundPool? = null
    private var sGun = 0
    private var sWall = 0
    private var sWarn = 0
    private var loaded = false
    private val handler = Handler(Looper.getMainLooper())

    // <-- new flag to enable/disable short sound effects
    @Volatile
    var effectsEnabled: Boolean = true

    fun init(context: Context) {
        // init soundPool if null
        if (soundPool != null) return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attrs)
            .build()

        // load your short effect files from res/raw (ensure these files exist)
        // adjust resource names if different
        sGun = soundPool!!.load(context, R.raw.gun, 1)
        sWall = soundPool!!.load(context, R.raw.wall_hit, 1)
        sWarn = soundPool!!.load(context, R.raw.warning, 1)

        // listen for load complete
        soundPool!!.setOnLoadCompleteListener { _, _, _ ->
            loaded = true
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        loaded = false
    }

    // helper to decide whether to play
    private fun shouldPlayEffects(): Boolean = loaded && effectsEnabled

    fun playShot() {
        if (!shouldPlayEffects()) return
        soundPool?.play(sGun, 1f, 1f, 1, 0, 1f)
    }

    fun playWallHit() {
        if (!shouldPlayEffects()) return
        soundPool?.play(sWall, 1f, 1f, 1, 0, 1f)
    }

    fun playWarning(repeats: Int = 4, intervalMs: Long = 300L) {
        if (!shouldPlayEffects()) return
        var counter = 0
        fun playOnce() {
            if (counter >= repeats) return
            soundPool?.play(sWarn, 1f, 1f, 1, 0, 1f)
            counter++
            handler.postDelayed({ playOnce() }, intervalMs)
        }
        playOnce()
    }
}
