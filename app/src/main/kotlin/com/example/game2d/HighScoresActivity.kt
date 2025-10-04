package com.example.game2d

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HighScoresActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var backButton: Button
    private lateinit var titleText: TextView
    private lateinit var scoreManager: ScoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_scores)

        scoreManager = ScoreManager(this)

        titleText = findViewById(R.id.titleText)
        recyclerView = findViewById(R.id.scoresRecyclerView)
        backButton = findViewById(R.id.backButton)

        setupRecyclerView()
        setupBackButton()
    }

    private fun setupRecyclerView() {
        val scores = scoreManager.getTopScores()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ScoresAdapter(scores)
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            finish()
        }
    }
}

data class PlayerScore(
    val coins: Int,
    val monsters: Int,
    val totalScore: Int,
    val timestamp: Long
)

class ScoreManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("high_scores", Context.MODE_PRIVATE)

    companion object {
        private const val MAX_SCORES = 8
        private const val COINS_KEY = "coins_"
        private const val MONSTERS_KEY = "monsters_"
        private const val TIMESTAMP_KEY = "timestamp_"
    }

    fun saveScore(coins: Int, monsters: Int) {
        val totalScore = coins + monsters * 10 // Monsters worth 10 points each
        val scores = getTopScores().toMutableList()

        val newScore = PlayerScore(coins, monsters, totalScore, System.currentTimeMillis())
        scores.add(newScore)

        // Sort by total score descending
        scores.sortByDescending { it.totalScore }

        // Keep only top scores
        val topScores = scores.take(MAX_SCORES)

        // Save to preferences
        val editor = prefs.edit()
        editor.clear() // Clear existing scores

        topScores.forEachIndexed { index, score ->
            editor.putInt("$COINS_KEY$index", score.coins)
            editor.putInt("$MONSTERS_KEY$index", score.monsters)
            editor.putLong("$TIMESTAMP_KEY$index", score.timestamp)
        }

        editor.apply()
    }

    fun getTopScores(): List<PlayerScore> {
        val scores = mutableListOf<PlayerScore>()

        for (i in 0 until MAX_SCORES) {
            val coins = prefs.getInt("$COINS_KEY$i", -1)
            if (coins == -1) break // No more scores

            val monsters = prefs.getInt("$MONSTERS_KEY$i", 0)
            val timestamp = prefs.getLong("$TIMESTAMP_KEY$i", 0)
            val totalScore = coins + monsters * 10

            scores.add(PlayerScore(coins, monsters, totalScore, timestamp))
        }

        return scores
    }
}
