package com.example.game2d

import android.content.Context
import android.media.MediaPlayer

object MusicManager {
    private var mp: MediaPlayer? = null
    @Volatile private var enabled = false

    /** Gọi 1 lần khi khởi tạo game. resId là file trong res/raw (vd: R.raw.bgm) */
    fun init(ctx: Context, resId: Int) {
        release()
        mp = MediaPlayer.create(ctx, resId)?.apply {
            isLooping = true
            setVolume(1f, 1f)
        }
    }

    fun setEnabled(on: Boolean) {
        enabled = on
        val m = mp ?: return
        if (on) {
            if (!m.isPlaying) m.start()
        } else {
            if (m.isPlaying) m.pause()
        }
    }

    fun toggle(): Boolean {
        setEnabled(!enabled)
        return enabled
    }

    fun isEnabled(): Boolean = enabled

    fun release() {
        mp?.release()
        mp = null
        enabled = false
    }
}
