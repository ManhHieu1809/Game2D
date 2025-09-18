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

    /**
     * Call once from Activity/GameView with a real Context (e.g. GameActivity).
     * Requires files in res/raw: R.raw.gun, R.raw.wall_hit, R.raw.warning
     */
    fun init(ctx: Context) {
        if (soundPool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(6).setAudioAttributes(attrs).build()
        try {
            sGun = soundPool!!.load(ctx, R.raw.gun, 1)
            sWall = soundPool!!.load(ctx, R.raw.wall_hit, 1)
            sWarn = soundPool!!.load(ctx, R.raw.warning, 1)
            soundPool!!.setOnLoadCompleteListener { _, _, _ -> loaded = true }
        } catch (e: Exception) {
            // nếu thiếu resource sẽ không crash, chỉ không phát âm thanh
            loaded = false
        }
    }

    fun playShot() {
        if (!loaded) return
        soundPool?.play(sGun, 1f, 1f, 1, 0, 1f)
    }

    fun playWallHit() {
        if (!loaded) return
        soundPool?.play(sWall, 1f, 1f, 1, 0, 1f)
    }

    /**
     * Play warning sound `repeats` times with `intervalMs` ms between plays.
     */
    fun playWarning(repeats: Int = 4, intervalMs: Long = 300L) {
        if (!loaded) return
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
