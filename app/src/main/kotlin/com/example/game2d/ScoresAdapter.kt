package com.example.game2d

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ScoresAdapter(private val scores: List<PlayerScore>) : RecyclerView.Adapter<ScoresAdapter.ScoreViewHolder>() {

    class ScoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankText: TextView = itemView.findViewById(R.id.rankText)
        val coinsText: TextView = itemView.findViewById(R.id.coinsText)
        val monstersText: TextView = itemView.findViewById(R.id.monstersText)
        val totalScoreText: TextView = itemView.findViewById(R.id.totalScoreText)
        val dateText: TextView = itemView.findViewById(R.id.dateText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_score, parent, false)
        return ScoreViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) {
        val score = scores[position]
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

        holder.rankText.text = "${position + 1}"
        holder.coinsText.text = "Coins: ${score.coins}"
        holder.monstersText.text = "Monsters: ${score.monsters}"
        holder.totalScoreText.text = "Score: ${score.totalScore}"
        holder.dateText.text = dateFormat.format(Date(score.timestamp))
    }

    override fun getItemCount(): Int = scores.size
}
