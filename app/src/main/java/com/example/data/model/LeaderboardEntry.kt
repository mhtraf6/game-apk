package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,
    val category: String,
    val level: String,
    val timestamp: Long = System.currentTimeMillis()
)
