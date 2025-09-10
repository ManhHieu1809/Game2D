package com.example.game2d

import android.app.Activity
import android.content.pm.ActivityInfo
import android.os.Bundle

class GameActivity : Activity() {
    private lateinit var view: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // landscape for platformer
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        view = GameView(this)
        setContentView(view)
    }

    override fun onResume() {
        super.onResume()
        view.resume()
    }

    override fun onPause() {
        super.onPause()
        view.pause()
    }
}
