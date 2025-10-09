package com.example.game2d

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ScoresAdapter(private val scores: List<PlayerScore>) : RecyclerView.Adapter<ScoresAdapter.ScoreViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_score, parent, false)
        return ScoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val score = scores[position]
        holder.bind(score, position + 1)
    }

    override fun getItemCount(): Int = scores.size

    class ScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val rankText: TextView = itemView.findViewById(R.id.rankText)
        private val totalScoreText: TextView = itemView.findViewById(R.id.totalScoreText)
        private val coinsText: TextView = itemView.findViewById(R.id.coinsText)
        private val monstersText: TextView = itemView.findViewById(R.id.monstersText)
        private val dateText: TextView = itemView.findViewById(R.id.dateText)
        private val backgroundView: View = itemView.findViewById(R.id.scoreBackground)

        fun bind(score: PlayerScore, rank: Int) {
            // Set rank with special styling for top 3
            rankText.text = "#$rank"
            when (rank) {
                1 -> {
                    rankText.setTextColor(Color.parseColor("#8B4513")) // Dark brown for gold background
                    rankText.textSize = 28f
                    rankText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    backgroundView.setBackgroundResource(R.drawable.score_item_gold_background)

                    // Set all text colors for gold background
                    totalScoreText.setTextColor(Color.parseColor("#8B4513")) // Dark brown
                    coinsText.setTextColor(Color.parseColor("#B8860B")) // Dark goldenrod
                    monstersText.setTextColor(Color.parseColor("#8B0000")) // Dark red
                    dateText.setTextColor(Color.parseColor("#696969")) // Dim gray
                }
                2 -> {
                    rankText.setTextColor(Color.parseColor("#2F4F4F")) // Dark slate gray for silver
                    rankText.textSize = 26f
                    rankText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    backgroundView.setBackgroundResource(R.drawable.score_item_silver_background)

                    // Set all text colors for silver background
                    totalScoreText.setTextColor(Color.parseColor("#2F4F4F")) // Dark slate gray
                    coinsText.setTextColor(Color.parseColor("#DAA520")) // Goldenrod
                    monstersText.setTextColor(Color.parseColor("#DC143C")) // Crimson
                    dateText.setTextColor(Color.parseColor("#696969")) // Dim gray
                }
                3 -> {
                    rankText.setTextColor(Color.parseColor("#FFFFFF")) // White for bronze (good contrast)
                    rankText.textSize = 24f
                    rankText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    backgroundView.setBackgroundResource(R.drawable.score_item_bronze_background)

                    // Set all text colors for bronze background
                    totalScoreText.setTextColor(Color.parseColor("#FFFFFF")) // White
                    coinsText.setTextColor(Color.parseColor("#FFD700")) // Gold
                    monstersText.setTextColor(Color.parseColor("#FF6B6B")) // Light red
                    dateText.setTextColor(Color.parseColor("#E0E0E0")) // Light gray
                }
                else -> {
                    rankText.setTextColor(Color.WHITE)
                    rankText.textSize = 22f
                    rankText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    backgroundView.setBackgroundResource(R.drawable.score_item_background)

                    // Default colors for regular items
                    totalScoreText.setTextColor(Color.WHITE)
                    coinsText.setTextColor(Color.parseColor("#FFD700")) // Gold
                    monstersText.setTextColor(Color.parseColor("#FF6B6B")) // Light red
                    dateText.setTextColor(Color.WHITE)
                }
            }

            // Set score data
            totalScoreText.text = score.totalScore.toString()
            coinsText.text = "🪙 ${score.coins}"
            monstersText.text = "👹 ${score.monsters}"

            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateText.text = dateFormat.format(Date(score.timestamp))

            // Set total score size based on rank
            when (rank) {
                1 -> {
                    totalScoreText.textSize = 32f
                    totalScoreText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                2 -> {
                    totalScoreText.textSize = 30f
                    totalScoreText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                3 -> {
                    totalScoreText.textSize = 30f
                    totalScoreText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                else -> {
                    totalScoreText.textSize = 28f
                    totalScoreText.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
            }

            // Set date text transparency
            when (rank) {
                1, 2 -> dateText.alpha = 0.8f
                3 -> dateText.alpha = 0.9f
                else -> dateText.alpha = 0.6f
            }
        }
    }
}
