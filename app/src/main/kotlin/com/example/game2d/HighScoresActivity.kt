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

    // Podium elements
    private lateinit var firstPlaceScore: TextView
    private lateinit var secondPlaceScore: TextView
    private lateinit var thirdPlaceScore: TextView
    private lateinit var podiumSection: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_high_scores)

        scoreManager = ScoreManager(this)

        initializeViews()
        setupRecyclerView()
        setupBackButton()
        setupPodium()
    }

    private fun initializeViews() {
        titleText = findViewById(R.id.titleText)
        recyclerView = findViewById(R.id.scoresRecyclerView)
        backButton = findViewById(R.id.backButton)
        podiumSection = findViewById(R.id.podiumSection)

        // Podium score displays
        firstPlaceScore = findViewById(R.id.firstPlaceScore)
        secondPlaceScore = findViewById(R.id.secondPlaceScore)
        thirdPlaceScore = findViewById(R.id.thirdPlaceScore)
    }

    private fun setupRecyclerView() {
        val scores = scoreManager.getTopScores()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = ScoresAdapter(scores)

        // Add some spacing between items
        val spacing = resources.getDimensionPixelSize(R.dimen.score_item_spacing)
        recyclerView.addItemDecoration(SpacingItemDecoration(spacing))
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupPodium() {
        val scores = scoreManager.getTopScores()

        if (scores.isEmpty()) {
            podiumSection.visibility = View.GONE
            return
        }

        // Display top 3 scores on podium
        when (scores.size) {
            0 -> {
                podiumSection.visibility = View.GONE
            }
            1 -> {
                firstPlaceScore.text = formatPodiumScore(scores[0])
                secondPlaceScore.text = "---"
                thirdPlaceScore.text = "---"
            }
            2 -> {
                firstPlaceScore.text = formatPodiumScore(scores[0])
                secondPlaceScore.text = formatPodiumScore(scores[1])
                thirdPlaceScore.text = "---"
            }
            else -> {
                firstPlaceScore.text = formatPodiumScore(scores[0])
                secondPlaceScore.text = formatPodiumScore(scores[1])
                thirdPlaceScore.text = formatPodiumScore(scores[2])
            }
        }
    }

    private fun formatPodiumScore(score: PlayerScore): String {
        return "${score.totalScore}\n${score.coins}c ${score.monsters}m"
    }
}

// Helper class for RecyclerView item spacing
class SpacingItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: android.graphics.Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.bottom = spacing
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
