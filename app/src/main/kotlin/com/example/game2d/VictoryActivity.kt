package com.example.game2d

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class VictoryActivity : AppCompatActivity() {
    private lateinit var congratsImage: ImageView
    private lateinit var highScoresButton: Button
    private lateinit var playAgainButton: Button
    private lateinit var mainMenuButton: Button
    private lateinit var scoreText: TextView
    private lateinit var scoreManager: ScoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_victory)

        scoreManager = ScoreManager(this)

        congratsImage = findViewById(R.id.congratsImage)
        highScoresButton = findViewById(R.id.highScoresButton)
        playAgainButton = findViewById(R.id.playAgainButton)
        mainMenuButton = findViewById(R.id.mainMenuButton)
        scoreText = findViewById(R.id.scoreText)

        setupViews()
        setupButtons()
    }

    private fun setupViews() {
        // Load congratulations image from drawable-nodpi
        try {
            @Suppress("DiscouragedApi")
            val id = resources.getIdentifier("congratulations", "drawable", packageName)
            if (id != 0) {
                val bitmap = BitmapFactory.decodeResource(resources, id)
                congratsImage.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Get score data from intent
        val coins = intent.getIntExtra("COINS", 0)
        val monsters = intent.getIntExtra("MONSTERS", 0)
        val totalScore = coins + monsters * 10

        scoreText.text = "Final Score: $totalScore\nCoins: $coins | Monsters: $monsters"

        // Save the score
        scoreManager.saveScore(coins, monsters)
    }

    private fun setupButtons() {
        highScoresButton.setOnClickListener {
            val intent = Intent(this, HighScoresActivity::class.java)
            startActivity(intent)
        }

        playAgainButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        mainMenuButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }
}
