package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "high_scores")
data class HighScore(
    @PrimaryKey val id: Int = 1,
    val score: Int,
    val timestamp: Long = System.currentTimeMillis()
)
