package com.example.game2d

import android.content.Context
import android.media.MediaPlayer
import java.lang.IllegalStateException

object BackgroundMusic {
    private var player: MediaPlayer? = null
    @Volatile
    var musicEnabled: Boolean = false
        private set

    fun init(context: Context, resId: Int) {
        // init MediaPlayer
        if (player == null) {
            player = MediaPlayer.create(context, resId)
            player?.isLooping = true
        }
    }

    fun play() {
        try {
            if (player == null) return
            if (!player!!.isPlaying) player!!.start()
            musicEnabled = true
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun pause() {
        try {
            player?.let {
                if (it.isPlaying) it.pause()
            }
            musicEnabled = false
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    fun stopAndRelease() {
        try {
            player?.stop()
        } catch (e: Exception) { /* ignore */ }
        player?.release()
        player = null
        musicEnabled = false
    }

    fun toggle() {
        if (musicEnabled) pause() else play()
    }
}
