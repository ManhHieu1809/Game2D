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

    // Trạng thái bật/tắt SFX để gắn với toggle Z/O (sound_on/off)
    @Volatile
    private var sfxEnabled = true

    /**
     * Gọi 1 lần duy nhất từ Activity/GameView (truyền Context thật, vd: GameActivity).
     * Cần các file trong res/raw: R.raw.gun, R.raw.wall_hit, R.raw.warning
     */
    fun init(ctx: Context) {
        if (soundPool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attrs)
            .build()

        try {
            sGun = soundPool!!.load(ctx, R.raw.gun, 1)
            sWall = soundPool!!.load(ctx, R.raw.wall_hit, 1)
            sWarn = soundPool!!.load(ctx, R.raw.warning, 1)
            soundPool!!.setOnLoadCompleteListener { _, _, _ -> loaded = true }
        } catch (e: Exception) {
            // Nếu thiếu resource sẽ không crash, chỉ không phát âm thanh
            loaded = false
        }
    }

    /** Bật/tắt toàn bộ SFX (âm thanh ngắn). Dùng cho nút Z/O. */
    fun setSfxEnabled(enabled: Boolean) {
        sfxEnabled = enabled
    }

    /** Đảo trạng thái SFX, trả về trạng thái mới. */
    fun toggleSfx(): Boolean {
        sfxEnabled = !sfxEnabled
        return sfxEnabled
    }

    /** Trả về trạng thái SFX hiện tại. */
    fun isSfxEnabled(): Boolean = sfxEnabled

    fun playShot() {
        if (!loaded || !sfxEnabled) return
        soundPool?.play(sGun, 1f, 1f, 1, 0, 1f)
    }

    fun playWallHit() {
        if (!loaded || !sfxEnabled) return
        soundPool?.play(sWall, 1f, 1f, 1, 0, 1f)
    }

    /**
     * Phát âm "warning" lặp lại [repeats] lần, mỗi lần cách nhau [intervalMs] mili-giây.
     */
    fun playWarning(repeats: Int = 4, intervalMs: Long = 300L) {
        if (!loaded || !sfxEnabled) return
        var counter = 0
        fun playOnce() {
            if (!sfxEnabled) return // nếu tắt giữa chừng thì dừng
            if (counter >= repeats) return
            soundPool?.play(sWarn, 1f, 1f, 1, 0, 1f)
            counter++
            handler.postDelayed({ playOnce() }, intervalMs)
        }
        playOnce()
    }

    /** Giải phóng tài nguyên khi thoát game. */
    fun release() {
        handler.removeCallbacksAndMessages(null)
        soundPool?.release()
        soundPool = null
        loaded = false
    }
}
