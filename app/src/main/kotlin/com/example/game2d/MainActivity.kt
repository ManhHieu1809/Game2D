package com.example.game2d

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {
    private lateinit var menuView: MenuView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // PHẢI dùng Activity context (this)
        menuView = MenuView(this)
        setContentView(menuView)
    }

    override fun onResume() {
        super.onResume()
        menuView.resume()
    }

    override fun onPause() {
        super.onPause()
        menuView.pause()
    }
}
