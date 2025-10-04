package com.example.game2d

import android.content.Context
import android.media.MediaPlayer
import android.content.SharedPreferences

class SoundManager(private val context: Context) {
    private var menuMusic: MediaPlayer? = null
    private var backgroundMusic: MediaPlayer? = null
    private var coinSound: MediaPlayer? = null
    private var deathSound: MediaPlayer? = null
    private var gameOverSound: MediaPlayer? = null
    private var victorySound: MediaPlayer? = null
    private var checkpointSound: MediaPlayer? = null

    private val prefs: SharedPreferences = context.getSharedPreferences("sound_prefs", Context.MODE_PRIVATE)

    companion object {
        const val MUSIC_VOLUME_KEY = "music_volume"
        const val SFX_VOLUME_KEY = "sfx_volume"
        const val DEFAULT_VOLUME = 0.5f
    }

    var musicVolume: Float
        get() = prefs.getFloat(MUSIC_VOLUME_KEY, DEFAULT_VOLUME)
        set(value) {
            prefs.edit().putFloat(MUSIC_VOLUME_KEY, value.coerceIn(0f, 1f)).apply()
            updateMusicVolumes()
        }

    var sfxVolume: Float
        get() = prefs.getFloat(SFX_VOLUME_KEY, DEFAULT_VOLUME)
        set(value) {
            prefs.edit().putFloat(SFX_VOLUME_KEY, value.coerceIn(0f, 1f)).apply()
            updateSfxVolumes()
        }

    init {
        initializeSounds()
    }

    private fun initializeSounds() {
        try {
            // Menu music - loop
            menuMusic = MediaPlayer.create(context, R.raw.music_menu)?.apply {
                isLooping = true
                setVolume(musicVolume, musicVolume)
            }

            // Background music - loop
            backgroundMusic = MediaPlayer.create(context, R.raw.music_background)?.apply {
                isLooping = true
                setVolume(musicVolume, musicVolume)
            }

            // Sound effects
            coinSound = MediaPlayer.create(context, R.raw.music_coin)?.apply {
                setVolume(sfxVolume, sfxVolume)
            }

            deathSound = MediaPlayer.create(context, R.raw.music_death)?.apply {
                setVolume(sfxVolume, sfxVolume)
            }

            gameOverSound = MediaPlayer.create(context, R.raw.music_gameover)?.apply {
                setVolume(sfxVolume, sfxVolume)
            }

            victorySound = MediaPlayer.create(context, R.raw.congratulation)?.apply {
                setVolume(sfxVolume, sfxVolume)
            }

            checkpointSound = MediaPlayer.create(context, R.raw.congratulation)?.apply {
                setVolume(sfxVolume, sfxVolume)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateMusicVolumes() {
        menuMusic?.setVolume(musicVolume, musicVolume)
        backgroundMusic?.setVolume(musicVolume, musicVolume)
    }

    private fun updateSfxVolumes() {
        coinSound?.setVolume(sfxVolume, sfxVolume)
        deathSound?.setVolume(sfxVolume, sfxVolume)
        gameOverSound?.setVolume(sfxVolume, sfxVolume)
        victorySound?.setVolume(sfxVolume, sfxVolume)
        checkpointSound?.setVolume(sfxVolume, sfxVolume)
    }

    // Menu music
    fun playMenuMusic() {
        stopAllMusic()
        menuMusic?.let {
            if (!it.isPlaying) {
                it.start()
            }
        }
    }

    fun stopMenuMusic() {
        menuMusic?.let {
            if (it.isPlaying) {
                it.stop()
                it.prepare()
            }
        }
    }

    // Background music
    fun playBackgroundMusic() {
        stopAllMusic()
        backgroundMusic?.let {
            if (!it.isPlaying) {
                it.start()
            }
        }
    }

    fun stopBackgroundMusic() {
        backgroundMusic?.let {
            if (it.isPlaying) {
                it.stop()
                it.prepare()
            }
        }
    }

    fun pauseBackgroundMusic() {
        backgroundMusic?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    fun resumeBackgroundMusic() {
        backgroundMusic?.let {
            if (!it.isPlaying) {
                it.start()
            }
        }
    }

    // Sound effects
    fun playCoinSound() {
        coinSound?.let {
            if (it.isPlaying) {
                it.seekTo(0)
            } else {
                it.start()
            }
        }
    }

    fun playDeathSound() {
        deathSound?.let {
            if (it.isPlaying) {
                it.seekTo(0)
            } else {
                it.start()
            }
        }
    }

    fun playGameOverSound() {
        stopAllMusic()
        gameOverSound?.start()
    }

    fun playVictorySound() {
        stopAllMusic()
        victorySound?.start()
    }

    fun playCheckpointSound() {
        checkpointSound?.let {
            if (it.isPlaying) {
                it.seekTo(0)
            } else {
                it.start()
            }
        }
    }

    private fun stopAllMusic() {
        stopMenuMusic()
        stopBackgroundMusic()
    }

    fun pauseAll() {
        menuMusic?.pause()
        backgroundMusic?.pause()
    }

    fun resumeAll() {
        menuMusic?.let { if (!it.isPlaying) it.start() }
        backgroundMusic?.let { if (!it.isPlaying) it.start() }
    }

    fun release() {
        menuMusic?.release()
        backgroundMusic?.release()
        coinSound?.release()
        deathSound?.release()
        gameOverSound?.release()
        victorySound?.release()
        checkpointSound?.release()

        menuMusic = null
        backgroundMusic = null
        coinSound = null
        deathSound = null
        gameOverSound = null
        victorySound = null
        checkpointSound = null
    }
}
